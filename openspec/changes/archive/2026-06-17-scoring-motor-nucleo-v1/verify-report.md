# Verify Report: scoring-motor-nucleo-v1

VEREDICTO: **LISTO PARA ARCHIVAR.** 0 CRITICAL, 2 WARNING (deudas ya declaradas y acotadas),
3 SUGGESTION. (Recuperado de engram `sdd/scoring-motor-nucleo-v1/verify-report`, obs #1141.)

## Qué se validó

Implementación (PR-A..PR-G, 8 commits) contra los 4 delta specs (core-engine, facts-adapter,
points-mapping, base-state-policy), design, tasks y el contrato matemático
(`verificacion_modelo_oficial.py` = 27 asserts).

## Resultados REALES de build/test

- `testDebugUnitTest --rerun-tasks`: BUILD SUCCESSFUL in 40s (26 tasks, no cache). **366 tests,
  0 failures, 0 errors.** Solo warnings de deprecación pre-existentes (SleepLog legacy v12,
  unsafeCheckOpNoThrow) — no relacionadas al cambio.
- `assembleDebug`: BUILD SUCCESSFUL.

## Cobertura de los 27 axiomas (verificada)

Los 27 `chk()` del Python tienen test JUnit 1:1 — ANCLA AN1/AN2/AN3/AN6/AN7/AN8/AN10/AN11
(+§1.4 exacto: 1.000/1.289/1.266/0.544/1.31606/1.499); PESO PC2/PC3/PC5; AGREGACIÓN
AG-just/AG2-O3/I1/O2-C2/O5-Sol=Tin/I2-O11; SOPORTES SO2/SO4; TASKS TA5/TA-suma/TA3; BANDAS
BA1/BA2 (+BA3, +Inquebrantable 1.10 exacto); PUNTOS PU1/PU3/PU4. Cobertura EXTRA: VC3/VC4, PU5,
clamps. Los axiomas "verificados a mano" (AN12, SO6, TA6, O6, O9, PU2) NO están en el script
Python verificable — son doc-level, no gaps de test.

## Fidelidad al modelo (verificada)

Gate `base.pow(P)` con `P=ScoringConstantsV2.P=2.0` (no literal 2); `shadowTerm =
BETA·Σpesos·(1−M)` escala con Σpesos no N (`OptInPolicy.kt:42`); tasks efímeras (`n_tasks_hoy`);
cortes banda 0.40/0.62/0.85/1.10 en constantes (Plenitud entra 0.85);
`ESTADO=min(base_global,1)+extra_global`; puntos 1.0→941, 1.10→1011, rango [650,1100]
(`PointsMappingPolicy` sigmoide enfoque E).

## Invariante ancla=Minutes (verificada)

`requireAnchorUnit`/`isValidForAnchor` en `ActivityPolicy.kt`; enforcement en los 2 puntos de
creación (`AutonomiaRepository.addActivityAsAnchor:881`; `DashboardViewModel.createActivity:343`).
Seed: `userActivityConfigs=emptyList()` → CERO anclas con unidad no-Minutes.

## Seam de persistencia (verificado)

`SCORING_VERSION` bumpeado a `core-v2` (`WeeklyScoreSnapshotModels.kt:37`); `ScoreEngine` puebla
`weeklyBaseScore/weeklyScore=estadoFloat`, `state=band`, `visibleScore=PointsMappingPolicy.points(estado)`,
`worstLayerId=null`, `stability*=null`; `BuildWeeklyScoreSnapshotUseCase` compila + verde.

## Limpieza (verificada)

11 policies viejas borradas (sin archivos en disco, sin referencias en código vivo).
`StabilityScoringPolicy` INERTE. Room NO tocado (Camino A — git diff sin Database/Migration/Entity/Dao).

## Hallazgos (deudas y desviaciones)

- **WARNING-1** (deuda declarada, acotada): `LayerScore` se emite con `score=0f` placeholder
  (detalle por-capa = deuda de presentación). No rompe nada.
- **WARNING-2** (deuda declarada, acotada): `reasons=emptyList()` (`ScoreReasonPolicy` borrada).
  No rompe el seam.
- **SUGGESTION-1**: `PointsMappingPolicy` quedó como policy de dominio reutilizable (no privada
  en `DashboardProjection` como decía el design literal). Desviación DELIBERADA y correcta.
- **SUGGESTION-2**: caso integrado "Martín 0.821" cubierto a nivel `StateAggregationPolicyTest`,
  no en el end-to-end del engine. Aceptable.
- **SUGGESTION-3**: docs (`presets-actividades-v1.md` drift, taxonomía restraint-checks)
  parkeados como tarea aparte — fuera de alcance de este cambio.

next_recommended: sdd-archive (limpio, sin CRITICALs que bloqueen).
