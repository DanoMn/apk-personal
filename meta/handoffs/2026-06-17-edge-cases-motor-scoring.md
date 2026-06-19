# Handoff — Casos límite / ciclo de vida del motor de scoring (opt-ins, soportes, tasks)

> Sesión 2026-06-17. Proyecto: `apk-personal`. El motor de scoring núcleo v1 quedó implementado,
> verificado (366 tests, 27 axiomas 1:1) y archivado. La PRÓXIMA sesión investiga cómo el motor
> reacciona a **cambios de estado en el tiempo**: activar/desactivar/reactivar un opt-in (sueño,
> sobriedad), un soporte o una task, y si los datos "vuelven" al reactivar. Handoff autocontenido.

---

## 0. ¿Ya consideramos estos casos esta sesión? — Respuesta honesta: PARCIALMENTE

**SÍ cubrimos (axiomas + diseño) — casos límite ESTÁTICOS (foto de UNA semana):**
- Opt-in **sin dato → inactivo/neutral** (sueño `M=null` → sin término-sombra; track de sobriedad limpio → factor 1, invisible). (axiomas O2/C2, sueño null)
- Opt-in `M=1` → **invisible** (neutralidad exacta, incluso con anclas en déficit). (O5/O11)
- **Anti-incentivo (O12)**: activar un opt-in solo puede **empatar o bajar** el estado, nunca subirlo.
- Soporte **sin registro = sostenido** (UX inversa, ventana 4 días). (SO2/SO4)
- Task **efímera**: solo cuenta si está `Done` HOY, con capa, no `Neutral`. (TA3/TA5)
- **Semana vacía** → `ESTADO = 0` / `NoData`. `NotDone`/`Omitted`/duplicados manejados en el adapter.

**NO diseñamos ni testeamos — casos límite DINÁMICOS / de CICLO DE VIDA (lo de la próxima sesión):**
- Qué pasa cuando el usuario **desactiva** un opt-in/soporte/ancla a mitad de semana.
- Qué pasa cuando lo **reactiva**: ¿los datos viejos "vuelven a cargar"?
- Cómo interactúan, en el tiempo, tres cosas: **(a)** los hechos persistidos (nunca se borran),
  **(b)** la **ventana móvil de 7 días** (se re-lee entera cada vez), **(c)** el filtro por el flag
  `active` de la config.

No hay NI UN test que ejercite "desactivo sueño el miércoles y lo reactivo el viernes". El
comportamiento EMERGE de los seams, pero no está fijado por tests ni razonado explícitamente.

---

## 1. El principio que gobierna TODO (clave para razonar los casos)

```
hechos diarios (daily_activity_logs / abstinence_logs / sleep_nights)  ← NUNCA se borran
   → se re-lee la VENTANA de los últimos 7 días, ENTERA, en cada cálculo
   → se filtra por el flag `active` de la config ACTUAL
   → motor calcula
```

De acá salen 3 hipótesis (a VERIFICAR con tests la próxima sesión):

- **H1 — Desactivar = deja de contribuir YA.** Al desactivar, la entidad sale de
  `visibleActivities`/`activeTracks` (filtro `active`), así que el motor la ignora desde ese
  instante. Sus hechos en la ventana **siguen en la base pero quedan sin usar**.
- **H2 — Reactivar = los datos "vuelven", pero SOLO los de los últimos 7 días.** Como los hechos
  nunca se borran y la ventana se re-lee entera, al reactivar la entidad vuelve a
  `visibleActivities` y **sus logs dentro de la ventana se vuelven a leer**. Lo anterior a 7 días
  es irrelevante por diseño (ventana móvil). → La respuesta a "¿se cargan nuevamente?" sería **sí,
  pero acotado a la ventana**.
- **H3 — El historial semanal NO se reescribe solo.** Los `weekly_score_snapshots` ya escritos de
  semanas pasadas no se recalculan al toggear (salvo back-fill explícito). Verificar si esto deja
  inconsistencias entre lo que el usuario ve "ahora" y lo persistido.

---

## 2. Casos límite a investigar, por feature (con el seam de código)

### 2.1 Sueño (opt-in) — OJO: hay una duda de PRODUCTO primero
- **Seam**: el motor toma la señal de `input.sleepNights` →
  `ScoringFactsAdapter.sleepSignal(...)`; las noches salen de `getSleepNightsInRange(ventana)`
  (`AutonomiaDao:162`). Se materializan con `materializeSleepNight` (`DailyClosureWorker:30`,
  `DashboardViewModel:206`).
- **Hallazgo clave**: `sleep_config` (`Entities.kt:237`) **NO tiene flag `enabled`/`active`**
  (solo `targetSleepAt`, `targetWakeAt`, `digitalWindDownMinutes`). → **¿Existe siquiera un
  "desactivar sueño" en la UI, o el sueño es telemetría siempre-activa?** Esta es la PRIMERA
  pregunta a resolver: el caso "lo activa y luego lo quita del registro" puede no mapear a un
  toggle real. Aclarar el modelo de producto del sueño antes de testear.
- **Si "desactivar" = dejar de materializar noches**: a medida que pasan los días, la ventana se
  queda sin noches con dato → `M = null` → opt-in inactivo (neutral). Reactivar → nuevas noches →
  `M` vuelve. Noches viejas DENTRO de la ventana persisten y se re-leen.

### 2.2 Sobriedad (opt-in)
- **Seam**: `WeeklyScoringContextBuilder` filtra `activeSobrietyTracks = abstinenceTracks.filter { active }`;
  los logs por track salen de `weeklyAbstinenceLogsByTrack` (ventana). `M_sobr = Π(1−A)^días_recaída`.
- **Casos**: desactivar un track → sale de `activeTracks` → su arrastre desaparece YA (H1).
  Reactivar → vuelve, y sus `abstinence_logs` en la ventana se re-leen (H2) → el arrastre puede
  "reaparecer" si hubo recaídas en los últimos 7 días. ¿Es el comportamiento deseado, o reactivar
  debería "perdonar" lo viejo? **Decisión de producto + test.**
- Ojo `materializeAssumedAbstinenceRelapses` (`DailyClosureWorker:22`): asume recaídas no
  registradas. Interactúa con desactivar/reactivar — verificar.

### 2.3 Soportes
- **Seam**: `visibleActivities = activities.filter { active && !archived }`. La señal de soporte
  = `sustainedSupportDays` (ventana 4 días, sin-registro=sostenido). El cierre diario
  (`closeActivityDay`) solo escribe logs de cierre para configs **activas**
  (`AutonomiaRepository:315-322`).
- **Caso interesante**: desactivar un soporte a mitad de semana → los días previos pueden tener
  (o no) logs de cierre; los días posteriores NO generan cierre (config inactiva). Al computar
  `días_sostenidos`, ¿qué pasa con la ventana de 4 días partida? Reactivar → vuelve a
  `visibleActivities` y sus logs de ventana se re-leen. Verificar que "sin registro = sostenido"
  no premie injustamente un soporte que estuvo desactivado (sin registro PORQUE estaba apagado, no
  porque el usuario lo sostuvo).

### 2.4 Tasks
- **Seam**: `tasksTodayByLayer` cuenta `Done` HOY, con `layerId`, no `Neutral`
  (`ScoringFactsAdapter:106` + `ScoringExtensions:44`). Efímero: solo el día.
- **Casos**: task `Done` y luego **archivada/des-completada** el mismo día → ¿sigue contando hoy?
  Task `Done` ayer → no cuenta hoy (efímero) — ¿y el snapshot de ayer ya la contó? Una task que se
  completa y se "descompleta" varias veces en el día. Cambiar `layerId` o `contributionRole` de una
  task ya completada. Verificar idempotencia/efímero bajo edición.

### 2.5 Anclas (no es opt-in, pero el toggle también aplica)
- Desactivar un ancla a mitad de semana la saca de `visibleActivities` → se cae ENTERA del cálculo,
  incluidos los días que SÍ se cumplió. ¿Deseado? ¿O debería contar lo hecho mientras estuvo activa?
  Reactivar → vuelve con sus logs de ventana. **Caso de diseño a decidir.**

---

## 3. Cómo encarar la próxima sesión (sugerencia)
1. **Resolver la duda de producto del sueño** (¿hay toggle real? ¿qué significa "desactivar"?).
2. Definir, por feature, el comportamiento DESEADO al desactivar/reactivar (decisión del dueño;
   usar el skill `scoring-historias-usuario` para plantear casos autocontenidos con estado esperado).
3. Escribir tests (Strict TDD) que ejerciten la dinámica temporal: construir `ScoreInput` de la
   misma semana con la entidad activa vs desactivada vs reactivada, y afirmar el comportamiento.
4. Validar H1/H2/H3 contra el código; ajustar el adapter/builder si el comportamiento emergente no
   coincide con el deseado.
5. Cuidar el seam de persistencia (H3): ¿los snapshots viejos quedan inconsistentes al toggear?

---

## 4. Estado al cerrar esta sesión (contexto git + pendientes)
- **Rama**: `feat/scoring-motor-nucleo-v1` (creada desde `main` con aprobación del dueño). **SIN
  push.** `main` intacto. 18 commits en la rama: 7 PR del motor (A–G) + seed + plan SDD + archive +
  4 correcciones post-PR (unificación V2, detalle por-capa, promedio/peor capa, razones) + borrado
  de restraint-checks del seed.
- **Build**: `assembleDebug` + `testDebugUnitTest` verdes (366 tests).
- **PENDIENTES dejados a propósito (decisión del dueño)**:
  - `StabilityScoringPolicy` = **BORRADA** al cierre de esta sesión (commit `944172b`): código muerto
    + sus 2 constantes huérfanas (`WEEKLY_AVERAGE_WEIGHT`/`WEEKLY_WORST_WEIGHT`) + la data class
    `StabilityEvaluation`. Build + suite completa + lint en verde tras el borrado. Estabilidad
    multi-semana queda como decisión de producto aparcada: si entra, se diseña de cero contra el
    modelo nuevo.
  - **Reconciliación de docs vivos** con el seed final (parkeada): `preset-soportes-v1.md` (faltan
    2 soportes nuevos), `actividades-ancla-predeterminadas-v1.md` (lista 2 que ya son soportes),
    `presets-actividades-v1.md` (muy stale: Count, IDs `act_*` de soportes, anclas fantasma).
  - `reasons`/`worstLayer`/detalle por-capa → YA restaurados esta sesión.
- **Memoria engram** (project `apk-personal`): `scoring/implementacion-motor-kotlin`,
  `scoring/anclas-solo-minutes`, `preferencia-opus`. Buscar por "scoring".

## 5. Archivos clave para arrancar
- `domain/scoring/WeeklyScoringContextBuilder.kt` — recorte a ventana 7d + filtros `active`.
- `domain/scoring/ScoringFactsAdapter.kt` — `sleepSignal`, `relapseDaysByTrack`,
  `sustainedSupportDays`, `tasksTodayByLayer`, `anchorWindow`.
- `domain/scoring/ScoreEngine.kt` — cableado opt-ins (sueño→Cuerpo, sobriedad→Conducta).
- `AutonomiaRepository.kt:285` `closeActivityDay` (cierre diario, filtro `active`).
- `data/scoring/WeeklyScoreSnapshotWriter.kt` — back-fill y re-cálculo semanal (H3).
- `data/Entities.kt:237` `sleep_config` (sin flag enabled — duda de producto).
