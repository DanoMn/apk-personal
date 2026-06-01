# Handoff — Sueño como consumidor de `device-telemetry`

> Para arrancar en **sesión nueva** sin contaminar contexto. Cargar este doc +
> `docs/sueno/sleep-feature-preliminar.md` (edge cases detallados) + recuperar memoria de
> Engram (proyecto `apk-personal`).
>
> Fecha: 2026-05-29 · Proyecto: apk-personal (Vocal) · Estado del prerrequisito:
> **`device-telemetry` ENTREGADO** (en `main`; ciclo SDD completo, archive #611).

---

## 0. Qué es esto (y qué NO es)

Sueño es el **primer consumidor** de `device-telemetry`. La telemetría ya existe y
produce hechos crudos de actividad del dispositivo en Room. **Sueño no capta nada**:
**lee** esos hechos, los **interpreta** ("¿durmió?, ¿despertó de verdad?") y produce
**un solo número 0..1** que el núcleo de scoring ubica en la capa **Cuerpo**.

**NO es** infraestructura de captura (eso ya está hecho). NO mezclar con telemetría:
si Sueño y telemetría se exploran juntos se acopla el productor al consumidor y se
pierde la reutilización (decisión D3 de telemetría).

```
[device-telemetry]  hechos crudos en Room (YA EXISTE)
      │  (Room. PULL — telemetría no conoce a Sueño)
      ▼
[Sueño·dominio]  LEE hechos → interpreta → sleepScore 0..1   ← LO QUE HAY QUE CONSTRUIR
      ▼
[Núcleo·scoring]  SpecialLayerScoringPolicy ubica sleepScore en Cuerpo (SLEEP_WEIGHT_IN_BODY)
      ▼
[LayerScore Cuerpo] → BaseState → ScoreReport
```

---

## 1. El contrato que la telemetría ya dejó listo (cómo consume Sueño)

Todo esto YA existe y compila. Sueño solo tiene que usarlo:

| Necesidad | API disponible (en `main`) |
|-----------|----------------------------|
| Encender la recolección al activar modo automático | `DeviceTelemetryWorkScheduler.register(context, "sleep")` (suspend) |
| Apagarla al desactivar | `DeviceTelemetryWorkScheduler.unregister(context, "sleep")` (suspend) |
| Leer los hechos de la noche | `AppGraph.telemetryRepository(context).eventsInRange(from, to)` → `List<DeviceActivityEvent>` (suspend) |
| Saber si falta el permiso (para UX) | `telemetryRepository.permissionState()` → `GRANTED | MISSING` |
| Mandar al usuario a conceder el permiso | `TelemetryPermission.settingsIntent()` |

El hecho crudo que recibe Sueño:

```kotlin
DeviceActivityEvent(
    eventType: DeviceActivityEventType, // SCREEN_ON/SCREEN_OFF/UNLOCK/LOCK/
                                        // APP_FOREGROUND/APP_BACKGROUND/USER_INTERACTION
    packageName: String?,               // qué app (null en eventos de pantalla)
    timestamp: Long,                    // epoch millis
    source: String,
)
```

> Importante: la telemetría habla de **unlock/lock/pantalla**, NO de "despertar".
> Traducir eventos crudos → dormidas/despertadas reales **es trabajo de Sueño** (§4).
> En API 26/27 puede faltar SCREEN_*/UNLOCK; llegan APP_FOREGROUND/USER_INTERACTION
> como proxy. La interpretación de Sueño debe tolerar señal más gruesa.

---

## 2. Estado REAL de Sueño hoy (a corregir)

Lo que existe es un flujo **manual de una sola sesión** (sin telemetría conectada):

- `AutonomiaRepository.startSleepSession()` (`:443`) — "ir a dormir": escribe **una**
  `SleepSessionState` y opcionalmente bloquea (DeviceAdmin).
- `AutonomiaRepository.finishSleepSession()` (`:459`) — "Desperté": guarda **un**
  `SleepLog` (`sleptAt`/`wokeAt`). Una dormida, una despertada por día.

Problemas conocidos (de `sleep-feature-preliminar.md`):

1. **Fricción:** nadie aprieta "Desperté" a las 3am. → el flujo automático (telemetría) lo resuelve.
2. **El modelo NO soporta noche fragmentada:** `SleepLog` tiene UN solo par
   `sleptAt`/`wokeAt` (`Models.kt:85`). Soportar telemetría exige **segmentos** →
   cambio de modelo → **migración Room** (recordar: índices `index_*`, no `idx_*`).
3. **Detox cableado a medias:** `digitalWindDownMinutes` se valida pero `SleepScoring`
   no lo mira; y `sleptAt` se estampa al apretar el botón → el detox **se cuenta como sueño**.
4. **Scoring incompleto:** `SleepScoring` usa 2 de 4 componentes
   (`duration*0.70 + schedule*0.30`); el árbol pide **4**:
   `Duration 0.40 / Continuity 0.25 / ScheduleAlignment 0.20 / DigitalInterruption 0.15`
   (`docs/scoring/arbol-scoring-vocal-v1.md`). Continuity y DigitalInterruption faltan.

---

## 3. Decisiones ya tomadas (no reabrir)

- **Telemetría = opt-in** vía `register("sleep")`; corre sola en background mientras el
  modo automático esté activo (no se aprieta nada cada noche). Apagada por defecto.
- **Local-first / privacy-first:** todo en el dispositivo.
- **Sueño es core** del scoring (alimenta Cuerpo). Sin dato de sueño → no se llega a
  estados altos (techo, no piso — `nucleo-dominio-autonomia.md`).
- **La interpretación vive en `domain/sleep/`** (puro), NO en telemetría. Sueño produce
  un `0..1`; el núcleo decide la capa y el peso. Confirmado: `domain/sleep/` es el lugar
  correcto (gemelo de sobriedad en `SpecialLayerScoringPolicy`).
- **Dos modos conviven:** A) telemetría automática (default esperado), B) manual/metódico.

---

## 4. Edge cases a resolver (el corazón del `sdd-explore` de Sueño)

Esto es lo que falta DEFINIR antes de escribir tests con valores confiables. Detalle
completo en `sleep-feature-preliminar.md` §3:

| # | Edge case | Pregunta abierta |
|---|-----------|------------------|
| 1 | ¿Despertó DE VERDAD? | Un unlock de 30s a las 3am NO es despertar. ¿Umbral? ¿debounce? ¿ventana mínima de actividad? Vive en `domain/sleep`. |
| 2 | Frontera detox / inicio de sueño | ¿El `sleptAt` real arranca al terminar el detox o al primer lock sostenido? Hoy sobre-cuenta. |
| 3 | Noche fragmentada | ¿Cómo se agregan N segmentos en una métrica diaria? ¿Suma de duración? ¿penaliza fragmentación (Continuity)? |
| 4 | Sin marcar "ir a dormir" | ¿La telemetría infiere la dormida sola, o requiere el disparo manual como ancla? |
| 5 | Cierre de la noche | ¿Cuándo se materializa el `SleepLog` del día? ¿primer unlock sostenido tras la ventana objetivo? |
| 6 | Convivencia A/B | Si hay manual Y telemetría, ¿cuál gana? |

---

## 5. Qué hay que construir (alcance probable)

1. **Lectura + interpretación (dominio):** un componente en `domain/sleep` que toma
   `List<DeviceActivityEvent>` de la noche → infiere segmentos de sueño → resuelve los
   edge cases §4 → produce los 4 sub-scores y el `sleepScore` final 0..1.
2. **Modelo de datos:** migrar `SleepLog` de un par único a **segmentos** (o tabla de
   segmentos) para soportar noche fragmentada. → migración Room numerada.
3. **Scoring completo:** llevar `SleepScoring` de 2 a **4 componentes**
   (Duration/Continuity/ScheduleAlignment/DigitalInterruption) según el árbol; conectar
   `digitalWindDownMinutes` (hoy inerte).
4. **Wiring de los 2 modos:** al activar modo automático → `register("sleep")`; al
   desactivar → `unregister("sleep")`; UX de permiso si `permissionState()==MISSING`.
5. **Cierre de la noche:** quién/ cuándo materializa el `SleepLog` diario (probablemente
   vía `DailyClosureWorker` / al abrir la app), leyendo la telemetría de la ventana.

---

## 6. Cómo arrancar (sesión nueva)

1. Recuperar contexto de Engram (`mem_search` + `mem_get_observation`):
   - `sdd/device-telemetry/archive-report` (#611) — qué dejó listo la telemetría.
   - `sdd/device-telemetry/spec` (#604) — el contrato de hechos.
   - `device-telemetry/scoping-decisions` (#600) — D1–D8.
   - `sdd-init/apk-personal` (#418) — capacidades de testing + Strict TDD.
2. Leer este handoff + `docs/sueno/sleep-feature-preliminar.md` (§3 edge cases) +
   `docs/scoring/arbol-scoring-vocal-v1.md` (fórmula de los 4 componentes) +
   `docs/producto/nucleo-dominio-autonomia.md` (Sueño es core).
3. Lanzar **`sdd-explore`** de `sleep-consumer`: resolver los edge cases §4 (la parte
   conceptual difícil), decidir el modelo de segmentos y los 4 componentes del scoring.
   **NO** re-explorar telemetría — ya está; solo se consume (§1).
4. Seguir el ciclo SDD (proposal → spec → design → tasks → apply → verify → archive).
   Strict TDD activo: la interpretación de sueño es **lógica pura JVM** → test-first.

> Recordatorio de build/tests (CLAUDE.md): compilar para verificar; desde la shell del
> agente escapar `\$env`; filtrar tests con `testDebugUnitTest --tests '...'`.

---

## 7. Archivos relevantes

| Pieza | Ruta |
|-------|------|
| Contrato de consumo (telemetría) | `data/repository/TelemetryRepository.kt`, `platform/telemetry/*` |
| Encender/apagar recolección | `platform/telemetry/DeviceTelemetryWorkScheduler.kt` |
| Scoring de sueño (2 de 4 — completar) | `domain/sleep/SleepScoring.kt` |
| Validación/política | `domain/sleep/SleepPolicy.kt` |
| Modelos (a migrar a segmentos) | `Models.kt:85-109` (`SleepLog`, `SleepConfig`, `SleepSessionState`) |
| Flujo manual actual | `AutonomiaRepository.kt:417-471` |
| Ubicación en capa | `domain/scoring/SpecialLayerScoringPolicy.kt` (sueño → Cuerpo) |
| Cierre diario | `AutonomiaRepository.closeElapsedActivityDays` + `data/worker/DailyClosureWorker.kt` |
| Contrato matemático | `docs/scoring/arbol-scoring-vocal-v1.md` |

---

## 8. Secuencia global (recordatorio)

```
1. device-telemetry        → contrato de hechos genérico         ✅ HECHO (#611)
2. Sueño como consumidor    → ESTE handoff (lee + interpreta)     ← PRÓXIMO
3. Tests con valores confiables → recién cuando 2 esté cerrado
```

Deuda pre-release pendiente del proyecto (no de Sueño): registrar `MIGRATION_10_11` +
`MigrationTestHelper` + `exportSchema` (tarea 5.2 de device-telemetry). Si Sueño agrega
otra migración (segmentos), aplicar la misma disciplina.
