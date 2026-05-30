# Proposal: sleep-consumer (Sueño como primer consumidor de `device-telemetry`)

## Intent

Hoy Sueño es un flujo **manual de una sola sesión**: el usuario aprieta "ir a dormir"
(`AutonomiaRepository.startSleepSession`) y "Desperté" (`finishSleepSession`), lo que
escribe **un** `SleepLog` con un único par `sleptAt`/`wokeAt` y un `quality` hardcodeado a
`Acceptable`. Esto rompe el contrato en tres frentes: (1) **fricción** — nadie aprieta un
botón a las 3am, así que la mejor noche queda sin registrar; (2) **modelo insuficiente** —
un par único no soporta una noche fragmentada con despertares reales; (3) **scoring
incompleto** — `SleepScoring` usa solo 2 de los 4 componentes sellados (`duration·0.70 +
schedule·0.30`), penaliza dormir de más, deja `digitalWindDownMinutes` inerte, y la
ausencia de dato baja a `0f` hundiendo la capa Cuerpo en vez de leerse como base incompleta.

`device-telemetry` ya está **entregado en `main`** (archive #611) y produce hechos crudos
de actividad del dispositivo en Room. Sueño es su **primer consumidor**: debe **leer** esos
hechos, **interpretarlos** ("¿durmió?, ¿despertó de verdad?") en lógica pura JVM, y producir
un único `sleepScore` 0..1 que el núcleo ubica en Cuerpo al 30%. El diseño conceptual está
**cerrado y acordado con el dueño** (`docs/decisiones-diseno-sueno-v1.md`); esta propuesta
lo traduce a alcance ejecutable sin reabrir decisiones.

**Por qué ahora**: la telemetría sin consumidor no produce valor; Sueño es core del scoring
(`nucleo-dominio-autonomia.md`) y sin él la base nunca se completa. Las fórmulas de sueño
están **selladas** (`arbol-scoring-vocal-v1.md` §11) y los edge cases conceptuales ya están
resueltos, así que el riesgo de diseño es mínimo y el camino crítico es la implementación
disciplinada (modelo de datos durable + interpretación test-first).

**Éxito** = una noche con el teléfono quieto se lee como buen sueño de alta confianza sin
que el usuario toque nada; una noche sin señal se lee como NoData (nunca como 0 que hunde
Cuerpo); los 4 componentes sellados se calculan desde segmentos durables; y el agregado
semanal promedia solo las noches con dato.

## Scope

### In Scope

- **Interpretación de telemetría → segmentos (`domain/sleep/`, lógica pura JVM, test-first)**:
  un intérprete que toma `List<DeviceActivityEvent>` de la ventana de detección y produce la
  **línea de tiempo completa** de la noche (segmentos `Asleep` | `AwakeUse`). Resuelve el
  edge case #1 (despertar real = uso real `USER_INTERACTION`/`APP_FOREGROUND`, no `SCREEN_ON`
  solo); inicio del sueño = quietud tras el último uso real (disuelve el bug detox→sueño);
  tolerancia a señal más gruesa en API 26/27 (proxy `APP_FOREGROUND`/`USER_INTERACTION`).
- **Las dos ventanas (§3)**: ventana de DETECCIÓN biológica fija `20:00`–`12:00` del día
  siguiente (alimenta Duración/Continuidad/InterrupciónDigital) y ventana OBJETIVO configurable
  (alimenta solo AlineaciónHorario). Anclaje al objetivo para descartar siestas. La noche
  pertenece al **día del despertar**.
- **Modelo de datos nuevo + migración Room (esquema v11 → v12)**: cabecera de la noche
  (evolución de `SleepLog`, PK = fecha de despertar: `targetSleepAt`/`targetWakeAt`,
  `sleepOnsetAt`, `definitiveWakeAt`, nivel de **confianza** alta/ambigua/NoData, `note`) +
  **`SleepSegmentEntity`** nueva (hija de la noche: `startAt`, `endAt`, `kind`). Disciplina
  estricta: índices `index_<tabla>_<col>` (no `idx_*`), `MigrationTestHelper`, `exportSchema`.
  Segmentos = HECHO PRIMARIO durable (la telemetría cruda se purga; los agregados se recalculan
  desde segmentos al recalibrar umbrales).
- **Scoring de 4 componentes sellados (corregir `SleepScoring.kt`)**: derivar de los segmentos
  `Duration 0.40 / Continuity 0.25 / ScheduleAlignment 0.20 / DigitalInterruption 0.15`. Sin
  superávit en v1: cumplir el objetivo = 1.0 y se queda (neutro, no decae).
- **Lectura NoData en el pipeline (corregir `SpecialLayerScoringPolicy`/pipeline)**: ausencia
  de sueño = base incompleta, NO `0f` fabricado. "Poca señal" ≠ "baja confianza" (§4.2).
- **Agregación semanal (corregir `WeeklyScoringContextBuilder`)**: cada noche → su `sleepScore`;
  la semana = **promedio de las noches CON dato** (cobertura suave). Una noche sin dato no entra
  como cero; pocas noches con dato → lectura débil que no habilita estados altos.
- **Cierre de noche híbrido (§7)**: materializar la noche al detectar el despertar definitivo
  (uso real sostenido tras la hora objetivo de despertar, sin volver a dormir); tope de
  seguridad = fin de la ventana biológica. Disparo: reusar `DailyClosureWorker` (medianoche)
  + garantía al abrir la app. Debe materializar **antes** de que la telemetría se purgue.
- **Wiring de los dos modos + UX de permiso**: al activar el modo automático →
  `DeviceTelemetryWorkScheduler.register(context, "sleep")`; al desactivar → `unregister`.
  UX que detecta `telemetryRepository.permissionState() == MISSING` y manda a
  `TelemetryPermission.settingsIntent()`. Modo manual/metódico (B) sigue conviviendo.
- **Bugs del §10 a corregir** (detallados en Affected Areas): los 7 bugs del código actual.

### Out of Scope (deuda diferida a propósito — §9, NO son olvidos)

- **D1 — Piso de cobertura DURO** (mínimo N noches con dato o NoData explícito). v1 usa
  cobertura suave; el N exacto se calibra con datos reales.
- **D2 — Superávit de sueño** (bonus de margen por dormir hacia el techo de 8h). Cuando entre,
  será bonus de margen (gemelo de `AnchorSurplusBonus`), NUNCA dentro del puntaje base; el
  "vaso" se corta en 1.0.
- **D3 — Detox digital en el scoring**: `digitalWindDownMinutes` queda inerte **a propósito**
  en v1 (recordatorio visual en config, no puntúa). `DigitalInterruption` se calcula solo por
  uso real durante el sueño detectado.
- **D4 — Término de consistencia explícito**: el árbol sellado tiene 4 componentes sin término
  de consistencia propio; se evalúa a futuro.
- **Cambios en pesos/fórmulas selladas** (`arbol-scoring-vocal-v1.md` §11): NO se rediscuten.
- **Distinción "dormido" vs "despierto sin tocar el teléfono"** (leer un libro): un tramo en
  silencio se asume `Asleep`. Aceptado para v1 (§6).
- **UI final / pantallas pulidas de Sueño**: esta propuesta cubre el wiring del modo automático
  y la UX mínima de permiso, no el rediseño visual completo.

## Capabilities

### New Capabilities

- `sleep-interpretation`: lectura de `List<DeviceActivityEvent>` de la ventana de detección →
  segmentos de la noche (`Asleep`/`AwakeUse`) con nivel de confianza, lógica pura JVM.
- `sleep-night-model`: cabecera de la noche + `SleepSegmentEntity` como hecho primario durable,
  con migración Room v11→v12.
- `sleep-scoring-v1`: los 4 componentes sellados derivados de segmentos + agregación semanal
  por promedio de noches con dato.
- `sleep-auto-mode`: wiring `register`/`unregister("sleep")` + UX de permiso de telemetría.

### Modified Capabilities

- `base-state-policy` (indirecto): la lectura NoData de sueño afecta cómo Cuerpo entra al
  estado base — ausencia = base incompleta, no piso fabricado. No se altera ningún umbral
  sellado; solo se corrige el origen del `SleepWeeklyScore` que ya consume.

## Approach

**Frontera de responsabilidad (no reabrir)**: cada caja ciega a la siguiente.

```
device-telemetry  → guarda HECHO crudo en Room        (ya existe; no conoce a Sueño)
Sueño (domain/sleep) → LEE telemetría + interpreta → segmentos + sleepScore 0..1
Motor de scoring   → recibe el número ya digerido → LayerScore Cuerpo → ScoreReport
```

El motor de scoring NUNCA toca telemetría cruda; solo `domain/sleep` la lee. La interpretación
es lógica pura → **test-first** (Strict TDD activo).

**1. Interpretación (el corazón, test-first).** Un intérprete puro recibe los eventos crudos
de la ventana de detección `20:00`–`12:00` y los colapsa en episodios de uso real: una tanda
seguida de `USER_INTERACTION`/`APP_FOREGROUND` que termina cuando el teléfono se vuelve a
quedar quieto = **un** `AwakeUse`. Un `SCREEN_ON` aislado sin uso = vistazo, se ignora. El
inicio del sueño es la quietud tras el último uso real (no un botón). Salida: línea de tiempo
completa de segmentos + nivel de confianza (alta si hay quietud limpia; ambigua si la señal es
contradictoria; NoData si no hay señal). Regla de oro: poca señal ≠ baja confianza — no
castigar la mejor noche por generar poca señal.

**2. Modelo + migración (hecho durable).** Como la telemetría cruda se purga en días, los
segmentos son el hecho primario durable, no un cache descartable; guardarlos permite recalcular
agregados al recalibrar umbrales (los agregados no se des-agregan). `SleepLog` evoluciona a la
cabecera (PK = fecha de despertar); nace `SleepSegmentEntity` (hija). Migración v11→v12 con la
disciplina del proyecto (índices `index_*`, `MigrationTestHelper`, `exportSchema`). DB de dev
descartable → preferir instalación limpia para probar el esquema; el seed canónico NO se toca.

**3. Scoring (de 2 a 4 componentes).** De los segmentos: suma de `Asleep` → Duración; conteo
de `AwakeUse` + `Asleep` más largo → Continuidad; suma de `AwakeUse` → InterrupciónDigital;
cercanía de `sleepOnsetAt`/`definitiveWakeAt` a la ventana objetivo → AlineaciónHorario.
`SleepWeeklyScore = 0.40·Duración + 0.25·Continuidad + 0.20·Alineación + 0.15·Interrupción`
(sellado). Sin superávit: Duración llega a 1.0 al cumplir el objetivo y se queda. Cuerpo:
`Body = 0.70·BodyBaseWithoutSleep + 0.30·SleepWeeklyScore`.

**4. NoData en el pipeline.** Corregir el camino ausencia→`null`→`0f` para que una noche sin
dato no entre como cero ni fabrique estado bajo. Pocas noches con dato → lectura débil (techo,
no piso): sueño es core, no habilita estados altos sin dato suficiente.

**5. Agregación semanal.** Promedio de las noches con dato (cobertura suave). Reemplaza la
mirada de una sola noche en `WeeklyScoringContextBuilder`.

**6. Cierre híbrido.** Reusar `DailyClosureWorker` (medianoche) + garantía al abrir la app;
sin maquinaria nueva. El cierre lee la telemetría de la ventana e infiere el despertar
definitivo (tope: fin de ventana biológica) ANTES de la purga.

**7. Wiring de modos + permiso.** Toggle de modo automático → `register`/`unregister("sleep")`;
si `permissionState()==MISSING`, UX compasiva que ofrece conceder el permiso. Manual (B) sigue.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `domain/sleep/` (intérprete nuevo) | New | Eventos crudos → segmentos `Asleep`/`AwakeUse` + confianza; lógica pura, test-first |
| `domain/sleep/SleepScoring.kt` | Modified | **Bug §10**: 2→4 componentes sellados; neutralizar decaimiento por dormir de más (cumplir objetivo = 1.0) |
| `domain/sleep/SleepPolicy.kt` | Modified | Validación/política de las dos ventanas (detección fija + objetivo configurable, mín. 5h) |
| `Models.kt:85` (`SleepLog`/`SleepConfig`/`SleepSessionState`) | Modified | **Bug §10**: par único `sleptAt`/`wokeAt` → cabecera de noche; eliminar `quality` hardcodeado |
| `data/local/` `SleepSegmentEntity` + DAO | New | Tabla de segmentos (hecho durable); índices `index_<tabla>_<col>` |
| `AutonomiaDatabase.kt` (migración v11→v12) | Modified | Migración numerada nueva + `MigrationTestHelper` + `exportSchema` (disciplina D8) |
| `AutonomiaRepository.kt:417-471, :435` | Modified | **Bug §10**: `quality` hardcodeado fuera; flujo manual coexiste con cierre automático |
| `data/worker/DailyClosureWorker.kt` + `closeElapsedActivityDays` | Modified | Cierre de noche híbrido: materializar segmentos antes de la purga de telemetría |
| `domain/scoring/SpecialLayerScoringPolicy.kt` / pipeline | Modified | **Bug §10**: ausencia → NoData/base incompleta, NO `0f` que hunde Cuerpo |
| `ScoreInputSource` / `BuildScoreInputUseCase` | Modified | **Bug §10**: `digitalWindDownMinutes` queda inerte a propósito (D3), documentado |
| `WeeklyScoringContextBuilder.kt:32` | Modified | **Bug §10**: de una noche → promedio semanal de noches con dato |
| Modo automático (toggle + UX permiso) | New/Modified | `register`/`unregister("sleep")`; `permissionState()` → `settingsIntent()` |
| `.../domain/sleep/*Test.kt` | New | Tests de interpretación + 4 componentes + agregación semanal (TDD primero) |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Interpretación de "despertar real" mal calibrada (falsos positivos/negativos) | Med | Gatillo cualitativo cerrado (uso real, no `SCREEN_ON`); umbrales calibrables porque los segmentos durables permiten recalcular; piso de cobertura duro queda como D1 |
| Señal más gruesa en API 26/27 (faltan `SCREEN_*`/`UNLOCK`) | Med | El intérprete tolera proxy `APP_FOREGROUND`/`USER_INTERACTION`; nivel de confianza absorbe la ambigüedad |
| Migración Room mal hecha pasa tests de dominio y crashea en device | Med | `MigrationTestHelper` obligatorio + `exportSchema`; índices `index_*` (no `idx_*`); probar con instalación limpia (DB de dev descartable) |
| Telemetría se purga antes del cierre → se pierde la noche | Med | Cierre híbrido dispara en `DailyClosureWorker` (medianoche) + garantía al abrir la app, antes de la purga |
| Tramo en silencio asumido `Asleep` sobrecuenta sueño (leer un libro) | Low | Aceptado para v1 (§6); documentado como límite conocido |
| Ausencia de dato vuelve a hundir Cuerpo si el fix de NoData es incompleto | Med | Test explícito: noche NoData no produce `0f`; pocas noches → techo no piso |

## Rollback Plan

Cambio con migración Room (v11→v12), así que no es puramente reversible por `git revert` en
una DB ya migrada — pero la fase de dev tiene **DB local descartable** (decisión #29): revertir
= `git revert` del/los commits + instalación limpia (`adb uninstall` + `adb install`). El seed
canónico no se toca, así que la app se repuebla sola. El motor de scoring vuelve al
`SleepScoring` de 2 componentes y a la lectura de una sola noche. Sin datos de producción en
riesgo.

## Dependencies

- **`device-telemetry`** entregado en `main` (archive #611): `DeviceTelemetryWorkScheduler.register/unregister("sleep")`,
  `TelemetryRepository.eventsInRange(from, to)`, `permissionState()`, `TelemetryPermission.settingsIntent()`,
  el shape `DeviceActivityEvent` y el enum `DeviceActivityEventType`. Ninguna otra externa.
- Deuda pre-release del proyecto (D8): registrar `MIGRATION_10_11` + `MigrationTestHelper` +
  `exportSchema` (tarea 5.2 de telemetría); aplica la misma disciplina a la migración de segmentos.

## Success Criteria

- [ ] Una noche con el teléfono quieto se interpreta como buen sueño de **alta confianza** sin que el usuario marque nada.
- [ ] Una noche **sin señal** se lee como **NoData** (nunca `0f` que hunde Cuerpo); pocas noches con dato → lectura débil que no habilita estados altos.
- [ ] La interpretación produce segmentos `Asleep`/`AwakeUse` correctos: `USER_INTERACTION`/`APP_FOREGROUND` = despertar real; `SCREEN_ON` aislado = vistazo ignorado.
- [ ] Los **4 componentes sellados** se calculan desde segmentos con los pesos `0.40/0.25/0.20/0.15`; dormir de más es **neutro** (1.0, no decae).
- [ ] `SleepWeeklyScore` = promedio de las **noches con dato** (cobertura suave); noche sin dato no entra como cero.
- [ ] Modelo migrado: cabecera de noche + `SleepSegmentEntity`; migración v11→v12 con `index_<tabla>_<col>`, `MigrationTestHelper` verde y `exportSchema`.
- [ ] Cierre de noche híbrido materializa los segmentos **antes** de la purga de telemetría (vía `DailyClosureWorker` + apertura de app).
- [ ] Modo automático: `register("sleep")` al activar, `unregister("sleep")` al desactivar; UX compasiva de permiso si `permissionState()==MISSING`.
- [ ] Los 7 bugs del §10 corregidos (o documentados como inertes a propósito en el caso de `digitalWindDownMinutes`/D3).
- [ ] Tests de interpretación + 4 componentes + agregación semanal escritos **ANTES** de la implementación (Strict TDD).
- [ ] La frontera se respeta: el motor de scoring no toca telemetría cruda; solo `domain/sleep` la lee.
