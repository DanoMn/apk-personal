# Tasks: sleep-consumer (Sueño como primer consumidor de `device-telemetry`)

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 900–1 200 (new files + modified files across 7 work-units) |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 (WU-1+WU-2) → PR 2 (WU-3+WU-4) → PR 3 (WU-5+WU-6) → PR 4 (WU-7) |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: pending
400-line budget risk: High

> **Decisión requerida antes de apply:** estrategia de cadena (stacked-to-main vs
> feature-branch-chain) + registro de `MIGRATION_11_12` (ver §ADR-4: rompe el
> patrón actual del repo donde NINGUNA migración está registrada; primera migración
> testeada con `MigrationTestHelper` + `exportSchema = true`).

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| WU-1 | Room schema v12: entidades + migración + DAO | PR 1 | Test con `MigrationTestHelper`, `exportSchema=true`, índices `index_*` |
| WU-2 | Modelos de dominio: `NightTimeline`, `SleepSegment`, `InterpretationParams` | PR 1 | Tipos puros JVM, sin Room ni Android |
| WU-3 | `SleepInterpreter` (eventos → timeline) | PR 2 | Test-first: 6 escenarios del spec |
| WU-4 | `SleepScoring` refactor (2 → 4 componentes, sin decay) | PR 2 | Test-first: pesos sellados, sin superávit |
| WU-5 | Fix `SpecialLayerScoringPolicy` NoData + agregación semanal | PR 3 | Fix bug §10 crítico; cambia `ScoreInput`/`ScoreInputSource` |
| WU-6 | Cierre de noche: `materializeSleepNight` + `DailyClosureWorker` + mappers | PR 3 | Idempotente; convivencia manual |
| WU-7 | Wiring modo automático + UX permiso + toggle UI | PR 4 | Depende de WU-1+WU-6 |

---

## Phase 1: Datos y modelo Room (WU-1) — fundamento del que dependen todo lo demás

> Spec: `sleep-night-model`. Bugs cubiertos: Bug §10 `quality` hardcodeado, Bug §10 `SleepLog` un solo par.
> TOCA ROOM Y MIGRACIÓN — disciplina estricta obligatoria.

- [x] 1.1 [DECISIÓN] Confirmar estrategia de cadena de PRs y registrar `MIGRATION_11_12` antes de iniciar apply. Bloquea WU-1+. — Resuelto: auto-chain, PR1=WU-1+WU-2.
- [x] 1.2 Activar `exportSchema = true` en `AutonomiaDatabase.kt:35` (hoy es `false`). Verificar que `room.schemaLocation = "$projectDir/schemas"` ya está en `app/build.gradle.kts:21` (ya existe — solo cambiar el flag).
- [x] 1.3 Agregar entidad `SleepNightEntity` en `data/Entities.kt` (tabla `sleep_nights`, PK `nightDate`, campos: `targetSleepAt`, `targetWakeAt`, `sleepOnsetAt Long?`, `definitiveWakeAt Long?`, `confidenceLevel String`, sub-scores `Float?` × 5, `note`, `source`, `updatedAt`). Eliminar `quality` del modelo.
- [x] 1.4 Agregar entidad `SleepSegmentEntity` en `data/Entities.kt` (tabla `sleep_segments`, FK → `sleep_nights.nightDate`, `Index("nightDate")` → genera `index_sleep_segments_nightDate`). Reemplazar `SleepLogEntity` en la lista `entities` de `@Database` por `SleepNightEntity` + `SleepSegmentEntity`.
- [x] 1.5 Escribir `MIGRATION_11_12` en `AutonomiaDatabase.kt`: CREATE `sleep_nights`, CREATE `sleep_segments` + `CREATE INDEX index_sleep_segments_nightDate` (naming **exacto**, NO `idx_*`), DROP `sleep_logs`. Registrar en `addMigrations(MIGRATION_10_11, MIGRATION_11_12)`. Mantener `.fallbackToDestructiveMigration`.
- [x] 1.6 [TEST-FIRST — Room] Crear `app/src/androidTest/java/dev/panopt/autonomia/data/SleepMigration11To12Test.kt` con `MigrationTestHelper`. RED: compilación falla antes de las entidades. GREEN: BUILD SUCCESSFUL + schema v12 generado. Nota: test instrumentado (requiere device/emulador para ejecutar — no JVM).
- [x] 1.7 Compilar con `assembleDebug` y verificar que el JSON de esquema v12 es generado en `app/schemas/dev.panopt.autonomia.data.AutonomiaDatabase/12.json`. — Verificado: archivo existe con sleep_nights + sleep_segments, sin sleep_logs.
- [x] 1.8 Agregar DAOs en `AutonomiaDao.kt`: `upsertSleepNight`, `getSleepNight(date)`, `getSleepNightsInRange(from, to)`, `observeSleepNightForDate(date): Flow<SleepNightEntity?>`, `getSleepSegments(date)`, `deleteSleepSegmentsForNight(date)`, `insertSleepSegments(segments)`.
- [x] 1.9 Actualizar mapper `SleepNightEntity.toDomain()` en `data/local/mapper/DomainMappers.kt:206`. Eliminar referencias a `SleepLogEntity`/`quality` de `DomainMappers.kt`.

---

## Phase 2: Modelos de dominio puros (WU-2)

> Sin Room ni Android. Base de tipos para WU-3 y WU-4.

- [x] 2.1 Crear `domain/sleep/interpretation/SleepModels.kt` con: `SleepSegment(startAt, endAt, kind)`, `SleepSegmentKind { Asleep, AwakeUse }`, `SleepConfidence { High, Ambiguous, NoData }`, `NightTimeline(nightDate, segments, sleepOnsetAt?, definitiveWakeAt?, confidence)`.
- [x] 2.2 Crear `domain/sleep/interpretation/InterpretationParams.kt` con los 5 umbrales calibrables (`quietGapMillis` ~15min, `napSeparationMillis` ~90min, `napAnchorWindowMinutes` ~120, `definitiveWakeMinMinutes` ~10, `returnToSleepMinMinutes` ~30) y `companion object { val DEFAULT }`.
- [x] 2.3 Crear `domain/sleep/SleepNightScore.kt` con: `SleepNightScore(duration, continuity, alignment, digitalInterruption, sleepScore, confidence)`, `SleepTargetWindow(targetSleepAt, targetWakeAt)`.
- [x] 2.4 Actualizar `Models.kt:85`: agregar `SleepNight` (cabecera), `SleepLog` marcado @Deprecated (UI legacy). `SleepConfig`/`SleepSessionState` se mantienen. `SleepQuality` queda para UI legacy.

---

## Phase 3: `SleepInterpreter` puro (WU-3)

> Spec: `sleep-interpretation`. Strict TDD: RED → GREEN → REFACTOR por escenario.
> Test runner: `gradlew testDebugUnitTest --tests 'dev.panopt.autonomia.domain.sleep.SleepInterpreterTest'`

- [x] 3.1 [TEST-FIRST RED] Crear `app/src/test/java/dev/panopt/autonomia/domain/sleep/SleepInterpreterTest.kt`. Escribir casos fallidos (spec `sleep-interpretation`): (a) eventos fuera de ventana descartados, (b) `SCREEN_ON` solo = vistazo no crea `AwakeUse`, (c) `USER_INTERACTION` crea `AwakeUse`, (d) `APP_FOREGROUND` crea `AwakeUse`, (e) tanda agrupada = un solo `AwakeUse`, (f) onset = quietud tras último uso real, (g) detox no cuenta como onset anchor, (h) siesta lejos del objetivo excluida, (i) noche pertenece al día de despertar, (j) teléfono quieto → `High`, (k) sin señal → `NoData` sin score, (l) señal contradictoria → `Ambiguous`, (m) API 26 sin `SCREEN_*` funciona con proxies.
- [x] 3.2 Crear `domain/sleep/interpretation/SleepInterpreter.kt` como `object` puro JVM. Implementar: ventana de detección, discriminación vistazo/uso-real, agrupación de episodios (`quietGapMillis`), construcción de línea de tiempo, `sleepOnsetAt`, `definitiveWakeAt`, anclaje al objetivo (anti-siesta), `nightDate` = día del despertar, espectro de confianza. [GREEN para todos los RED de 3.1]
- [x] 3.3 Refactor: extraer funciones auxiliares privadas (`groupAwakeUseEpisodes`, `selectMainSleepBlock`, `detectDefinitiveWake`, `assignConfidence`). Todos los tests de 3.1 siguen en verde.

---

## Phase 4: `SleepScoring` refactor (WU-4)

> Spec: `sleep-scoring-v1` §componentes + §weekly. Bugs cubiertos: Bug §10 `SleepScoring` 2→4 componentes, Bug §10 decay de superávit.
> Test runner: `gradlew testDebugUnitTest --tests 'dev.panopt.autonomia.domain.sleep.SleepScoringTest'`

- [x] 4.1 [TEST-FIRST RED] Crear `app/src/test/java/dev/panopt/autonomia/domain/sleep/SleepScoringTest.kt`. Casos: (a) pesos sellados `0.40/0.25/0.20/0.15` (escenario con valores conocidos = 0.76), (b) dormir de más → `DurationScore = 1.0` (no decae), (c) duración exacta → 1.0, (d) duración parcial → proporcional, (e) cero `AwakeUse` → `ContinuityScore = 1.0`, (f) múltiples `AwakeUse` bajan continuidad, (g) dormir fuera del objetivo baja solo Alineación, (h) cero uso nocturno → `DigitalInterruptionScore = 1.0`, (i) `digitalWindDownMinutes` no afecta score, (j) `NoData` → devuelve `null`, (k) `Ambiguous` → score atenuado por `ambiguousConfidenceFactor`, (l) Cuerpo con sueño al 30% (escenario BodyScore = 0.74).
- [x] 4.2 Reemplazar `SleepScoring.kt`: cambiar firma a `fun scoreNight(timeline: NightTimeline, target: SleepTargetWindow): SleepNightScore?`. Implementar los 4 componentes con pesos sellados. `DurationScore = clamp(actual/target, 0, 1)` sin decay. `ContinuityScore = clamp(0.5·exp(-awakeCount/k) + 0.5·longestAsleepRatio, 0, 1)` con `k ≈ 2`. `ScheduleAlignmentScore` reusar `SleepPolicy.scheduleCloseness`. `DigitalInterruptionScore = exp(-awakeUseMinutes/m)` con `m ≈ 30`. Atenuación `Ambiguous` por `ambiguousConfidenceFactor ≈ 0.85`. `NoData` → `null`. [GREEN para 4.1]
- [x] 4.3 Refactor: constantes calibrables (`k`, `m`, `ambiguousConfidenceFactor`) en un `object SleepScoringParams` o en `InterpretationParams`. Tests verdes.
- [x] 4.4 Documentar en `SleepScoring.kt` (comentario KDoc) que `digitalWindDownMinutes` es inerte a propósito (D3). Documentado también en `SleepScoringParams.kt` (campo inerte). ScoreInputSource.kt docs = WU-5 scope.

---

## Phase 5: Fix `SpecialLayerScoringPolicy` NoData + agregación semanal (WU-5)

> Spec: `base-state-policy` + `sleep-scoring-v1` §weekly. Bugs cubiertos: Bug §10 `null → 0f → hunde Cuerpo`, Bug §10 `una sola noche`.
> Test runner: `gradlew testDebugUnitTest --tests 'dev.panopt.autonomia.domain.scoring.*'`

- [x] 5.1 [TEST-FIRST RED] Crear `SpecialLayerScoringPolicyTest.kt` con 8 casos: (a) `sleepScore = null` → Cuerpo NO usa `0f` sino solo `baseWithoutSpecial` (re-normalizado), (b) `sleepScore = null` semana A vs `0.2` semana B → Cuerpo(A) ≥ Cuerpo(B) (ausente ≠ malo), (c) NoData no infla (no mejor que sueño perfecto), (d)-(g) casos adicionales formula + non-body layers.
- [x] 5.2 Corregir `SpecialLayerScoringPolicy.kt` en ambos métodos `baseScore` y `rawScore`: sustituir `sleepScore ?: 0f` por la rama re-normalizada: si `sleepScore == null` usar solo `baseWithoutSpecial`; si no-null, usar la fórmula completa. [GREEN — 8 tests pasan]
- [x] 5.3 Cambiar `ScoreInput` y `ScoreInputSource` en `ScoreModels.kt`/`ScoreInputSource.kt`: reemplazar campo `sleepLog: SleepLog?` por `sleepNights: List<SleepNightScore>`. Todos los callers actualizados (DashboardProjection, BuildScoreInputUseCase, tests).
- [x] 5.4 [TEST-FIRST RED → GREEN] Crear `WeeklySleepAggregationTest.kt` con 5 casos: (a) [0.8, 0.7, NoData, 0.9, NoData] → 0.800, (b) 3×1.0 + 3×NoData → 1.0 (no 0.5), (c) all NoData → null, (d) empty → null, (e) single night → that score.
- [x] 5.5 Actualizar `WeeklyScoringContextBuilder.kt:32`: reemplazar `input.sleepLog?.let(SleepScoring::score)` por promedio aritmético de noches con dato. Si lista vacía → `null`. [GREEN para 5.4]
- [x] 5.6 Actualizar `hasAnyFact` en `WeeklyScoringContextBuilder.kt:56`: reemplazar `input.sleepLog != null` por `input.sleepNights.any { it.sleepScore != null }`.
- [x] 5.7 Actualizar `ScoreSnapshotHashPolicy.kt:48`: reemplazar la entrada `sleep:${log.date}:...` por hash iterando `sleepNights` (confidence+sleepScore+duration+continuity).
- [x] 5.8 Actualizar `WeeklyScoreSnapshotWriter.kt:36`: reemplazar `sleepLog = null` puente por `dao.getSleepNightsInRange(weekStart, dateKey).mapNotNull { it.toSleepNightScore() }`.

---

## Phase 6: Cierre de noche + mappers + DailyClosureWorker (WU-6)

> Spec: `sleep-auto-mode` §cierre + §garantía al abrir. Bug cubierto: cierre sin materialización de segmentos.

- [x] 6.1 Agregar `suspend fun materializeSleepNight(nightDate: LocalDate, zoneId: ZoneId): Boolean` en `AutonomiaRepository.kt`. Implementado: convivencia manual → skip; ventana 20:00 D-1 → 12:00 D; TelemetryRepository.eventsInRange; SleepInterpreter.interpret; SleepScoring.scoreNight; upsertSleepNight + deleteSleepSegmentsForNight + insertSleepSegments (idempotente).
- [x] 6.2 `saveSleepLog`/`finishSleepSession` ya escriben `SleepNightEntity` con `source="manual"` desde PR1 (WU-1). Segmento Asleep único = deuda WU-6 menor (PRE-EXISTING — todo manual write already uses SleepNightEntity). SleepQuality.Acceptable eliminado en PR1.
- [x] 6.3 Actualizar `DailyClosureWorker.kt`: tras `closeElapsedActivityDays`, llamar `materializeSleepNight(today, zoneId)` ANTES de `refreshCurrentWeeklyScoreSnapshot`. Orden correcto: cierre → materialización → snapshot.
- [x] 6.4 Agregar llamada a `materializeSleepNight(nightDate = today)` en `DashboardViewModel.init` (app-open guarantee) después de `closeElapsedActivityDays`.
- [x] 6.5 `assembleDebug` BUILD SUCCESSFUL — sin errores de compilación. Solo deprecation warnings de SleepLog en UI (UI migration = WU-7).

---

## Phase 7: Wiring modo automático + UX permiso (WU-7)

> Spec: `sleep-auto-mode` §register/unregister + §permission UX + §manual coexistence.

- [x] 7.1 Agregar función `toggleSleepAutoMode(enabled: Boolean, context: Context)` en `AutonomiaRepository.kt` (o ViewModel de Sueño): cuando `enabled = true` verifica `telemetryRepository.permissionState()`; si `GRANTED` → `DeviceTelemetryWorkScheduler.register(context, "sleep")`; si `MISSING` → emitir estado para UX compasiva. Cuando `enabled = false` → `DeviceTelemetryWorkScheduler.unregister(context, "sleep")`.
- [x] 7.2 Persistir estado del toggle (en `SleepConfigEntity` o como preferencia DataStore): añadir campo `autoModeEnabled: Boolean` para sobrevivir reinicio de app. Implementado via SharedPreferences "sleep_auto_mode_enabled" (mismo patrón que dark_mode — evita nueva migración Room).
- [x] 7.3 Actualizar `SleepConfigScreen.kt`: agregar toggle para modo automático. Cuando falta permiso, mostrar prompt compasivo (tono AGENTS.md) con botón que abre `TelemetryPermission.settingsIntent()`. Sin crash, sin fallo silencioso (spec `sleep-auto-mode` §permiso).
- [x] 7.4 Verificar que el modo manual (`startSleepSession`/`finishSleepSession`) funciona sin activar telemetría (no llama register, no muestra prompt de permiso). Verificado: manual flow usa saveSleepLog directamente sin tocar DeviceTelemetryWorkScheduler.

---

## Phase 8: Tests de integración y verificación final

- [x] 8.1 Correr suite de scoring: `gradlew testDebugUnitTest --tests 'dev.panopt.autonomia.domain.scoring.*'`. Todos los tests deben ser verdes, incluyendo `ScoreEngineTest`, `BaseStatePolicyTest`, `BuildScoreInputUseCaseTest`. Verificado: BUILD SUCCESSFUL (PR4).
- [x] 8.2 Correr suite de sueño: `gradlew testDebugUnitTest --tests 'dev.panopt.autonomia.domain.sleep.*'`. Verde: `SleepInterpreterTest`, `SleepScoringTest`, `SleepPolicyTest`. Verificado: BUILD SUCCESSFUL (PR4).
- [ ] 8.3 Correr test de migración (androidTest en device/emulador): `SleepMigration11To12Test` debe pasar con `MigrationTestHelper`, esquema v12 coincide con entidades. PENDIENTE: requiere dispositivo/emulador.
- [ ] 8.4 Instalación limpia en device (`adb uninstall dev.panopt.autonomia` + `adb install app-debug.apk`): verificar que la app abre sin crash de migración, el dashboard muestra Cuerpo correctamente sin sueño (no hunde al 30%), y el toggle de modo automático aparece en pantalla de configuración de Sueño. PENDIENTE: requiere dispositivo.
- [x] 8.5 Verificar snapshot JSON generado: confirmar que `app/schemas/.../12.json` está generado y commiteado. Verificado: 12.json existe desde PR1.

---

## Bug-to-Task Map (§10 del contrato)

| Bug | Tarea |
|-----|-------|
| `SleepScoring` usa 2 de 4 componentes | 4.2 |
| Dormir de más decae el puntaje (hasta 0.5) | 4.2 |
| `null → 0f` hunde Cuerpo | 5.2 |
| `digitalWindDownMinutes` inerte sin documentar | 4.4 |
| `quality` hardcodeado a `Acceptable` | 1.3, 1.4, 6.2 |
| `SleepLog` = un solo par (no soporta fragmentación) | 1.3, 1.4, 2.1, 6.2 |
| Scoring mira una sola noche, árbol pide semanal | 5.3, 5.5 |
