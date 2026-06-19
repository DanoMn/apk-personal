> **Estado: foto de investigación (2026-06-17).** Este doc NO es contrato vivo: es el MAPA de
> comportamiento ACTUAL del motor ante la dinámica activar/desactivar/reactivar, hecho para
> planificar fixes en una sesión posterior. No se actualiza con el código; si el comportamiento
> cambia, se escribe la decisión en el contrato (`modelo-scoring-oficial-v1.md` /
> `plan-tecnico-scoring.md`) y este mapa queda como registro de cómo estaba antes.

# Mapa de casos límite — ciclo de vida del motor de scoring

Sesión de **mapeo** (no de implementación). Caracteriza el comportamiento REAL del motor núcleo v1
cuando una entidad se **activa / desactiva / reactiva** dentro de la ventana móvil de 7 días.
Verificado contra el código (seams con `archivo:línea`) y con **sondas ejecutables**
(`app/src/test/java/dev/panopt/autonomia/domain/scoring/ScoreLifecycleProbeTest.kt`, 5 sondas verdes).

---

## 0. Sueño — DECIDIDO: es OPT-IN (igual que la sobriedad)

**Decisión del dueño (2026-06-17, ya cerrada durante el armado del motor matemático):** el sueño **es
un opt-in**, no un "pilar CORE 30% de Cuerpo". Modelo canónico:

- **Opt-in de sueño ACTIVO + sin datos esa semana** → `M = B_SLEEP = 0.5` (no tira al piso; la
  telemetría puede fallar). Con datos → `M = cobertura·promedio + (1−cobertura)·0.5`.
- **Opt-in de sueño INACTIVO** → el sueño **no aparece, no pesa, no limita** (`B_SLEEP` NO aplica),
  exactamente como un track de sobriedad inactivo.

Contrato actualizado en esta sesión: `axiomas-opt-in-v1.md` O8 (matiz "solo con opt-in activo"),
`modelo-scoring-oficial-v1.md` §7/§16, y se **archivó** el modelo viejo `arbol-scoring-v1.md` a
`old/` (era la fuente de la contaminación "sueño = pilar / no opt-in", §11/§16.7).

> **Divergencia código↔modelo, PENDIENTE de implementación** (no se toca en esta sesión de mapeo):
> el código de hoy infiere "opt-in activo" por **presencia de datos** (sin datos → `M = null`,
> término ausente; con datos → promedio simple, sin `B_SLEEP` ni cobertura) — ver
> `ScoringFactsAdapter.sleepSignal` (`ScoringFactsAdapter.kt:146`) y `WeeklyScoringContextBuilder.kt:35-39`.
> Para alinear con el modelo decidido hace falta: (1) un estado real de **opt-in de sueño activo/inactivo**
> separado de "hay datos", y (2) implementar `B_SLEEP=0.5` + cobertura cuando está activo y sin datos.
> Hoy NO existe flag de opt-in en `SleepConfigEntity` (`data/Entities.kt:237`). Queda para la planificación.

---

## 1. El principio que gobierna todo (confirmado)

```
hechos diarios (daily_activity_logs / abstinence_logs / sleep_nights)  ← NUNCA se borran
   → el ScoreInput se arma SOLO con configs activas (filtro upstream)
   → se re-lee la VENTANA de los últimos 7 días, ENTERA, en cada cálculo
   → motor calcula
```

**Hallazgo central:** el filtro `active` ocurre **aguas arriba** del motor, en dos lugares:
`BuildScoreInputUseCase.kt:8-18` (arma el input solo con activos) y, para el seam de persistencia,
`getActiveUserActivityConfigs()` (`WeeklyScoreSnapshotWriter.kt:113`). Cuando el motor corre, las
entidades desactivadas **ya no están en el input**. Por eso "desactivar" = la entidad desaparece;
sus logs siguen en Room sin usarse. "Reactivar" = vuelve a entrar y sus logs **dentro de la ventana**
se re-leen (`WeeklyScoringContextBuilder.kt:16-19` agrupa TODOS los logs por `activityId`; solo se
consumen los de las activities visibles).

**H1 (desactivar = deja de contribuir YA): CONFIRMADA.**
**H2 (reactivar = los datos vuelven, acotados a la ventana 7d): CONFIRMADA.** Sonda P2: reactivada =
0.75 = nunca-desactivada (mismos logs de ventana → mismo estado).
**H3 (el historial semanal NO se reescribe solo): CONFIRMADA** (ver §7).

---

## 2. MAPA — Anclas

| Caso | Comportamiento actual observado | Veredicto | Seam |
|------|--------------------------------|-----------|------|
| Desactivar un ancla a mitad de semana | Sale de `visibleActivities` → su término R desaparece ENTERO, incluidos los días que SÍ se cumplió. Sus logs siguen en la base, sin usar. | **NECESITA TU DECISIÓN** (handoff §2.5: ¿debería contar lo hecho mientras estuvo activa?) | `WeeklyScoringContextBuilder.kt:13-15`; `ScoreEngine.kt:67-75` |
| Desactivar un ancla cuando hay EXACTAMENTE 3 capas-con-ancla | Cae el gate de config mínima (`MIN_ACTIVE_LAYERS_WITH_ANCHOR = 3`) → **el reporte entero colapsa a `NoData`** (estado 0.0), no a "un poco más bajo". **Sonda P1:** activo 0.75 → desactivado `NoData/0.0`. | **CORRECTO** como consecuencia del gate (sin config mínima no hay estado), pero es un **acantilado** abrupto a documentar | `ScoreEngine.kt:40-45,175-181`; `ScoringConstants.MIN_ACTIVE_LAYERS_WITH_ANCHOR=3` |
| Desactivar 1 ancla habiendo 4+ capas-con-ancla | No cae a NoData: solo se pierde el término de esa capa (baja parcial) | **CORRECTO** (degradación proporcional) | `ScoreEngine.kt:65-92` |
| Reactivar el ancla | Vuelve a `visibleActivities`; sus logs de la ventana 7d se re-leen → estado idéntico a no haberse desactivado. Lo anterior a 7 días es irrelevante por diseño (ventana móvil). | **CORRECTO** (coherente con la ventana móvil) | `WeeklyScoringContextBuilder.kt:16-19`; sonda P2 |

---

## 3. MAPA — Soportes (UX inversa)

Semántica confirmada (NO era el bug que sospeché): el cierre diario escribe `status=Done` para un
soporte, pero el mapper lo traduce a `completed=false` (`DomainMappers.kt:131`:
`Done → subjectType != Support`), y `countsAsDone()` (`ScoringExtensions.kt:36`) da `false`. O sea
**`Done` de soporte = sostenido** (no resta). Solo `Omitted` (el usuario marcó que SÍ hizo lo que el
soporte evita) → `completed=true` → resta días sostenidos. La inversión está bien implementada.

| Caso | Comportamiento actual observado | Veredicto | Seam |
|------|--------------------------------|-----------|------|
| Soporte sin registro en la ventana | `días_sostenidos = 4` (s = 1.0): "sin registro = sostenido". **Sonda P3** + test existente `soporteSinRegistros_totalmenteSostenido`. | **CORRECTO** (por diseño UX inversa) | `ScoringFactsAdapter.kt:89-98` |
| Desactivar un soporte a mitad de semana | Sale de `visibleActivities` → no aporta nada mientras está apagado. Además el cierre diario **no escribe logs** para configs inactivas (`closeActivityDay` filtra activas). | **CORRECTO** (deja de contribuir, H1) | `WeeklyScoringContextBuilder.kt:13-15`; `AutonomiaRepository.kt:310,315-322` |
| **Reactivar un soporte** que estuvo días apagado | La ventana indulgente de 4 días ve esos días OFF **sin logs** → los cuenta como **sostenidos** (s sube). Premia como "te mantuviste limpio" días en que el soporte estaba **apagado**, no sostenido. | **NECESITA TU DECISIÓN** (handoff §2.3: ¿reactivar debería "perdonar" o NO acreditar los días OFF?) | `ScoringFactsAdapter.kt:89-98` (ausencia de log = sostenido, sin distinguir "apagado" de "limpio") |

---

## 4. MAPA — Sobriedad (opt-in, tracks)

| Caso | Comportamiento actual observado | Veredicto | Seam |
|------|--------------------------------|-----------|------|
| Track limpio (0 recaídas en ventana) | Factor `M_sobr = 1` → invisible, no diluye | **CORRECTO** | `ScoringFactsAdapter.kt:127-139`; test `trackLimpioNoDiluye` |
| Desactivar un track a mitad de semana | `activeSobrietyTracks = filter { active }` → sale; su arrastre desaparece YA. `weeklyAbstinenceLogsByTrack` solo agrupa logs de tracks activos (`WeeklyScoringContextBuilder.kt:28-29`). | **CORRECTO** (H1) | `WeeklyScoringContextBuilder.kt:20-22,28-29` |
| **Reactivar un track** con recaídas en los últimos 7 días | Vuelve a `activeTracks`; sus `abstinence_logs` de la ventana se re-leen → **el arrastre de la recaída REAPARECE**. **Sonda P4:** con track+recaída 0.657 < 0.75 sin track. | **CORRECTO — DECIDIDO** (anti-trampa #3): la recaída de la ventana DEBE reaparecer al reactivar; no se perdona apagando/prendiendo. | `WeeklyScoringContextBuilder.kt:28-33`; `ScoreEngine.kt:51-59`; sonda P4 |
| `materializeAssumedAbstinenceRelapses` (cierre diario asume recaídas no registradas) | Corre en el cierre; interactúa con desactivar/reactivar (un track apagado no recibe cierre → no se asumen recaídas mientras está OFF). | **A VERIFICAR en planificación** (no sondeado en profundidad esta sesión) | `DailyClosureWorker` (materialize); `AutonomiaRepository` |

---

## 5. MAPA — Tasks (efímero diario)

Nota: las tasks **no se filtran por `active`** en `BuildScoreInputUseCase` (pasan enteras desde
`source.tasks`, `BuildScoreInputUseCase.kt:21`). Lo que las gobierna es el filtro "completada HOY,
con capa, no Neutral".

| Caso | Comportamiento actual observado | Veredicto | Seam |
|------|--------------------------------|-----------|------|
| Task `Done` HOY con capa | Cuenta `n_tasks_hoy` para esa capa (canal "extra") | **CORRECTO** | `ScoringFactsAdapter.kt:106-120` |
| Task `Done` AYER | No cuenta hoy (efímero: compara `completedDate == today`). El snapshot de ayer SÍ la contó ese día. | **CORRECTO** (efímero por diseño) | `ScoringFactsAdapter.kt:112-120`; test `tasksDeHoyCuentan_lasDeAyerNo` |
| Task se completa y se "descompleta" varias veces hoy | Solo cuenta el estado ACTUAL: si `status != Done` o `completedAt` no es hoy → no cuenta. Idempotente respecto al estado final del día. | **CORRECTO** (lee estado actual, no historial de toggles) | `ScoringFactsAdapter.kt:112-120` |
| Cambiar `layerId` o `contributionRole` de una task ya completada hoy | El conteo se recalcula con los valores ACTUALES (capa nueva / si pasa a Neutral, deja de contar). | **CORRECTO** (sin estado pegado), pero **anotá**: el snapshot de un día pasado que ya la contó NO se reescribe (ver §7) | `ScoringFactsAdapter.kt:106-120` |

---

## 6. MAPA — Sueño (telemetría, sin toggle — ver §0)

| Caso | Comportamiento actual observado | Veredicto | Seam |
|------|--------------------------------|-----------|------|
| "Desactivar sueño" | **No existe** como toggle de scoring. Lo único que apaga el término es la ausencia de noches con dato. | **NECESITA TU DECISIÓN** (¿querés un toggle real? hoy es ausencia de feature, no bug) | `Entities.kt:237` (sin flag); `BuildScoreInputUseCase.kt:22` (sin filtro) |
| Sin noches con dato en la ventana | `M = null` → término-sombra inactivo (neutral). **Sonda P5:** estado 0.75. | **CORRECTO** | `ScoringFactsAdapter.kt:146-150`; `ScoreEngine.kt:50` |
| Noches con dato presentes | La señal entra SIEMPRE (promedio de noches con dato, NoData excluidas). Sonda P5: noches malas → 0.532. | **CORRECTO** | `WeeklyScoringContextBuilder.kt:35-39` |
| Dejar de materializar noches (apagar auto_mode) a mitad de semana | Las noches viejas DENTRO de la ventana persisten y se leen; al avanzar la ventana sin noches nuevas, `M → null` gradualmente. | **CORRECTO** (coherente con ventana móvil) | `AutonomiaRepository.kt:585` (materialize); `ScoringFactsAdapter.kt:146` |

---

## 7. MAPA — Persistencia semanal (H3): los snapshots viejos NO se recalculan

| Caso | Comportamiento actual observado | Veredicto | Seam |
|------|--------------------------------|-----------|------|
| Semana EN CURSO tras togglear algo | `refreshCurrentWeek` recalcula la semana entera desde los hechos + config ACTUAL → siempre coherente con lo que ves. | **CORRECTO** | `WeeklyScoreSnapshotWriter.kt:20-21,60-103` |
| Semana PASADA ya snapshotteada, tras togglear | `closeElapsedWeeks` **saltea** semanas con snapshot existente (`if (weekStart in existingWeekStarts) continue`). El snapshot viejo **queda congelado**, calculado con la config que había entonces; NO se recalcula. | **NECESITA TU DECISIÓN** (¿es deseable que el historial sea inmutable, o debería reflejar la config actual? El handoff lo marca como posible inconsistencia entre "lo que ves ahora" y lo persistido) | `WeeklyScoreSnapshotWriter.kt:44-51` |
| Back-fill de una semana pasada que NUNCA se snapshotteó | Se calcula con la **config ACTUAL** (`getActiveUserActivityConfigs`), no con la histórica → una entidad hoy desactivada no aparece en el back-fill de una semana en que estaba activa. | **NECESITA TU DECISIÓN** (el propio comentario del código lo admite: "El back-fill usa la configuración actual") | `WeeklyScoreSnapshotWriter.kt:60-91,109-120` |

---

## 8. Resumen ejecutivo — decisiones del dueño (2026-06-17) y trabajo pendiente

**Bugs reales encontrados:** ninguno. El motor es determinista y coherente con su diseño (ventana
móvil + filtro `active` upstream + hechos inmutables). La semántica inversa de soportes está **bien
implementada** (`DomainMappers.kt:131`).

**Principio anti-trampa (decisión transversal del dueño):** lo que pasó **dentro de la ventana de 7
días** sigue contando aunque el usuario desactive y reactive, **para todas las features** (anclas,
soportes, tracks). Apagar/prender NO borra ni perdona lo de la ventana — si no, los usuarios abusarían.
Esto YA es el comportamiento actual (la ventana se re-lee entera) → **CORRECTO, se conserva.**

**Decisiones tomadas:**
| # | Caso | Decisión del dueño | Qué implica |
|---|------|--------------------|-------------|
| 1 | Ancla desactivada → puede caer a `NoData` bajo el mínimo | **CANDADO EN UI**: deben estar activas **mínimo 3 capas con ≥1 ancla cada una**, o el scoring NO funciona. | **✅ IMPLEMENTADO (2026-06-17)**: regla canónica `domain/activity/AnchorCoverageRule.kt` (`canRemoveAnchor`, fuente única del umbral; `OnboardingAnchorsRule` delega en ella). Candado en `AutonomiaRepository.removeActivityAsAnchor` → devuelve `RemoveAnchorResult.BlockedByMinimum` y NO borra; `DashboardViewModel` emite mensaje compasivo; `AnchorConfigScreen` lo muestra. Tests: `AnchorCoverageRuleTest`. |
| 2 | Soporte: días sin registro = sostenidos | **Gracia SOLO en la PRIMERA habilitación** (para no tumbar el puntaje de un soporte recién activado). Si el soporte YA fue habilitado antes y se REACTIVA, se cuenta su **historial real** — NO se regalan días sostenidos por el período apagado. | **FIX pendiente**: hoy el código NO distingue primera habilitación de reactivación (sin registro = sostenido siempre, `ScoringFactsAdapter.kt:89-98`). Hace falta rastrear si el soporte tuvo activación previa y, en reactivación, no acreditar los días OFF. |
| 3 | Track reactivado → recaída de la ventana reaparece y arrastra | **Correcto, que pese** (anti-trampa). | Comportamiento actual correcto, se conserva. |
| 4 | Sueño | **Es OPT-IN** (ver §0). | Doc actualizada esta sesión. Divergencia código↔modelo (opt-in activo + `B_SLEEP`) pendiente — §0. |
| 5 | Snapshots de semanas pasadas | **Historial INMUTABLE**; semana vacía → rellenar con config actual. | Comportamiento actual correcto, se conserva. |

**Resuelto (antes lo planteé como tensión):** la "gracia" de acreditar días sin registro como
sostenidos aplica **solo a la primera habilitación** del soporte. En una **reactivación** se respeta
el historial real y NO se acreditan los días apagados — esto cierra el hueco anti-trampa "apago el
soporte los días que voy a fallar". Es coherente con el principio #3 (la ventana no se regala al
reactivar). Implica el FIX de la fila #2: el código debe distinguir primera-habilitación de reactivación.

**Pendiente de sondeo más fino (planificación):** interacción de
`materializeAssumedAbstinenceRelapses` con desactivar/reactivar un track (§4, última fila).

---

## 10. Gaps de MANEJO de operaciones (exploración 2 subagentes + verificación propia, 2026-06-17)

Exploración de los caminos de **borrar/archivar/activar/desactivar/editar** las 4 superficies
(anclas, soportes, tasks, sobriedad). Disparada por la sospecha de que el `delete` está compartido
indebidamente. Verificado en código (no de memoria). Severidad re-evaluada por el orquestador.

### 10.1 BUGS / gaps de manejo (NO necesitan decisión — hay que arreglarlos)

| Gap | Evidencia | Severidad |
|-----|-----------|-----------|
| **Puerta trasera 1:** `deleteCustomActivity` borra un ancla custom sin pasar por `AnchorCoverageRule` (la config cae por FK CASCADE). El candado del mínimo se saltea desde el botón "borrar" de la misma pantalla. | `AutonomiaRepository.kt:999-1004`; `Entities.kt` FK CASCADE config→definition | **ALTA** |
| **Puerta trasera 2:** `toggleActivityArchive` archiva (=`active false`) sin candado. Hoy sin callers UI, pero es método público latente. | `AutonomiaRepository.kt:986-993` | MEDIA (latente) |
| **Puerta trasera 3:** `deleteUserActivityConfig` público sin ningún guard ni filtro de superficie. | `AutonomiaRepository.kt:995-997` | MEDIA (latente) |
| **Editar ancla pisa `createdAt = now`** → el cierre diario (`date >= createdLocalDate`) deja de generar logs para los días previos de la semana. | `AutonomiaRepository.kt:964,980` vía `addActivityAsAnchor`→`configureActivity`; cierre `:325` | MEDIA |
| **Editar reactiva/desarchiva en silencio:** `configureActivity` no preserva `active`/`archived` → quedan en default (`true`/`false`). Editar una actividad archivada la vuelve a activar. | `AutonomiaRepository.kt:965-983`; defaults `Entities.kt:220-221` | MEDIA |
| **Logs huérfanos:** `removeActivityAsAnchor` y `removeSupport` borran la config pero NO los `daily_activity_logs` (sin FK CASCADE). `deleteCustomActivity` sí los borra → inconsistencia entre caminos. Al re-agregar el mismo `activityId`, los logs viejos "resucitan". | `AutonomiaRepository.kt:907-913,1042-1047` vs `:1002` | MEDIA |
| **Relapse events huérfanos:** `deleteCustomAbstinenceTrack` borra logs + track pero NO `abstinence_relapse_events` (sin cascade ni delete por trackId). | `AutonomiaRepository.kt:552-558`; falta `deleteAbstinenceRelapseEventsForTrack` en DAO | MEDIA |
| **Crear soporte custom bypasea `addSupport`** (que valida que la capa exista): va por `createActivity`→upsert directo. | `DashboardViewModel.createActivity` vs `AutonomiaRepository.addSupport:1016-1018` | BAJA |

### 10.2 NECESITAN TU DECISIÓN (no son bugs claros — son comportamiento a definir)

| Tema | Comportamiento actual | Pregunta para el dueño |
|------|----------------------|------------------------|
| **Config retroactiva (el gap más profundo)** | El motor lee `f`/`t` de la config ACTUAL y los aplica a los hechos viejos de la ventana (`ScoringFactsAdapter.anchorWindow`). Editar el target de minutos/frecuencia a mitad de semana **recalcula los días ya cumplidos** con el target nuevo → el score baja sin que el usuario hiciera nada peor. | ¿Editar targets debe afectar solo de hoy en adelante (config versionada por fecha) o recalcular toda la ventana? Asimetría con la decisión #5 (snapshots pasados inmutables, pero la semana EN CURSO sí se reescribe). |
| **Reactivar track: recaídas ASUMIDAS durante la pausa** | `materializeAssumedAbstinenceRelapses` asume recaídas por días sin tracking; al reactivar, si la ventana incluye días pausados, podrían materializarse recaídas de un período en que el track estaba apagado. (Distinto de una recaída REAL registrada en la ventana, que YA decidiste que pesa — #3.) | ¿Asumir recaídas de un período PAUSADO es justo, o la pausa debe excluir esos días del "asumido"? |
| **Re-agregar un ancla quitada** | Sus `daily_activity_logs` de la ventana resucitan (anti-trampa, coherente con #3) pero el usuario podría no esperarlo. | ¿Confirmás que aplica el principio anti-trampa #3 también a anclas re-agregadas? |

### 10.3 Veredicto sobre el `delete` compartido (tu intuición)
Correcta. A nivel DAO compartir `deleteUserActivityConfig` está bien (es una sola tabla). El problema
está en el **Repository**: hay 3 rutas de borrado con protección dispar (`removeActivityAsAnchor` con
candado; `deleteCustomActivity` y `deleteUserActivityConfig` sin nada). Conviene **encapsular por
superficie con `sealed Result`** (como `RemoveAnchorResult`) y hacer `deleteUserActivityConfig`
privado. No hace falta multiplicar métodos en el DAO.

---

## 9. Artefactos de esta sesión
- **Sondas:** `app/src/test/java/dev/panopt/autonomia/domain/scoring/ScoreLifecycleProbeTest.kt`
  (P1–P5, verdes). Caracterización, no fix — revelan el comportamiento actual.
- **Doc consolidada:** se archivó `arbol-scoring-v1.md` a `old/` y se reapuntó el contrato a
  `modelo-scoring-oficial-v1.md` (CLAUDE.md, docs/README.md, docs/sueno/*); el sueño quedó canónico
  como opt-in (`axiomas-opt-in-v1.md` O8).
- **Próximo paso:** planificar (a) el candado de UI del mínimo 3 capas-con-ancla, (b) la alineación
  código↔modelo del opt-in de sueño, (c) confirmar la tensión soporte-OFF vs anti-trampa; y recién
  ahí iniciar SDD.
