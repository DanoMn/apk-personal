# Plan de implementación — Rotación de frases ancla (Manera A)

> **Tipo:** pro-prompt / plan de implementación
> **Fecha:** 2026-06-04
> **Spec de producto (fuente de verdad):** `docs/dominio/frases-ancla.md` (vivo)
> **Estado del código al planificar:** andamiaje de datos ~70% hecho; falta seed + motor + persistencia.

> ⚠️ **SUPERSEDED (2026-06-04):** el **Slice 2 (migración)** y todo lo referido a
> `MIGRATION_12_13`/DB v13/`MigrationTestHelper`/"bug latente de migración" quedó **anulado**
> por la decisión **Camino A** (en dev NO se escriben/testean migraciones; ver `CLAUDE.md` y el
> handoff `meta/handoffs/2026-06-04-rotacion-frases-y-camino-a.md`). La DB sigue en **v12**.
> El resto del plan (slices 1, 3-7) se implementó y se archivó.

---

## 0. TL;DR (qué vamos a hacer y por qué)

El dashboard muestra hoy **una frase hardcodeada** (Kierkegaard). No es por descuido: es el
*fallback* del default de `DashboardAnchorPhraseState` porque **el catálogo de frases está
vacío** y el selector actual es un stub. Las entidades Room, el DAO y el modelo de dominio
**ya existen**; falta: (1) sembrar las 83 frases + reglas, (2) un selector real ponderado por
estado del score y fase del día, y (3) persistir qué frase se mostró para dar estabilidad y
evitar repeticiones (la "libreta").

**Decisión cerrada:** vamos con **Manera A** (con libreta). Guardar "qué frase se mostró hoy"
**es un hecho**, no un caché de estado derivado → es exactamente lo que Room debe guardar. No
viola la arquitectura. Y replica un patrón **ya probado en este repo**: el resolver de frases
es a la "libreta" lo que `WeeklyScoreSnapshotWriter` es a la historia semanal.

---

## 1. Diagnóstico del estado actual (research)

### Lo que YA está implementado ✅

| Pieza | Ubicación | Estado |
|---|---|---|
| Entidades Room (5 tablas) | `data/Entities.kt:128-185` | ✅ `anchor_phrases`, `anchor_phrase_state_rules`, `anchor_phrase_phase_rules`, `anchor_phrase_impressions`, `anchor_phrase_daily_slots` |
| Registro en DB v12 | `data/AutonomiaDatabase.kt:21-25` | ✅ las 5 en `entities = [...]` |
| DAO completo | `data/AutonomiaDao.kt:119-145` | ✅ catálogo, reglas, impresiones, slots (lee y escribe) |
| Modelo de dominio | `Models.kt:72-83` (`AnchorPhrase`) | ✅ + enums `PhraseFamily`, `AttributionStatus` (`Models.kt:160-161`) |
| Mapper | `data/local/mapper/DomainMappers.kt:193` | ✅ `AnchorPhraseEntity.toDomain()` |
| Flujo al dashboard | `ui/dashboard/DashboardViewModel.kt:132` → `DashboardProjection.kt:157` | ✅ ya llega `List<AnchorPhrase>` a la proyección |

### Lo que FALTA ❌ (el agujero real)

| Pieza | Evidencia | Impacto |
|---|---|---|
| **Seed de 83 frases + reglas** | `DefaultSeeds.kt` no contiene ni una frase | Catálogo vacío → fallback hardcodeado |
| **Motor de rotación** | `selectAnchorPhrase()` en `DashboardProjection.kt:364-378` agarra la **primera** por `sortOrder`; ignora fase, estado, pesos, impresiones y ventana de 7 días | No hay rotación real |
| **Enum `DayPhase` (Dawn/Dusk)** | No existe en `Models.kt` | Sin fases del día |
| **Persistencia de impresiones/slots** | DAO tiene métodos; **nadie los llama** | Sin estabilidad ni anti-repetición |
| **Default hardcodeado** | `DashboardState.kt:44-45` | Placeholder a remover |

### ⚠️ Bug latente de release detectado (fuera del happy-path de dev)

Las 5 tablas `anchor_phrase*` están en `entities` de la DB v12, **pero NINGUNA migración las
crea** (revisado: `MIGRATION_11_12` es de `sleep_nights`, no de frases). Consecuencia:

- **Instalación limpia v12** → Room crea las tablas desde el esquema → funciona. (Es el caso de dev.)
- **Dispositivo que migró 11→12** → la tabla no existe → **crash de validación de esquema Room**.

En fase de dev con DB descartable esto está **enmascarado** (reinstalás limpio y listo, ver
`CLAUDE.md`). Pero para release es un bug. El plan lo cubre como tarea explícita (slice 2):
confirmar/añadir la migración que crea las 5 tablas con nombres de índice correctos
(`index_<tabla>_<col>`, regla de `CLAUDE.md`) y cubrirla con `MigrationTestHelper`.

---

## 2. Decisión de arquitectura (el corazón del plan)

### Por qué esta feature es la excepción al "solo leer y mostrar"

El 99% de la app es read-only: **hechos (Room) → dominio (interpreta) → Compose (muestra)**.
El **estado del score NO se guarda**: lo calcula `ScoreEngine.calculate(...)`
(`DashboardProjection.kt:96`) de forma reactiva — solo recalcula cuando cambian los hechos
(`combine` + `stateIn(WhileSubscribed(5_000))`, `DashboardViewModel.kt:66-161`). Eso es
correcto por diseño: cachear estado derivado trae bugs de invalidación; recalcular desde la
fuente de verdad nunca miente. (La app **sí** cachea lo inmutable: las semanas cerradas en
`WeeklyScoreSnapshotWriter.kt:46`.)

Las frases rompen ese molde por **dos requisitos de producto** (`frases-ancla.md §8`):

1. **Estabilidad dentro de la fase del día** — reabrir la app no debe cambiar la frase.
2. **No repetir** una frase mostrada en los últimos **7 días**.

Para cumplirlas, la app debe **acordarse de qué frase mostró**. Esa anotación es un **hecho**
("el 2026-06-04, fase Dawn, se mostró `phrase_X`"), igual que un `DailyActivityLog`. Guardarla
es lo correcto, no una violación.

### El patrón gemelo que ya existe en el repo

```
WeeklyScoreSnapshotWriter   →  escribe la "foto" de la semana cerrada  →  dashboard la LEE vía weeklyScoreHistoryFlow
AnchorPhraseResolver (nuevo) →  escribe el "slot" + "impresión" del día →  dashboard lo LEE vía anchorPhraseSlotFlow
```

Mismo lugar (paso de mantenimiento en `runDailyMaintenance`), mismo principio (escribir hecho
derivado en la capa de datos, leerlo reactivo en el dashboard). **Consistencia total.**

### Reparto de responsabilidades (respeta dominio puro)

| Capa | Componente | Responsabilidad | ¿Toca Room? |
|---|---|---|---|
| **Dominio (puro)** | `DayPhasePolicy` | `now → Dawn/Dusk` por ventana horaria | ❌ |
| **Dominio (puro)** | `AnchorPhraseSelector` | ranking ponderado: dadas (date, dayPhase, scoreState, catálogo, reglas, impresiones recientes) → elige frase, **determinístico** | ❌ |
| **Datos (coordinador)** | `AnchorPhraseResolver` | lee slot/impresiones/catálogo, llama al selector puro, persiste slot + impresión | ✅ (única autorizada) |
| **UI (proyección pura)** | `DashboardProjection` | mapea slot resuelto → `DashboardAnchorPhraseState` (lookup, sin lógica) | ❌ |
| **UI (Compose)** | `AnchorPhraseCard` | muestra cita + autor (sin cambios) | ❌ |

---

## 3. Diseño técnico detallado

### 3.0 Principio de diseño: engranajes, no monolitos

Cada pieza hace **una sola cosa**, vive en su propio archivo y se **compone** (se llama) desde
quien la necesita — nunca un archivo gigante que hace todo. Esto es **responsabilidad única +
composición**, y es la convención **ya establecida** en este repo: el scoring son 13 policies
atómicas (`domain/scoring/*Policy.kt`) que `ScoreEngine` solo **orquesta** (`ScoreEngine.kt:21-63`),
no calcula él. Las frases siguen el mismo molde:

| Engranaje | Una sola cosa | Reemplazable sin tocar el resto |
|---|---|---|
| `DayPhasePolicy` | hora → Dawn/Dusk | cambiar ventanas horarias |
| `AnchorPhraseSelector` | inputs → frase elegida | cambiar la fórmula de elección |
| `AnchorPhraseResolver` | orquestar leer/elegir/guardar | cambiar cuándo se persiste |
| `AnchorPhraseSeed` | datos del catálogo | editar frases/pesos |
| proyección (lookup) | slot → estado de UI | cambiar el mapeo a UI |
| `AnchorPhraseCard` | dibujar cita + autor | cambiar el visual |

Beneficio directo (lo que pediste): si mañana querés cambiar **solo** los pesos, tocás
`AnchorPhraseSeed`; si querés cambiar **solo** las franjas horarias, tocás `DayPhasePolicy`. Cero
efecto dominó. Eso es lo que evita la deuda técnica.

### 3.1 Selector puro (determinístico y testeable)

```kotlin
// domain/phrase/AnchorPhraseSelector.kt  (PURO, sin Room, sin suspend)
data class AnchorPhraseSelectorInput(
    val date: LocalDate,
    val dayPhase: DayPhase,
    val scoreState: ScoreState,
    val catalog: List<AnchorPhrase>,                 // activas, con authorReference
    val stateRules: List<AnchorPhraseStateRule>,     // peso por (phraseId, scoreState)
    val phaseRules: List<AnchorPhrasePhaseRule>,      // peso por (phraseId, dayPhase)
    val recentPhraseIds: Set<String>,                // mostradas en ventana de 7 días
)

object AnchorPhraseSelector {
    fun select(input: AnchorPhraseSelectorInput): AnchorPhraseSelection?
}
```

`select()` es **delgado**: solo compone funciones puras chiquitas, una por regla (de
`frases-ancla.md §6, §8, §9`) — igual que `ScoreEngine` compone policies:

| Función interna (un solo trabajo) | Regla |
|---|---|
| `filterEligible(catalog)` | `active && authorReference no vacío` |
| `filterByState(phrases, scoreState)` | solo familias permitidas (§6); **nunca** `Contemplation` fuera de `Plenitude`/`Unbreakable` (§8.6) |
| `excludeRecent(phrases, recentIds)` | ventana 7 días; si queda vacío, **relajar la ventana, no las reglas de estado** (§8.7) |
| `weightOf(phrase, state, phase)` | `pesoEstado(familia) + pesoFase(familia)` (§9) |
| `weightedPick(weighted, seed)` | elección ponderada con `Random(seed)`, `seed = hash(date, dayPhase)` → estable en la fase + reproducible en tests |

*(Context7 — kotlinlang: `Random(seed)` produce secuencias reproducibles → cada función se
testea aislada.)* Cada una entra a su propio caso de test; si una regla cambia, se toca una sola
función.

> **Por qué seed determinístico y no `Random()` global:** que el test pueda fijar la semilla y
> verificar la elección. Y que, ante el mismo (date, dayPhase, estado, catálogo), el resultado
> sea el mismo — refuerza la estabilidad sin depender solo del slot.

### 3.2 `DayPhasePolicy` (puro)

`frases-ancla.md §7`: `Dawn` = 05:00–14:59 local; `Dusk` = 15:00–04:59 local. Recibe la hora
local (inyectada), devuelve `DayPhase`. Sin geolocalización (§17).

### 3.3 `AnchorPhraseResolver` (coordinador, capa de datos)

Espejo de `WeeklyScoreSnapshotWriter`. Pseudocódigo:

```
suspend fun resolveForToday(today: LocalDate, now: LocalDateTime):
    phase   = DayPhasePolicy.phaseFor(now)
    slot    = dao.getAnchorPhraseDailySlot(today, phase.name)
    state   = dao.<snapshot semana actual>.state   # YA escrito por refreshCurrentWeeklyScoreSnapshot
    if slot != null && slot.scoreState == state.name:
        return                       # reusar: estabilidad dentro de la fase
    catalog       = dao.observeAnchorPhrases().first()  (o snapshot suspend)
    stateRules    = dao.getAnchorPhraseStateRules()
    phaseRules    = dao.getAnchorPhrasePhaseRules()
    recentIds     = dao.<impresiones últimos 7 días>(today-7 .. today)  # NUEVA query
    selection     = AnchorPhraseSelector.select(input)
    if selection == null: return     # fallback grácil: no escribir
    @Transaction:
        dao.upsertAnchorPhraseDailySlot(slot REPLACE)
        dao.upsertAnchorPhraseImpression(impresión nueva, id = UUID)
```

*(Context7 — Android Architecture: `@Insert(onConflict = OnConflictStrategy.REPLACE)` para el
slot idempotente; envolver slot+impresión en `@Transaction` para atomicidad.)*

**Disparadores** (igual que el mantenimiento diario):
- `runDailyMaintenance(date)` (`DashboardViewModel.kt:181`) — al abrir la app, **después** de
  `repository.refreshCurrentWeeklyScoreSnapshot(today)` (`:189`), para que `snapshot.state` ya
  esté fresco cuando el resolver lo lea.
- `onResumed()` (`DashboardViewModel.kt:173`) — **extender** para re-resolver si cambió la
  fecha **o la fase** (cruce de las 15:00 con la app en background).

**Nueva query DAO necesaria:** impresiones en rango de fecha (ventana 7 días). El resto del DAO
ya existe.

### 3.3.1 Wiring de DI (punto de inyección exacto)

Espejo literal del `WeeklyScoreSnapshotWriter` (`AutonomiaRepository.kt:66-67`):

```kotlin
// AutonomiaRepository.kt, junto al :66
private val anchorPhraseResolver = AnchorPhraseResolver(dao)   // o DaoAnchorPhraseDataSource(dao)

// método expuesto, espejo de refreshCurrentWeeklyScoreSnapshot (:340)
suspend fun resolveAnchorPhraseForToday(today: LocalDate, now: LocalDateTime) =
    anchorPhraseResolver.resolveForToday(today, now)
```

- **Fuente del `scoreState`:** `WeeklyScoreSnapshotEntity.state` (`Entities.kt:330`) de la semana
  actual. El resolver lee el snapshot recién refrescado — **no recalcula** desde los hechos.
- **Llamada:** en `runDailyMaintenance` (`DashboardViewModel.kt:181-190`), agregar
  `repository.resolveAnchorPhraseForToday(date, LocalDateTime.now())` **después** de
  `refreshCurrentWeeklyScoreSnapshot` (`:189`).

### 3.4 Lectura reactiva en el dashboard

- Nueva query DAO: `observeAnchorPhraseDailySlots(date): Flow<List<AnchorPhraseDailySlotEntity>>`.
- `DashboardRepository` expone `anchorPhraseSlotFlow(dateKey)`.
- En `DashboardViewModel` combinar el slot del día (ya se observa el catálogo).
- `DashboardProjection.selectAnchorPhrase(...)` se reemplaza por un **lookup puro**: tomar el
  slot de la fase actual, buscar `phraseId` en el catálogo, devolver `text` + `authorReference`.
  Si no hay slot todavía → fallback grácil (frase neutra de la app o vacío), **sin** la cita
  hardcodeada.
- `DashboardAnchorPhraseState` (`DashboardState.kt:43-46`): quitar el default Kierkegaard;
  default neutro/vacío.

*(Context7 — Compose: la pantalla colecta el `StateFlow` con `collectAsStateWithLifecycle`; ya
es el patrón del proyecto, sin cambios en `AnchorPhraseCard`.)*

### 3.5 Seed de las 83 frases — estrategia (NO un muro hardcodeado)

**Decisión:** seed en **Kotlin** (no JSON/asset), consistente con la convención del repo
(`DefaultSeeds` ya tiene `layers`, `activityDefinitions`, `abstinenceTracks` hardcodeados) +
seguridad en compilación (un typo de familia no compila, no falla en runtime) + son data
**canónica respaldada por doc** (`frases-ancla.md §13`), no data editable por el usuario. JSON
añadiría parsing, dependencia y modos de fallo en runtime para data estática → over-engineering.

**Pero estructurado para que NO sea repetición masiva** — dos técnicas:

1. **Helper builder para frases compactas** (mismo patrón que `activityDef(...)` y
   `abstinenceTrack(...)`, `DefaultSeeds.kt:210,237`): un `phrase(id, text, author, family,
   attribution)` → cada frase es **un one-liner**. 83 líneas legibles, no 83 bloques.

2. **Las reglas se GENERAN, no se escriben a mano.** Clave: en `frases-ancla.md §9` los pesos
   son **por familia**, no por frase. Entonces se definen **dos mapas chicos**
   (`stateWeights: Map<ScoreState, Map<PhraseFamily, Int>>` y
   `phaseWeights: Map<DayPhase, Map<PhraseFamily, Int>>`) y un builder recorre las 83 frases y
   **deriva** todas las filas de `anchor_phrase_state_rules` y `anchor_phrase_phase_rules` desde
   la familia de cada frase. Cero filas de regla escritas a mano.

**Aislamiento:** todo esto vive en un archivo dedicado `data/local/seed/AnchorPhraseSeed.kt`
(`object` con `phrases`, `stateRules`, `phaseRules` derivadas), referenciado desde
`DefaultSeeds`/`ensureSeeded`. Así no infla `DefaultSeeds.kt` y queda como un PR encadenado
aislado y revisable.

**Wiring:** en `ensureSeeded()` (`AutonomiaRepository.kt:244`): `upsert` siempre (idempotente,
mismo criterio que actividades — llega a instalaciones existentes sin pisar datos de usuario).

- **Invariante de seed (§15):** 83 frases activas, 0 con `authorReference` vacío, reglas
  derivadas coherentes con los mapas. Cubrir con test (`DefaultSeedsAnchorPhraseTest`).
- El seed es **data predeterminada, NO descartable** (`CLAUDE.md`): preservar como el catálogo
  de actividades.

---

## 4. Mejores prácticas aplicadas (Context7)

| Práctica | Fuente Context7 | Dónde se aplica |
|---|---|---|
| `@Insert(onConflict = OnConflictStrategy.REPLACE)` + `suspend` | Android Architecture (Room DAO) | seed idempotente, upsert de slot |
| `@Transaction` para escrituras compuestas atómicas | Android Architecture | slot + impresión juntos |
| Observar queries como `Flow` y derivar UiState (no almacenar estado derivado) | Android Architecture | slot del día reactivo |
| `collectAsStateWithLifecycle` para colectar `StateFlow` en Compose | Android Architecture | ya en uso, sin cambios |
| `Random(seed)` para aleatoriedad **reproducible** | kotlinlang | selección ponderada testeable |

---

## 5. Plan de implementación por slices (Strict TDD activo)

> **Strict TDD = enabled** (`CLAUDE.md` / engram `sdd-init/apk-personal`). Test runner:
> `testDebugUnitTest`. **Test primero, luego implementación.** Dominio puro = JUnit4 JVM.
> El cambio total supera holgadamente las **400 líneas** (solo el seed son ~83 frases + reglas)
> → **PRs encadenados** (skill `gentle-ai-chained-pr` / `chained-pr`), commits por unidad de
> trabajo (`work-unit-commits`: test + código + doc juntos).

| # | Slice (unidad de trabajo) | Archivos clave | Tests | ¿Compilar? |
|---|---|---|---|---|
| **1** | Enums + modelos dominio + política pura: `DayPhase`, `AnchorPhraseSelection`, `AnchorPhraseStateRule`, `AnchorPhrasePhaseRule` (NO existen en `Models.kt`) + `DayPhasePolicy` + mappers | `Models.kt`, `domain/phrase/DayPhasePolicy.kt`, `DomainMappers.kt` | `DayPhasePolicyTest` (ventanas, bordes 14:59/15:00) | sí (dominio) |
| **2** | **Migración de las 5 tablas** + cobertura | `data/AutonomiaDatabase.kt` (nueva migración), test | `MigrationTestHelper` (índices `index_<tabla>_<col>`) | **sí (crítico)** |
| **3** | Seed aislado: `AnchorPhraseSeed.kt` (83 frases vía helper `phrase(...)` + reglas **derivadas** de mapas familia→peso) + wiring `ensureSeeded` | `data/local/seed/AnchorPhraseSeed.kt` (nuevo), `DefaultSeeds.kt`, `AutonomiaRepository.kt:244` | `DefaultSeedsAnchorPhraseTest` (83 activas, 0 sin autor, reglas coherentes con mapas) | sí |
| **4** | Selector puro ponderado (`Random(seed)` determinístico) | `domain/phrase/AnchorPhraseSelector.kt` | `AnchorPhraseSelectorTest` (filtros por estado, contemplación gateada, ventana 7d, relajación, determinismo por seed, pesos) | sí |
| **5** | Resolver coordinador + queries DAO nuevas + wiring mantenimiento/onResumed | `data/phrase/AnchorPhraseResolver.kt`, `AutonomiaDao.kt`, `AutonomiaRepository.kt`, `DashboardViewModel.kt:173,181` | `AnchorPhraseResolverTest` (reusa slot si estado igual, re-elige si cambia, escribe impresión, transaccional) | **sí (wiring/DI)** |
| **6** | Integración dashboard: flow del slot + lookup en proyección + quitar hardcode | `DashboardRepository.kt`, `DashboardViewModel.kt`, `DashboardProjection.kt:364`, `DashboardState.kt:44` | `DashboardProjectionTest` (mapea slot→estado, fallback grácil sin cita hardcodeada) | sí |
| **7** | Doc viva | `docs/dominio/frases-ancla.md` (§17/§18: marcar implementado) | — | no |

> El orden respeta dependencias: políticas → migración → seed → selector → resolver → UI → doc.
> Cada slice es un PR encadenado verificable (< 400 líneas, salvo el seed que se aísla solo).

---

## 6. Skills referenciadas

| Skill | Cuándo en este plan |
|---|---|
| `sdd-spec` / `sdd-design` / `sdd-tasks` | formalizar este plan en el flujo SDD (auto) — el spec ya está en `frases-ancla.md` |
| `sdd-apply` | implementar cada slice (Strict TDD: test primero) |
| `sdd-verify` | validar contra spec + capas de `meta/guias/verificacion-por-capas.md` |
| `work-unit-commits` | commits por unidad: test + código + doc juntos |
| `gentle-ai-chained-pr` (alias `chained-pr`) | dividir en PRs encadenados (>400 líneas por el seed) |
| `branch-pr` | crear los PRs |
| `verify` / `run` | si se pide validación en dispositivo (no obligatorio por defecto, `AGENTS.md`) |
| `cognitive-doc-design` | actualizar `frases-ancla.md` (slice 7) |

> No aplican: `compose-canvas-icons` (no hay iconos nuevos), `go-testing` (es Go).

---

## 7. Definición de Terminado (gates obligatorios)

- [ ] Slices 1–6 con tests en verde (`testDebugUnitTest`).
- [ ] **Build debug compila** (cambios no triviales: Room/migración/DI — `CLAUDE.md` obliga a compilar).
- [ ] Migración de las 5 tablas cubierta con `MigrationTestHelper` (índices `index_<tabla>_<col>`).
- [ ] Seed: 83 frases activas, 0 sin `authorReference`.
- [ ] Capas aplicables de `meta/guias/verificacion-por-capas.md` en verde.
- [ ] `docs/dominio/frases-ancla.md` actualizado (deja de contradecir al código).
- [ ] Frase hardcodeada de `DashboardState.kt` eliminada.

---

## 8. Fuera de alcance (de `frases-ancla.md §17`)

- Editor de frases propias · personalización manual de familias · explicación visible de
  desbloqueos · rotación por cada apertura · geolocalización para amanecer/atardecer real ·
  algoritmo complejo por capa más baja/riesgo/sobriedad.

---

## 9. Riesgos / decisiones abiertas menores

1. ~~**Fuente del `scoreState` en el resolver**~~ ✅ **RESUELTO:** el resolver lee
   `WeeklyScoreSnapshotEntity.state` (`Entities.kt:330`) de la semana actual, ya refrescado por
   `refreshCurrentWeeklyScoreSnapshot` (`DashboardViewModel.kt:189`) justo antes. No recalcula.
   *Único supuesto a confirmar en slice 5:* que el estado del snapshot de la semana actual
   coincide con el estado que muestra el dashboard (ambos vía `ScoreEngine`; deberían igualar).
2. **"Cambio significativo de estado" dentro de la fase (§8.3):** v1 = re-elegir si
   `slot.scoreState != currentState`. Política simple y suficiente para MVP.
3. **Cruce de fase con app abierta:** resolución en `onResumed` cubre el caso al volver del
   background; cruce con la app en primer plano se resuelve en la próxima apertura (aceptable v1).
