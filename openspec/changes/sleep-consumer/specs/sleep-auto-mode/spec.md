# Sleep Auto Mode Specification

Change: `sleep-consumer`
Source: `docs/decisiones-diseno-sueno-v1.md` §2, §7 · `proposal.md` In Scope

## Purpose

Defines the wiring for automatic sleep detection mode: `register`/`unregister("sleep")` lifecycle, hybrid night closure, and the permission UX when telemetry is unavailable. Manual mode (mode B) continues to coexist.

---

## Requirements

### Requirement: Register/Unregister on Mode Toggle

When the user activates automatic sleep mode, the system MUST call `DeviceTelemetryWorkScheduler.register(context, "sleep")`. When the user deactivates automatic sleep mode, the system MUST call `DeviceTelemetryWorkScheduler.unregister(context, "sleep")`. These MUST be the only entry points for toggling telemetry capture for sleep.

#### Scenario: Activar modo automático registra telemetría

- GIVEN automatic sleep mode is currently OFF
- WHEN the user enables automatic mode
- THEN `DeviceTelemetryWorkScheduler.register(context, "sleep")` is called
- AND telemetry capture for sleep begins

#### Scenario: Desactivar modo automático desregistra telemetría

- GIVEN automatic sleep mode is currently ON
- WHEN the user disables automatic mode
- THEN `DeviceTelemetryWorkScheduler.unregister(context, "sleep")` is called
- AND telemetry capture for sleep stops

---

### Requirement: Permission Missing UX

When `telemetryRepository.permissionState() == MISSING` and the user attempts to enable automatic mode, the system MUST present a compassionate UI prompt explaining why the permission is needed and offering a direct path to grant it via `TelemetryPermission.settingsIntent()`. The UI MUST NOT show an error crash or a silent failure.

#### Scenario: Permiso faltante → UX compasiva con acción

- GIVEN `permissionState() == MISSING`
- WHEN the user tries to activate automatic sleep mode
- THEN a non-blocking, compassionate prompt is shown (no crash, no silent failure)
- AND the prompt offers a button/action that opens `TelemetryPermission.settingsIntent()`

#### Scenario: Permiso disponible → activación directa sin prompt

- GIVEN `permissionState() != MISSING`
- WHEN the user activates automatic mode
- THEN `register("sleep")` is called directly without showing a permission prompt

---

### Requirement: Manual Mode Coexistence

Manual mode (the existing `startSleepSession` / `finishSleepSession` flow) MUST continue to function when automatic mode is OFF. A user who prefers manual entry MUST be able to log sleep without enabling automatic mode.

#### Scenario: Modo manual funciona sin modo automático

- GIVEN automatic sleep mode is OFF
- WHEN the user uses the manual "voy a dormir" / "Desperté" flow
- THEN a `SleepLog` is written correctly
- AND the system does not attempt to register telemetry or show permission prompts

---

### Requirement: Hybrid Night Closure

The night MUST be closed (segments materialized) when the definitive wakeup is detected: sustained real use (`USER_INTERACTION`/`APP_FOREGROUND`) after the goal wake time without returning to sleep. If no definitive wakeup is detected, the night MUST be closed at the end of the biological detection window (`12:00`). Both triggers MUST fire BEFORE raw telemetry is purged.

#### Scenario: Cierre al despertar definitivo detectado

- GIVEN automatic mode is ON, goal wake time is `06:00`
- WHEN sustained `USER_INTERACTION` events are detected after `06:00` with no return to sleep
- THEN the night is closed: segments are materialized and persisted

#### Scenario: Tope de seguridad al fin de ventana biológica

- GIVEN automatic mode is ON, no definitive wakeup detected
- WHEN the clock reaches `12:00` (end of detection window)
- THEN the night is closed via `DailyClosureWorker` using available telemetry up to that point

#### Scenario: Cierre antes de la purga de telemetría

- GIVEN the night closure trigger fires (either wakeup detected or `12:00` tope)
- WHEN segments are materialized
- THEN this happens BEFORE raw `DeviceActivityEvent` rows for that window are purged
- AND no segments are lost due to purge race condition

#### Scenario: Garantía al abrir la app

- GIVEN the nightly `DailyClosureWorker` did not run (e.g., device off at midnight)
- WHEN the user opens the app
- THEN the app checks for unclosed nights and closes them using available telemetry before presenting the dashboard
