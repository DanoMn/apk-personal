# Tasks — Rotación de frases ancla (`anchor-phrase-rotation`)

> **Estado SDD:** archivado (2026-06-04) — 22/22 tareas completadas
> **Proyecto:** apk-personal — "Autonomía sin límites"
> **Estrategia de entrega:** chained PRs (`auto-chain` activo — ver Review Workload Forecast)
> **Modo TDD:** Strict TDD habilitado (`testDebugUnitTest`). Toda tarea de implementación está precedida por su tarea de test en rojo.
> **Fuentes:** `openspec/changes/anchor-phrase-rotation/specs/index.md` · `design.md` · `proposal.md` · `meta/instructions/2026-06-04-rotacion-frases-ancla.md`
>
> **NOTA CAMINO A (post-apply):** El slice 2 (migración v12→v13) fue implementado y luego
> REVERTIDO. La DB permanece en v12. El androidTest y schemas/13.json fueron eliminados.
> La tarea 2.1 y 2.2 se marcan [x] por haber sido ejecutadas, pero la migración no existe
> en el código final. Ver archive-report para el detalle completo.

---

## Review Workload Forecast

| Slice | PR # | Líneas estimadas | Riesgo 400L |
|---|---|---|---|
| 1 — Enums + DayPhasePolicy + mappers | PR-1 | ~120 | Bajo |
| 2 — Migración v12→v13 | PR-2 | ~150 | Bajo |
| 3 — Seed canónico (83 frases + reglas derivadas) | PR-3 | ~350–450 | **Alto** |
| 4 — AnchorPhraseSelector puro | PR-4 | ~200 | Medio |
| 5 — AnchorPhraseResolver + DAO + wiring | PR-5 | ~250 | Medio |
| 6 — Integración dashboard | PR-6 | ~180 | Bajo |
| 7 — Doc viva | PR-7 | ~30 | Nulo |

**Chained PRs recommended: Yes**
**400-line budget risk: High** (slice 3 puede superar el techo por sí solo; se aísla en PR propio)
**Decision needed before apply: Yes** — `auto-chain` está activo; `sdd-apply` implementa un slice a la vez.

---

## Slice 1 — Enums + modelos de dominio + `DayPhasePolicy` + mappers ✅

| # | Tipo | Estado | Archivos afectados |
|---|---|---|---|
| 1.1 | [T] | [x] DONE — 6 casos, RED+GREEN confirmados | `DayPhasePolicyTest.kt` (nuevo) |
| 1.2 | [I] | [x] DONE | `Models.kt` (DayPhase, AnchorPhraseSelection, AnchorPhraseStateRule, AnchorPhrasePhaseRule) |
| 1.3 | [I] | [x] DONE — `internal object DayPhasePolicy` | `domain/phrase/DayPhasePolicy.kt` (nuevo) |
| 1.4 | [I] | [x] DONE — con runCatching fallback | `data/local/mapper/DomainMappers.kt` |

---

## Slice 2 — Migración v12 → v13 ⚠ REVERTIDO

| # | Tipo | Estado | Archivos afectados |
|---|---|---|---|
| 2.1 | [T] | [x] DONE (escrito, luego eliminado) | `AnchorPhraseMigration12To13Test.kt` (eliminado) |
| 2.2 | [I] | [x] DONE (implementado, luego revertido) | `AutonomiaDatabase.kt` v12 (version bump revertido) |

**Decisión post-apply:** Camino A — DB permanece en v12. No hay migración en el código final.

---

## Slice 3 — Seed canónico de 83 frases ✅

| # | Tipo | Estado | Archivos afectados |
|---|---|---|---|
| 3.1 | [T] | [x] DONE — 15 test cases, RED+GREEN confirmados | `AnchorPhraseSeedTest.kt` (nuevo) |
| 3.2 | [I] | [x] DONE — 83 frases, reglas derivadas de mapas | `data/local/seed/AnchorPhraseSeed.kt` (nuevo) |
| 3.3 | [I] | [x] DONE — 3 upsert calls; BUILD SUCCESSFUL | `AutonomiaRepository.kt` |

---

## Slice 4 — `AnchorPhraseSelector` puro ponderado ✅

| # | Tipo | Estado | Archivos afectados |
|---|---|---|---|
| 4.1 | [T] | [x] DONE — 17 test cases, RED+GREEN confirmados | `AnchorPhraseSelectorTest.kt` (nuevo) |
| 4.2 | [I] | [x] DONE — 5 private fun gears, allowedFamiliesByState map, stableHash | `domain/phrase/AnchorPhraseSelector.kt` (nuevo) |

---

## Slice 5 — `AnchorPhraseResolver` + queries DAO + wiring DI + `onResumed` ✅

| # | Tipo | Estado | Archivos afectados |
|---|---|---|---|
| 5.1 | [T] | [x] DONE — 7 test cases, RED+GREEN confirmados | `AnchorPhraseResolverTest.kt` (nuevo) |
| 5.2 | [I] | [x] DONE | `AutonomiaDao.kt` (2 queries nuevas) |
| 5.3 | [I] | [x] DONE — AnchorPhraseDataSource + DaoAnchorPhraseDataSource | `data/phrase/AnchorPhraseDataSource.kt` (nuevo), `data/phrase/AnchorPhraseResolver.kt` (nuevo) |
| 5.4 | [I] | [x] DONE — BUILD SUCCESSFUL | `AutonomiaRepository.kt`, `ui/dashboard/DashboardRepository.kt` |
| 5.5 | [I] | [x] DONE — call added AFTER refreshCurrentWeeklyScoreSnapshot | `ui/dashboard/DashboardViewModel.kt` |

---

## Slice 6 — Integración dashboard ✅

| # | Tipo | Estado | Archivos afectados |
|---|---|---|---|
| 6.1 | [T] | [x] DONE — 4 test cases, RED+GREEN confirmados | `DashboardProjectionTest.kt` (modificado) |
| 6.2 | [I] | [x] DONE — text="", authorReference="" | `domain/dashboard/DashboardState.kt` |
| 6.3 | [I] | [x] DONE | `ui/dashboard/DashboardRepository.kt`, `AutonomiaRepository.kt` |
| 6.4 | [I] | [x] DONE — DashboardAnchorPhraseSnapshot data class | `ui/dashboard/DashboardViewModel.kt` |
| 6.5 | [I] | [x] DONE — BUILD SUCCESSFUL in 11s | `domain/dashboard/DashboardProjection.kt`, `DashboardEngine.kt` |

---

## Slice 7 — Documentación viva ✅

| # | Tipo | Estado | Archivos afectados |
|---|---|---|---|
| 7.1 | [D] | [x] DONE — §18 tabla de implementado; §17 actualizado; ADR-3 documentado | `docs/dominio/frases-ancla.md` |

---

## Definición de Terminado del cambio completo

- [x] Slices 1–6 con todos los tests en verde (`testDebugUnitTest`). Slice 2 revertido.
- [x] Build `assembleDebug` pasa sin errores.
- [ ] `MIGRATION_12_13` cubierta con `MigrationTestHelper` — **N/A (Camino A; migración revertida).**
- [x] Seed: 83 frases activas, 0 con `authorReference` vacío/nulo, reglas derivadas de mapas.
- [x] `DashboardState.kt` sin cita Kierkegaard ni ningún texto atribuido hardcodeado.
- [x] `docs/dominio/frases-ancla.md` actualizado (slice 7 completo).
- [x] Capas aplicables verificadas (ver verify-report: 0 CRITICAL).
