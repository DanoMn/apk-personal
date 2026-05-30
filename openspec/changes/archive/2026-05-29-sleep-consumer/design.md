# Design: sleep-consumer (Sueño como primer consumidor de `device-telemetry`)

> Diseño técnico de la fase SDD. Traduce el contrato conceptual
> (`docs/decisiones-diseno-sueno-v1.md`) y las fórmulas selladas
> (`docs/arbol-scoring-vocal-v1.md` §11) a una arquitectura ejecutable. **No
> reabre decisiones**: aterriza los detalles que el contrato dejó para esta fase
> (algoritmo de interpretación, modelo de datos durable, migración Room v11→v12,
> espectro de confianza, cierre híbrido, agregación semanal, wiring de modo
> automático).
>
> Verificado contra el código real en `main` (esquema Room v11, `SleepScoring.kt`
> de 2 componentes, `SpecialLayerScoringPolicy.kt`, `DeviceActivityEventType`,
> `TelemetryRepository`, `DeviceTelemetryWorkScheduler`, `DailyClosureWorker`).

---

## 0. Frontera de responsabilidad (no reabrir)

Cada caja es ciega a la siguiente. **El motor de scoring NUNCA toca telemetría
cruda; solo `domain/sleep` la lee.**

```
platform/telemetry      → captura HECHO crudo en Room          (ya existe; ciega a Sueño)
TelemetryRepository      → PULL read: eventsInRange(from,to)    (ya existe)
domain/sleep (PURO)      → interpreta eventos → segmentos → 4 sub-scores → sleepScore 0..1?
data/sleep (Room)        → persiste cabecera de noche + segmentos (HECHO PRIMARIO durable)
domain/scoring           → recibe sleepScore ya digerido → Cuerpo → ScoreReport
```

La capa nueva `domain/sleep` se divide en dos sub-responsabilidades, ambas puras
y test-first:

1. **Interpretación** (`SleepInterpreter`): `List<DeviceActivityEvent>` → `NightTimeline`.
2. **Scoring** (`SleepScoring` refactor): `NightTimeline` + objetivo → 4 sub-scores → `SleepNightScore`.

El **wiring Android** (lectura del repo, register/unregister, cierre) vive fuera de
`domain/sleep` (en `data/`/repositorio/worker), e inyecta a las funciones puras los
datos ya extraídos. Strict TDD: las dos cajas puras se testean primero con eventos
sintéticos; el wiring se prueba con instalación limpia en device.

---

## 1. Pipeline de interpretación (`domain/sleep`, puro)

### 1.1 Entradas y salida

```kotlin
// domain/sleep/interpretation/SleepInterpreter.kt — PURO, sin tipos Android
object SleepInterpreter {
    fun interpret(
        events: List<DeviceActivityEvent>, // ya filtrados a la ventana de detección
        params: InterpretationParams = InterpretationParams.DEFAULT,
    ): NightTimeline
}

data class NightTimeline(
    val nightDate: LocalDate,           // día del despertar (§3 del contrato)
    val segments: List<SleepSegment>,   // línea de tiempo completa, ordenada por startAt
    val sleepOnsetAt: Instant?,         // inicio del sueño principal
    val definitiveWakeAt: Instant?,     // despertar definitivo detectado (o tope de ventana)
    val confidence: SleepConfidence,    // High | Ambiguous | NoData
)

data class SleepSegment(
    val startAt: Instant,
    val endAt: Instant,
    val kind: SleepSegmentKind,         // Asleep | AwakeUse
)

enum class SleepSegmentKind { Asleep, AwakeUse }
enum class SleepConfidence { High, Ambiguous, NoData }
```

> `DeviceActivityEvent` ya es un modelo puro (sin tipos Android, verificado en
> `DeviceActivityEventType.kt:20`), así que `domain/sleep` puede importarlo sin
> romper la pureza JVM. Si se prefiere desacoplar de `platform`, se mapea a un
> `RawActivityEvent` local en la frontera; el diseño acepta importarlo directo (es
> un value object inmutable, ya es del lenguaje del dominio de telemetría).

### 1.2 Ventana de detección (fija) vs ventana objetivo (configurable)

Dos relojes, no mezclar (contrato §3):

- **DETECCIÓN biológica = `20:00`–`12:00` del día siguiente.** Quien arma la lista
  de eventos (el wiring, §5) llama `eventsInRange(from, to)` con
  `from = 20:00 del día D-1` y `to = 12:00 del día D` (epoch millis, zona local).
  Esta ventana alimenta Duración, Continuidad e InterrupciónDigital.
- **OBJETIVO configurable** (`targetSleepAt`/`targetWakeAt`, mín. 5h): NO toca la
  interpretación de segmentos; entra **solo** en `ScheduleAlignmentScore` (§2.3).

El intérprete NO conoce el objetivo para segmentar — lo recibe el scorer. La única
excepción es el **anclaje al objetivo** para descartar siestas (§1.5), donde el
intérprete recibe el objetivo solo para elegir cuál bloque es "la noche".

### 1.3 Episodio de uso real (la primitiva — edge case #1)

Gatillo **cualitativo** (contrato §4.1): un despertar real es **uso real**, no un
vistazo.

- **Eventos de uso real** (cuentan): `USER_INTERACTION`, `APP_FOREGROUND`.
- **Eventos de vistazo** (NO disparan por sí solos): `SCREEN_ON`, `UNLOCK`.
  Encender la pantalla para mirar la hora a las 3am no rompe el sueño.
- **Eventos de quietud** (cierran un episodio): `SCREEN_OFF`, `LOCK`,
  `APP_BACKGROUND`, o simplemente ausencia de eventos.

Algoritmo de agrupación de un `AwakeUse`:

1. Recorrer los eventos ordenados por `timestamp`.
2. Un `AwakeUse` **abre** con el primer evento de uso real.
3. Mientras lleguen eventos (de cualquier tipo) separados por menos que
   `quietGapMillis` del anterior, el episodio sigue abierto.
4. El `AwakeUse` **cierra** cuando transcurre `quietGapMillis` sin ningún evento
   (el teléfono se quedó quieto) → `endAt` = timestamp del último evento del episodio.
5. Un `SCREEN_ON`/`UNLOCK` aislado (sin uso real dentro de `quietGapMillis`) **no
   abre** un `AwakeUse` → se ignora como vistazo.

> `quietGapMillis` = gap de quietud que separa dos despertares. Default calibrable
> propuesto: **15 min** (la calibración fina es deuda D1/futuro). Dos usos reales
> separados por más de 15 min de silencio = dos despertares distintos; separados por
> menos = un mismo despertar continuado.

### 1.4 Inicio del sueño y construcción de la línea de tiempo

- **`sleepOnsetAt`** = el instante en que el teléfono **se queda quieto tras el
  último uso real previo al período de sueño principal** (contrato §4.1). NO es un
  botón. Esto disuelve el bug histórico "el detox se cuenta como sueño": el detox es
  uso real, así que el onset arranca DESPUÉS de él.
- Los huecos de quietud entre `AwakeUse` se materializan como segmentos `Asleep`.
- **`definitiveWakeAt`** = inicio del **último** `AwakeUse` sostenido que ocurre
  tras la hora objetivo de despertar y no es seguido por otro bloque `Asleep`
  significativo (§1.6). Tope de seguridad: fin de la ventana biológica (`12:00`).

Resultado: una línea de tiempo alternante `Asleep`/`AwakeUse` que cubre desde
`sleepOnsetAt` hasta `definitiveWakeAt`, sin huecos.

### 1.5 Anclaje al objetivo (mitigación de siesta — contrato §3)

Dentro de la ventana de detección puede haber varios bloques de sueño (siesta de
20:30 + noche real). "La noche" = el **período de sueño principal que solapa o está
cerca del horario objetivo**.

Algoritmo:

1. Agrupar los segmentos `Asleep` en **bloques de sueño** (un bloque = `Asleep`
   contiguos separados por `AwakeUse` cortos, < `napSeparationMillis`).
2. Para cada bloque, medir solapamiento/cercanía con la ventana objetivo.
3. El bloque ganador = el de mayor duración entre los que solapan o están a menos de
   `napAnchorWindowMinutes` del objetivo. Si ninguno solapa, gana el bloque más largo.
4. Bloques aislados lejos del objetivo (siesta de 20:30 con objetivo 00:00) → NO son
   la noche; se excluyen de Duración/Continuidad.

Defaults calibrables: `napSeparationMillis` ≈ 90 min, `napAnchorWindowMinutes` ≈ 120 min.

### 1.6 Detección del despertar definitivo (cierre — contrato §7)

- Candidato = `AwakeUse` que **(a)** empieza tras `targetWakeAt`, **(b)** es
  sostenido (duración ≥ `definitiveWakeMinMinutes`, default ≈ 10 min), y **(c)** no
  hay un `Asleep` significativo (≥ `returnToSleepMinMinutes`, ≈ 30 min) después.
- Si no aparece candidato antes de `12:00`, `definitiveWakeAt` = `12:00` (tope) y la
  confianza baja a `Ambiguous` (la noche quedó abierta).

### 1.7 Espectro de confianza (contrato §4.2)

| Situación de señal | `confidence` | Efecto en scoring |
|---|---|---|
| Teléfono quieto con onset/wake limpios; bloque principal claro | `High` | sleepScore normal |
| Sin eventos en toda la ventana, o solo ruido sin bloque de sueño identificable | `NoData` | `sleepScore = null` (no se computa) |
| Señal contradictoria: sin onset claro, despertar no detectado (cae al tope), bloques solapados ambiguos, o solo señal gruesa de proxy en API 26/27 sin SCREEN_*/UNLOCK | `Ambiguous` | sleepScore se computa pero **se atenúa** (§2.5) |

> **Regla de oro:** "poca señal" ≠ "baja confianza". Una noche perfecta genera poca
> señal (teléfono quieto) → eso es `High`, NO `NoData` ni `Ambiguous`. `NoData` es
> *ausencia total* de base para inferir una noche, no quietud.

**Señal gruesa API 26/27** (contrato/handoff §1): si faltan `SCREEN_*`/`UNLOCK` y solo
hay `APP_FOREGROUND`/`USER_INTERACTION`, el intérprete funciona igual (esos dos son los
gatillos de uso real). La pérdida de los eventos de quietud explícitos (`SCREEN_OFF`/
`LOCK`) se compensa con el `quietGapMillis` (ausencia de eventos = quietud). El nivel
`Ambiguous` absorbe la incertidumbre cuando la señal es demasiado escasa para anclar onset.

### 1.8 Parámetros calibrables (centralizados)

```kotlin
data class InterpretationParams(
    val quietGapMillis: Long,            // ~15 min — separa despertares
    val napSeparationMillis: Long,       // ~90 min — agrupa bloques de sueño
    val napAnchorWindowMinutes: Int,     // ~120   — cercanía al objetivo
    val definitiveWakeMinMinutes: Int,   // ~10    — uso sostenido = despertar
    val returnToSleepMinMinutes: Int,    // ~30    — vuelta a dormir
) {
    companion object { val DEFAULT = InterpretationParams(/* ... */) }
}
```

Todos son **defaults calibrables**; la calibración fina con datos reales es deuda
D1/futuro. Vivir en un único data class permite recalcular agregados desde los
segmentos durables al recalibrar (contrato §6).

---

## 2. Cómputo de los 4 componentes (pesos SELLADOS)

`SleepScoring` se refactoriza para recibir la `NightTimeline` (no el `SleepLog`
manual de hoy) + el objetivo. Fórmula sellada (`arbol-scoring-vocal-v1.md` §11.2):

```
SleepWeeklyScore = 0.40·DurationScore + 0.25·ContinuityScore
                 + 0.20·ScheduleAlignmentScore + 0.15·DigitalInterruptionScore
```

```kotlin
object SleepScoring {
    fun scoreNight(timeline: NightTimeline, target: SleepTargetWindow): SleepNightScore?
    // null cuando timeline.confidence == NoData
}

data class SleepNightScore(
    val duration: Float, val continuity: Float,
    val alignment: Float, val digitalInterruption: Float,
    val sleepScore: Float,        // combinación sellada, 0..1
    val confidence: SleepConfidence,
)
```

### 2.1 DurationScore (0.40) — SIN superávit (decisión §5, bug §10)

- `asleepMinutes` = suma de la duración de los segmentos `Asleep` del bloque principal.
- `targetMinutes` = minutos de la ventana objetivo (mín. 5h).
- `DurationScore = clamp(asleepMinutes / targetMinutes, 0, 1)`.
- **Dormir de más = NEUTRO (1.0, NO decae).** Se corrige el bug actual de
  `SleepScoring.kt:16` (`coerceIn(0.50f, 1f)` con decay). El "vaso" se corta en 1.0.
  El superávit como bonus de margen es deuda D2.

### 2.2 ContinuityScore (0.25)

Lee fragmentación. Dos señales (contrato §4.1, §6):

- `awakeCount` = número de `AwakeUse` dentro del bloque principal.
- `longestAsleepRatio` = (bloque `Asleep` más largo) / (total `Asleep`).

```
fragmentationPenalty = exp(-awakeCount / k)   // k ≈ 2, curva decreciente
ContinuityScore = clamp(0.5·fragmentationPenalty + 0.5·longestAsleepRatio, 0, 1)
```

> Default calibrable `k ≈ 2` (mismo patrón que `TaskMomentumRaw`/`RelapseProtection`
> del árbol). Pondera 50/50 entre "cuántas veces se despertó" y "qué tan compacto fue
> el sueño". 0 despertares + bloque único → 1.0.

### 2.3 ScheduleAlignmentScore (0.20) — ÚNICO que usa la ventana objetivo

Reusa `SleepPolicy.scheduleCloseness` (ya existe, `SleepPolicy.kt:72`), que mide
cercanía circular en minutos con tolerancia de 120 min:

```
ScheduleAlignmentScore = average(
    closeness(sleepOnsetAt, targetSleepAt),
    closeness(definitiveWakeAt, targetWakeAt),
)
```

Dormir fuera del objetivo baja SOLO este 20% (contrato §3); las horas reales no se
pierden porque Duración las captura desde la ventana de detección.

### 2.4 DigitalInterruptionScore (0.15)

Suma de la duración de los `AwakeUse` (uso nocturno real). Penalización por curva:

```
awakeUseMinutes = Σ duración(AwakeUse)
DigitalInterruptionScore = exp(-awakeUseMinutes / m)   // m ≈ 30, calibrable
```

0 min de uso → 1.0; cuanto más uso nocturno, más baja, saturando suave. **D3:**
`digitalWindDownMinutes` queda **inerte a propósito** — NO entra acá; este componente
se calcula solo por uso real *durante el sueño detectado*, no por la config de detox.
Documentar el inert en `ScoreInputSource`/`BuildScoreInputUseCase`.

### 2.5 Atenuación por confianza Ambiguous

Cuando `confidence == Ambiguous`, el `sleepScore` combinado se multiplica por un
factor `ambiguousConfidenceFactor` (≈ 0.85, calibrable) ANTES del clamp. `NoData`
no llega acá (devuelve `null`). `High` no se atenúa. Esto materializa "señal
genuinamente ambigua → ahí sí baja el puntaje" (contrato §4.2).

---

## 3. Modelo de datos + migración Room v11→v12

### 3.1 Cabecera de noche (evolución de `SleepLogEntity`)

`SleepLogEntity` hoy (`Entities.kt:255`) es un par único `sleptAt`/`wokeAt` con
`quality` hardcodeado. Evoluciona a **cabecera de la noche** (PK = fecha de despertar):

```kotlin
@Entity(tableName = "sleep_nights")
data class SleepNightEntity(
    @PrimaryKey val nightDate: String,     // fecha del despertar (ISO yyyy-MM-dd)
    val targetSleepAt: String,             // objetivo configurado al cerrar la noche
    val targetWakeAt: String,
    val sleepOnsetAt: Long?,               // epoch millis; null si NoData
    val definitiveWakeAt: Long?,
    val confidenceLevel: String,           // SleepConfidence.name
    val durationScore: Float?,             // sub-scores cacheados (recalculables)
    val continuityScore: Float?,
    val alignmentScore: Float?,
    val digitalInterruptionScore: Float?,
    val sleepScore: Float?,                // null cuando NoData
    val note: String = "",
    val source: String,                    // "auto" (telemetría) | "manual"
    val updatedAt: Long,
)
```

Cambios vs hoy: se **elimina `quality`** (bug §10), se cambia PK conceptual a
`nightDate` (= fecha de despertar), se agregan onset/wake/confianza/sub-scores. Los
sub-scores se cachean pero son **derivables** desde los segmentos (verdad primaria).

> Nombre de tabla: pasa de `sleep_logs` a `sleep_nights`. La migración crea la tabla
> nueva; ver §3.4 sobre datos legacy.

### 3.2 Nueva `SleepSegmentEntity` (hecho primario durable)

```kotlin
@Entity(
    tableName = "sleep_segments",
    foreignKeys = [ForeignKey(
        entity = SleepNightEntity::class,
        parentColumns = ["nightDate"],
        childColumns = ["nightDate"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("nightDate")],   // genera index_sleep_segments_nightDate
)
data class SleepSegmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nightDate: String,            // FK → sleep_nights.nightDate
    val startAt: Long,                // epoch millis
    val endAt: Long,
    val kind: String,                 // SleepSegmentKind.name (Asleep | AwakeUse)
)
```

**Por qué durable y no cache** (contrato §6): la telemetría cruda se **purga en
días** (`DeviceTelemetryDrainWorker` ya borra eventos viejos, verificado
`deleteDeviceActivityEventsOlderThan`). Los segmentos son la materialización
permanente de la noche → permiten recalcular los 4 componentes al recalibrar
`InterpretationParams`/pesos, sin la telemetría cruda original.

### 3.3 DAO

```kotlin
// SleepNight
@Query("SELECT * FROM sleep_nights WHERE nightDate = :date")
suspend fun getSleepNight(date: String): SleepNightEntity?
@Query("SELECT * FROM sleep_nights WHERE nightDate BETWEEN :from AND :to ORDER BY nightDate")
suspend fun getSleepNightsInRange(from: String, to: String): List<SleepNightEntity>
@Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertSleepNight(night: SleepNightEntity)
@Query("...") fun observeSleepNightForDate(date: String): Flow<SleepNightEntity?>

// SleepSegment
@Query("SELECT * FROM sleep_segments WHERE nightDate = :date ORDER BY startAt")
suspend fun getSleepSegments(date: String): List<SleepSegmentEntity>
@Query("DELETE FROM sleep_segments WHERE nightDate = :date")
suspend fun deleteSleepSegmentsForNight(date: String)
@Insert suspend fun insertSleepSegments(segments: List<SleepSegmentEntity>)
```

Para `getSleepNightsInRange` el orden lexicográfico de `yyyy-MM-dd` coincide con el
cronológico (mismo patrón que el resto del esquema).

### 3.4 Migración v11→v12 (disciplina estricta D8)

```kotlin
private val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Cabecera de noche nueva
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS sleep_nights (
                nightDate TEXT NOT NULL PRIMARY KEY,
                targetSleepAt TEXT NOT NULL,
                targetWakeAt TEXT NOT NULL,
                sleepOnsetAt INTEGER,
                definitiveWakeAt INTEGER,
                confidenceLevel TEXT NOT NULL,
                durationScore REAL,
                continuityScore REAL,
                alignmentScore REAL,
                digitalInterruptionScore REAL,
                sleepScore REAL,
                note TEXT NOT NULL DEFAULT '',
                source TEXT NOT NULL,
                updatedAt INTEGER NOT NULL
            )
        """.trimIndent())
        // 2. Segmentos (hijo, FK CASCADE)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS sleep_segments (
                id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                nightDate TEXT NOT NULL,
                startAt INTEGER NOT NULL,
                endAt INTEGER NOT NULL,
                kind TEXT NOT NULL,
                FOREIGN KEY(nightDate) REFERENCES sleep_nights(nightDate) ON DELETE CASCADE
            )
        """.trimIndent())
        // Índice OBLIGATORIO con naming Room: index_<tabla>_<col> (NO idx_*)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_sleep_segments_nightDate ON sleep_segments(nightDate)")
        // 3. Tabla legacy sleep_logs: DROP (datos dev descartables; sin backfill)
        db.execSQL("DROP TABLE IF EXISTS sleep_logs")
    }
}
```

**Disciplina (la regla más quemada del proyecto):**

- Índices con naming Room `index_<tabla>_<col>` — **NUNCA `idx_*`**. La FK
  `Index("nightDate")` en la entidad genera `index_sleep_segments_nightDate`; la SQL
  debe coincidir EXACTO o el `MigrationTestHelper` falla con "schema mismatch".
- `exportSchema = true` en `@Database` (hoy está en `false`, `AutonomiaDatabase.kt:35`)
  + `room.schemaLocation` en `build.gradle`, para generar el JSON v12 y que
    `MigrationTestHelper` valide contra él.
- Test `SleepMigration11To12Test` con `MigrationTestHelper`: crea v11, corre la
  migración, valida que el esquema resultante coincide con las entidades v12.

**Estrategia para datos dev (decisión):** la DB local es **descartable** (AGENTS.md
#29, CLAUDE.md). NO se hace backfill de `sleep_logs` → `sleep_nights` (el par único
manual no se mapea limpio a segmentos, y no hay datos reales que preservar). La
migración **DROP**ea `sleep_logs`. Para probar el esquema: **instalación limpia**
(`adb uninstall` + `adb install`). El seed canónico no se toca. La migración igual
queda **correcta y testeada** con `MigrationTestHelper` para el eventual release
(los `gradlew test` de dominio NO ejercen migraciones reales).

> **Decisión de registro:** romper con el patrón actual de NO registrar las
> `MIGRATION_*` (que existe por el bug histórico de índices `idx_*` + DEFAULTs
> espurios, ver `AutonomiaDatabase.kt:56-65`). Para `MIGRATION_11_12` se escribe
> **correcta desde el día uno** y se decide: **registrarla** vía `addMigrations()`
> junto con `MIGRATION_10_11` (que ya está escrita correctamente, línea 342), y
> **mantener** `fallbackToDestructiveMigration` como red de seguridad durante dev.
> Esto paga parte de la deuda D8 sin esperar al release. Alternativa rechazada:
> dejarla sin registrar como las demás — rechazada porque queremos cobertura de
> migración real (`MigrationTestHelper`) verde ahora, no acumular más deuda.

### 3.5 `Models.kt` — modelos de dominio

`SleepLog` (`Models.kt:85`) se reemplaza por `SleepNight` (cabecera) + lista de
`SleepSegment`. `SleepConfig`/`SleepSessionState` se mantienen (la config del
objetivo y el botón opcional "voy a dormir" siguen vivos en modo manual). `quality`
y `SleepQuality` salen del modelo de scoring (pueden quedar para UI legacy si algo
los referencia; el scoring ya no los lee). Los mappers en `DomainMappers.kt:206`
se actualizan a `SleepNightEntity.toDomain()`.

---

## 4. NoData en el pipeline (corregir el hundimiento de Cuerpo)

El bug actual (contrato §10, `SpecialLayerScoringPolicy.kt:13`): `sleepScore ?: 0f`
convierte ausencia en un 0 que hunde Cuerpo al 30%.

**Distinguir "ausente/NoData" de "0":**

- `SleepScoring.scoreNight` devuelve `null` cuando `confidence == NoData`.
- El pipeline propaga ese `null` SIN coaccionar a 0.
- En `SpecialLayerScoringPolicy.baseScore`/`rawScore`: cuando `sleepScore == null`
  (NoData), Cuerpo se calcula **sin el término de sueño y re-normalizando los pesos**:

```kotlin
layerId == BODY_LAYER_ID -> {
    if (sleepScore == null) {
        baseWithoutSpecial          // Cuerpo = solo BodyBaseWithoutSleep (sin fabricar 0)
    } else {
        (1f - SLEEP_WEIGHT_IN_BODY) * baseWithoutSpecial + SLEEP_WEIGHT_IN_BODY * sleepScore
    }
}
```

> Ausencia de sueño = **base incompleta**, NO piso fabricado. Cuando hay dato, el
> sueño entra al 30% como manda el árbol; cuando no hay, Cuerpo no se castiga con un
> 0 inexistente. Esto NO altera ningún umbral sellado — solo corrige el origen del
> `SleepWeeklyScore` que el motor ya consume.

**"Poca señal" ≠ "baja confianza":** el techo de no-habilitar-estados-altos sin dato
suficiente se materializa en la agregación semanal (§5), no fabricando un piso bajo.

---

## 5. Agregación semanal (de una noche → promedio de noches con dato)

Hoy `WeeklyScoringContextBuilder.kt:32` mira **un solo** `sleepLog`:
`input.sleepLog?.let(SleepScoring::score)`. Se reemplaza por el **promedio de las
noches CON dato** de la semana (contrato §8):

```kotlin
// ScoreInput pasa de sleepLog: SleepLog? a sleepNights: List<SleepNightScore>
val scoredNights = input.sleepNights.filter { it != null /* NoData ya es null */ }
sleepScore = if (scoredNights.isEmpty()) null
             else scoredNights.map { it.sleepScore }.average().toFloat()
```

- Cada noche → su `SleepNightScore` (ya computado al cerrar, cacheado en la cabecera).
- La semana = **promedio aritmético de las noches con dato** (cobertura suave).
- Una noche `NoData` NO entra como cero (no aporta al promedio).
- **Pocas noches con dato → lectura débil (techo, no piso):** sueño es core. Esto se
  logra propagando `null` cuando no hay ninguna noche con dato (→ §4 deja Cuerpo sin
  el término de sueño), y dejando el promedio sin inflar artificialmente. El piso de
  cobertura DURO (mín. N noches) es deuda D1 — v1 usa cobertura suave.

`ScoreInput`/`BuildScoreInputUseCase` cambian para traer las noches de la semana
(`getSleepNightsInRange(weekStart, today)`) en vez del único `sleepLog`. El
`hasAnyFact` (`WeeklyScoringContextBuilder.kt:56`) y `ScoreSnapshotHashPolicy.kt:48`
se actualizan al nuevo shape. `WeeklyScoreSnapshotWriter.kt:36` deja de leer
`getSleepLogForDate` y pasa a leer el rango de noches.

---

## 6. Cierre de noche híbrido (contrato §7)

Materializa la noche (cabecera + segmentos) **antes** de que la telemetría se purgue.
Sin maquinaria nueva: reusa el cierre diario existente.

### 6.1 Dónde vive

Nuevo método en `AutonomiaRepository`, paralelo a `closeElapsedActivityDays`:

```kotlin
suspend fun materializeSleepNight(nightDate: LocalDate, zoneId: ZoneId): Boolean
```

Pasos:
1. Calcular la ventana de detección biológica: `from = nightDate-1 @ 20:00`,
   `to = nightDate @ 12:00` (epoch millis en `zoneId`).
2. `telemetryRepository.eventsInRange(from, to)` → eventos crudos.
3. `SleepInterpreter.interpret(events, targetWindow)` → `NightTimeline`.
4. `SleepScoring.scoreNight(timeline, target)` → `SleepNightScore?`.
5. Persistir: `upsertSleepNight(...)` + `deleteSleepSegmentsForNight` +
   `insertSleepSegments(...)` (idempotente: recalcular sobrescribe).
6. Convivencia A/B: si `source == "manual"` ya existe una noche con marca manual del
   usuario para esa fecha, NO la pisa el automático (manual gana cuando el usuario
   marcó explícitamente; ver §6.3).

### 6.2 Disparo

- **`DailyClosureWorker`** (medianoche local, WorkManager): tras
  `closeElapsedActivityDays`, llama `materializeSleepNight(today)` para la noche que
  acaba de cerrar. Es el camino principal — corre antes de la próxima purga de drain.
- **Garantía al abrir la app**: el mismo path que ya garantiza `closeElapsedActivityDays`
  al abrir, materializa las noches pendientes (idempotente). Cubre el caso de que el
  worker no haya corrido.
- **Tope de seguridad**: si nunca se detecta el despertar definitivo, `definitiveWakeAt`
  cae al fin de la ventana (`12:00`) y la noche se cierra igual con `Ambiguous` (§1.6).

### 6.3 Relación con el cierre diario y la purga

- La purga de telemetría (`DeviceTelemetryDrainWorker`,
  `deleteDeviceActivityEventsOlderThan`) corre cada 3h y borra eventos viejos. El
  cierre de noche debe materializar **antes** de que se borre la ventana
  `20:00`–`12:00`. La medianoche + garantía-al-abrir dan margen sobrado (la ventana
  se cierra a las 12:00; el cierre dispara a medianoche del día siguiente y al abrir).
- Modo manual (B) sigue vivo: `startSleepSession`/`finishSleepSession` pueden escribir
  una `SleepNightEntity` con `source = "manual"` (un único segmento `Asleep` del par
  `sleptAt`/`wokeAt`). El cierre automático respeta la marca manual de esa fecha.

---

## 7. Wiring de modo automático + UX de permiso (contrato §2, handoff §1)

El toggle del modo automático de Sueño es un consumidor de telemetría con clave `"sleep"`:

```kotlin
// Al activar el modo automático de Sueño:
DeviceTelemetryWorkScheduler.register(context, "sleep")     // suspend
// Al desactivar:
DeviceTelemetryWorkScheduler.unregister(context, "sleep")   // suspend
```

`register`/`unregister` ya manejan el conteo de leases y el schedule/cancel del drain
(verificado `DeviceTelemetryWorkScheduler.kt:25-42`). Sueño solo aporta su lease;
no conoce el mecanismo de gating.

**UX de permiso:**

```kotlin
when (telemetryRepository.permissionState()) {
    GRANTED -> register("sleep")  // activar normal
    MISSING -> // UX compasiva (tono AGENTS.md): explicar para qué sirve,
               // ofrecer TelemetryPermission.settingsIntent() para conceder.
               // NO activar el modo hasta que el permiso esté concedido.
}
```

El estado del toggle se persiste (junto a `SleepConfig` o como preferencia). Modo
manual/metódico (B) convive: el usuario puede tener el modo automático apagado y
seguir usando "voy a dormir"/"Desperté".

---

## 8. ADRs (decisiones de arquitectura de esta fase)

### ADR-1: La interpretación importa `DeviceActivityEvent` directo (no se re-mapea)
- **Decisión:** `domain/sleep` importa `platform.telemetry.DeviceActivityEvent` tal cual.
- **Rationale:** ya es un value object puro sin tipos Android; re-mapearlo a un modelo
  local de `domain/sleep` sería ceremonia sin beneficio. La pureza JVM se mantiene.
- **Rechazado:** crear `RawActivityEvent` en `domain/sleep` — descartado por
  duplicación 1:1 sin ganancia. Si en el futuro telemetría cambia su shape, se
  introduce el mapper en la frontera (un solo punto).

### ADR-2: Segmentos como hecho primario durable (no cache)
- **Decisión:** `SleepSegmentEntity` se persiste como verdad primaria; los 4 sub-scores
  se cachean en la cabecera pero son recalculables desde segmentos.
- **Rationale:** la telemetría cruda se purga en días; sin segmentos no se podría
  recalcular al recalibrar `InterpretationParams`/pesos. Los agregados no se des-agregan.
- **Rechazado:** guardar solo los sub-scores agregados — perdería la capacidad de
  recalibrar; rechazado por el contrato §6.

### ADR-3: NoData propaga `null` (no 0) y re-normaliza Cuerpo
- **Decisión:** ausencia de dato de sueño → `sleepScore = null` → Cuerpo se computa sin
  el término de sueño (re-normalizado), no con un 0 fabricado.
- **Rationale:** un 0 fabricado hunde Cuerpo y viola "poca señal ≠ baja confianza"
  (contrato §4.2). El techo de no-habilitar-estados-altos vive en la cobertura semanal,
  no en un piso bajo.
- **Rechazado:** mantener `sleepScore ?: 0f` — es el bug §10; rechazado.

### ADR-4: Migración 11→12 SE REGISTRA (rompe el patrón de "no registrar")
- **Decisión:** escribir `MIGRATION_11_12` correcta, registrarla con `addMigrations()`
  junto a `MIGRATION_10_11`, activar `exportSchema = true`, agregar `MigrationTestHelper`.
- **Rationale:** paga deuda D8 ahora; queremos cobertura de migración real verde, no
  acumular el bug histórico de `idx_*`. `fallbackToDestructiveMigration` queda como red.
- **Rechazado:** dejarla sin registrar como las demás `MIGRATION_*` — rechazado por no
  querer más deuda de migración no testeada.

### ADR-5: Sin backfill de `sleep_logs` legacy → DROP
- **Decisión:** la migración DROPea `sleep_logs`; no se mapean los pares manuales a
  segmentos.
- **Rationale:** DB de dev descartable (AGENTS.md #29); el par único no mapea limpio a
  la línea de tiempo de segmentos; no hay datos reales que preservar. Probar = install limpio.
- **Rechazado:** backfill heurístico (un `Asleep` del par) — ceremonia sin valor en dev,
  riesgo de datos sintéticos sucios en el histórico.

### ADR-6: Umbrales de interpretación = defaults calibrables centralizados
- **Decisión:** `InterpretationParams` con todos los umbrales (`quietGapMillis` ~15min,
  etc.) en un único data class con `DEFAULT`.
- **Rationale:** la calibración fina es deuda D1/futuro; centralizarlos permite ajustar
  y recalcular agregados desde segmentos sin tocar la lógica.
- **Rechazado:** constantes dispersas inline — dificultan recalibrar y testear.

---

## 9. Componentes y archivos afectados (mapa)

| Componente | Tipo | Archivo |
|---|---|---|
| `SleepInterpreter` (eventos → timeline) | New | `domain/sleep/interpretation/SleepInterpreter.kt` |
| `NightTimeline`, `SleepSegment`, `SleepConfidence`, `InterpretationParams` | New | `domain/sleep/interpretation/*.kt` |
| `SleepScoring` (refactor 2→4 componentes, sin decay) | Modified | `domain/sleep/SleepScoring.kt` |
| `SleepNightScore`, `SleepTargetWindow` | New | `domain/sleep/*.kt` |
| `SleepPolicy` (ventana detección fija + objetivo; reusar `scheduleCloseness`) | Modified | `domain/sleep/SleepPolicy.kt` |
| `SleepNight`/`SleepSegment` modelos; `SleepLog`/`quality` fuera del scoring | Modified | `Models.kt:85` |
| `SleepNightEntity` (evol. de `SleepLogEntity`) + `SleepSegmentEntity` + DAO | New/Modified | `data/Entities.kt:254`, `data/AutonomiaDao.kt` |
| Migración `MIGRATION_11_12` + registro + `exportSchema=true` + `MigrationTestHelper` | Modified | `data/AutonomiaDatabase.kt` |
| Mappers `SleepNightEntity.toDomain()` | Modified | `data/local/mapper/DomainMappers.kt:206` |
| `materializeSleepNight` + convivencia manual | Modified | `AutonomiaRepository.kt:417-471` |
| Disparo de cierre de noche | Modified | `data/worker/DailyClosureWorker.kt`, garantía al abrir |
| NoData → Cuerpo sin término (no 0) | Modified | `domain/scoring/SpecialLayerScoringPolicy.kt:13,33` |
| `digitalWindDownMinutes` inerte documentado (D3) | Modified | `ScoreInputSource`/`BuildScoreInputUseCase` |
| Una noche → promedio semanal de noches con dato | Modified | `domain/scoring/WeeklyScoringContextBuilder.kt:32`, `ScoreInput` |
| Hash/snapshot al nuevo shape | Modified | `domain/scoring/ScoreSnapshotHashPolicy.kt:48`, `data/scoring/WeeklyScoreSnapshotWriter.kt:36` |
| Toggle modo auto + `register`/`unregister("sleep")` + UX permiso | New/Modified | UI Sueño + repositorio |
| Tests interpretación + 4 componentes + agregación + migración | New | `domain/sleep/*Test.kt`, `data/.../SleepMigration11To12Test.kt` |

---

## 10. Riesgos arquitectónicos y supuestos a validar

| Riesgo / Supuesto | Mitigación |
|---|---|
| `quietGapMillis`/umbrales mal calibrados (falsos despertares) | Centralizados en `InterpretationParams`; segmentos durables permiten recalcular; D1 difiere el piso de cobertura duro |
| Señal gruesa API 26/27 sin `SCREEN_*`/`UNLOCK` | El intérprete usa solo `USER_INTERACTION`/`APP_FOREGROUND` como gatillo; quietud = ausencia de eventos; `Ambiguous` absorbe |
| Migración v11→v12 pasa tests de dominio y crashea en device | `MigrationTestHelper` + `exportSchema=true` + índices `index_*` exactos; instalación limpia en dev |
| Telemetría purgada antes del cierre | Cierre a medianoche + garantía al abrir, ambos antes de la próxima ventana de purga |
| Re-normalizar Cuerpo en NoData podría inflar el score si BodyBaseWithoutSleep es alto | Test explícito: NoData NO sube Cuerpo por encima de lo que dan las anclas/soportes; solo quita el término de sueño |
| Importar `DeviceActivityEvent` en `domain/sleep` acopla a `platform` | Aceptado (ADR-1); es value object puro; mapper en frontera si telemetría cambia |
| Tramo en silencio asumido `Asleep` (leer un libro) | Aceptado para v1 (contrato §6, fuera de scope) |
| `exportSchema=true` puede romper el build si falta `room.schemaLocation` | Configurar `room.schemaLocation` en `build.gradle` como parte de la tarea de migración |

---

## 11. Qué NO se hace (out of scope — deuda diferida a propósito)

D1 (piso de cobertura duro), D2 (superávit de sueño/bonus de margen), D3
(`digitalWindDownMinutes` en scoring — queda inerte), D4 (término de consistencia
explícito), cambios en pesos/fórmulas selladas, distinción "dormido" vs "despierto
sin tocar el teléfono", y la UI final pulida de Sueño. Todos documentados en el
contrato §9 y la propuesta.
