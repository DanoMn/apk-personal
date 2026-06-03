# Plan: actualizar docs en drift (brief para agentes)

> Cada agente actualiza UN doc. Lee este archivo (verdad compartida + reglas + tu
> sección) + tu doc objetivo + los archivos de código indicados. Editá, no reescribas.

## Verdad actual del sistema (verificada contra código, 2026-06-01)

- **Esquema Room: v12** (`app/src/main/java/dev/panopt/autonomia/data/AutonomiaDatabase.kt`).
- **22 entidades REGISTRADAS** en `@Database`: LayerEntity, ActivityDefinitionEntity,
  UserActivityConfigEntity, DailyActivityLogEntity, AbstinenceTrackEntity,
  AbstinenceLogEntity, AbstinenceRelapseEventEntity, RiskEventEntity, TaskEntity,
  AnchorPhraseEntity, AnchorPhraseStateRuleEntity, AnchorPhrasePhaseRuleEntity,
  AnchorPhraseImpressionEntity, AnchorPhraseDailySlotEntity, SleepConfigEntity,
  SleepSessionStateEntity, SleepNightEntity, SleepSegmentEntity, DailyClosureEntity,
  WeeklyScoreSnapshotEntity, DeviceActivityEventEntity, TelemetryCollectionLeaseEntity.
- **Legacy / NO registradas** (existen como clase pero fuera de `@Database`):
  - `ActivityLogEntity` (tabla `activity_logs`) — legacy; la canónica diaria es
    `DailyActivityLogEntity` (`daily_activity_logs`).
  - `SleepLogEntity` (tabla `sleep_logs`) — **DROPEADA en MIGRATION_11_12**.
  - `ActivityEntity` (tabla `activities`) — **DROPEADA en MIGRATION_3_4**, ya no existe.
- **Sueño v2**: `SleepNightEntity` (sleep_nights) + `SleepSegmentEntity` (sleep_segments)
  reemplazan a `SleepLog`. Config: `SleepConfigEntity`; estado de sesión:
  `SleepSessionStateEntity`. Interpretación en `domain/sleep/interpretation/`
  (`SleepInterpreter`). Scoring de sueño = 4 componentes: duración 0.40, continuidad
  0.25, alineación 0.20, interrupción digital 0.15 (ver `domain/scoring/` y
  `docs/scoring/arbol-scoring-v1.md`).
- **Telemetría (local, sin red/backend)**: `DeviceActivityEventEntity`
  (device_activity_events) + `TelemetryCollectionLeaseEntity` (telemetry_collection_lease).
  Captura uso del dispositivo (UsageStats), drenada por worker. Capa
  `platform/telemetry/`; `TelemetryRepository` en `data/repository/`.
- **Cierre diario**: `DailyClosureEntity` (daily_closures) + `DailyClosureWorker`.
- **Cache de scoring**: `WeeklyScoreSnapshotEntity` (weekly_score_snapshots), derivado.
- **Recaídas**: `AbstinenceRelapseEventEntity` (abstinence_relapse_events).
- **Pantallas reales** (enum `AppScreen` en `MainActivity.kt`): Dashboard, Scoring,
  AnchorConfig (anclas), Supports (soportes), Tasks (pendientes), Sobriety, SleepConfig.
  **NO existen** pantallas "Checklist" ni "Progreso". `RiskEvent` es entidad, sin
  pantalla dedicada. UI: `ui/{anchors,dashboard,scoring,sleep,sobriety,supports,tasks}`.
- **Capas de paquetes**: `data/{local,repository,scoring,worker}`,
  `domain/{abstinence,activity,closure,dashboard,scoring,sleep,task}`,
  `platform/telemetry`, `sleep/` (DeviceAdminReceiver), `ui/`.
- **Modelo de actividades**: ActivityDefinitionEntity → UserActivityConfigEntity
  (Anchor/Support/Task) → DailyActivityLogEntity. `ActivitySurface{Anchor,Support,Task}`
  es el enum vigente; `DisplaySurface` es legacy.

## Reglas para TODOS los agentes

1. **EDITÁ el doc existente; NO lo reescribas de cero.** Preservá estructura, voz
   (español, tono del proyecto) y el header `> **Estado: vivo**`.
2. Actualizá SOLO lo desactualizado. No toques lo que ya es correcto.
3. **VERIFICÁ contra el código** antes de escribir un dato. No inventes campos ni
   nombres. Si dudás de un campo exacto, leé el `*Entity` correspondiente.
4. Términos de código/clases en inglés; prosa en español.
5. NO toques otros docs ni el código. Solo tu doc objetivo.
6. Al terminar, devolvé un resumen corto (qué cambiaste) como mensaje final. No
   guardes nada en engram.

---

## Doc 1 — `docs/datos-room/definicion-tablas-room-v1.md`
- **Mal**: dice esquema v5; faltan entidades nuevas; `sleep_logs` figura como activa/futura.
- **Hacer**: actualizar a **v12**; documentar las 22 entidades registradas (sobre todo
  las nuevas: sleep_nights, sleep_segments, sleep_config, sleep_session_state,
  daily_closures, weekly_score_snapshots, device_activity_events,
  telemetry_collection_lease, abstinence_relapse_events); marcar `activity_logs` y
  `sleep_logs` como legacy/dropeada y `activities` como eliminada; mencionar que las
  migraciones llegan a la 12.
- **Leer**: `data/AutonomiaDatabase.kt` (entities, version, migraciones) + los archivos
  de entidades (buscá `*Entity.kt` / `Entities.kt`) para los campos exactos.

## Doc 2 — `docs/producto/nucleo-dominio-autonomia.md`
- **Mal**: describe `SleepLog` (sleptAt/wokeAt/quality); usa `TrackedActivity`; nota de
  "investigar sueño automático en el futuro".
- **Hacer**: reemplazar el modelo `SleepLog` por `SleepNight`/`SleepSegment`; cambiar
  `TrackedActivity` por `ActivityDefinition`; marcar el sueño automático como
  **IMPLEMENTADO** vía telemetría (ya no es futuro).
- **Leer**: `domain/sleep/`, `platform/telemetry/`, entidades de sueño.

## Doc 3 — `docs/producto/estado-actual-mvp.md`
- **Mal**: lista pantallas `Checklist`/`Progreso` (no existen); "no hacer: tracking
  automático de celular" (ya está hecho); dice "Tablas v1".
- **Hacer**: poner la lista real de pantallas (Dashboard, Scoring, SleepConfig, Tasks,
  Sobriety, Anclas, Soportes); sacar "tracking automático" de "no hacer" (ya
  implementado vía telemetría); actualizar a tablas v12.
- **Leer**: `MainActivity.kt` (enum `AppScreen`).

## Doc 4 — `docs/frontend/mapa_componentes_v_0_2_borrador.md`
- **Mal**: al mapa de componentes le falta la capa `platform/telemetry`,
  `domain/sleep/interpretation`, y el modelo `SleepNight`/`SleepSegment`.
- **Hacer**: agregar esas capas/componentes al mapa, ubicándolas en la arquitectura.
- **Leer**: estructura de paquetes (`domain/sleep`, `platform/telemetry`, `ui/`).

## Doc 5 — `docs/dominio/mapa-flujos-estado-actual-2026-05-24.md`
- **Mal**: muestra `sleepLogForDate` como fuente (es stub `flowOf(null)`, TODO WU-6);
  faltan las pantallas Scoring y SleepConfig en el mapa; sueño pre-v2.
- **Hacer**: actualizar el flujo de sueño a `sleepNightForDateFlow` + segmentos;
  agregar Scoring y SleepConfig a la navegación; reflejar sueño v2.
- **Leer**: `data/repository/AutonomiaRepository.kt` (flows de sueño), `MainActivity.kt`.

## Doc 6 — `docs/producto/plan-maestro-roadmap.md`
- **Mal**: ítems ya implementados sin marcar como hechos.
- **Hacer**: marcar como completados los que el código confirma (ej. `SleepConfigScreen`
  existe; scoring de sueño sin "calidad subjetiva"; presets de soporte en
  `SupportsConfigScreen`). Verificá CADA ítem contra el código antes de marcarlo.
- **Leer**: `ui/sleep/`, `domain/scoring/` (SleepScoring), `ui/supports/`.
