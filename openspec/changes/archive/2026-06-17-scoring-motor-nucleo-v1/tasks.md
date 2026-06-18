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
  `§0.1`. (objeto NUEVO, no reescritura; ver decisión de convivencia arriba)
- [ ] 1.2 Eliminar las constantes del modelo viejo. **(Diferido a PR-F: hay callers vivos;
  borrarlas ahora rompe el build. Se eliminan junto a las policies en PR-F 11.x.)**

### 2. NIVEL 1 — Ancla `R(F,T,mins)`
- [x] 2.1 [TEST] `AnchorScoringPolicyV2Test.kt` (nuevo): AN1/AN2/AN3/AN6/AN7/AN8/AN10/AN11 + §1.4.
- [x] 2.2 [CÓDIGO] `AnchorScoringPolicyV2.kt` (nuevo): `r(f,t,mins): Double` = Best-F. Verde.

---

## PR-B — NIVELES 2–3 (valor de capa, soportes, tasks, pesos)

### 3. NIVEL 2 — Valor de capa (dos canales) + soportes + tasks
- [x] 3.1 [TEST] `LayerValuePolicyTest.kt` (nuevo): VC3/VC4, SO2, SO4, TA5, TA-suma, TA3, task_lift.
- [x] 3.2 [CÓDIGO] `LayerValuePolicy.kt` (NUEVO): dos canales + blend soportes + tasks joint. Verde.

### 4. NIVEL 3 — Peso de capa (votos)
- [x] 4.1 [P][TEST] `LayerWeightPolicyTest.kt` (nuevo): PC2, PC3, PC5.
- [x] 4.2 [P][CÓDIGO] `LayerWeightPolicy.kt` (NUEVO): `votes(n)=(1−r^n)/(1−r)`; `ρ=RHO` para `n=0`. Verde.

---

## PR-C — NIVELES 4–6 (opt-ins, agregación, bandas)

### 5. NIVEL 4 — Opt-ins (señal M + término-sombra)
- [x] 5.1 [TEST] `OptInPolicyTest.kt` (nuevo): `shadowTerm`, `M_sobr`. (O2/C2, I2/O11 → StateAggregationPolicyTest)
- [x] 5.2 [CÓDIGO] `OptInPolicy.kt` (nuevo): `shadowTerm(M, Σpesos)=BETA·Σpesos·(1−M)`; `M_sobr`. Verde.

### 6. NIVEL 5 — Agregación bolsa-global → ESTADO
- [x] 6.1 [TEST] `StateAggregationPolicyTest.kt` (nuevo): AG-just, AG2/O3, O5/Sol=Tin, I1, O2/C2, I2/O11, sin capas→0.
- [x] 6.2 [CÓDIGO] `StateAggregationPolicy.kt` (nuevo): bolsa-global → `ESTADO=min(base_global,1)+extra_global`. Verde.

### 7. NIVEL 6 — Bandas `banda(ESTADO)`
- [x] 7.1 [P][TEST] `BandPolicyTest.kt` (nuevo): BA1, BA2, Inquebrantable 1.10 exacto, cumplir-justo 1.0, piso 0.0.
- [x] 7.2 [P][CÓDIGO] `BandPolicy.kt` (nuevo): `band(estado: Double): ScoreState` puro sobre cortes. Verde.

---

## PR-D — Invariante "ancla = solo Minutes" (enforcement + test)

### 8. Invariante ancla=Minutes
- [x] 8.1 [TEST] `ActivityPolicyTest.kt` (ampliado): `requireAnchorUnit`/`isValidForAnchor`.
- [x] 8.2 [CÓDIGO] `domain/activity/ActivityPolicy.kt`: `isValidForAnchor()` + `requireAnchorUnit(unit)`. Verde.
- [x] 8.3 [CÓDIGO] Validar al asignar surface `Anchor` en los 2 puntos: `AutonomiaRepository.kt` + `DashboardViewModel.kt`. `assembleDebug` verde.

---

## PR-E — Adapter (foco de riesgo) + WeeklyScoringContextBuilder

### 9. ScoringFactsAdapter
- [x] 9.1 [CÓDIGO] `ScoreModels.kt`: data classes `AnchorWindow`, `LayerFacts`. (opt-in = `Double?` directo, no wrapper)
- [x] 9.2 [TEST] `ScoringFactsAdapterTest.kt` — anclas: mins, superhabit, dedup, NotDone, Omitted, null. GREEN.
- [x] 9.3 [TEST] `ScoringFactsAdapterTest.kt` — soportes/tasks/tracks/sueño + casos límite. GREEN.
- [x] 9.4 [CÓDIGO] `ScoringFactsAdapter.kt` (nuevo): hechos → `AnchorWindow/LayerFacts`. Minutes-only directo. GREEN.
- [x] 9.5 [CÓDIGO] `WeeklyScoringContextBuilder.kt`: expone `weeklyAbstinenceLogsByTrack`. GREEN.

---

## PR-F — Recableado ScoreEngine + ScoreReport.estado + seam + limpieza

### 10. ScoreReport.estado + seam de persistencia
- [x] 10.1 [CÓDIGO] `ScoreModels.kt`: `ScoreReport.estado: Float`.
- [x] 10.2 [TEST] Reescrito `ScoreEngineTest.kt` end-to-end. Verde.
- [x] 10.3 [CÓDIGO] Reescrito `ScoreEngine.kt`: orquesta adapter → niveles → BandPolicy; gate NoData. Verde.
- [x] 10.4 [CÓDIGO] Seam mapeado: `weeklyBaseScore=estado`, `state=band(estado)`, `visibleScore=points(estado)`, `worstLayerId=null`, `stability*=null`. Verde.
- [x] 10.5 [CÓDIGO] Bumpeado `SCORING_VERSION` `weekly-base-v1` → `core-v2`.

### 11. Limpieza de policies muertas
- [x] 11.1 [CÓDIGO] Borradas 11 policies viejas (+tests). Modelos huérfanos removidos de `ScoreModels.kt`.
- [x] 11.2 [CÓDIGO] `StabilityScoringPolicy.kt` INERTE; `stabilityScore=null` como deuda.
- [x] 11.3 [CÓDIGO] Constantes viejas eliminadas de `ScoringConstants`. Build verde.

---

## PR-G — NIVEL 7 (puntos en la proyección)

> **Decisión de ubicación (apply PR-G):** el mapeo E se implementó como objeto de **dominio puro
> reutilizable** `domain/scoring/PointsMappingPolicy.kt` (`points(estado: Double): Int`), NO como
> función privada de `DashboardProjection`. Razón: el seam y la proyección DEBEN usar el mismo
> cálculo; duplicarlo haría divergir snapshot y dashboard.

### 12. Mapeo a puntos (proyección)
- [x] 12.1 [TEST] `PointsMappingPolicyTest.kt` (nuevo): PU1, PU3, PU4, PU5 + clamp.
- [x] 12.2 [CÓDIGO] `PointsMappingPolicy.kt` (`points(estado)`): sigmoide enfoque E. Verde.
- [x] 12.3 [CÓDIGO] Cableado: `ScoreEngine.visibleScore = PointsMappingPolicy.points(estado)`; `DashboardProjection` usa el mismo.
- [x] 12.4 [CÓDIGO] `VisibleScorePolicy.kt` + test ya borrados (PR-F 11.1). Build verde.

---

## Cierre — verificación global

- [x] 13.1 Suite verde: `testDebugUnitTest` BUILD SUCCESSFUL (366 tests, 0 fallos).
- [x] 13.2 `assembleDebug` BUILD SUCCESSFUL.
- [x] 13.3 Doc vivo al día: `docs/scoring/plan-tecnico-scoring.md`.

---

## Mapeo task → requisito (trazabilidad)

| Task | Requisito de spec |
|------|-------------------|
| 1.1–1.2 | core-engine "17 constantes"; points-mapping "Constantes"; base-state "Cortes" |
| 2.1–2.2 | core-engine "NIVEL 1 — Ancla R(F,T,mins)" (AN1–AN11 + §1.4) |
| 3.1–3.2 | core-engine "NIVEL 2 — Valor de capa + soportes + tasks" |
| 4.1–4.2 | core-engine "NIVEL 3 — Peso de capa" (PC2/PC3/PC5) |
| 5.1–5.2 | core-engine "NIVEL 4 — Opt-ins"; facts-adapter "Tracks" |
| 6.1–6.2 | core-engine "NIVEL 5 — Agregación" |
| 7.1–7.2 | core-engine "NIVEL 6 — Bandas"; base-state-policy MODIFIED |
| 8.1–8.3 | facts-adapter "Invariante anclas = solo Minutes" |
| 9.1–9.5 | facts-adapter (todos los Requirements + casos límite) |
| 10.1–10.5 | core-engine "Salida del motor"; "El seam… sigue funcionando" |
| 11.1–11.3 | proposal "Mapeo viejo→nuevo"; core-engine "StabilityScoringPolicy inerte" |
| 12.1–12.4 | points-mapping "Mapeo E ESTADO→PUNTOS [650,1100]" |

---

## Review Workload Forecast

- **Estimado:** ~1500–1800 líneas. **PRs encadenados:** Sí (PR-A…PR-G).
- **Riesgo presupuesto 400:** High (cambio completo). Cada slice ~250–400.
- **Decisión antes de apply:** Sí (ask-on-risk → encadenados por slice). Ejecutado en 8 commits.
