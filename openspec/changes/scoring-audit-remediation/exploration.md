# Exploración — scoring-audit-remediation

Fecha: 2026-05-29
Fase SDD: explore
Proyecto: apk-personal (Vocal / Autonomía sin límites)
Artifact store: openspec
Fuente: auditoría verificada (2 subagentes Sonnet + verificación manual del orquestador) contra `docs/arbol-scoring-vocal-v1.md` y `docs/plan-tecnico-scoring-vocal.md`.

## Validación spot (hallazgos verificados contra código real)

- **`domain/scoring/BaseStatePolicy.kt:13-14`** — Los estados Restauración/Atención se evalúan sobre `visibleScore` (700–1000), no sobre `weeklyBaseScore` (0–1) como define la spec sección 7.1. Efecto: Restauración recién a base ≈0.167 (spec: <0.40); Atención a ≈0.333 (spec: <0.70). Faltan `worstLayerCollapse=0.30` y `stateHysteresisMargin=0.03`. La sección 7.1 está marcada "propuesta a discutir / calibrable".
- **`domain/scoring/WeeklyScorePolicy.kt:5-7`** — Promedio usa `rawScore` (con bonus, cap 1.2); peor capa usa `baseScore` (sin bonus, cap 1.0). La spec 7.0 usa un `LayerScore` único. Defendible como anti-compensación.
- **`domain/sleep/SleepScoring.kt:22`** — `0.70*duration + 0.30*schedule` (2 de 4 componentes). Spec: `0.40*Duration + 0.25*Continuity + 0.20*ScheduleAlignment + 0.15*DigitalInterruption`. Además es por día, no por sesiones.
- Confirmado ausentes en todo el proyecto: `BaseConfigurationEntity`, `AnchorInitialBaselineEntity`, `SleepSessionLogEntity`, `SleepInterruptionEventEntity`, y los DAO `observeAbstinenceLogsBetween` / `observeSleepSessionLogsBetween` / `observeRelapseEventsOverlapping`.
- `AbstinenceStatus` = `Unknown | Clean | Relapse` (sin `PendingConfirmation`).
- `activity_logs` / `ActivityLogEntity` siguen como código legacy muerto. DB en versión 10.
- `VisibleScorePolicy.stateFor()` existe como función paralela (solo visibleScore→estado), posible duplicado de `BaseStatePolicy`.

## Slicing recomendado (6 cambios secuenciales)

| Orden | Cambio | Depende de | Líneas est. | Riesgo |
|-------|--------|-----------|-------------|--------|
| 1 | **decisions-and-state**: resolver D1+D2; corregir thresholds y/o ratificar | — | 80–120 | Bajo |
| 2 | **sleep-sessions-infra**: `SleepSessionLogEntity`, `SleepInterruptionEventEntity`, migración, SleepScoring 4 componentes | — (paralelo) | 250–350 | Medio |
| 3 | **base-config-infra**: `BaseConfigurationEntity`, `AnchorInitialBaselineEntity`, amortización primera semana | — (paralelo) | 150–200 | Bajo |
| 4 | **dao-range-queries**: range queries sobriedad/sueño/relapse; refactor `WeeklyScoreSnapshotWriter` | 2 y 3 | 60–80 | Bajo |
| 5 | **abstinence-pending-status**: agregar `PendingConfirmation`, actualizar mappers | — | 30–50 | Bajo |
| 6 | **legacy-cleanup**: DROP `activity_logs`, eliminar `ActivityLogEntity` | después de 4 | 40–60 | Bajo |

Cambio 7 (UI explicativa, Fase 8) = cambio independiente posterior, >400 líneas, requerirá PRs encadenados.

## Decisiones abiertas (solo el usuario / dueño del spec resuelve)

- **D1 (crítica, bloquea el primer cambio):** ¿Alinear `BaseStatePolicy` al spec literal (thresholds sobre `weeklyBaseScore` 0–1, + `worstLayerCollapse` + histéresis) o ratificar los thresholds actuales sobre `visibleScore` y actualizar la spec?
- **D2 (media):** ¿Alinear `WeeklyScorePolicy` a usar `rawScore` en ambos términos, o ratificar la asimetría como diseño anti-compensación intencional y documentarlo? (La regla sellada "superávit no compensa capas caídas" respalda ratificar.)
- **D3 (baja):** ¿`PendingConfirmation` en el primer slice o en cambio posterior?
- **D4 (baja, limpieza):** ¿Cuándo dropear `activity_logs` / `ActivityLogEntity`?

## Recomendación del primer cambio

Bounded, dominio puro, < 400 líneas. Su forma exacta depende de D1/D2:
- Si D1=alinear → reescribir `BaseStatePolicy` con thresholds sobre `weeklyBaseScore`, mover umbrales a `ScoringConstants`, tests explícitos de estado.
- Si D2=ratificar → documentar la asimetría en spec, cero cambio de código en `WeeklyScorePolicy`.
- Híbrido (D1=alinear, D2=ratificar) ≈ 60–90 líneas + docs.

## Riesgos

1. D1 sin resolver bloquea el primer cambio.
2. La fórmula de sueño de 4 componentes depende de `SleepSessionLogEntity`, que a su vez depende de definir qué telemetría Android (desbloqueos, interrupciones, confianza de fuente) se puede capturar realmente. Decisión de producto pendiente.
3. `WeeklyScoreSnapshotWriter` hace full-scan de `abstinence_logs` — riesgo de performance con historial grande, mitigado por el cambio 4.

Artefacto espejo en engram: `sdd/scoring-audit-remediation/explore`.
