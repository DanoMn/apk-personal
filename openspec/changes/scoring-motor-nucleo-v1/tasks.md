# Tasks: scoring-motor-nucleo-v1 — portar el modelo de scoring cerrado

Fuentes de verdad: `docs/scoring/modelo-matematico-nucleo-v1.md` (7 niveles + §0.1),
`docs/scoring/axiomas-modelo-scoring-v1.md`, `docs/scoring/verificacion_modelo_oficial.py`
(27 asserts), y los 4 delta specs en `specs/`.

## Convenciones de ejecución (leer antes de empezar)

- **Strict TDD ACTIVO.** Cada task de lógica es un par `[ ] TEST` + `[ ] CÓDIGO`: el test
  se escribe y se ve ROJO antes de escribir el código. Runner:
  `gradlew.bat testDebugUnitTest --tests 'dev.panopt.autonomia.domain.scoring.*'`.
- **Cálculo interno en `Double`**, `Float` solo en la frontera (`ScoreReport.estado`).
  Asserts del motor en Double, tolerancia `1e-9` (`±0.001` para casos §1.4, `±1`/`±2`/`±3`
  para puntos según el escenario).
- **Esquema Room NO cambia** (Camino A). El adapter solo re-forma hechos existentes.
- **Mapeo a puntos va en la PROYECCIÓN**, no en el motor. El motor emite `estado ∈ [0,1.5]`.
- **Work-unit commit** = test + código + doc (si aplica) JUNTOS por unidad, conventional
  commits en inglés, sin atribución IA.
- **Las constantes nunca se hardcodean** en las policies — se leen de `ScoringConstants`
  (en particular el exponente del gate es el parámetro `p`, nunca el literal `2`).
- Marcar `[x]` solo cuando el test pasa verde y el doc vivo afectado (si lo hay) quedó al día.
- `[P]` = puede correr en paralelo con sus hermanos del mismo grupo (sin dependencia entre sí).

---

## PR-A — ScoringConstants + NIVEL 1 (ancla)

> Corte limpio. Habilita todo el resto. Depende solo de los docs.
>
> **Decisión de convivencia (apply PR-A):** para mantener el build verde y la suite vieja
> intacta (restricción dura: el código nuevo CONVIVE con el viejo hasta PR-F), los 17 parámetros
> §0.1 viven en un objeto NUEVO `ScoringConstantsV2` (no se reescribió `ScoringConstants` ni se
> borraron sus constantes viejas — eso es PR-F 1.2/11.3). El NIVEL 1 nuevo es un objeto NUEVO
> `AnchorScoringPolicyV2.r(f,t,mins): Double`; el `AnchorScoringPolicy.evaluate` viejo (consumido
> por `LayerScoringPolicy`) queda intacto. La unificación de nombres es PR-F.

### 1. ScoringConstants (17 parámetros §0.1)
- [x] 1.1 Crear `domain/scoring/ScoringConstantsV2.kt` con los 17 parámetros calibrados de
  `§0.1`: `G_` (γ), `LV` (λ_v), `KP` (κ), `P` (gate), `SMAX`, `S0`, `WS` (0.07), `TAU` (0.06),
  `N0`, `RG` (r=0.5 votos), `RHO` (ρ=0.15), `W0` (1.0), `BETA` (0.818), `A` (0.55), `B_SLEEP`,
  `DELTA` (δ=0.10), cortes de banda `0.40/0.62/0.85/1.10`, e hitos del mapeo de puntos (piso
  650, tope 1100, 5 hitos `(c,w,A)`). [→ satisface: core-engine §"17 constantes", points-mapping
  §"Constantes", base-state-policy §"Cortes en ScoringConstants"] (objeto NUEVO, no reescritura;
  ver decisión de convivencia arriba)
- [ ] 1.2 Eliminar las constantes del modelo viejo: `WORST_LAYER_*`, `UNBREAKABLE_*`,
  `ANCHOR_FREQUENCY/VALUE_WEIGHT`, `SUPPORT_WEIGHT`, `SLEEP_WEIGHT_IN_BODY`, `SOBRIETY_*`,
  `WEEKLY_AVERAGE/WORST_WEIGHT`, `TASK_MOMENTUM_MAX`, `STATE_HYSTERESIS_MARGIN`. **(Diferido a
  PR-F: hay callers vivos; borrarlas ahora rompe el build. Se eliminan junto a las policies en
  PR-F 11.x.)**

### 2. NIVEL 1 — Ancla `R(F,T,mins)`
- [x] 2.1 [TEST] `AnchorScoringPolicyV2Test.kt` (nuevo) traduciendo los asserts de ancla del
  Python: AN1 (rango acotado), AN2 (D=0→0), AN3 (cumplir-justo=1.0 para F=4 y F=7), AN6 (gate
  base²: `[60,60]` extra=0 vs `[60,60,60,60]` extra>0), AN7 (superhabit tiempo y días), AN8
  (monotonía día extra), AN10 (invarianza de escala), AN11 (continuidad ≤0.02), y §1.4
  (`F=3,T=30,[30,30,30]→1.000`; `[60]*4→1.289`; `[30]*6→1.266`; `[60,60]→0.544`; `[30]*7→1.0`;
  `[45]*7→1.31606`; `[120]*7→1.499`, ±0.001). En `Double`, tol `1e-9`/`±0.001`. Visto rojo.
  [→ core-engine §"NIVEL 1"]
- [x] 2.2 [CÓDIGO] `AnchorScoringPolicyV2.kt` (nuevo): `r(f,t,mins): Double` = Best-F
  (`u(r)=min(r,1)^γ`, `φ`, `V`, `base=1−(1−φ)·exp(−λ_v·V)`, superhabit `St`/`Sd` fundidos por
  `wt=(F/7)^κ` saturado a `smax`, `R = base + base^p·S`). Gate `base.pow(P)` con `P` de
  constantes. Verde.

---

## PR-B — NIVELES 2–3 (valor de capa, soportes, tasks, pesos)

> Depende de PR-A (constantes + ancla). Niveles 2 y 3 son independientes entre sí salvo que
> ambos consumen `R`; los tests de cada uno pueden escribirse en paralelo.

### 3. NIVEL 2 — Valor de capa (dos canales) + soportes + tasks
- [x] 3.1 [TEST] `LayerValuePolicyTest.kt` (nuevo): VC3/VC4 (3 anclas, una 1.5 dos 1.0 →
  `extra_capa = 0.1667`; anclas pesan igual), SO2 (blend bidireccional: descuidado < sin
  soporte < sostenido), SO4 (1 soporte = 5 soportes a igual cumplimiento, ±1e-9), TA5 (ancla
  `J` + `n_tasks=100` → banda NO Inquebrantable, ESTADO≈1.06 Plenitud), TA-suma (task nunca
  resta: `t1 ≥ t0`) + TA3 (efímera) + task_lift tope ≈TAU. Visto rojo (unresolved ref) → verde.
  [→ core-engine §"NIVEL 2"]
- [x] 3.2 [CÓDIGO] `LayerValuePolicy.kt` (NUEVO, no reescritura — convivencia PR-B): dos canales
  `baseAnchors = (1/n)·Σ min(R_i,1)`, `extraLayer = (1/n)·Σ max(R_i−1,0)`; blend soportes
  `baseEff = (1−WS)·baseAnchors + WS·G` con `G = supportSignal = promedio(min(días/4,1))` (si no
  hay anclas, `baseEff = G`); tasks saturación conjunta `extraFinal` (techo `TAU`, gate `base_eff^P`
  con `P` de constantes, conteo efímero `n_hoy`). Las policies viejas (`LayerScoringPolicy`,
  `LayerContributionPolicy`, `SupportScoringPolicy`, `TaskMomentumPolicy`) quedan INTACTAS — su
  reescritura/borrado es PR-F. Verde.

### 4. NIVEL 3 — Peso de capa (votos)
- [x] 4.1 [P][TEST] `LayerWeightPolicyTest.kt` (nuevo): PC2 (`peso(1)=1.0`, `peso(2)=1.5`,
  `peso(3)=1.75`, `peso(50)<2.0`), PC3 (3 capas `50/1/1` → fracción de la saturada ≤0.50),
  PC5 (capa solo-soportes `n=0` → `peso=ρ=0.15`). Visto rojo (unresolved ref) → verde.
  [→ core-engine §"NIVEL 3"]
- [x] 4.2 [P][CÓDIGO] `LayerWeightPolicy.kt` (NUEVO, convivencia PR-B):
  `votes(n) = (1−r^n)/(1−r)` con `r=RG`; `ρ=RHO` para `n=0`. El peso `W0` de capa solo-opt-in
  se decide en la agregación (NIVEL 5, PR-C), no aquí. Verde.

---

## PR-C — NIVELES 4–6 (opt-ins, agregación, bandas)

> Depende de PR-B (valor de capa + pesos). El agregado del NIVEL 5 consume opt-ins (4) y
> alimenta bandas (6). NIVEL 6 (bandas) es independiente del 4/5 y su test puede ir en paralelo.

### 5. NIVEL 4 — Opt-ins (señal M + término-sombra)
- [x] 5.1 [TEST] `OptInPolicyTest.kt` (nuevo): O2/C2 (neutralidad `M=1` ⟹ `w=0`, aun con
  déficit), I2/O11 (capa solo-opt-in con `M=1.0` pesa `W0=1` → ESTADO=1.0), señal de sobriedad
  `M_sobr = Π(1−A)^días` (track limpio → 1; multi-track compone sin tope). (AG2/O3 e I1 se
  validan en el NIVEL 5 porque necesitan el agregado.) Ver rojo. [→ core-engine §"NIVEL 4",
  facts-adapter §"Tracks"] (O2/C2 e I2/O11 se trasladaron a `StateAggregationPolicyTest` porque
  exigen la bolsa-global; en `OptInPolicyTest` quedan las piezas atómicas: `shadowTerm` y `M_sobr`.)
- [x] 5.2 [CÓDIGO] `OptInPolicy.kt` (nuevo): `shadowTerm(M, Σpesos) = BETA·Σpesos·(1−M)`
  (escala con `Σpesos`, NO con `N`); `M` clampeado a `[0,1]`; `M=null` → opt-in inactivo.
  Señal `M_sobr = Π(1−A)^días` (`sobrietySignal`). `SobrietyScoringPolicy.kt` viejo NO se reescribe
  (convivencia PR-A/PR-B: lo consume el motor viejo; su reescritura es PR-F). Verde.

### 6. NIVEL 5 — Agregación bolsa-global → ESTADO
- [x] 6.1 [TEST] `StateAggregationPolicyTest.kt` (nuevo): AG-just (3 capas con `J` → ESTADO=1.0),
  AG2/O3 (arrastre PLANO ≈0.55 en config A de 1 ancla y config B de 3 anclas, ±0.01), O5/Sol=Tin
  (superhabit PLANO: `XL` en capa 1 = `XL` en capa 2), I1 (opt-in global: capa pesada = capa
  liviana, ±1e-9), O2/C2 (neutralidad `M=1` con déficit), I2/O11 (capa solo-opt-in pesa W0=1.0),
  degradación sin capas → ESTADO=0. Ver rojo. [→ core-engine §"NIVEL 5"]
- [x] 6.2 [CÓDIGO] `StateAggregationPolicy.kt` (nuevo): tipo de entrada `LayerInput` (anclas como
  `R`-values, supportDays, nTasksToday, optIn); bolsa-global con `(base_eff, votos)` por
  capa-con-anclas, `(G, ρ)` solo-soportes, `(M, W0)` solo-opt-in, término-sombra
  `(M, BETA·Σpesos·(1−M))`; `base_global = Σ(valor·peso)/Σ(peso)`;
  `extra_global = (1/k)·Σ extra_final_capa` (PLANO); `ESTADO = min(base_global,1) + extra_global`;
  sin capas → 0. Verde.

### 7. NIVEL 6 — Bandas `banda(ESTADO)`
- [x] 7.1 [P][TEST] `BandPolicyTest.kt` (nuevo): BA1 (cortes
  `0.30→Restoration/0.50→Attention/0.70→Motion/0.90→Plenitude/1.15→Unbreakable`), BA2 (0.85→
  Plenitude, 0.84→Motion), Inquebrantable exacto (1.10→Unbreakable, 1.099→Plenitude),
  cumplir-justo (1.0→Plenitude), bordes inferiores inclusivos, piso cero (0.0→Restoration).
  Ver rojo. [→ core-engine §"NIVEL 6", base-state-policy MODIFIED] (Se creó `BandPolicy` NUEVO en
  vez de reescribir `BaseStatePolicy`: convivencia — el viejo lo consume el motor viejo. La
  reescritura/borrado del viejo y la conservación de `NoData` se cierran en PR-F.)
- [x] 7.2 [P][CÓDIGO] `BandPolicy.kt` (nuevo): `band(estado: Double): ScoreState` puro
  sobre cortes `0.40/0.62/0.85/1.10` (de [ScoringConstantsV2]), sin gates/worst-layer/histéresis/
  memoria. `NoData` queda para el orquestador (`ScoreEngine`) en PR-F. Verde.

---

## PR-D — Invariante "ancla = solo Minutes" (enforcement + test)

> Independiente de los niveles del motor; puede ir antes o después de PR-C. El seed YA está
> reclasificado (NO es task). Solo el enforcement + su test son tasks.

### 8. Invariante ancla=Minutes
- [x] 8.1 [TEST] `ActivityPolicyTest.kt` (ampliado): `requireAnchorUnit(Minutes)` pasa;
  `requireAnchorUnit(Boolean/Count/Time/Text)` lanza `IllegalArgumentException` (`assertThrows`);
  `ActivityUnit.isValidForAnchor()` cubre los 5 casos. Visto rojo (unresolved ref) → verde.
  [→ facts-adapter §"Invariante anclas = solo Minutes"]
- [x] 8.2 [CÓDIGO] `domain/activity/ActivityPolicy.kt`: agregado `ActivityUnit.isValidForAnchor()`
  (`== Minutes`) + `requireAnchorUnit(unit)` que rechaza unidad != `Minutes` con
  `IllegalArgumentException` (`require`, rechazo según design). JVM-puro, sin Room/Compose. Verde.
- [x] 8.3 [CÓDIGO] Validar al asignar surface `Anchor` en los 2 puntos: `AutonomiaRepository.kt`
  (`addActivityAsAnchor`, ~876: lee la unidad de la `ActivityDefinitionEntity` vía
  `dao.getActivityDefinition` y aplica `requireAnchorUnit`) y `DashboardViewModel.kt`
  (`createActivity`, ~339: `anchorUnit = Minutes` validado y reusado en el `unit` de la
  definición). `assembleDebug` verde.

---

## PR-E — Adapter (foco de riesgo) + WeeklyScoringContextBuilder

> Depende del contrato del motor (PR-A..C) para conocer las formas de salida. El grueso del
> riesgo del cambio. Tests con hechos sintéticos que reproducen §1.4.

### 9. ScoringFactsAdapter
- [ ] 9.1 [CÓDIGO] `ScoreModels.kt`: agregar las data classes mínimas del adapter
  (`AnchorWindow(f,t,mins)`, `LayerFacts(anchors, supportDays, nTasksToday, optIn)`,
  `OptInSignal`). [→ facts-adapter §"Salida"]
- [ ] 9.2 [TEST] `ScoringFactsAdapterTest.kt` (nuevo) — anclas: 3 días cumplidos →
  `mins=[30,30,30]` (motor R=1.000); superhabit por días (6 logs de 30 → R≈1.266); dedup
  `activityId:date` (duplicado no infla frecuencia); `NotDone`/`actualValue=0` = día sin
  actividad; `Omitted` excluido; `actualValue=null` con `Done` → 0 minutos. Ver rojo.
  [→ facts-adapter §"Derivar (F,T,mins[7])", §"Logs duplicados, omitidos y NotDone"]
- [ ] 9.3 [TEST] `ScoringFactsAdapterTest.kt` — soportes/tasks/tracks/sueño: soporte sin
  registros → `días_sostenidos=4` (UX inversa); soporte con omisión → `<4`; `n_tasks_hoy` solo
  cuenta tasks de HOY con capa (las de ayer y sin capa no); track limpio → `días_recaída=0`;
  multi-track compone; sueño sin noches → `M=null`; sueño con `0.8,0.6,1.0` → `M=0.8`; semana
  vacía → estructuras vacías + `M=null`. Ver rojo. [→ facts-adapter §"Soportes", §"Tasks",
  §"Tracks", §"Sueño", §"Casos límite"]
- [ ] 9.4 [CÓDIGO] `ScoringFactsAdapter.kt` (nuevo): hechos → `AnchorWindow/LayerFacts/OptInSignal`.
  Mapeo Minutes-only directo (`mins[día] = actualValue`); soportes ventana 4d UX inversa; tasks
  efímeras de hoy por capa; tracks días de recaída ventana 7d; sueño promedio noches con dato.
  Zona local del dispositivo para todo cómputo de fecha. Verde.
- [ ] 9.5 [CÓDIGO] Modificar `WeeklyScoringContextBuilder.kt` para exponer `mins[7]` por ancla,
  `supportDays`, `nTasksToday`, `relapseDays` (preservando el `distinctBy "activityId:date"`
  existente). Verde (no rompe tests existentes de `BuildScoreInputUseCaseTest`).

---

## PR-F — Recableado ScoreEngine + ScoreReport.estado + seam + limpieza

> El cierre. Conecta adapter → niveles 1–6 → ScoreReport, preserva el seam de persistencia,
> bumpea SCORING_VERSION, y borra las policies muertas. Hasta acá los slices son revertibles;
> este es el commit que "enciende" el motor nuevo.

### 10. ScoreReport.estado + seam de persistencia
- [ ] 10.1 [CÓDIGO] `ScoreModels.kt`: agregar `ScoreReport.estado: Float` (ESTADO crudo,
  `Float` solo en la frontera). Mantener los campos que `WeeklyScoreSnapshotDraft` consume.
  [→ core-engine §"Salida del motor"]
- [ ] 10.2 [TEST] Reescribir `ScoreEngineTest.kt` con el caso end-to-end (caso Martín del Python:
  ESTADO `0.821`, puntos `862`) y `ScoreReport expone ESTADO crudo` (ESTADO=1.0 →
  `report.estado=1.0`, `report.state=Plenitude`). Ver rojo. [→ core-engine §"Salida del motor"]
- [ ] 10.3 [CÓDIGO] Reescribir `ScoreEngine.kt`: orquesta `adapter → AnchorScoringPolicy →
  LayerValuePolicy → LayerWeightPolicy → OptInPolicy → StateAggregationPolicy →
  BaseStatePolicy`, emite `ScoreReport(estado, state=band(estado), …)`. Verde.
- [ ] 10.4 [CÓDIGO] Mapear el seam: `weeklyBaseScore=estado`, `weeklyScore=estado`,
  `state=band(estado)`, `visibleScore=points(estado)` (puntos pasados desde la proyección al
  draft — ver PR-G), `worstLayerId=null`, `stability*=null`. Verificar que
  `BuildWeeklyScoreSnapshotUseCase.kt` / `WeeklyScoreSnapshotWriter` compilan y persisten
  (`BuildWeeklyScoreSnapshotUseCaseTest` verde). [→ core-engine §"El seam… sigue funcionando"]
- [ ] 10.5 [CÓDIGO] Bumpear `SCORING_VERSION` de `weekly-base-v1` a `v2` (decidido). Confirmar el
  string exacto con el dueño si hay duda (Open Question del design).

### 11. Limpieza de policies muertas
- [ ] 11.1 [CÓDIGO] Borrar `WeeklyScorePolicy.kt` + `WeeklyScorePolicyTest` (si existe),
  `SpecialLayerScoringPolicy.kt` + `SpecialLayerScoringPolicyTest.kt`. Quitar referencias.
- [ ] 11.2 [CÓDIGO] Dejar `StabilityScoringPolicy.kt` inerte (no invocada en la banda); su test
  no debe afirmar que influye en la banda. Confirmar que `stabilityScore` persiste como deuda
  sin afectar el estado.
- [ ] 11.3 [CÓDIGO] Confirmar que las constantes viejas (PR-A 1.2) quedaron todas eliminadas y no
  hay callers colgando. Build verde (`testDebugUnitTest` + `assembleDebug`).

---

## PR-G — NIVEL 7 (puntos en la proyección)

> Independiente del motor (consume `estado`); puede ir en paralelo a PR-F una vez existe
> `ScoreReport.estado`. Cierra el `visibleScore` del seam.

### 12. Mapeo a puntos (proyección)
- [ ] 12.1 [TEST] `PointsMappingTest.kt` (nuevo, en proyección): PU1 (`PUNTOS(0)=650`,
  `PUNTOS(1.5)=1100`, ±1), PU3 (`PUNTOS(1.0)=941` ±2, `PUNTOS(1.10)=1011` ±3), PU4 (monótono no
  decreciente barriendo 0..1.5 en pasos de 0.001), PU5 (`0.40→721`, `0.62→788`, `0.85→873`, ±2).
  Ver rojo. [→ points-mapping §"Mapeo E"]
- [ ] 12.2 [CÓDIGO] Implementar el mapeo E en `DashboardProjection.kt` (`PointsMapping(estado)`):
  `σ(x)=1/(1+e^−x)`, 5 hitos `(c,w,A)` de constantes, `raw(e)`, normalización
  `650+(raw(e)−raw(0))·450/(raw(1.5)−raw(0))`, `e` clampeado a `[0,1.5]`. Verde.
- [ ] 12.3 [CÓDIGO] Cablear `DashboardProjection`/`ScoringScreen` para usar el nuevo mapeo en
  lugar de `VisibleScorePolicy`; pasar el `visibleScore` resultante al draft del seam (PR-F 10.4).
- [ ] 12.4 [CÓDIGO] Borrar `VisibleScorePolicy.kt` + `VisibleScorePolicyTest.kt`. Build verde.

---

## Cierre — verificación global

- [ ] 13.1 Suite completa verde: `gradlew.bat testDebugUnitTest --tests
  'dev.panopt.autonomia.domain.scoring.*'` (27 asserts + ampliaciones).
- [ ] 13.2 `assembleDebug` verde (WSL→PowerShell, escapando `\$env:JAVA_HOME`).
- [ ] 13.3 Docs vivos al día si el cambio los contradice: `docs/scoring/plan-tecnico-scoring.md`
  (estado por fases) refleja que el motor real ya corre el modelo nuevo. (Los docs matemáticos
  ya son el spec — no se reescriben.)

---

## Mapeo task → requisito (trazabilidad)

| Task | Requisito de spec |
|------|-------------------|
| 1.1–1.2 | core-engine "17 constantes"; points-mapping "Constantes"; base-state "Cortes" |
| 2.1–2.2 | core-engine "NIVEL 1 — Ancla R(F,T,mins)" (AN1–AN11 + §1.4) |
| 3.1–3.2 | core-engine "NIVEL 2 — Valor de capa + soportes + tasks" (VC3/VC4, SO2/SO4, TA5/TA-suma) |
| 4.1–4.2 | core-engine "NIVEL 3 — Peso de capa" (PC2/PC3/PC5) |
| 5.1–5.2 | core-engine "NIVEL 4 — Opt-ins" (O2/C2, I2/O11, M_sobr); facts-adapter "Tracks" |
| 6.1–6.2 | core-engine "NIVEL 5 — Agregación" (AG-just, AG2/O3, O5/Sol=Tin, I1) |
| 7.1–7.2 | core-engine "NIVEL 6 — Bandas"; base-state-policy MODIFIED (BA1/BA2/BA3) |
| 8.1–8.3 | facts-adapter "Invariante anclas = solo Minutes" |
| 9.1–9.5 | facts-adapter (todos los Requirements + casos límite) |
| 10.1–10.5 | core-engine "Salida del motor"; "El seam… sigue funcionando" |
| 11.1–11.3 | proposal "Mapeo viejo→nuevo"; core-engine "StabilityScoringPolicy inerte" |
| 12.1–12.4 | points-mapping "Mapeo E ESTADO→PUNTOS [650,1100]" (PU1/PU3/PU4/PU5) |

---

## Review Workload Forecast

- **Estimado de líneas cambiadas:** ~1500–1800 líneas (motor reescrito ~600, adapter +
  context builder ~350, suite de tests ~550, proyección/puntos ~150, invariante + wiring ~120).
- **PRs encadenados recomendados:** **Sí.** 7 slices (PR-A…PR-G) por nivel/work-unit. Cada slice
  test+código juntos, revertible de forma independiente hasta PR-F (recableado, que "enciende"
  el motor nuevo).
- **Riesgo de presupuesto 400 líneas:** **High.** El cambio completo (~1500+ líneas) excede de
  largo un solo PR de 400. Cada slice individual cabe en/cerca de 400: PR-A ~250, PR-B ~320,
  PR-C ~330, PR-D ~90, PR-E ~400 (el más cargado: adapter + tests — vigilar, posible sub-corte),
  PR-F ~280, PR-G ~180.
- **Decisión requerida antes de apply:** **Sí.** Política `ask-on-risk`: el orquestador debe
  decidir PRs encadenados/stacked vs `size:exception` aprobado por el maintainer ANTES de
  lanzar `sdd-apply`. Recomendación: encadenados por slice (PR-A→PR-G), con PR-E vigilado por si
  hay que partirlo en E1 (anclas+dedup) y E2 (soportes/tasks/tracks/sueño).
- **Corte sugerido (7 PRs encadenados, work-unit commits):**
  1. **PR-A** — `ScoringConstants` + NIVEL 1 ancla + tests.
  2. **PR-B** — NIVELES 2–3 (valor de capa, soportes, tasks, pesos) + tests.
  3. **PR-C** — NIVELES 4–6 (opt-ins, agregación, bandas) + tests.
  4. **PR-D** — invariante Minutes-only (helper + 2 puntos de validación) + test.
  5. **PR-E** — adapter + `WeeklyScoringContextBuilder` + tests de adapter. *(vigilar 400; partir si hace falta)*
  6. **PR-F** — recableado `ScoreEngine` + `ScoreReport.estado` + seam + `SCORING_VERSION v2` + borrado de policies muertas.
  7. **PR-G** — NIVEL 7 puntos en proyección + tests + borrado de `VisibleScorePolicy`.
