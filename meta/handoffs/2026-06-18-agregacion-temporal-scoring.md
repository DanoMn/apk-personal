# Handoff — Agregación temporal del scoring: ¿motor semanal o días congelados?

> Sesión 2026-06-18. Proyecto: `apk-personal`. Este handoff es una **foto congelada** de la
> tensión de diseño identificada al cerrar el SDD de "hardening de operaciones de ciclo de
> vida". NO es un contrato vivo: si la decisión se toma en una sesión futura, se documenta en
> `docs/scoring/modelo-scoring-oficial-v1.md` y `docs/scoring/plan-tecnico-scoring.md`, no acá.

---

## 1. Objetivo de la sesión futura

Decidir el **modelo de agregación temporal** del scoring: si el motor sigue siendo puramente
semanal (con o sin config versionada por fecha), o si se migra a un modelo de "días
congelados con promedio de 7". La decisión impacta tres gaps acoplados (§5) y conecta con
un diagnóstico antiguo (§6). NO se implementa sin una exploración SDD dedicada que compare
los dos caminos con ejemplos numéricos reales (§8).

---

## 2. El problema en una frase + ejemplo concreto del dueño

**El motor lee la config ACTUAL y la aplica retroactivamente a hechos viejos de la ventana.**

Ejemplo textual del dueño: "alguien puede tener el lunes sobriedad + sueño + 8 anclas, y
de repente mañana solo 3 anclas activas en 3 capas; es un quilombo recalcular todo; lo ideal
sería que cada día se quede con un score que finalmente se promedie en los 7 días".

Ejemplo numérico concreto:

- Usuario tiene ancla "Meditación" con target `f=5 días/sem, t=20 min/ses`.
- Lunes y martes: cumple 25 min cada día → los dos días contribuyen positivo.
- Miércoles: edita el target a `t=60 min/ses` porque quiere ser más exigente.
- El motor **recalcula la ventana entera con el target nuevo**: lunes y martes (25 min <
  60 min) quedan como días fallidos. El score baja **sin que el usuario haya hecho nada
  peor** — solo por editar un target futuro.

Seam exacto: `ScoringFactsAdapter.anchorWindow` (`ScoringFactsAdapter.kt:54-68`) construye
`AnchorWindow(f = def.targetDays(), t = def.targetDailyValue(), mins = ...)`. Las funciones
`targetDays()` y `targetDailyValue()` (`ScoringExtensions.kt:15-34`) leen
`weeklyFrequencyTarget`, `sessionTargetMinutes`, etc. desde `def`, que es la
`ActivityDefinition` configurada con los **targets actuales**. No existe versionado por fecha.

---

## 3. Por qué NO es trivial: la tensión semanal vs diario

El dueño describe "un score diario que se promedia". El problema es que el modelo actual
tiene señales **inherentemente semanales**, sin un "score diario natural":

| Señal | Naturaleza | ¿Tiene score diario natural? |
|-------|-----------|------------------------------|
| Anclas: `R(F, T, mins)` | `F` = cumplir N días de 7 en la semana entera (`ScoringFactsAdapter.kt:64`, `targetDays()`) | NO: el lunes solo no sabe si el ratio `F` se cumplirá |
| Soportes: `días_sostenidos` | Ventana 4 días; UX inversa (sin-registro=sostenido) (`ScoringFactsAdapter.kt:89-98`) | PARCIAL: se puede calcular al cierre de cada día |
| Sobriedad: `días_recaída` en 7 | `relapseDaysByTrack`, ventana 7d (`ScoringFactsAdapter.kt:127-139`) | PARCIAL: se puede ver acumulado por día |
| Tasks: `n_tasks_hoy` | Efímero diario (`ScoringFactsAdapter.kt:106-120`) | SÍ: ya es diario |
| Sueño: `M` | Promedio de noches con dato (`ScoringFactsAdapter.kt:146-150`) | PARCIAL: promedio acumulado al día |

La parte difícil es **anclas**: `R` en el modelo matemático es `R(F, T, mins)` donde `F` es
la **frecuencia semanal objetivo** (ej. 5/7). El lunes "¿cumpliste F días de 7?" es una
pregunta sin respuesta; solo se puede responder al final de la semana, o con una
proración como `F_ajustado = F * días_transcurridos/7`. Ese ajuste no existe en el código
actual y es uno de los root causes del diagnóstico #858 (§6).

---

## 4. Camino A vs Camino B — tradeoffs, tamaño, riesgo

### Camino A — Congelar la CONFIG por día (versionar targets por fecha)

**Qué hace:** el motor sigue siendo semanal, pero para cada hecho de la ventana usa la
**config que estaba vigente en la fecha de ese hecho**, no la actual.

**Cómo:** agregar un `config_snapshots` por fecha (o un campo `targetSnapshotAt` en
`daily_activity_logs`), de forma que `anchorWindow` pueda usar el target histórico para
cada día.

**Entidades afectadas:**
- `UserActivityConfigEntity` (`data/Entities.kt:217-235`): hoy solo tiene `createdAt`/`updatedAt`,
  sin historial. Habría que agregar una tabla de versiones de config por fecha o guardar el
  target al momento del cierre en `DailyActivityLogEntity`.
- `ScoringFactsAdapter.anchorWindow` (`ScoringFactsAdapter.kt:54-68`): recibe `def` plano;
  habría que recibir el target vigente para cada fecha.

**Tradeoffs:**
- Resuelve la retroactividad SIN cambiar el modelo matemático ni el concepto de "semana".
- Acotado: solo toca el seam de "qué config uso para este día" en el adapter.
- El modelo de anclas `R(F, T, mins)` **no cambia**: sigue siendo semanal, pero con targets
  históricos por día.
- Complejidad media: versionado de config es un cambio de esquema Room + migraciones
  cuando llegue el momento de release.
- NO resuelve el reinicio de lunes (diagnóstico #858) ni la falta de proración — eso es
  otro problema.
- **Recomendación del orquestador**: este camino da ~90% del beneficio percibido por el
  usuario con una fracción del riesgo. Es el punto de entrada natural.

### Camino B — Score/estado diario congelado + promedio de 7 días (idea literal del dueño)

**Qué hace:** cada día al cierre, se calcula y persiste un "estado del día"; el estado
semanal es el promedio de los 7 estados diarios.

**Lo que describe el dueño** es correcto a nivel de UX: el lunes queda fijo con su
score, aunque mañana cambie la config o bajen otras señales.

**Por qué es un motor nuevo en la práctica:**
- Anclas `R(F, T, mins)` necesitan una nueva definición de `R_diario` (¿proración? ¿solo
  el día? ¿ventana deslizante?). Sin eso el modelo matemático de anclas no aplica.
- La "ventana de soportes" (4 días de sostenido) necesita redefinirse: ¿el soporte del
  lunes sabe su estado final, o solo lo sabe el viernes?
- El `WeeklyScoringContextBuilder` (`WeeklyScoringContextBuilder.kt:8-74`) y el
  `ScoreEngine` (`ScoreEngine.kt:36-197`) son el orquestador semanal. Habría que crear
  un `DailyScoringContextBuilder` + `DailyScoreEngine`, y `WeeklyScore` pasaría a ser
  un promedio de 7 diarios.
- `WeeklyScoreSnapshotWriter` (`data/scoring/WeeklyScoreSnapshotWriter.kt:16-126`) escribe
  snapshots semanales; habría que agregar snapshots diarios (`DailyScoreSnapshotEntity`,
  esquema Room nuevo).
- Cierre diario (`AutonomiaRepository.closeActivityDay`, `AutonomiaRepository.kt:304-357`)
  hoy materializa HECHOS del día (logs de actividad) pero NO calcula ningún score diario.
  Habría que extender el cierre para calcular y persistir el estado del día.

**Tradeoffs:**
- Conceptualmente más limpio para el dueño ("lo de hoy queda fijo").
- Resuelve la retroactividad, el reinicio de lunes y el problema del usuario nuevo en un
  solo golpe.
- **Riesgo alto**: es un motor nuevo. Todo el contrato matemático (36+ tests, 27 axiomas
  del motor núcleo v1) se aplica a un modelo semanal. Un modelo diario necesitaría su
  propia spec, axiomas y suite de tests completa.
- Tamaño estimado: 3–5x el esfuerzo del SDD de "hardening de operaciones" que se está
  lanzando en paralelo.
- La invariante `docs/scoring/modelo-matematico-nucleo-v1.md` (contrato matemático vigente)
  requiere enmienda explícita.

### Tabla resumen

| | Camino A | Camino B |
|---|---|---|
| **Qué resuelve** | Config retroactiva en semana en curso | Config retroactiva + reinicio lunes + usuario nuevo |
| **Motor** | Sigue semanal | Motor nuevo (diario + agregación) |
| **Esfuerzo estimado** | Medio (schema + adapter) | Alto (spec + motor + tests completos) |
| **Riesgo** | Bajo-Medio | Alto |
| **Rompe contrato vigente** | No | Sí (enmienda obligatoria) |
| **Recomendación** | Punto de entrada | Solo si A no satisface al dueño tras prueba |

---

## 5. Los 3 gaps acoplados que se resuelven en esta sesión (NO antes)

Estos tres ítems están en `docs/scoring/mapa-casos-limite-ciclo-vida.md §10.2` y se difieren
explícitamente a la sesión de agregación temporal porque su resolución **depende de la
decisión A vs B**:

### Gap A — Config retroactiva (ya descrito en §2 y §4)
Raíz: `ScoringFactsAdapter.anchorWindow` (`ScoringFactsAdapter.kt:54-68`) + `targetDays()`/
`targetDailyValue()` (`ScoringExtensions.kt:15-34`) leen config actual.

### Gap B — Recaídas ASUMIDAS durante una PAUSA de track
`AbstinenceRelapseMaterializationPolicy.assumedRanges` (`AbstinenceRelapseMaterializationPolicy.kt:23-44`)
filtra `tracks.filter { it.active }` y, para cada track activo, busca días sin registro desde
`trackStart` hasta `cutoff`. Si el usuario **desactivó el track varios días y lo reactivó**,
los días de pausa (sin log) pueden quedar en la ventana del "asumir recaída" y materializarse
como `Relapse` aunque el track estaba apagado.

Distinción clave: esto es DISTINTO de una recaída REAL registrada durante la ventana, que
ya se decidió que DEBE pesar (anti-trampa, decisión #3 del dueño). La pregunta es si los días
de pausa DEBEN asumirse como recaída o excluirse.

La respuesta depende del modelo de agregación: si el score del día se congela cuando el track
está inactivo (Camino B), el problema no existe. Si seguimos con Camino A, hay que decidir si
`assumedRanges` debe respetar el rango de actividad del track.

Seam: `AutonomiaRepository.materializeAssumedAbstinenceRelapses` (`AutonomiaRepository.kt:388`)
delega en `AbstinenceRelapseMaterializationPolicy.assumedRanges`.

### Gap C — "Gracia" de soporte en primera habilitación vs reactivación
Decisión del dueño (2026-06-17, `mapa-casos-limite-ciclo-vida.md §8 decisión #2`):
la semántica "sin-registro = sostenido" debe aplicar **SOLO en la primera habilitación**.
En una reactivación, los días OFF (sin registro porque el soporte estaba apagado) NO deben
acreditarse como "sostenidos".

El dueño mencionó que "quizá se soluciona con el planteamiento anterior (días congelados)":
efectivamente, si el score diario se congela cuando el soporte está inactivo, los días OFF
no contribuyen a nada. En Camino A, hay que distinguir primera-habilitación de reactivación
en `ScoringFactsAdapter.sustainedSupportDays` (`ScoringFactsAdapter.kt:89-98`), que hoy
trata "sin registro" uniformemente sin saber si el soporte estaba apagado o activo ese día.

---

## 6. Conexión con el diagnóstico viejo #858 (engram, project `apk-personal`)

**Observación engram #858** (2026-06-04): "Diagnóstico: scoring reinicia cada lunes y
castiga al usuario nuevo (raíz: motor 100% semanal + proración faltante + amortiguación
inicial nunca implementada)".

Tres root causes documentados entonces:

1. **Motor 100% semanal** (`WeeklyScoringContextBuilder.kt:11`): `weekStart = today.previousOrSame(MONDAY)`. Cada lunes, la ventana empieza de cero.
2. **Sin proración por días transcurridos**: `frequencyRatio = doneDates.size / targetDays`, donde `targetDays` es la frecuencia semanal COMPLETA (no prorrateada al día actual de la semana). Al inicio de semana, el máximo posible es 1/targetDays → el estado colapsa aunque el usuario hizo el 100% de lo posible.
3. **Amortiguación inicial diseñada pero nunca implementada**: `docs/scoring/plan-tecnico-scoring.md` §2.1.1 define el contrato, pero `grep amortiz|provisional|firstWeek` en `domain/scoring/` da 0 resultados.

La idea del dueño (días congelados) **apunta exactamente a este dolor**: si el lunes ya
tiene un estado congelado del ciclo anterior, el "reinicio" es menos brutal. Camino B lo
resuelve estructuralmente; Camino A lo mitiga parcialmente (la proración + amortiguación
siguen sin implementarse).

**Esta es la conexión central**: los síntomas son el mismo dolor de siempre, visto desde
otro ángulo. La sesión futura no puede ignorar #858; debe decidir si Camino A/B absorbe o
reemplaza esos tres fix pendientes.

---

## 7. Seams de código verificados (archivo:línea)

| Seam | Archivo | Línea | Qué hace |
|------|---------|-------|---------|
| `anchorWindow` lee config actual | `domain/scoring/ScoringFactsAdapter.kt` | 54–68 | `f = def.targetDays()`, `t = def.targetDailyValue()` — `def` es la config ACTUAL |
| `targetDays()` | `domain/scoring/ScoringExtensions.kt` | 15–25 | Lee `weeklyFrequencyTarget` / `targetCount` de la config actual |
| `targetDailyValue()` | `domain/scoring/ScoringExtensions.kt` | 27–34 | Lee `sessionTargetMinutes` / `targetValue` de la config actual |
| Ventana 7 días (lunes→hoy) | `domain/scoring/WeeklyScoringContextBuilder.kt` | 11–19 | `weekStart = today.previousOrSame(MONDAY)` |
| Orquestador semanal | `domain/scoring/ScoreEngine.kt` | 36–197 | `calculate(input)` corre pipeline completo con ventana semanal |
| Snapshot semanal (refreshCurrentWeek) | `data/scoring/WeeklyScoreSnapshotWriter.kt` | 20–21 | Recalcula la semana en curso; snapshots de semanas pasadas NO se reescriben |
| closeElapsedWeeks salta semanas ya snapshotteadas | `data/scoring/WeeklyScoreSnapshotWriter.kt` | 44–51 | `if (weekStart in existingWeekStarts) continue` |
| Back-fill usa config ACTUAL | `data/scoring/WeeklyScoreSnapshotWriter.kt` | 109–120 | `getActiveUserActivityConfigs()` — no config histórica |
| closeActivityDay materializa hechos, NO score diario | `AutonomiaRepository.kt` | 304–357 | Cierre de día escribe `DailyActivityLogEntity` y `DailyClosureEntity`; cero scoring |
| materializeAssumedAbstinenceRelapses | `AutonomiaRepository.kt` | 388 | Delega en `AbstinenceRelapseMaterializationPolicy.assumedRanges` |
| assumedRanges filtra solo tracks activos | `domain/abstinence/AbstinenceRelapseMaterializationPolicy.kt` | 34 | `tracks.filter { it.active }` — los días de pausa de un track inactivo no quedan excluidos explícitamente; quedan sin log y pueden caer en la ventana de "asumir" cuando se reactiva |
| UserActivityConfigEntity SIN versionado de targets | `data/Entities.kt` | 217–235 | Solo `createdAt`/`updatedAt`; no hay tabla de historial de config |

---

## 8. Sugerencia de método: exploración SDD dedicada

No iniciar implementación sin antes hacer una exploración SDD (`/sdd-explore`) que:

1. **Calcule a mano** (con datos de ejemplo concretos) cómo quedan los estados con Camino A
   vs Camino B en los dos escenarios que le molestan al dueño:
   - Editar target a mitad de semana (el ejemplo de §2).
   - Lunes nuevo vs fin de semana bueno (el reinicio del diagnóstico #858).

2. **Evalúe el tamaño real** de Camino A: ¿qué esquema Room nuevo necesita, cuántas
   entidades cambian, cuántos tests hay que actualizar? Con el motor actual (366 tests,
   27 axiomas), cualquier cambio al seam de config necesita re-verificación.

3. **Decida si Camino B es viable en esta fase**: con la rama `feat/scoring-motor-nucleo-v1`
   aún sin merge a `main` y los bugs de operaciones de ciclo de vida pendientes, un motor
   nuevo es un riesgo alto. La exploración debe hacer ese trade-off explícito.

4. **Evalúe si Gap B (recaídas asumidas en pausa) requiere decisión antes de Camino A**:
   podría ser un fix separado, más pequeño, que no espere la decisión de agregación
   temporal.

Formato sugerido: exploración con **ejemplos numéricos calculados a mano**, sin código,
que el dueño pueda revisar y "marcar con la panza" antes de comprometerse con una ruta.

---

## 9. Qué quedó FUERA de esta sesión (no se tocó en el SDD de "hardening")

El **SDD de "hardening de operaciones de ciclo de vida"** YA está **COMPLETO y commiteado**
(2026-06-18/19): Lote 1 (candado de cobertura de anclas + cierre de las 3 puertas traseras
`deleteCustomActivity`/`toggleActivityArchive`/`deleteUserActivityConfig`, commit `d916011`) y
Lote 2 (preservar `createdAt`/`active`/`archived`/`sortOrder` al editar config + validar capa al
crear soporte custom + cleanup de letra muerta, commit `e309f2d`). 398 tests verdes. Branch
`feat/scoring-motor-nucleo-v1`, SIN push. **Importante:** ese SDD **NO borró ningún hecho** — el
dueño decidió firme que *los hechos no se borran nunca* (sostiene el anti-trampa de la ventana). No
hubo limpieza de "huérfanos". Todo eso es **ortogonal** a la decisión de agregación temporal.

Los tres gaps del §5 de este handoff (A/B/C) están **explícitamente excluidos** del SDD de
hardening porque su resolución depende de la decisión de agregación temporal. No se tocan
en ese SDD aunque el código cambie cerca.

Lo que tampoco se toca en ninguno de los dos SDDs actuales:
- Proración por días transcurridos (denominador de `frequencyRatio`) — diagnóstico #858, root cause 2.
- Amortiguación inicial para usuarios nuevos / inicio de semana — diagnóstico #858, root cause 3.
- Opt-in de sueño con `B_SLEEP=0.5` y cobertura (`mapa-casos-limite-ciclo-vida.md §0`) — divergencia código↔modelo pendiente.
- Estabilidad multi-semana (`StabilityScoringPolicy` borrada en `944172b`; si entra, se diseña de cero).

---

## Referencias

- Contrato matemático vigente: `docs/scoring/modelo-scoring-oficial-v1.md` (FUENTE DE VERDAD)
- Matemática completa 7 niveles: `docs/scoring/modelo-matematico-nucleo-v1.md`
- Mapa de ciclo de vida (foto 2026-06-17): `docs/scoring/mapa-casos-limite-ciclo-vida.md` (§0 sueño opt-in; §8 decisiones del dueño; §10.2 los 3 gaps acoplados)
- Plan técnico / estado por fases: `docs/scoring/plan-tecnico-scoring.md`
- Engram `apk-personal` topic `scoring/diagnostico-reset-lunes-y-usuario-nuevo` (#858) — diagnóstico reinicio lunes
- Engram `apk-personal` topic `scoring/gaps-manejo-operaciones-ciclo-vida` (#1200) — gaps de manejo
- Engram `apk-personal` topic `scoring/decisiones-ciclo-vida-y-doc-oficial` (#1194) — decisiones del dueño 2026-06-17
