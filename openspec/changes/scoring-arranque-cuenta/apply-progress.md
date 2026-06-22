# Apply Progress — `scoring-arranque-cuenta`

> Estado de implementación por lote. Actualizado al cerrar cada lote.

## Lote 1 — Núcleo: `rFromRatios` con `windowDays` — ✅ COMPLETO

Strict TDD (RED → GREEN → REFACTOR). Entregable aislado y aditivo puro:
el motor maduro queda byte-idéntico (nadie pasa `windowDays ≠ 7` todavía).

| Task | Estado | Nota |
|------|--------|------|
| 1.1 (TEST-RED) regresión default ≡ 7 | [x] | RED por firma de 3 args inexistente |
| 1.2 (GREEN) `windowDays=7` + `f_eff` en `sd`/`wt` | [x] | `n=coerceIn(1,7)`, `fEff=min(f,n)` |
| 1.3 suite `AnchorScoringPolicyTest` verde sin tocar | [x] | cero regresión |
| 1.4 ventana parcial reparte sobre N | [x] | f=3/N=4→1.0; f=2/N=4 superhábit>base |
| 1.5 guard f≥N (sin div/0, wt≤1, sin NaN) | [x] | f=5/N=2, f=7/N=1, vacío, transversal 2..7×1..6 |
| 1.6 clamp `windowDays` a [1,7] | [x] | 9≡7, 0≡1, -5≡1 |
| 1.7 doc vivo modelo-matemático §1.3.1 | [x] | parámetro `windowDays` documentado |

### Cambios de producción
- `app/src/main/java/dev/panopt/autonomia/domain/scoring/AnchorScoringPolicy.kt`
  - Firma: `fun rFromRatios(f: Int, dayRatios: List<Double>, windowDays: Int = 7): Double`.
  - `val n = windowDays.coerceIn(1, 7)`; `val fEff = min(f, n)`.
  - `sd = if (fEff < n) v / (n - fEff).toDouble() else 0.0` (reemplaza `7` literal).
  - `wt = (fEff.toDouble() / n.toDouble()).pow(kappa)` (reemplaza `7` literal).
  - `phi`, `cut`, `st` SIN tocar (mantienen `f` crudo). Llamador `r(...)` y call sites intactos.
  - ~9 líneas netas de producción + KDoc.

### Tests nuevos
- `app/src/test/java/dev/panopt/autonomia/domain/scoring/AnchorScoringWindowDaysTest.kt`
  (10 tests; el nombre de archivo sigue task 1.1, no `AnchorScoringPolicyTest`).

### Doc vivo
- `docs/scoring/modelo-matematico-nucleo-v1.md` — nueva §1.3.1 "Ventana de N días (`windowDays`)".

### Verificación (output real)
- `testDebugUnitTest --tests '...AnchorScoringWindowDaysTest'` → **BUILD SUCCESSFUL** (test nuevo verde).
- `testDebugUnitTest --tests '...domain.scoring.*'` → **BUILD SUCCESSFUL** (suite scoring completa, cero regresión).
- `assembleDebug` → **BUILD SUCCESSFUL** (compila).

### Commit
- `feat(scoring): generalize rFromRatios to N-day window (windowDays)` en branch `feat/scoring-motor-nucleo-v1`.

## Lote 2 — Dominio de arranque — ⬜ PENDIENTE
## Lote 3 — Proyección + UI — ⬜ PENDIENTE
