# Tasks — `scoring-arranque-cuenta`

> Checklist de implementación. Orden estricto por dependencia. 3 lotes, cada uno
> < 400 líneas y entregable en aislamiento. **Strict TDD ACTIVO** (`testDebugUnitTest`):
> por CADA tarea de lógica, primero el TEST (RED) → implementación (GREEN) → refactor.
> Dominio puro JVM. Camino A (sin migraciones Room). Persistencia NO se toca.

## Reconciliación cerrada (spec gana sobre design en R3)

Los términos de superhábit de ventana usan **`f_eff = min(f, windowDays)`** (versión SPEC),
NO `f` crudo. Esto garantiza `wt = (f_eff/windowDays)^κ ≤ 1` por construcción y evita
`wt>1`/NaN. `phi` y `cut` mantienen `f` crudo (versión DESIGN). Guard de `sd`:
`if (f_eff < windowDays) v/(windowDays - f_eff) else 0.0`.

Fórmula efectiva a implementar en `rFromRatios(f, dayRatios, windowDays=7)`:

```
N      = windowDays.coerceIn(1, 7)
f_eff  = min(f, N)
phi    = commit.sumOf{u} / f          # f CRUDO (sin cambio)
cut    = min(d, f)                     # f CRUDO (sin cambio)
st     = commit.sumOf{max(it-1,0)} / f # f CRUDO (sin cambio)
sd     = if (f_eff < N) v/(N - f_eff) else 0.0   # f_eff, guard
wt     = (f_eff / N).pow(κ)                       # f_eff, ≤ 1 siempre
```

Invariante de regresión: con `N=7`, `f_eff=min(f,7)=f` para todo `f∈[2,7]` → byte-idéntico
al actual. Es un TEST obligatorio.

---

## Lote 1 — Núcleo: `rFromRatios` con `windowDays`

Capability: `anchor-scoring`. Archivo único de producción: `AnchorScoringPolicy.kt`.
100% dominio puro JVM. **Sin dependencia de los lotes 2 y 3.**

- [x] **1.1 (TEST-RED) Regresión byte-idéntica `windowDays` default ≡ explícito 7**
  → Req: "Default windowDays=7 es byte-idéntico" · Scenarios f=3 superhábit días, f=7 sin sd.
  - Archivo test: `app/src/test/java/dev/panopt/autonomia/domain/scoring/AnchorScoringWindowDaysTest.kt` (nuevo).
  - Assert: para batería `(f, dayRatios)` representativa (f∈{2,3,4,7}, ratios variados),
    `rFromRatios(f, ratios) == rFromRatios(f, ratios, 7)` byte-idéntico (`assertEquals` Double sin delta).
  - Debe FALLAR a compilar (parámetro `windowDays` aún no existe) → RED válido.

- [x] **1.2 (GREEN) Agregar `windowDays: Int = 7` y generalizar `sd`/`wt` con `f_eff`**
  → Req: "windowDays<7 reparte el superhábit" + "f ≥ N no produce div/0".
  - Archivo: `AnchorScoringPolicy.kt` L39 (firma), L65-68 (`sd`/`wt`).
  - L39: `fun rFromRatios(f: Int, dayRatios: List<Double>, windowDays: Int = 7): Double`.
  - Al inicio del cuerpo: `val n = windowDays.coerceIn(1, 7)`; `val fEff = min(f, n)`.
  - L67: `val sd = if (fEff < n) v / (n - fEff).toDouble() else 0.0`.
  - L68: `val wt = (fEff.toDouble() / n.toDouble()).pow(kappa)`.
  - `phi` (L60), `cut` (L53), `st` (L66): SIN tocar (siguen `f` crudo).
  - Corre 1.1 → GREEN.

- [x] **1.3 (TEST) Suite existente `AnchorScoringPolicyTest` verde sin modificarse**
  → Req: "La suite existente queda verde sin tocarse" (cero regresión).
  - NO editar `AnchorScoringPolicyTest.kt`. Ejecutar
    `--tests 'dev.panopt.autonomia.domain.scoring.AnchorScoringPolicyTest'` → todo verde.

- [x] **1.4 (TEST) Ventana parcial reparte superhábit sobre N (f<N y f=N)**
  → Req: "windowDays<7 reparte el superhábit" · Scenarios f=3/N=4 (R=1.0) y f=2/N=4 (sd=1.0).
  - En `AnchorScoringWindowDaysTest.kt`.
  - f=3, ratios=[1,1,1], N=4 → R == 1.0 (tolerancia 1e-9): sin castigo por día 4 no vivido.
  - f=2, ratios=[1,1,1,1], N=4 → `sd = 2.0/(4-2) = 1.0`, `wt=(2/4)^κ`; assert R>base y finito.

- [x] **1.5 (TEST) Guard f≥N: sin div/0, `wt≤1`, sin NaN/Infinity**
  → Req: "f ≥ N no produce división por cero ni peso fuera de rango" · Scenarios f=5/N=2, f=7/N=1, vacío.
  - f=5, ratios=[1,0.8], N=2 → `fEff=2`, `sd=0.0`, `wt=(2/2)^κ=1.0`; `assertFalse(R.isNaN()/isInfinite())`.
  - f=7, ratios=[1], N=1 → `fEff=1`, `sd=0.0`, `wt=1.0`; R finito.
  - f=3, ratios=[], N=4 → R == 0.0 (guarda temprana `d==0`).
  - Assert transversal: para todo `(f∈2..7, N∈1..6)` con ratios válidos, R ∈ [0, 1.5] y finito.

- [x] **1.6 (TEST) Clamp `windowDays` a [1,7]**
  → Req: "windowDays se clampa a [1,7]" · Scenarios >7→7, ≤0→1.
  - f=3, ratios=[1,1,1]: `rFromRatios(3, r, 9) == rFromRatios(3, r, 7)`.
  - f=3, ratios=[1]: `rFromRatios(3, r, 0) == rFromRatios(3, r, 1)`.

- [x] **1.7 (DOC vivo) Documentar `windowDays` en modelo-matemático**
  → Success criterion: doc vivo actualizado.
  - `docs/scoring/modelo-matematico-nucleo-v1.md` §NIVEL 1: documentar el parámetro
    `windowDays` (default 7 = semana madura; `f_eff=min(f,N)` en `sd`/`wt`; `phi`/`cut`/`st` crudos).
  - (Trivial-doc: no requiere test ni build.)

**Cierre Lote 1:** `assembleDebug` verde + toda la suite de scoring verde. Entregable aislado
(el motor maduro queda byte-idéntico; nadie pasa `windowDays≠7` todavía).

---

## Lote 2 — Dominio de arranque

Capability: `startup-counter` (dominio). **Depende del Lote 1** (`rFromRatios` ya acepta
`windowDays`). Archivos nuevos en `domain/scoring/` + 2 cambios aditivos en use cases existentes.

- [x] **2.1 (GREEN-aditivo) `BuildScoreInputUseCase` gana `includeGraceAnchors=false`**
  → Req: "Proyección corre el motor con windowDays=d y sin filtrar gracia" · ADR-3.
  - Archivo: `BuildScoreInputUseCase.kt` L7 (firma), L16-19 (guard del `filterNot`).
  - L7: `operator fun invoke(source: ScoreInputSource, includeGraceAnchors: Boolean = false)`.
  - L16-19: agregar `!includeGraceAnchors &&` como primer término del `filterNot`.
  - Cambio aditivo y neutro: default `false` = camino maduro byte-idéntico (lo cubre la suite del Lote 1).
  - (Sin test propio nuevo: el comportamiento `false` lo verifica la regresión existente;
    el `true` se ejercita en 2.3.)

- [x] **2.2 (GREEN-refactor neutro) Seam `ScoreEngine.calculateProjection(input, windowDays)`**
  → Req: "Proyección corre el motor con windowDays=d" · ADR-4.
  - Archivo: `ScoreEngine.kt` L36-78.
  - Extraer `private fun calculateInternal(input, windowDays: Int): ScoreReport` con el pipeline actual;
    propagar `windowDays` al único punto que resuelve anclas: L78
    `rFromRatios(window.f, ratios)` → `rFromRatios(window.f, ratios, windowDays)`.
  - `fun calculate(input) = calculateInternal(input, windowDays = 7)` (firma pública intacta).
  - `internal fun calculateProjection(input, windowDays) = calculateInternal(input, windowDays)`.
  - Refactor neutro: la suite existente de `ScoreEngineTest` debe quedar verde sin tocarse (regresión).

- [x] **2.3 (TEST-RED→GREEN) `StartupDetectionRule.isStartup(...)`**
  → Req: "Detección de arranque por historial sin score real + gate de cobertura".
  - Test: `app/src/test/java/dev/panopt/autonomia/domain/scoring/StartupDetectionRuleTest.kt` (nuevo).
    Los 4 scenarios de la spec:
    - historial vacío + 3 capas en gracia → `true`.
    - historial con 1 entrada `state != NoData` → `false` (ya maduró).
    - 2 capas con ancla (< MIN=3) → `false` (gate manda, NoData real).
    - historial 6× `NoData` + 3 capas → `true`.
    - extra: `report.state != NoData` → `false` (solo aplica sobre NoData real).
  - Prod: `domain/scoring/StartupDetectionRule.kt` (object puro). Firma del design §2.1
    (`report, activities, layers, weeklyHistory, today`). Cuenta capas con ≥1 ancla activa
    no-archivada SIN filtrar gracia; exige `≥ ScoringConstants.MIN_ACTIVE_LAYERS_WITH_ANCHOR`.

- [x] **2.4 (TEST-RED→GREEN) `StartupProjectionUseCase(source, windowDays)`**
  → Req: "Proyección corre el motor con windowDays=d y sin filtrar gracia" + invariante "no muta el maduro".
  - Test: `StartupProjectionUseCaseTest.kt` (nuevo).
    - 3 anclas en gracia, d=4: la proyección llama `BuildScoreInputUseCase(..., includeGraceAnchors=true)`
      + `calculateProjection(input, windowDays=4)` → `projectedEstado > 0` (las anclas en gracia aportan).
    - El `ScoreReport` REAL (`BuildScoreInputUseCase(source)` default + `calculate`) sigue `NoData`
      sobre los mismos hechos (la proyección no muta el camino maduro).
    - Si ni con gracia alcanza el gate → `invoke` devuelve `null`.
  - Prod: `domain/scoring/StartupProjectionUseCase.kt` (object; orquesta pero JVM puro, sin Room/IO).
    Devuelve `StartupProjection(estado: Double, windowDays: Int)?` (data class en el mismo archivo).

- [x] **2.5 (TEST-RED→GREEN) `StartupCounterPolicy.counter(projectedEstado, daysLived)`**
  → Req: "Contador = scoreProyectado × d/7 con clamp de d".
  - Test: `StartupCounterPolicyTest.kt` (nuevo). Atenuación `× d/7` (verifica sobre el ESTADO,
    luego mapeado por `PointsMappingPolicy.points`):
    - d=1 → `windowProgress=1/7`, `daysRemaining=6`; contador = points(estado×1/7).
    - d=4 → `windowProgress=4/7`, `daysRemaining=3`.
    - d=7 → `windowProgress=1.0`, `daysRemaining=0`; contador = points(estado) (sin atenuar).
    - d=9 → clamp a 7: `windowProgress=1.0`, `daysRemaining=0`.
    - `projectedEstado=0`, d=3 → `counterPoints = points(0)`, `daysRemaining=4`; card activa (no NoData).
  - Prod: `domain/scoring/StartupCounterPolicy.kt` (object puro) + `data class StartupCounter`
    (`counterPoints`, `daysLived`, `daysRemaining`, `windowProgress`). Usa `AnchorGraceRule.GRACE_DAYS`
    como divisor (= 7, fuente única) y `PointsMappingPolicy.points`.

- [x] **2.6 (TEST obligatorio) No-salto día 7→8 (convergencia)**
  → Req: "No-salto día 7→8 (convergencia con el score maduro)".
  - Test: en `StartupProjectionUseCaseTest.kt` (o `StartupConvergenceTest.kt`).
  - Con un set FIJO de hechos de anclas para 7 días:
    - día 7: `counter(projection(windowDays=7).estado, daysLived=7)` → atenuación `×7/7`.
    - día 8: motor maduro normal (anclas fuera de gracia) `windowDays=7`.
  - Assert: `counterPoints(día7) == scoreMaduroPoints(día8)` con **tolerancia exacta = 0 puntos**
    sobre los MISMOS hechos sin desplazar la ventana (el rodaje de 1 día de la ventana móvil se
    neutraliza usando el mismo conjunto de 7 días en ambos cálculos). Si el test compara con
    ventana desplazada, la tolerancia es el delta de 1 día de hechos, no un escalón → documentar
    el assert como `|counter7 - maduro8| ≤ deltaUnDíaDeHechos` y, en el caso de hechos idénticos
    en el día rodado, `== 0`.

- [x] **2.7 (TEST) Invariante de persistencia (no escribe snapshot)**
  → Req: "Persistencia NO se toca" · ADR-7.
  - Verificar (test de dominio, sin Room): en arranque el `ScoreReport` real producido por
    `ScoreEngine.calculate` es `NoData` → `visibleScore=0` (lo que el writer persistiría hoy).
    Ningún componente nuevo de arranque expone escritura. (No hay clase nueva que tocar Room → el
    test es de no-regresión del `ScoreReport` real, ya cubierto en 2.4; marcar explícito aquí.)

**Cierre Lote 2:** `assembleDebug` verde + suite de scoring verde (incluida regresión de
`ScoreEngineTest`/`AnchorScoringPolicyTest`). Entregable aislado: el dominio de arranque existe y
está testeado, pero NADIE lo consume todavía (sin wiring de UI). El dashboard se comporta como hoy.

---

## Lote 3 — Proyección + UI

Capability: `startup-counter` (presentación). **Depende del Lote 2**. Wiring + Compose + docs.

- [x] **3.1 (GREEN) `StartupCardState` + campo `DashboardState.startup`**
  → Req: "Canal de presentación separado (DashboardState.startup)" · §5.1, §5.2.
  - Archivo: `domain/dashboard/DashboardState.kt` (L23-24).
  - `internal data class StartupCardState(counterLabel, counterPoints, windowProgress, daysRemaining,
    daysRemainingLabel, headline, body)`.
  - `DashboardState` gana `val startup: StartupCardState? = null` (nullable → rollback gratis).

- [x] **3.2 (TEST-RED→GREEN) `DashboardProjection` computa `startup`**
  → Req: "Canal de presentación separado" · Scenarios arranque→startup!=null & NoData; madura→null; <3 capas→null+NoData.
  - Test: `app/src/test/java/dev/panopt/autonomia/domain/dashboard/DashboardProjectionStartupTest.kt` (nuevo).
    - cuenta en arranque (3 anclas en gracia, historial vacío) → `state.startup != null` (counter,
      daysRemaining) **Y** `state.scoreState == NoData`.
    - cuenta madura (≥1 score real en weeklyHistory) → `state.startup == null` y usa scoreState real.
    - <3 capas con ancla → `state.startup == null` y `scoreState == NoData` ("configurá tu base").
  - Prod: `domain/dashboard/DashboardProjection.kt` (~L101-151).
    - Extraer el `ScoreInputSource(...)` inline (L103-119) a un `val scoreInputSource` (refactor neutro, reuso).
    - Tras `scoreReport`: `val startup = if (StartupDetectionRule.isStartup(...)) { daysLived →
      StartupProjectionUseCase(scoreInputSource, daysLived) → StartupCounterPolicy.counter(...) →
      toStartupCardState() } else null`. Helper `startupDaysLived(anchorActivities, today)` (design §3.4:
      `ChronoUnit.DAYS.between(oldestCreatedAt, today)+1`, clamp [1,7]).
    - Mapeo de copy (`daysRemainingLabel` singular/plural, `headline`, `body`) con tono compasivo (AGENTS.md):
      "Faltan N días para tu puntaje real" / "La base está cargando". Sin términos prohibidos.
    - `return DashboardState(..., startup = startup)`.

- [x] **3.3 (GREEN) `StartupStatusCard` (Compose hermano)**
  → Req: "UI — StartupStatusCard separado, StatusCard intacto" · §7.
  - Archivo nuevo: `ui/dashboard/components/StartupStatusCard.kt`.
  - `@Composable internal fun StartupStatusCard(palette, startup: StartupCardState)`.
  - Número central: `animateIntAsState(startup.counterPoints)`. Arco d/7: `animateFloatAsState(startup.windowProgress)`.
  - Reusa `ScoreOrbit` pasándole los valores animados (misma forma que `StatusCard`). Color cálido
    propio vía `mix(palette.colorCoral, 0.35f, palette.colorCardboard)` (design §7.3, sin token nuevo).
  - **Sin lógica de negocio**: recibe `StartupCardState` ya resuelto, solo presenta/anima.
  - (Compose: no se le exige test unitario JVM; la capa visual se valida en runtime — `verificacion-por-capas.md`.)

- [x] **3.4 (GREEN) Dashboard elige card según `startup != null`**
  → Req: "UI — Dashboard elige StartupStatusCard cuando startup != null" · §6.
  - Archivo: `ui/dashboard/DashboardScreen.kt` L114.
  - `if (state.startup != null) StartupStatusCard(palette, state.startup) else StatusCard(palette, state.status)`.
  - `StatusCard` y `ScoreOrbit` NO se tocan (su código fuente queda idéntico).

- [x] **3.5 (DOC vivo) Actualizar docs de scoring y frontend**
  → Success criterion: docs vivos al día.
  - `docs/scoring/modelo-matematico-nucleo-v1.md`: confirmar `windowDays` (si no quedó del 1.7) + nota arranque.
  - `docs/scoring/modelo-scoring-oficial-v1.md`: describir la barra de arranque como canal de
    presentación (no estado del motor; `ScoreReport` real sigue NoData).
  - `docs/frontend/frontend-design.md`: documentar `StartupStatusCard` (forma, color cálido derivado).

**Cierre Lote 3 (y del cambio):** `assembleDebug` verde + suite de scoring/dashboard verde. Capa
runtime (`verificacion-por-capas.md`): install limpio → usuario nuevo con 3 anclas ve contador
0→score proyectado, NO "Sin datos"; transición día 7→8 sin salto visual.

---

## Dependencias y paralelismo

```
Lote 1 (1.1→1.2→{1.3,1.4,1.5,1.6}, 1.7 doc)
   │  (rFromRatios acepta windowDays)
   ▼
Lote 2:  2.1 ─┐
         2.2 ─┤→ 2.3, 2.4 → 2.5 → 2.6, 2.7
   │  (dominio de arranque testeado)
   ▼
Lote 3:  3.1 → 3.2 → 3.4
              3.3 ──┘     3.5 doc
```

- **Secuencial entre lotes:** Lote 2 necesita el `windowDays` del Lote 1; Lote 3 necesita el
  dominio del Lote 2. NO se pueden solapar lotes.
- **Paralelizable DENTRO del Lote 1:** tras 1.2, las tareas de test 1.3/1.4/1.5/1.6 son
  independientes entre sí (misma firma, distintos asserts); 1.7 (doc) es independiente de todo.
- **Paralelizable DENTRO del Lote 2:** 2.1 y 2.2 son cambios aditivos independientes (distintos
  archivos: `BuildScoreInputUseCase` vs `ScoreEngine`); 2.3 es independiente de 2.4/2.5. 2.4
  depende de 2.1+2.2. 2.5 depende de 2.4 (consume `projectedEstado`). 2.6/2.7 al final.
- **Paralelizable DENTRO del Lote 3:** 3.1 primero (define el tipo). 3.2 (dominio) y 3.3 (Compose
  card) pueden ir en paralelo tras 3.1. 3.4 necesita 3.3. 3.5 (docs) independiente.

---

## Review Workload Forecast

| Lote | Archivos prod | Archivos test | Líneas estimadas (prod+test) | Riesgo 400 |
|------|---------------|---------------|------------------------------|------------|
| 1 | `AnchorScoringPolicy.kt` (~6 líneas netas) + doc | `AnchorScoringWindowDaysTest.kt` (~120) | ~160 | Bajo |
| 2 | `BuildScoreInputUseCase` (+2) + `ScoreEngine` (refactor ~30) + 3 objects nuevos (~150) | 3-4 test files (~220) | ~380 | **Medio-Alto** (cerca de 400) |
| 3 | `DashboardState` (+12) + `DashboardProjection` (~40) + `StartupStatusCard.kt` (~110) + `DashboardScreen` (+4) + 3 docs | `DashboardProjectionStartupTest.kt` (~110) | ~290 | Bajo-Medio |

- **Chained PRs recommended:** Sí — los 3 lotes ya están diseñados como PRs encadenados
  independientes (cada uno compila y queda verde solo). Recomendado 1 PR por lote.
- **400-line budget risk:** Medio (Lote 2 es el más pesado, ~380 líneas). Si al implementar 2.2
  el refactor de `ScoreEngine` crece, **partir Lote 2** en 2a (seams aditivos: 2.1+2.2) y 2b
  (objects de arranque: 2.3–2.7).
- **Decision needed before apply:** Sí — confirmar estrategia de entrega (chained vs single-PR
  con `size:exception`) antes de arrancar el apply, por el riesgo del Lote 2.

---

## Trazabilidad task → requirement

| Task | Capability · Requirement |
|------|--------------------------|
| 1.1, 1.3 | anchor-scoring · Default windowDays=7 byte-idéntico |
| 1.4 | anchor-scoring · windowDays<7 reparte superhábit sobre ventana |
| 1.5 | anchor-scoring · f ≥ N sin div/0 ni peso fuera de rango |
| 1.6 | anchor-scoring · windowDays se clampa a [1,7] |
| 1.2 | anchor-scoring · (implementación de los 3 anteriores) |
| 1.7, 3.5 | (docs vivos — success criteria) |
| 2.1, 2.2, 2.4 | startup-counter · Proyección con windowDays=d sin filtrar gracia |
| 2.3 | startup-counter · Detección por historial + gate de cobertura |
| 2.5 | startup-counter · Contador = scoreProyectado × d/7 con clamp |
| 2.6 | startup-counter · No-salto día 7→8 (convergencia) |
| 2.7 | startup-counter · Persistencia NO se toca (invariante) |
| 3.1, 3.2 | startup-counter · Canal de presentación separado (DashboardState.startup) |
| 3.3, 3.4 | startup-counter · UI — StartupStatusCard separado, StatusCard intacto |
