# Design técnico — `scoring-arranque-cuenta`

> Barra de arranque de cuenta: contador `0 → score real proyectado` durante los primeros
> 7 días, reemplazando el blackout `NoData` provocado por la gracia de anclas
> (`#858` root cause 3). Este documento define el **CÓMO** (contrato cerrado). El **QUÉ**
> ya está fijado en el proposal `sdd/scoring-arranque-cuenta/proposal` — no se rediscute.

Fuente de verdad matemática: `docs/scoring/modelo-matematico-nucleo-v1.md` (NIVEL 1) y
`docs/scoring/modelo-scoring-oficial-v1.md`. Arquitectura: hechos → dominio → estado →
Compose. **Compose NO calcula.** Scoring = dominio puro JVM (no Room, no Compose).

---

## 0. Principios de diseño (los dos "por qué" estructurales)

### 0.1 ¿Por qué un canal de presentación aparte y NO `ScoreState.Arranque`?

El arranque **NO es un estado del modelo de scoring**. Es una **decisión de presentación**
sobre cómo mostrar el período en que el motor todavía no tiene veredicto real.

Agregar `ScoreState.Arranque` rompería el contrato del enum y obligaría a tocar **5 `when`
exhaustivos** (verificados file:line):

- `DashboardProjection.kt:347` (`scoreTitle`), `:357` (`scoreHeadline`), `:367` (`scoreBody`).
- `StatusCard.kt:179` (`statusColor`).
- `ScoringVisuals.kt:45` (`scoreStateColor`).
- `BandPolicy.band` — y **band JAMÁS debe emitir Arranque**: el gate vive en `ScoreEngine`,
  no en la banda pura. Meter Arranque ahí contaminaría una policy pura con una preocupación
  de presentación.

Además ensuciaría la **semántica del dominio**: el motor seguiría sin tener datos reales
(la cuenta está en gracia), así que el `ScoreReport` real **sigue siendo `NoData`** —
correcto y verdadero. El arranque es una **capa de presentación que se superpone** cuando
el dominio dice `NoData` PERO la causa es "cuenta nueva en gracia" (no "config insuficiente").

**Decisión:** canal separado `DashboardState.startup: StartupCardState?` (nullable). El motor
nunca se entera del arranque. `ScoreState` queda intacto (6 ramas). Cuando `startup != null`,
el dashboard renderiza `StartupStatusCard` en lugar de `StatusCard`; el `StatusCard` real
**no se toca**.

**Alternativas consideradas:**

| Alternativa | Por qué NO |
|-------------|------------|
| `ScoreState.Arranque` (rama nueva en el enum) | Rompe 5 `when` exhaustivos, contamina `BandPolicy` pura, miente sobre el estado real del dominio (`NoData`). |
| Reusar `NoData` + flag booleano en `DashboardStatusState` | El `StatusStatusCard` tendría que branchear lógica de arranque adentro → Composable con lógica de negocio. Viola container/presentational. |
| **Canal `DashboardState.startup` + card hermano** ✅ | El dominio computa un value object cerrado; el Composable solo elige cuál renderizar y anima. Cero contaminación del enum/motor. |

### 0.2 ¿Por qué generalizar el motor (windowDays) y NO una policy paralela?

La frecuencia `f` gobierna **4 términos** de `rFromRatios`, y el literal `7` está hardcodeado
en **2 de ellos** (verificado en `AnchorScoringPolicy.kt`):

- L60 `phi = commit.sumOf{u} / f` — usa `f`, no `7`.
- L67 `sd = if (f < 7) v / (7 - f) else 0.0` — **el `7` literal** (superhábit por días).
- L68 `wt = (f / 7.0).pow(kappa)` — **el `7` literal** (peso tiempo vs días).
- L53 `cut = min(d, f)` — usa `f`.

Una **fórmula paralela** duplicaría el contrato matemático del NIVEL 1 y se desincronizaría
del original en cuanto cambie cualquier constante. Y **escalar `f` por afuera** (ej. pasar
`f * d/7`) distorsionaría `phi`, `cut`, `sd` y `wt` de formas inconsistentes — el superhábit
quedaría roto.

La forma **coherente y prolija** es generalizar el `7` semanal a un parámetro `windowDays`
DENTRO de la policy, con `DEFAULT = 7` → comportamiento maduro **byte-idéntico**.

**Alternativas consideradas:**

| Alternativa | Por qué NO |
|-------------|------------|
| `StartupAnchorScoringPolicy` paralela | Duplica el contrato NIVEL 1; se desincroniza; dos fuentes de verdad matemática. |
| Escalar `f` afuera (`f_scaled = f*d/7`) | Distorsiona `phi/cut/sd/wt` de forma inconsistente; rompe superhábit; `f` deja de ser "frecuencia meta". |
| **`rFromRatios(..., windowDays=7)`** ✅ | Una sola fuente de verdad. Default 7 = idéntico a hoy. `7→windowDays` solo en `sd` y `wt`. |

**Importante — dos piezas matemáticas DISTINTAS y no redundantes:**

1. `windowDays = d` en el motor = **JUSTICIA**. Produce el score proyectado correcto para una
   ventana de `d` días; no castiga días que todavía no llegaron. Esto es el "score real
   proyectado".
2. `× d/7` en `StartupCounterPolicy` (aparte) = **BARRA DE CARGA**. Atenúa el score proyectado
   para que el contador suba: día 1 ≈ `score × 1/7` … día 7 = `score × 7/7 = score`.

En el día 7, ambas convergen al score real maduro → **sin salto día 7→8**.

---

## 1. Núcleo: generalizar `rFromRatios` con `windowDays`

### 1.1 Firma

```kotlin
// AnchorScoringPolicy.kt
fun rFromRatios(
    f: Int,
    dayRatios: List<Double>,
    windowDays: Int = 7,   // NUEVO — default 7 = comportamiento maduro byte-idéntico
): Double
```

### 1.2 Las 2 líneas que cambian (de las 4 que tocan `f`/`7`)

Solo cambian las **2 líneas con `7` literal** (`sd` y `wt`). `phi` (L60) y `cut` (L53) **NO
se tocan**: usan `f`, que sigue siendo la frecuencia meta real del ancla.

`f` **NO** pasa a un `f_eff`. La frecuencia meta del ancla no cambia por estar en arranque —
el usuario sigue comprometiéndose a `f` días/semana. Lo que cambia es la **longitud de la
ventana de juicio** (`windowDays`), no la meta.

**Antes (L66-68):**

```kotlin
val st = commit.sumOf { max(it - 1.0, 0.0) } / f.toDouble()
val sd = if (f < 7) v / (7 - f).toDouble() else 0.0
val wt = (f.toDouble() / 7.0).pow(kappa)
```

**Después:**

```kotlin
val st = commit.sumOf { max(it - 1.0, 0.0) } / f.toDouble()   // L66 SIN CAMBIO
val sd = if (f < windowDays) v / (windowDays - f).toDouble() else 0.0   // 7 → windowDays
val wt = (f.toDouble() / windowDays.toDouble()).pow(kappa)             // 7 → windowDays
```

### 1.3 Cómo se evita `N - f ≤ 0` (división por cero / negativa)

El guard `if (f < windowDays)` YA protege `sd`: si `f >= windowDays`, `sd = 0.0` (rama else),
nunca se evalúa `windowDays - f`. Esto es **exactamente** el mismo guard que el código maduro
(`if (f < 7)`), solo que parametrizado.

**Caso límite del arranque:** día 1 → `windowDays = 1`. Si `f = 3` (3 días/semana), entonces
`f >= windowDays` (3 ≥ 1) → `sd = 0.0`. Correcto: con 1 día de ventana **no hay superhábit por
días** posible. `wt = (3/1)^κ` podría ser > 1 — por eso `windowDays` se **clampa a `[1, 7]`**
en el caller (`StartupProjectionUseCase`, §3.3), pero además el invariante natural del arranque
es `windowDays ≤ 7` y crecientemente `f` puede superarlo en días tempranos, lo cual el guard
`else → 0.0` ya cubre sin romperse.

> Nota matemática: `wt = (f/windowDays)^κ` se usa como peso de mezcla `wt*st + (1-wt)*sd`. Con
> `sd = 0` (porque `f ≥ windowDays`), el término `(1-wt)*sd = 0` y solo queda `wt*st`. Aunque
> `wt > 1`, el superhábit `s = smax*(1 - exp(-(wt*st)/s0))` sigue acotado por `smax` y crece
> monótonamente. NO produce NaN ni valores fuera de `[0, 1.5]`. La spec cierra la validación
> numérica fina de los 4 términos.

### 1.4 Equivalencia `windowDays = 7` ≡ hoy

Con `windowDays = 7` (default), `sd = if (f < 7) v/(7-f) else 0.0` y `wt = (f/7)^κ` son
**textualmente** las expresiones actuales. Cero cambio de comportamiento. Test obligatorio
(lote 1): suite existente verde sin tocar + test explícito `rFromRatios(f, ratios) ==
rFromRatios(f, ratios, windowDays = 7)`.

### 1.5 ¿Quién pasa `windowDays` y de dónde sale?

- **Camino maduro (hoy):** `ScoreEngine.calculate` → `rFromRatios(window.f, ratios)` (L78).
  **NO se toca.** Usa el default `windowDays = 7`. El motor maduro queda byte-idéntico.
- **Camino arranque (nuevo):** `StartupProjectionUseCase` corre el motor con `windowDays = d`.
  Como `ScoreEngine.calculate` **no recibe** `windowDays` (y no debe, para no ensuciar la firma
  del orquestador maduro), el use case de arranque **no reusa `ScoreEngine.calculate`** — ver
  decisión en §3.3.

`d` = días desde el `createdAt` más viejo de las anclas configuradas, clampeado a `[1, 7]`.
Fuente: `ActivityDefinition.createdAt: Long` (verificado, `ActivityDefinition.kt:34`). Cálculo
vía `AnchorGraceRule` / `ChronoUnit.DAYS.between` (mismo criterio que la gracia).

**Decisión sobre la firma del orquestador:** `ScoreEngine.calculate(input)` NO gana parámetro
`windowDays`. Mantener el orquestador maduro intocado es prioridad arquitectónica. El arranque
es un cliente aparte (§3.3). Alternativa rechazada: `calculate(input, windowDays=7)` —
ensuciaría la firma del orquestador con una preocupación que solo aplica al arranque y obligaría
a `WeeklyScoreSnapshotWriter` y todos los callers a conocer un parámetro irrelevante para ellos.

---

## 2. `StartupDetectionRule` (object puro)

Ubicación: `domain/scoring/StartupDetectionRule.kt`. Dominio puro JVM.

### 2.1 Firma

```kotlin
object StartupDetectionRule {
    /**
     * `true` si la cuenta está en ARRANQUE: nunca tuvo un score real (todo su historial
     * semanal es NoData o vacío) Y tiene la cobertura mínima de anclas configurada —
     * aunque esas anclas estén dentro de su gracia y por eso el motor diga NoData hoy.
     *
     * Cuando es `true`, el blackout NoData se reemplaza por la barra de arranque.
     * Cuando es `false` (config insuficiente: < MIN capas con ancla), el NoData real manda.
     */
    fun isStartup(
        report: ScoreReport,              // veredicto real del motor (debe ser NoData)
        activities: List<ActivityDefinition>,
        layers: List<Layer>,
        weeklyHistory: List<WeeklyScoreHistoryEntry>,
        today: LocalDate,
    ): Boolean
}
```

### 2.2 Lógica

1. **Solo aplica sobre NoData real.** Si `report.state != ScoreState.NoData` → `false`
   (la cuenta ya tiene score real; no es arranque).
2. **Sin historial de score real:** `weeklyHistory.none { it.state != ScoreState.NoData }`.
   Una sola semana con score real ⇒ la cuenta ya "arrancó" alguna vez ⇒ NO es arranque
   (su NoData de hoy tiene otra causa, no la gracia inicial). `weeklyHistory` YA fluye al
   dominio (`ScoreInput.weeklyHistory`, verificado `ScoreModels.kt:32`; nace de
   `repository.weeklyScoreHistoryFlow()`, `DashboardViewModel.kt:132`).
3. **Cobertura mínima configurada** (el GATE manda): contar capas activas con ≥1 ancla
   configurada (activa, no archivada) **sin** filtrar gracia, y exigir
   `≥ ScoringConstants.MIN_ACTIVE_LAYERS_WITH_ANCHOR` (= 3). Si `< 3` → `false`: es NoData
   real ("configurá tu base"), NO arranque. Esta es la misma cuenta que
   `ScoreEngine.activeLayersWithAnchor` (`ScoreEngine.kt:182-188`) pero ignorando el filtro de
   gracia (que es justo lo que el arranque quiere ver).

> El predicado de gracia (anclas con `createdAt < 7d`) NO se chequea aquí explícitamente: si
> hubiera ≥3 capas con ancla y ninguna en gracia, el motor NO habría dado NoData (paso 1 ya
> filtró). El arranque emerge naturalmente de "NoData real + sin historial + cobertura ok".

### 2.3 Dónde se invoca

En `buildDashboardState` (`DashboardProjection.kt`), justo después de calcular `scoreReport`
(L101-121) y antes de armar `DashboardState` (L151). Ver §5.

---

## 3. `StartupProjectionUseCase`

Ubicación: `domain/scoring/StartupProjectionUseCase.kt`. Dominio (use case, no es object puro
porque orquesta — pero sigue siendo JVM puro, sin Room/IO).

### 3.1 Firma

```kotlin
object StartupProjectionUseCase {
    /**
     * Corre el motor de scoring SIN filtrar las anclas en gracia y con `windowDays = d`
     * (días vividos), para obtener el SCORE REAL PROYECTADO de la cuenta nueva.
     * No persiste nada. No toca el ScoreReport real (que sigue NoData).
     *
     * @return el ESTADO ∈ [0, 1.5] proyectado y el `d` usado, o null si no se pudo proyectar.
     */
    operator fun invoke(
        source: ScoreInputSource,
        windowDays: Int,           // d ∈ [1,7], lo provee StartupCounterPolicy/projection
    ): StartupProjection?          // data class: estado: Double, windowDays: Int
}
```

### 3.2 Cómo corre el motor sin filtrar gracia

El bloqueo está en `BuildScoreInputUseCase.kt:16-19` (`filterNot { Anchor && isWithinGrace }`).
Hay dos formas de saltarlo:

| Opción | Mecanismo | Veredicto |
|--------|-----------|-----------|
| A. Flag en `BuildScoreInputUseCase` | Agregar `skipGrace: Boolean = false` al invoke; arranque pasa `true` | Toca el use case maduro pero de forma aditiva y neutra (default mantiene comportamiento) |
| B. `ScoreInput` alterno armado por el arranque | El use case de arranque construye su propio `ScoreInput` sin el `filterNot` | Duplica la lógica de mapeo de `BuildScoreInputUseCase` |

**Decisión: Opción A.** `BuildScoreInputUseCase.invoke` gana un parámetro
`includeGraceAnchors: Boolean = false` (nombre que dice la intención). El camino maduro
**no cambia** (default `false` = filtra gracia como hoy). El arranque llama con `true`. Razón:
B duplicaría todo el mapeo `ScoreInputSource → ScoreInput` (12 campos) y se desincronizaría.
A es aditivo, un solo punto de verdad, y el default preserva el contrato maduro byte-idéntico.

```kotlin
// BuildScoreInputUseCase.kt — cambio aditivo
operator fun invoke(
    source: ScoreInputSource,
    includeGraceAnchors: Boolean = false,
): ScoreInput =
    ScoreInput(
        ...
        activities = source.activities
            .filter { it.active && !it.archived && it.activityType != ActivitySurface.Task }
            .filterNot {
                !includeGraceAnchors &&                        // arranque: NO filtra gracia
                it.activityType == ActivitySurface.Anchor &&
                    AnchorGraceRule.isWithinGrace(it.createdAt, source.today)
            }
            .sortedBy { it.sortOrder },
        ...
    )
```

### 3.3 ¿Reusa `ScoreEngine.calculate` o arma camino alterno?

**Problema:** `ScoreEngine.calculate(input)` no recibe `windowDays` (decisión §1.5: no ensuciar
la firma del orquestador maduro). Pero el arranque NECESITA `windowDays = d` para que
`rFromRatios` proyecte con justicia.

**Decisión:** `ScoreEngine` expone un seam interno **mínimo** para el arranque, SIN cambiar la
firma pública `calculate(input)`. Concretamente: `calculate` delega su pipeline a una función
privada parametrizada por `windowDays`, y `calculate(input)` la invoca con `windowDays = 7`.
El arranque accede vía un punto de entrada dedicado:

```kotlin
// ScoreEngine.kt
fun calculate(input: ScoreInput): ScoreReport =
    calculateInternal(input, windowDays = 7)          // maduro, byte-idéntico

// Punto de entrada del arranque (mismo pipeline, ventana parcial):
internal fun calculateProjection(input: ScoreInput, windowDays: Int): ScoreReport =
    calculateInternal(input, windowDays)

private fun calculateInternal(input: ScoreInput, windowDays: Int): ScoreReport {
    ...
    // única diferencia: al resolver cada ancla
    AnchorScoringPolicy.rFromRatios(window.f, ratios, windowDays)
    // (el ramo r(...) sin ratios — legacy sin versiones — usa el default 7;
    //  en arranque temprano lo normal es dayRatios != null porque hay versiones,
    //  pero si fuese null el legacy mantiene 7: aceptable, la atenuación d/7 lo corrige)
    ...
}
```

> Esta es la **única** intrusión en `ScoreEngine`: una extracción de método interna
> (refactor neutro) + un punto de entrada `internal`. La firma pública `calculate(input)` y su
> comportamiento quedan intactos. El gate NoData, los opt-ins, la agregación: todo idéntico,
> solo se propaga `windowDays` al resolver anclas.
>
> **Alternativa rechazada:** que `StartupProjectionUseCase` reimplemente el pipeline del motor.
> Eso duplicaría NIVELES 1-6 (gate, opt-ins, agregación, banda) → dos motores → desincronización
> garantizada. Inaceptable para la prolijidad del núcleo.

**Flujo del use case:**

1. `input = BuildScoreInputUseCase(source, includeGraceAnchors = true)` (anclas en gracia
   entran).
2. `report = ScoreEngine.calculateProjection(input, windowDays = d)`.
3. Si `report.state == NoData` (no alcanzó el gate ni con gracia) → `return null` (no hay
   proyección posible; el arranque no aplica). Si tiene estado real → devolver
   `StartupProjection(estado = report.estado.toDouble(), windowDays = d)`.

### 3.4 Cómo obtiene `d`

`d = ` días desde el `createdAt` **más viejo** de las anclas configuradas (las que importan para
el arranque), clampeado a `[1, 7]`:

```kotlin
// dentro de la projection (DashboardProjection) o helper en StartupCounterPolicy:
val oldestCreatedAt = anchorActivities.minOfOrNull { it.createdAt }   // Long epoch millis
val daysLived = oldestCreatedAt?.let {
    ChronoUnit.DAYS.between(Instant.ofEpochMilli(it).atZone(zone).toLocalDate(), today) + 1
}?.coerceIn(1L, 7L)?.toInt() ?: 1
```

`+ 1` porque el día de creación cuenta como día 1 (no día 0). Clamp `[1,7]`: día 8 ya sale de
gracia → motor maduro toma la posta. Este cálculo es coherente con `AnchorGraceRule.GRACE_DAYS=7`.

---

## 4. `StartupCounterPolicy` (object puro)

Ubicación: `domain/scoring/StartupCounterPolicy.kt`. Dominio puro JVM.

### 4.1 Firma

```kotlin
object StartupCounterPolicy {
    /**
     * Atenúa el ESTADO proyectado por la fracción de ventana vivida (× d/7) para producir el
     * CONTADOR de la barra de arranque, y calcula los días restantes hasta el score real.
     *
     * @param projectedEstado ESTADO ∈ [0,1.5] real proyectado (StartupProjectionUseCase).
     * @param daysLived d ∈ [1,7].
     * @return contador (puntos visibles atenuados), progreso d/7, días restantes.
     */
    fun counter(projectedEstado: Double, daysLived: Int): StartupCounter
}

data class StartupCounter(
    val counterPoints: Int,      // PointsMappingPolicy.points(projectedEstado * d/7)
    val daysLived: Int,          // d
    val daysRemaining: Int,      // GRACE_DAYS - d  (0 en día 7)
    val windowProgress: Float,   // d / 7f  → para el arco
)
```

### 4.2 Lógica

```kotlin
val attenuated = projectedEstado * (daysLived.toDouble() / AnchorGraceRule.GRACE_DAYS.toDouble())
val counterPoints = PointsMappingPolicy.points(attenuated)        // ESTADO→[650,1100]
val daysRemaining = (AnchorGraceRule.GRACE_DAYS.toInt() - daysLived).coerceAtLeast(0)
val windowProgress = (daysLived.toFloat() / AnchorGraceRule.GRACE_DAYS.toFloat()).coerceIn(0f, 1f)
```

- `× d/7` = la **barra de carga**. Día 1 → `estado × 1/7`; día 7 → `estado × 7/7 = estado`.
- `daysRemaining` vía `AnchorGraceRule.GRACE_DAYS` (= 7, fuente única; verificado
  `AnchorGraceRule.kt:15`). Día 7 → `0` ("ya está, mañana tu puntaje real").
- **Convergencia día 7→8 (sin salto):** en día 7, `windowDays=7` (proyección sin atenuar de
  ventana) `× 7/7` = score real maduro que el motor dará en día 8 (cuando sale de gracia). Test
  explícito obligatorio (lote 2).

> `PointsMappingPolicy.points` se reusa tal cual (`PointsMappingPolicy.kt:53-59`,
> ESTADO`[0,1.5]`→PUNTOS`[650,1100]`). El tramo `0–650` "muerto" en el modelo maduro es,
> justamente, la zona visual que la barra de arranque recorre conceptualmente — pero como el
> mapeo arranca en 650, el contador sube dentro de `[650, score]`. Esto es coherente: la barra
> no muestra "0 puntos", muestra el número proyectado atenuado, que crece hacia el real.

---

## 5. `StartupCardState` (value object de presentación) y dónde se computa

Ubicación: `domain/dashboard/DashboardState.kt` (junto a los demás `Dashboard*State`).

### 5.1 Data class

```kotlin
internal data class StartupCardState(
    val counterLabel: String,        // counterPoints.toString() — el número central
    val counterPoints: Int,          // para animateIntAsState
    val windowProgress: Float,       // d/7 — para animateFloatAsState (arco)
    val daysRemaining: Int,
    val daysRemainingLabel: String,  // "Faltan N días para tu puntaje real" (1 día → singular)
    val headline: String,            // copy cálido de arranque (tono AGENTS.md)
    val body: String,
)
```

> El número (`counterPoints`/`counterLabel`) lo calcula el DOMINIO. Compose solo lo anima.
> El copy (`daysRemainingLabel`, `headline`, `body`) se arma en el dominio respetando el tono
> (`AGENTS.md`: "La base está cargando", "Faltan N días para tu puntaje real" — sin "fallaste",
> sin tono clínico).

### 5.2 Entra a `DashboardState`

```kotlin
internal data class DashboardState(
    ...
    val scoreReport: DashboardScoreReportState = DashboardScoreReportState(),
    val startup: StartupCardState? = null,   // NUEVO — null = no arranque (render normal)
)
```

`nullable` por diseño: `null` = comportamiento de hoy (StatusCard real). Esto da el rollback
gratis: si nunca se setea, el dashboard renderiza como siempre.

### 5.3 Dónde lo computa la proyección

En `buildDashboardState` (`DashboardProjection.kt`), después de `scoreReport` (L121) y antes
del `return DashboardState(...)` (L151). Todos los insumos YA están en scope: `activities`,
`layers`, `weeklyHistory`, `today`, `targetVersions`, y el `ScoreInputSource` ya se construye
ahí (L103-119).

```kotlin
// DashboardProjection.kt, tras calcular scoreReport (~L121):
val startup: StartupCardState? =
    if (StartupDetectionRule.isStartup(scoreReport, activities, layers, weeklyHistory, today)) {
        val anchorActivities = activities.filter {
            it.active && !it.archived && it.activityType == ActivitySurface.Anchor
        }
        val daysLived = startupDaysLived(anchorActivities, today)          // §3.4
        val projection = StartupProjectionUseCase(scoreInputSource, daysLived)
        projection?.let {
            val counter = StartupCounterPolicy.counter(it.estado, daysLived)
            counter.toStartupCardState()                                   // mapping de copy
        }
    } else null
```

`scoreInputSource` = el mismo `ScoreInputSource(...)` que hoy se arma inline en L103-119;
se extrae a un `val` para reusarlo (refactor neutro). El `else null` garantiza que cuando NO
es arranque, el dashboard se comporta exactamente como hoy.

**Decisión de ubicación:** la proyección (`DashboardProjection`/`DashboardEngine`) es la capa
correcta — es "estado" en `hechos→dominio→estado→Compose`. NO se computa en el ViewModel
(sería lógica de negocio fuera del dominio) ni en Compose (Compose no calcula). Coherente con
cómo hoy se computa `scoreReport` ahí mismo.

---

## 6. Elección de card en Compose

En `DashboardScreen.kt:114`, donde hoy hay:

```kotlin
StatusCard(palette = palette, status = state.status)
```

pasa a:

```kotlin
if (state.startup != null) {
    StartupStatusCard(palette = palette, startup = state.startup)
} else {
    StatusCard(palette = palette, status = state.status)
}
```

Una sola condición de presentación, cero lógica de negocio (la decisión `startup != null` ya
viene resuelta del dominio). `StatusCard` y `ScoreOrbit` **no se tocan**.

---

## 7. `StartupStatusCard` (Compose)

Ubicación: `ui/dashboard/components/StartupStatusCard.kt`. Hermano de `StatusCard.kt`.

### 7.1 Estructura

Misma **forma** que `StatusCard` (Row: columna de texto + orbe a la derecha) para coherencia
visual, pero componente **independiente** — NO se refactoriza `StatusCard` para compartir. El
orbe reusa la **forma** de `ScoreOrbit` (Canvas con `drawCircle` de fondo + `drawArc` de
progreso + número central), implementado como composable hermano `StartupOrbit` (o reuso directo
de `ScoreOrbit` pasándole los valores animados — ver §7.2). Sin lógica de negocio: recibe
`StartupCardState` ya resuelto y solo presenta/anima.

```kotlin
@Composable
internal fun StartupStatusCard(
    palette: DashboardPalette,
    startup: StartupCardState,
) {
    val animatedCounter by animateIntAsState(
        targetValue = startup.counterPoints,
        label = "startupCounter",
    )
    val animatedProgress by animateFloatAsState(
        targetValue = startup.windowProgress,
        label = "startupArc",
    )
    Row(/* misma forma que StatusCard: clip + bgSurface + padding */) {
        Column(Modifier.weight(1f)) {
            // pill de estado "Arranque" (color cálido), headline (serif), body (sans),
            // daysRemainingLabel
        }
        ScoreOrbit(                       // reuso directo del orbe existente
            palette = palette,
            score = animatedCounter.toString(),
            label = "cargando",
            progress = animatedProgress,
            color = startupColor(palette),
        )
    }
}
```

### 7.2 Animación — patrón oficial de Compose (ya usado en el repo)

- **Número central:** `animateIntAsState(targetValue = counterPoints)` → el contador "sube".
- **Arco d/7:** `animateFloatAsState(targetValue = windowProgress)` → el anillo se llena.
  `ScoreOrbit` ya pinta el arco con `drawArc(sweepAngle = 360f * progress)` (`StatusCard.kt:141-149`)
  → se le pasa el `progress` animado.

`animateFloatAsState` YA está en uso en el proyecto (`CheckItem.kt:62`, `SupportsPreviewSection.kt:239`)
→ patrón establecido, sin dependencias nuevas. `animateIntAsState` es del mismo paquete
`androidx.compose.animation.core`.

### 7.3 Color cálido propio (sin inventar paleta)

El arranque usa un token **cálido existente** de `DashboardPalette`, NO un color nuevo
hardcodeado. Candidato: `palette.colorCardboard` (cartón/beige cálido — el "calor estructural"
que pide `docs/frontend/frontend-design.md`), o una derivación suave vía la utilidad `mix(...)`
ya existente (`DashboardPalette.kt:77`) entre `colorCardboard` y `colorCoral` para distinguirlo
del `Attention` (que usa `colorCardboard` puro).

```kotlin
private fun startupColor(palette: DashboardPalette): Color =
    mix(palette.colorCoral, 0.35f, palette.colorCardboard)   // cálido, propio, derivado
```

**Decisión:** derivar de tokens existentes (`colorCardboard`/`colorCoral`) vía `mix`. NO se
agrega un campo nuevo a `DashboardPalette` salvo que el dueño/diseño lo pida explícitamente.
Respeta `docs/frontend` (base oscura orgánica, cartón/beige, coral mate; nada de neón). La
elección final del valor exacto se confirma contra el prototipo en la fase de apply (capa
visual de `verificacion-por-capas.md`).

---

## 8. Wiring — del dominio al Compose

**El wiring es mínimo porque `weeklyHistory` YA fluye end-to-end** (verificado):

```
Room (snapshots)
  → repository.weeklyScoreHistoryFlow()          DashboardRepository.kt:56,147
  → DashboardViewModel.kt:132 (combine)           ya inyecta weeklyHistory en facts
  → DashboardEngine.buildState(..., weeklyHistory = facts.weeklyHistory)   :172
  → buildDashboardState(...)                       DashboardProjection.kt:56 (param ya existe)
       ├─ scoreReport = ScoreEngine.calculate(...)            (intacto, sigue NoData)
       ├─ startup = StartupDetectionRule.isStartup(...)?       (NUEVO, §5.3)
       │     └─ StartupProjectionUseCase → StartupCounterPolicy → StartupCardState
       └─ DashboardState(..., startup = startup)               (NUEVO campo, §5.2)
  → DashboardScreen.kt:114  if (state.startup != null) StartupStatusCard else StatusCard
```

**Flujos/archivos a tocar para el wiring:**

| Archivo | Cambio | Nota |
|---------|--------|------|
| `domain/scoring/AnchorScoringPolicy.kt` (L67, L68) | `7 → windowDays` param | Lote 1 |
| `domain/scoring/BuildScoreInputUseCase.kt` (L16-19) | param `includeGraceAnchors=false` | Lote 2 |
| `domain/scoring/ScoreEngine.kt` (L36-78) | extraer `calculateInternal(input, windowDays)` + entry `calculateProjection` | Lote 2 |
| `domain/scoring/StartupDetectionRule.kt` | NUEVO | Lote 2 |
| `domain/scoring/StartupProjectionUseCase.kt` | NUEVO | Lote 2 |
| `domain/scoring/StartupCounterPolicy.kt` | NUEVO | Lote 2 |
| `domain/dashboard/DashboardState.kt` | NUEVO `StartupCardState` + campo `startup` | Lote 3 |
| `domain/dashboard/DashboardProjection.kt` (~L101-151) | extraer `scoreInputSource` a val; computar `startup`; pasar a `DashboardState` | Lote 3 |
| `ui/dashboard/components/StartupStatusCard.kt` | NUEVO | Lote 3 |
| `ui/dashboard/DashboardScreen.kt` (L114) | branch `if startup` | Lote 3 |
| `docs/scoring/modelo-matematico-nucleo-v1.md` | documentar `windowDays` (doc vivo) | Lote 1 |

**NO se toca:** `DashboardViewModel.kt`, `DashboardRepository.kt`, persistencia
(`WeeklyScoreSnapshotWriter`), `Models.kt` (`ScoreState`), `StatusCard.kt`, `ScoreOrbit`,
`BandPolicy.kt`. El arranque no se persiste; el `ScoreReport` real sigue `NoData`.

---

## 9. Mapa de archivos a tocar (resumen file:line)

| file:line | Acción |
|-----------|--------|
| `AnchorScoringPolicy.kt:39` | firma `+ windowDays: Int = 7` |
| `AnchorScoringPolicy.kt:67` | `if (f < 7)` / `(7 - f)` → `windowDays` |
| `AnchorScoringPolicy.kt:68` | `(f / 7.0)` → `(f / windowDays)` |
| `BuildScoreInputUseCase.kt:7` | firma `+ includeGraceAnchors: Boolean = false` |
| `BuildScoreInputUseCase.kt:16-19` | guard `!includeGraceAnchors &&` en el `filterNot` |
| `ScoreEngine.kt:36-78` | extraer `calculateInternal(input, windowDays)`; `calculate`→`windowDays=7`; `internal calculateProjection` |
| `ScoreEngine.kt:78` | `rFromRatios(window.f, ratios)` → `rFromRatios(window.f, ratios, windowDays)` |
| `StartupDetectionRule.kt` (nuevo) | `isStartup(...)` |
| `StartupProjectionUseCase.kt` (nuevo) | `invoke(source, windowDays)` + `startupDaysLived` helper |
| `StartupCounterPolicy.kt` (nuevo) | `counter(projectedEstado, daysLived)` + `StartupCounter` |
| `DashboardState.kt:23-24` | `StartupCardState` + `val startup: StartupCardState? = null` |
| `DashboardProjection.kt:103-119` | extraer `scoreInputSource` a `val` |
| `DashboardProjection.kt:~121` | computar `val startup` |
| `DashboardProjection.kt:151` | `DashboardState(..., startup = startup)` |
| `StartupStatusCard.kt` (nuevo) | card + animaciones |
| `DashboardScreen.kt:114` | branch `if (state.startup != null)` |
| `docs/scoring/modelo-matematico-nucleo-v1.md` | parámetro `windowDays` (doc vivo) |

---

## 10. ADRs (decisiones registradas)

| ADR | Decisión | Rechazado | Razón |
|-----|----------|-----------|-------|
| ADR-1 | Canal de presentación `DashboardState.startup` | `ScoreState.Arranque` | No romper 5 `when` exhaustivos ni contaminar `BandPolicy`/motor; el dominio sigue diciendo la verdad (`NoData`). |
| ADR-2 | Generalizar `rFromRatios(windowDays=7)` | Policy paralela / escalar `f` afuera | Una sola fuente de verdad NIVEL 1; default 7 byte-idéntico; no distorsiona los 4 términos. |
| ADR-3 | `includeGraceAnchors` aditivo en `BuildScoreInputUseCase` | `ScoreInput` alterno duplicado | Evitar duplicar el mapeo de 12 campos; default preserva camino maduro. |
| ADR-4 | `ScoreEngine.calculateInternal` interno + `calculateProjection` | Reimplementar el pipeline en el use case | No duplicar NIVELES 1-6; firma pública `calculate(input)` intacta. |
| ADR-5 | Justicia (`windowDays`) y barra (`×d/7`) son piezas separadas | Una sola fórmula que haga ambas | Convergen en día 7 → sin salto día 7→8; cada pieza con responsabilidad única. |
| ADR-6 | Color cálido derivado de `colorCardboard`/`colorCoral` vía `mix` | Token nuevo en `DashboardPalette` | Respetar `docs/frontend`; no inflar la paleta sin pedido del diseño. |
| ADR-7 | Arranque NO se persiste; `ScoreReport` real = `NoData` | Persistir contador en snapshot | El snapshot es cache derivado; el "0" de NoData sirve para testear "sin puntaje"; cero contaminación. |

---

## 11. Riesgos arquitectónicos / supuestos a validar

- **R1 — Regresión del núcleo (windowDays):** mitigado por default 7 + test
  `rFromRatios(...) == rFromRatios(..., 7)` + suite existente verde. Validación numérica fina de
  los 4 términos se cierra en la spec.
- **R2 — Salto visual día 7→8:** mitigado por test explícito de convergencia (`×7/7` proyectado
  == score maduro día 8). **Supuesto a validar en spec:** que `windowDays=7` en proyección
  produce exactamente el mismo `estado` que el motor maduro día 8 (misma ventana, mismas anclas
  ya fuera de gracia).
- **R3 — `wt > 1` en arranque temprano** (`f > windowDays`): el guard `sd=0` lo absorbe; `s`
  sigue acotado por `smax`. **Validar numéricamente en spec** que no hay NaN ni valores fuera de
  `[0,1.5]` para `windowDays ∈ {1..6}` y `f ∈ {2..7}`.
- **R4 — Doble corrida del motor en arranque** (real NoData + proyección): costo trivial
  (dominio puro JVM, sin IO). Aceptado.
- **R5 — `calculateProjection` internal expone seam del motor:** acotado a un punto de entrada
  `internal` + extracción de método neutra; la firma pública no cambia. Riesgo bajo de mal uso.
- **Supuesto:** el `createdAt` de las anclas es confiable como ancla temporal del arranque
  (mismo que ya usa `AnchorGraceRule`). Si el seed re-crea anclas con `createdAt` nuevo tras un
  wipe de dev, el arranque reaparece — comportamiento correcto para "cuenta nueva".
