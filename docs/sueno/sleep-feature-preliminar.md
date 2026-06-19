# Sueño — Documento preliminar (BORRADOR)

> **Estado: vivo** — se actualiza cuando cambia el codigo que describe.

> **Estado: requisitos / edge cases (NO diseño cerrado).** Captura lo que se ESPERA
> de la feature de Sueño y los edge cases detectados. **`device-telemetry` YA está
> entregado** (en `main`), así que el prerrequisito está resuelto: este doc es ahora
> el **apéndice de edge cases** del handoff de arranque
> **`meta/handoffs/handoff-sleep-consumer.md`** — empezá por ahí.
>
> Fecha: 2026-05-29 · Proyecto: apk-personal (Autonomía sin límites)

---

## 0. Por qué este doc existe

Sueño es **la fase más automatizada del producto y la base del scoring** (aporta a
la capa Cuerpo). Si su lógica de configuración y procesamiento no se define con
detalle, el scoring entrega métricas incorrectas. El agente anterior dejó la
feature a medias: hay un botón, pero NO la telemetría que se suponía que existía.

---

## 1. Estado REAL hoy (verificado en código)

La **telemetría YA existe** (`device-telemetry`, en `main`): produce hechos crudos de
actividad del dispositivo y se consume vía `TelemetryRepository` +
`DeviceTelemetryWorkScheduler.register("sleep")` (ver `handoff-sleep-consumer.md` §1).
Lo que falta es **conectar Sueño como consumidor**. El flujo de Sueño hoy sigue siendo
**manual de una sola sesión** (la telemetría todavía no está cableada a Sueño):

- `AutonomiaRepository.startSleepSession()` (`:443`) — al apretar "ir a dormir",
  escribe **una** `SleepSessionState` con `startedAt = ahora` y, opcionalmente,
  bloquea el teléfono (`lockPhoneNow` vía DeviceAdmin).
- `AutonomiaRepository.finishSleepSession()` (`:459`) — al apretar "Desperté",
  calcula `wokeAt = ahora` y guarda **un** `SleepLog` (`sleptAt=startedAt`,
  `wokeAt=ahora`). Una dormida, una despertada, por día.
- `SleepDeviceAdminReceiver.kt` está **vacío** (`: DeviceAdminReceiver()`); existe
  solo para el permiso de `lockNow`. NO escucha `SCREEN_ON/OFF`, NI `USER_PRESENT`,
  NI hay `BroadcastReceiver`, NI `UsageStats`.

### Problemas que esto genera

1. **Rompe el principio de mínima fricción.** El usuario debe volver a abrir la app
   y apretar "Desperté" manualmente. Nadie hace eso a las 3am, menos 3 veces por
   noche. Notificar cada vez sería igual de molesto.
2. **El modelo de datos NO soporta noche fragmentada.** `SleepLog` tiene UN solo
   par `sleptAt`/`wokeAt` (`Models.kt:85`). No puede representar "despertó, volvió a
   dormir, N veces". Soportar telemetría exige cambiar el modelo (segmentos), no es
   fix chico → migración Room.
3. **El detox digital está cableado a medias.** `digitalWindDownMinutes` se
   configura y valida (`SleepPolicy`) pero `SleepScoring` **ni lo mira** — es
   inerte. Y `startSleepSession` estampa `sleptAt = ahora` apenas se aprieta, así
   que el tiempo de detox **se cuenta como sueño** (sobre-cuenta).
4. **El scoring usa 2 de 4 componentes** (`SleepScoring`: `duration*0.70 +
   schedule*0.30`); el árbol pide `0.40 / 0.25 / 0.20 / 0.15`
   (Duration / Continuity / ScheduleAlignment / DigitalInterruption). Ver
   `docs/scoring/old/arbol-scoring-v1.md`.

---

## 2. Qué se espera (los dos flujos)

Sueño debe ofrecer **dos modos**, y el usuario elige:

### Flujo A — Telemetría automática (mínima fricción, modo por defecto esperado)

- El usuario aprieta "ir a dormir" una vez. A partir de ahí, **el telemetry-core
  detecta** desbloqueos/bloqueos de pantalla y los registra como hechos crudos.
- Sueño **lee** esos hechos e **infiere** las dormidas/despertadas reales de la
  noche, hasta el día siguiente.
- El usuario no toca nada más.

### Flujo B — Manual / metódico

- Para usuarios que prefieren marcar explícitamente cada vez que se van a dormir,
  desde la app. Es el flujo actual, pero debe convivir con A, no ser el único.

> **Importante:** Sueño NO implementa la detección. La detección la hace el
> telemetry-core (infraestructura aparte, reutilizable). Sueño es **consumidor**:
> lee hechos crudos y los interpreta. Ver el handoff del telemetry-core.

---

## 3. Edge cases sin resolver (lo que falta definir)

Estos son los problemas que faltaron plantear y que hay que cerrar ANTES de
escribir tests con valores esperados confiables:

| # | Edge case | Pregunta abierta |
|---|-----------|------------------|
| 1 | **¿Cuándo despertó DE VERDAD?** | Un unlock de 30s a las 3am para ver la hora NO es despertar. ¿Umbral de actividad mínima? ¿Debounce? ¿Ventana? Esta interpretación vive en el DOMINIO de sueño, no en el telemetry-core. |
| 2 | **Frontera detox / inicio de sueño** | Si el detox bloquea 20min antes de dormir, ¿ese tiempo cuenta como sueño? Hoy sí (mal). ¿El `sleptAt` real arranca al terminar el detox, o al primer lock sostenido? |
| 3 | **Noche fragmentada** | ¿Cómo se agregan N segmentos de sueño en una métrica diaria? ¿Suma de duración? ¿Penaliza la fragmentación (Continuity del árbol)? |
| 4 | **Sin marcar "ir a dormir"** | Si el usuario se duerme sin apretar nada, ¿la telemetría puede inferir igual la dormida? ¿O requiere el disparo manual como ancla? |
| 5 | **Cierre de la noche** | ¿Cuándo se considera "terminada" la noche y se materializa el `SleepLog` del día? ¿Primer unlock sostenido tras la ventana objetivo? |
| 6 | **Convivencia A/B** | Si el usuario usa el modo manual pero también hay eventos de telemetría, ¿cuál gana? |

---

## 4. Procesamiento hasta el scoring (cómo encaja, sin acoplar)

El flujo correcto, con cada caja ignorando a la siguiente:

```
[Telemetría]   detecta unlock/lock → escribe HECHO crudo en Room (neutral, ciego)
      │  (Room. Telemetría NO conoce a sueño)
      ▼
[Sueño·dominio] LEE los hechos → interpreta ("¿despertó de verdad?") → sleepScore 0..1
      │  (sueño entrega UN número. NO conoce capas)
      ▼
[Núcleo·scoring] SpecialLayerScoringPolicy coloca sleepScore en Cuerpo con su peso
      ▼
[LayerScore Cuerpo] → BaseState → ScoreReport
```

- Sueño produce **un score 0..1**; NO sabe que existe la capa Cuerpo.
- El núcleo (`SpecialLayerScoringPolicy.kt:12-16`) lo ubica en Cuerpo con
  `SLEEP_WEIGHT_IN_BODY`, combinado con anclas/soportes de esa capa.

---

## 5. Archivos relevantes

| Pieza | Ruta |
|-------|------|
| Config UI | `app/src/main/java/dev/panopt/autonomia/ui/sleep/SleepConfigScreen.kt` |
| Validación/política | `domain/sleep/SleepPolicy.kt` |
| Scoring de sueño | `domain/sleep/SleepScoring.kt` (⚠ 2 de 4 componentes) |
| Modelos | `Models.kt:85-109` (`SleepLog`, `SleepConfig`, `SleepSessionState`) |
| Persistencia/flujo | `AutonomiaRepository.kt:417-471` |
| Ubicación en capa | `domain/scoring/SpecialLayerScoringPolicy.kt` (30% Cuerpo) |
| DeviceAdmin (vacío) | `sleep/SleepDeviceAdminReceiver.kt` |

---

## 6. Dependencia y secuencia

1. ✅ **HECHO**: `device-telemetry` (infraestructura genérica de captura) — entregado
   en `main`, ciclo SDD completo (archive Engram #611). Ver `meta/handoffs/handoff-device-telemetry.md`.
2. **AHORA**: explorar Sueño como **consumidor** de ese contrato de hechos, resolver
   los edge cases de la §3, y recién ahí escribir tests con valores esperados
   confiables. Punto de arranque: **`meta/handoffs/handoff-sleep-consumer.md`**.

> No mezclar ambos scopes: si Sueño y telemetría se exploran juntos, se acopla el
> productor al consumidor y se pierde la reutilización (tracking de proyecto futuro).
