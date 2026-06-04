# Diseño técnico — Rotación de frases ancla (`anchor-phrase-rotation`)

> **Estado SDD:** diseño (formaliza el plan, no lo rediseña)
> **Proyecto:** apk-personal — "Autonomía sin límites" (Android, Kotlin + Jetpack Compose + Room, local-first)
> **Spec de producto (fuente de verdad):** `docs/dominio/frases-ancla.md` (vivo)
> **Plan técnico (fuente de verdad):** `meta/instructions/2026-06-04-rotacion-frases-ancla.md`
> **Decisión de arquitectura:** Manera A (con persistencia / "libreta") — ya cerrada.
> **Verificación contra código:** todas las referencias `file:line` de abajo se confirmaron en este repo. Las desviaciones encontradas se marcan con **⚠ DRIFT**.

---

## 1. Resumen ejecutivo

Completar el motor de rotación de frases ancla cuyo andamiaje de datos ya existe (~70%), siguiendo el patrón **gemelo ya probado** del scoring semanal: piezas pequeñas de responsabilidad única (dominio puro) orquestadas por un coordinador de capa de datos (`AnchorPhraseResolver`, espejo de `WeeklyScoreSnapshotWriter`), con lectura reactiva en el dashboard. Se corrige además un **bug latente de release**: las 5 tablas `anchor_phrase*` están en la DB v12 pero ninguna migración las crea.

---

## 2. Principio de diseño rector: engranajes, no monolitos

La convención **ya establecida** en el repo es responsabilidad única + composición. El scoring son 13 policies atómicas (`domain/scoring/*Policy.kt`) que `ScoreEngine.calculate()` **solo orquesta** — no calcula él mismo. Verificado en `ScoreEngine.kt:8-65`: el motor delega en `LayerScoringPolicy`, `WeeklyScorePolicy`, `VisibleScorePolicy`, `StabilityScoringPolicy`, `BaseStatePolicy`, `ScoreReasonPolicy`, etc., y solo ensambla el `ScoreReport`.

Las frases siguen el **mismo molde**. Ningún archivo "hace todo":

| Engranaje | Una sola cosa | Cambiar solo esto toca solo… |
|---|---|---|
| `DayPhasePolicy` | hora local → `Dawn`/`Dusk` | franjas horarias |
| `AnchorPhraseSelector` | inputs puros → frase elegida | la fórmula de elección |
| `AnchorPhraseResolver` | orquestar leer/elegir/persistir | cuándo se persiste |
| `AnchorPhraseSeed` | datos del catálogo + mapas de peso | frases/pesos |
| `DashboardProjection.selectAnchorPhrase` | slot resuelto → estado de UI (lookup) | el mapeo a UI |
| `AnchorPhraseCard` | dibujar cita + autor (sin cambios) | el visual |

`AnchorPhraseSelector.select()` es **delgado**: compone funciones puras chiquitas (una por regla de `frases-ancla.md §6, §8, §9`), igual que `ScoreEngine` compone policies. **No** se construye una función monolítica.

---

## 3. Decisión de arquitectura (ADR-style)

### ADR-1 — Manera A: persistir la "libreta" (slot + impresión)

- **Decisión:** persistir en Room qué frase se mostró (`anchor_phrase_daily_slots` + `anchor_phrase_impressions`).
- **Contexto:** el 99% de la app es read-only (hechos → dominio → Compose) y el estado del score **NO se cachea**: se recalcula reactivo (`combine` + `stateIn(WhileSubscribed(5_000))`, verificado en `DashboardViewModel`). Pero las frases tienen dos requisitos de producto (`frases-ancla.md §8`): **estabilidad dentro de la fase** (reabrir no cambia la frase) y **no repetir en 7 días**.
- **Rationale:** "el 2026-06-04, fase Dawn, se mostró `phrase_X`" es un **hecho**, no un caché de estado derivado. Guardarlo en Room **respeta** la arquitectura local-first (Room guarda hechos), no la viola. Es el mismo rol que `DailyActivityLogEntity`.
- **Alternativa rechazada — Manera B (selector puro sin persistencia, determinístico por `seed = hash(date, dayPhase, state)`):** daría estabilidad pero **no** puede garantizar la no-repetición en 7 días sin leer impresiones históricas; y al cambiar el estado dentro de la fase perdería trazabilidad de lo ya mostrado. Se descarta como mecanismo único, pero el determinismo por seed **se conserva** dentro del selector como refuerzo y para testeo.
- **Patrón gemelo (consistencia):** `AnchorPhraseResolver : libreta :: WeeklyScoreSnapshotWriter : historia semanal`. Ambos escriben un hecho derivado en la capa de datos durante el mantenimiento diario; el dashboard lo lee reactivo. Verificado: `WeeklyScoreSnapshotWriter` se construye en `AutonomiaRepository.kt:66-67` y se expone vía `refreshCurrentWeeklyScoreSnapshot` (`AutonomiaRepository.kt:340-344`).

### ADR-2 — Seed en Kotlin (no JSON/asset), reglas DERIVADAS de mapas

- **Decisión:** las 83 frases (`frases-ancla.md §13, §15`) se siembran en Kotlin vía un helper `phrase(...)` one-liner (mismo patrón que `activityDef(...)`/`abstinenceTrack(...)`, verificado en `DefaultSeeds.kt:210-235,237`). Las filas de `anchor_phrase_state_rules` y `anchor_phrase_phase_rules` **no se escriben a mano**: se **generan** desde dos mapas chicos (`stateWeights: Map<ScoreState, Map<PhraseFamily, Int>>`, `phaseWeights: Map<DayPhase, Map<PhraseFamily, Int>>`) recorriendo cada frase y derivando filas por su familia.
- **Rationale:** los pesos de `§9` son **por familia**, no por frase. Seed en Kotlin = seguridad en compilación (un typo de familia no compila), consistente con `DefaultSeeds`, y es **data canónica respaldada por doc** (no editable por el usuario, `CLAUDE.md`: data predeterminada NO descartable).
- **Alternativa rechazada — JSON/asset:** añade parsing, dependencia y modos de fallo en runtime para data estática → over-engineering.

### ADR-3 — Fuente del `scoreState`: leer el snapshot de la semana actual, no recalcular

- **Decisión:** el resolver lee `WeeklyScoreSnapshotEntity.state` (`Entities.kt:330`) de la semana en curso, ya refrescado por `refreshCurrentWeeklyScoreSnapshot` (`DashboardViewModel.kt:189`) **inmediatamente antes** en `runDailyMaintenance`.
- **Rationale:** evita un segundo cálculo de `ScoreEngine`; el snapshot está fresco en ese punto del pipeline de mantenimiento (verificado el orden en `DashboardViewModel.kt:181-190`).
- **⚠ Gap de DAO (ver §6):** no existe una query "snapshot de la semana actual". El DAO solo expone `getWeeklyScoreSnapshotsSnapshot()` (suspend, todas las filas, `AutonomiaDao.kt:303-304`) y `observeWeeklyScoreSnapshots()` (Flow, `:300-301`). El resolver usará `getWeeklyScoreSnapshotsSnapshot()` y elegirá en memoria la fila de `weekStart == mondayOf(today)` con `scoringVersion == SCORING_VERSION`. Si no hay fila aún → `scoreState = NoData` (fallback grácil).
- **Supuesto a confirmar (slice 5):** que el `state` del snapshot de la semana actual **coincide** con el estado que muestra el dashboard. Ambos salen de `ScoreEngine`, deberían igualar. Se verifica al integrar.

---

## 4. Componentes y capas (dominio puro intacto)

| Capa | Componente | Archivo | ¿Toca Room? |
|---|---|---|---|
| Dominio (puro) | `DayPhasePolicy` | `domain/phrase/DayPhasePolicy.kt` (nuevo) | No |
| Dominio (puro) | `AnchorPhraseSelector` | `domain/phrase/AnchorPhraseSelector.kt` (nuevo) | No |
| Dominio (modelos) | `DayPhase`, `AnchorPhraseSelection`, `AnchorPhraseStateRule`, `AnchorPhrasePhaseRule` | `Models.kt` (nuevos) | No |
| Datos (mappers) | mappers de las nuevas entidades/reglas | `data/local/mapper/DomainMappers.kt` | No |
| Datos (coordinador) | `AnchorPhraseResolver` | `data/phrase/AnchorPhraseResolver.kt` (nuevo) | **Sí (única autorizada)** |
| Datos (seed) | `AnchorPhraseSeed` | `data/local/seed/AnchorPhraseSeed.kt` (nuevo) | No (entrega filas) |
| Datos (DAO) | 2 queries nuevas | `data/AutonomiaDao.kt` | Sí |
| Datos (migración) | crea 5 tablas | `data/AutonomiaDatabase.kt` | Sí |
| UI (proyección pura) | `selectAnchorPhrase` → lookup | `domain/dashboard/DashboardProjection.kt:364` | No |
| UI (estado) | quitar default Kierkegaard | `domain/dashboard/DashboardState.kt:43-46` | No |
| UI (repo/VM) | flow del slot + combine + wiring | `ui/dashboard/DashboardRepository.kt`, `DashboardViewModel.kt` | No (leen Flow) |
| UI (Compose) | `AnchorPhraseCard` | sin cambios | No |

> **⚠ DRIFT de rutas vs. plan/proposal:** `DashboardProjection.kt` y `DashboardState.kt` viven en `domain/dashboard/`, **no** en `ui/dashboard/`. Verificado por Glob. El resto de las rutas del plan son correctas. `DashboardRepository.kt` sí está en `ui/dashboard/` (untracked, ya creado en el árbol).

### 4.1 Modelos de dominio nuevos (`Models.kt`)

- `enum class DayPhase { Dawn, Dusk }` — no existe aún (confirmado: el plan lo lista como faltante; `frases-ancla.md §11` lo sugiere).
- `data class AnchorPhraseSelection(phraseId, text, authorReference, family, scoreState, dayPhase)` — espejo de `frases-ancla.md §8` salida / `§12`.
- `data class AnchorPhraseStateRule(phraseId, scoreState, weight)` y `data class AnchorPhrasePhaseRule(phraseId, dayPhase, weight)` — modelos de dominio para las reglas (las entidades ya existen: `Entities.kt:146-160`).
- Reutilizar `ScoreState` y `PhraseFamily`/`AttributionStatus` existentes (`frases-ancla.md §11`: `ScoreState` no se duplica).

### 4.2 `DayPhasePolicy` (puro)

`frases-ancla.md §7`: `Dawn` = 05:00–14:59 local; `Dusk` = 15:00–04:59 local. Recibe la hora local inyectada (`LocalDateTime`/`LocalTime`), devuelve `DayPhase`. Sin geolocalización (`§17`). Test cubre bordes 14:59/15:00 y 04:59/05:00.

### 4.3 `AnchorPhraseSelector` (puro, determinístico)

```kotlin
// domain/phrase/AnchorPhraseSelector.kt  (PURO, sin Room, sin suspend)
data class AnchorPhraseSelectorInput(
    val date: LocalDate,
    val dayPhase: DayPhase,
    val scoreState: ScoreState,
    val catalog: List<AnchorPhrase>,
    val stateRules: List<AnchorPhraseStateRule>,
    val phaseRules: List<AnchorPhrasePhaseRule>,
    val recentPhraseIds: Set<String>,   // mostradas en ventana de 7 días
)

object AnchorPhraseSelector {
    fun select(input: AnchorPhraseSelectorInput): AnchorPhraseSelection?
}
```

`select()` compone funciones puras chiquitas (una por regla):

| Función interna | Regla (`frases-ancla.md`) |
|---|---|
| `filterEligible(catalog)` | `active && authorReference no vacío` (§3, §8.5) |
| `filterByState(phrases, scoreState)` | solo familias permitidas (§6); **nunca** `Contemplation` fuera de `Plenitude`/`Unbreakable` (§8.6) |
| `excludeRecent(phrases, recentIds)` | ventana 7 días; si queda vacío → **relajar la ventana, NO las reglas de estado** (§8.7) |
| `weightOf(phrase, state, phase)` | `pesoEstado(familia) + pesoFase(familia)` (§9) |
| `weightedPick(weighted, seed)` | elección ponderada con `Random(seed)`, `seed = hash(date, dayPhase)` → estable en la fase + reproducible en tests |

Retorna `null` si no hay frase elegible → el resolver hace fallback grácil (no escribe). `Random(seed)` (Context7 / kotlinlang): secuencias reproducibles → cada función se testea aislada.

### 4.4 `AnchorPhraseResolver` (coordinador, capa de datos)

Espejo de `WeeklyScoreSnapshotWriter`. Pseudocódigo:

```
suspend fun resolveForToday(today: LocalDate, now: LocalDateTime):
    phase = DayPhasePolicy.phaseFor(now)
    slot  = dao.getAnchorPhraseDailySlot(today.toString(), phase.name)   // existe: AutonomiaDao.kt:144
    state = currentWeekState(today)   // ver §6: deriva de getWeeklyScoreSnapshotsSnapshot()
    if slot != null && slot.scoreState == state.name: return   // ADR-1: estabilidad en la fase
    catalog    = dao.observeAnchorPhrases().first()            // existe: :120-121
    stateRules = dao.getAnchorPhraseStateRules()               // existe: :126-127
    phaseRules = dao.getAnchorPhrasePhaseRules()               // existe: :132-133
    recentIds  = dao.getAnchorPhraseImpressionsBetween(today.minusDays(6), today)  // NUEVA query
    selection  = AnchorPhraseSelector.select(input)
    if selection == null: return                              // fallback grácil: no escribir
    @Transaction:
        dao.upsertAnchorPhraseDailySlot(slot REPLACE)          // existe: :141-142
        dao.upsertAnchorPhraseImpression(impression, id = UUID) // existe: :138-139
```

Inyección de dependencia idéntica al writer (envuelve el DAO; puede usar una `AnchorPhraseDataSource` fina si se quiere testear con fake, como `WeeklySnapshotDataSource`). Context7 / Android Architecture: `@Insert(onConflict = REPLACE)` + `suspend` para idempotencia; `@Transaction` para que slot+impresión sean atómicos.

### 4.5 Lectura reactiva en el dashboard

- Nueva query DAO: `observeAnchorPhraseDailySlots(date): Flow<List<AnchorPhraseDailySlotEntity>>`.
- `DashboardRepository` expone `anchorPhraseSlotFlow(dateKey)`.
- `DashboardViewModel` **combina** el slot del día (ya observa el catálogo, `DashboardViewModel.kt:132`).
- `DashboardProjection.selectAnchorPhrase(...)` (`:364-378`) deja de ser stub: pasa a **lookup puro** — toma el slot de la fase actual, busca `phraseId` en el catálogo, devuelve `text` + `authorReference`. Sin slot → fallback grácil **vacío** (no la cita hardcodeada).
- `DashboardState.kt:43-46`: **quitar** el default Kierkegaard; default neutro/vacío (`""`/`""`). Verificado el hardcode en `DashboardState.kt:44-45`.

---

## 5. Wiring de DI (puntos exactos, verificados)

```kotlin
// AutonomiaRepository.kt, junto al :66 (al lado del weeklyScoreSnapshotWriter)
private val anchorPhraseResolver = AnchorPhraseResolver(dao)

// método expuesto, espejo de refreshCurrentWeeklyScoreSnapshot (:340-344)
suspend fun resolveAnchorPhraseForToday(today: LocalDate, now: LocalDateTime) =
    anchorPhraseResolver.resolveForToday(today, now)
```

- **Seed en `ensureSeeded()` (`AutonomiaRepository.kt:244-253`):** `upsert` siempre (idempotente), mismo criterio que actividades/tracks (`dao.upsertActivityDefinitions`, `:252`). Invariante (`§15`): 83 frases activas, 0 con `authorReference` vacío, reglas derivadas coherentes.
- **Llamada en `runDailyMaintenance` (`DashboardViewModel.kt:181-190`):** agregar `repository.resolveAnchorPhraseForToday(date, LocalDateTime.now())` **después** de `refreshCurrentWeeklyScoreSnapshot(date)` (`:189`), para que el snapshot esté fresco (ADR-3).
- **`onResumed()` (`DashboardViewModel.kt:173-179`):** **extender**. Hoy solo re-corre el mantenimiento si cambió la fecha (`if (now != currentDate.value)`). Hay que **re-resolver también si cambió la fase del día** (cruce de las 15:00 con la app en background) aunque la fecha sea la misma. Diseño: comparar fase actual contra la fase de la última resolución (o re-resolver siempre en `onResumed`, barato: el resolver reusa el slot si nada cambió).

---

## 6. Cambios de DAO necesarios

Verificado en `AutonomiaDao.kt:119-145`: ya existen lectura del catálogo activo (Flow), reglas de estado/fase, upsert de impresión, upsert+lectura del slot por `(date, dayPhase)`. **Faltan dos queries:**

1. **Impresiones en ventana de 7 días** (para `recentPhraseIds`):
   ```kotlin
   @Query("SELECT * FROM anchor_phrase_impressions WHERE date BETWEEN :start AND :end")
   suspend fun getAnchorPhraseImpressionsBetween(start: String, end: String): List<AnchorPhraseImpressionEntity>
   ```
2. **Observar slots del día como Flow** (lectura reactiva del dashboard):
   ```kotlin
   @Query("SELECT * FROM anchor_phrase_daily_slots WHERE date = :date")
   fun observeAnchorPhraseDailySlots(date: String): Flow<List<AnchorPhraseDailySlotEntity>>
   ```

**Estado del score de la semana actual (ADR-3):** no hay query dedicada. El resolver deriva en memoria de `getWeeklyScoreSnapshotsSnapshot()` (`:303-304`) filtrando `weekStart == mondayOf(today).toString()` y `scoringVersion == WeeklyScoreSnapshotConstants.SCORING_VERSION`. Opcional (mejora): agregar `getWeeklyScoreSnapshot(weekStart, scoringVersion)` para no traer todas las filas; no es bloqueante.

---

## 7. Migración (nota histórica — REVERTIDA por Camino A)

> **IMPORTANTE:** La migración v12→v13 fue planificada en esta propuesta/diseño pero
> **REVERTIDA** antes de archivar. Decisión "Camino A": en fase dev, DB descartable,
> NO se escriben migraciones Room manuales. La DB permanece en **versión 12**. Las
> 5 tablas `anchor_phrase*` se crean en instalación limpia desde el esquema Room.
> El androidTest `AnchorPhraseMigration12To13Test.kt` y `schemas/13.json` fueron eliminados.
> Esto es un **bug latente para release** — deberá resolverse antes de liberar a usuarios.

---

## 8. Mejores prácticas aplicadas (Context7)

| Práctica | Fuente | Dónde |
|---|---|---|
| `@Insert(onConflict = OnConflictStrategy.REPLACE)` + `suspend` | Android Architecture (Room DAO) | seed idempotente, upsert de slot |
| `@Transaction` para escrituras compuestas atómicas | Android Architecture | slot + impresión juntos en el resolver |
| Observar queries como `Flow`, derivar UiState (no almacenar estado derivado) | Android Architecture | `observeAnchorPhraseDailySlots` reactivo; proyección hace lookup, no guarda |
| `collectAsStateWithLifecycle` | Android Architecture | ya en uso en el dashboard, sin cambios |
| `Random(seed)` para aleatoriedad reproducible | kotlinlang | `weightedPick` testeable y estable en la fase |

---

## 9. Flujo de datos (extremo a extremo)

```
[mantenimiento diario / onResume]
  runDailyMaintenance(date)  (DashboardViewModel.kt:181)
    → refreshCurrentWeeklyScoreSnapshot(date)            (:189)  [escribe WeeklyScoreSnapshotEntity.state]
    → resolveAnchorPhraseForToday(date, now)             [NUEVO, después de :189]
        → AnchorPhraseResolver.resolveForToday
            → DayPhasePolicy.phaseFor(now)               [dominio puro]
            → lee slot / state(snapshot) / catálogo / reglas / impresiones-7d
            → AnchorPhraseSelector.select(input)         [dominio puro, Random(seed)]
            → @Transaction: upsert slot + impresión       [única escritura Room]
[lectura reactiva]
  dao.observeAnchorPhraseDailySlots(date): Flow
    → DashboardRepository.anchorPhraseSlotFlow(dateKey)
    → DashboardViewModel.combine(...)                    (junto al catálogo, :132)
    → DashboardProjection.selectAnchorPhrase(slot, catálogo)  [lookup puro, :364]
    → DashboardAnchorPhraseState(text, authorReference)  [sin default Kierkegaard, :44]
    → AnchorPhraseCard  [Compose, sin cambios]
```

---

## 10. Estrategia de entrega (chained PRs — el cambio supera 400 líneas)

Solo el seed (83 frases + reglas derivadas) ya supera el presupuesto. Se entrega en **PRs encadenados/apilados** (~400 líneas c/u), commits por **unidad de trabajo** (test + código + doc juntos). Orden por dependencias (= los slices del plan):

1. Enums + modelos dominio + `DayPhasePolicy` (+ mappers) — dominio puro, tests JVM.
2. **Migración 12→13 de las 5 tablas** + cobertura `MigrationTestHelper` (crítico/bloqueante). — **REVERTIDA (Camino A)**
3. Seed aislado `AnchorPhraseSeed.kt` + wiring `ensureSeeded` (PR grande pero aislado).
4. `AnchorPhraseSelector` puro ponderado (+ tests por regla).
5. `AnchorPhraseResolver` + 2 queries DAO + wiring mantenimiento/`onResumed`.
6. Integración dashboard: flow del slot + lookup en proyección + quitar hardcode.
7. Doc viva `docs/dominio/frases-ancla.md` (§17/§18: marcar implementado).

Strict TDD activo (`testDebugUnitTest`): test primero. Compilar en slices 2 y 5 (Room/migración/DI no triviales, `CLAUDE.md`).

---

## 11. Riesgos y decisiones abiertas

1. **Migración (BLOQUEANTE release):** sin `MIGRATION_12_13`, todo dispositivo que migró 11→12 crashea. **En dev: aceptado bajo Camino A.** Deberá resolverse antes de release.
2. **Estado del snapshot de la semana actual (ADR-3):** sin query dedicada; se deriva en memoria. Verificado en slice 5: coincide con el estado del dashboard.
3. **`authorReference` NULLable** (§7): "obligatorio" es regla de dominio/seed, no de esquema.
4. **"Cambio significativo de estado" en la fase (§8.3):** v1 = re-elegir si `slot.scoreState != currentState`. Simple, suficiente para MVP.
5. **Cruce de fase con app abierta:** `onResumed` cubre el regreso del background; aceptable v1.

---

## 12. Definición de Terminado (gates)

- Slices 1, 3–6 con tests verdes (`testDebugUnitTest`). Slice 2 revertido.
- Build debug compila (cambios no triviales: Room/DI).
- Seed: 83 frases activas, 0 sin `authorReference`; reglas derivadas de mapas.
- Cero cita hardcodeada en `DashboardState.kt`.
- `docs/dominio/frases-ancla.md` actualizado (deja de contradecir al código).
- Capas aplicables de `meta/guias/verificacion-por-capas.md` en verde.
