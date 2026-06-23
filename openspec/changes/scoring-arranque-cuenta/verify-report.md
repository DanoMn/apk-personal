# Verify Report — `scoring-arranque-cuenta`

> **VEREDICTO: LISTO PARA runtime/archivar.** 0 CRITICAL · 0 WARNING · 2 SUGGESTION.

## Resultados de ejecución (output real)
- Suite dominio: `testDebugUnitTest --tests 'dev.panopt.autonomia.domain.*' --rerun-tasks` → **BUILD SUCCESSFUL**.
  **367 tests · 0 failures · 0 errors · 0 skipped.**
- Build: `assembleDebug` → **BUILD SUCCESSFUL**.

### Conteo por clase relevante (XML)
| Clase | tests | fail | err |
|-------|-------|------|-----|
| AnchorScoringWindowDaysTest | 10 | 0 | 0 |
| StartupCounterPolicyTest | 9 | 0 | 0 |
| StartupDetectionRuleTest | 6 | 0 | 0 |
| StartupProjectionUseCaseTest | 7 | 0 | 0 |
| DashboardProjectionStartupTest | 3 | 0 | 0 |
| ScoreEngineTest (regresión) | 15 | 0 | 0 |
| AnchorScoringPolicyTest (regresión) | 15 | 0 | 0 |
| DashboardProjectionTest (regresión) | 23 | 0 | 0 |
| PointsMappingPolicyTest (regresión) | 8 | 0 | 0 |

## Checklist de contrato (contra el código real)
1. **windowDays** — `n=coerceIn(1,7)`, `fEff=min(f,n)`; `sd=if(fEff<n) v/(n-fEff) else 0.0`, `wt=(fEff/n)^κ` (versión SPEC con f_eff → `wt≤1` por construcción; gana a design). `phi`/`cut`/`st` con `f` crudo. Default 7 byte-idéntico (delta 0.0). Guard sin div/0 (test transversal f∈2..7 × N∈1..6, R∈[0,1.5], sin NaN/Inf). ✅
2. **FIX A (crítico)** — `counter = round(points(estado) × d/7)` atenúa PUNTOS ya mapeados. Ejemplos del dueño verificados (estado→900 por bisección): d1=129, d4=514, d7=900. Día 1 < 650 (recorre zona muerta). ✅
3. **FIX B** — `r(f,t,mins,windowDays=7)` propaga; ScoreEngine ramo legacy `else` pasa windowDays. Cuenta nueva sin versiones no se castiga; cero regresión con default 7. ✅
4. **No-salto día 7→8** — `assertEquals(matureReport.visibleScore, counter.counterPoints)` exacto, mismos hechos. ✅
5. **Detección** — NoData real + weeklyHistory sin score real + ≥3 capas con ancla (sin filtrar gracia); <3 → NoData real (gate soberano). ✅
6. **daysLived = createdDaysAgo + 1** — `ChronoUnit.DAYS.between(createdDate,today)+1` clamp[1,7]; test dashboard confirma (createdDaysAgo=2 → daysLived=3 → daysRemaining=4). ✅
7. **Invariantes** — persistencia intacta (snapshot NoData→0, contador no se persiste); `ScoreState` 6 ramas SIN `Arranque`; `StatusCard.kt` sin diff vs main; `DashboardState.startup` canal aparte; `scoreState` sigue NoData en arranque. ✅
8. **Docs vivos** — modelo-matematico-nucleo-v1 §1.3.1 + §7.1; modelo-scoring-oficial-v1 §12.1; frontend-design.md (StartupStatusCard). ✅

## Suggestions (no bloquean)
- **S1** — `StartupStatusCard` sin test JVM (Compose); cubierto por build + capa runtime. Aceptado por design.
- **S2** — copy `headline`/`body` hardcodeados en `toStartupCardState` (no en tabla de nombres canónicos UI). Respeta tono AGENTS.md, sin términos prohibidos.

## Pendiente (fuera de esta fase)
- Capa runtime (`verificacion-por-capas.md`): install limpio + usuario nuevo con 3 anclas ve contador 0→score, no "Sin datos"; transición día 7→8 sin salto. La corre el orquestador (emulador).
