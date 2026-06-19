# Handoff — Agregación temporal del scoring: ¿cómo debería el motor agregar en el tiempo?

> Sesión 2026-06-18. Proyecto: `apk-personal`. Este handoff es una **foto congelada** de un
> problema de diseño identificado al cerrar el SDD de "hardening de operaciones de ciclo de vida".
> NO propone soluciones: la próxima sesión es de **EXPLORACIÓN DE IDEAS** abierta. Acá está el
> problema, la tensión técnica, los seams y el contexto — nada de caminos prefabricados.

---

## 1. Objetivo de la sesión futura

**EXPLORAR ideas** sobre cómo el scoring debería agregar en el tiempo. La sesión está **abierta**,
sin soluciones predefinidas ni opciones cerradas para elegir. El problema (§2) y la tensión técnica
(§3) acotan el terreno; el dueño tiene una **intuición de partida** (§4) que es solo un disparador,
no la respuesta. La exploración debe ABRIR alternativas y compararlas con números reales. Impacta
tres gaps acoplados (§5) y conecta con un diagnóstico antiguo (§6). **NO se implementa nada**:
primero se exploran ideas con ejemplos numéricos a mano (§8) hasta que el dueño marque una dirección
con la panza.

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
pregunta sin respuesta; solo se puede responder al final de la semana, o con alguna forma de
proración. Ese ajuste no existe en el código actual y es uno de los root causes del diagnóstico
#858 (§6). **Cualquier idea que se explore tiene que resolver esta tensión** — no asumas que hay
una salida obvia.

---

## 4. La intuición de partida del dueño (es un DISPARADOR, no la solución)

El dueño describe, con sus palabras, una dirección posible: *"que cada día se quede con un score
que finalmente se promedie en los 7 días"* — días que, una vez consolidados, no se recalculan.

Esto es una **intuición de partida**, NO una decisión ni la única salida. La sesión debe tomarla
como punto de arranque y **explorar el espacio completo de ideas** — entre ellas (sin que esta
lista las agote ni las jerarquice): mantener el motor semanal pero usar la config vigente de cada
fecha; prorratear la frecuencia por días transcurridos; congelar el estado por día; amortiguar el
inicio de semana; o algo que todavía no pensamos. Cada idea se compara con **números reales** (§8)
antes de elegir. La tensión del §3 es la vara: la idea que gane tiene que responderla.

---

## 5. Los 3 gaps acoplados que se resuelven en esta sesión (NO antes)

Estos tres ítems están en `docs/scoring/mapa-casos-limite-ciclo-vida.md §10.2` y se difieren
explícitamente a esta sesión porque su resolución **depende del modelo de agregación temporal que
se explore y decida**:

### Gap A — Config retroactiva (ya descrito en §2)
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
de pausa DEBEN asumirse como recaída o excluirse. (Puede que este gap se resuelva solo según la
idea de agregación que se elija, o puede ser un fix separado más chico — eso lo decide la sesión.)

Seam: `AutonomiaRepository.materializeAssumedAbstinenceRelapses` (`AutonomiaRepository.kt:388`)
delega en `AbstinenceRelapseMaterializationPolicy.assumedRanges`.

### Gap C — "Gracia" de soporte en primera habilitación vs reactivación
Decisión del dueño (2026-06-17, `mapa-casos-limite-ciclo-vida.md §8 decisión #2`):
la semántica "sin-registro = sostenido" debe aplicar **SOLO en la primera habilitación**.
En una reactivación, los días OFF (sin registro porque el soporte estaba apagado) NO deben
acreditarse como "sostenidos".

El dueño mencionó que "quizá se soluciona con el planteamiento de los días que no se recalculan".
Hoy `ScoringFactsAdapter.sustainedSupportDays` (`ScoringFactsAdapter.kt:89-98`) trata "sin
registro" uniformemente, sin saber si el soporte estaba apagado o activo ese día. Cómo se resuelva
depende del modelo de agregación que se explore.

---

## 6. Conexión con el diagnóstico viejo #858 (engram, project `apk-personal`)

**Observación engram #858** (2026-06-04): "Diagnóstico: scoring reinicia cada lunes y
castiga al usuario nuevo (raíz: motor 100% semanal + proración faltante + amortiguación
inicial nunca implementada)".

Tres root causes documentados entonces:

1. **Motor 100% semanal** (`WeeklyScoringContextBuilder.kt:11`): `weekStart = today.previousOrSame(MONDAY)`. Cada lunes, la ventana empieza de cero.
2. **Sin proración por días transcurridos**: `frequencyRatio = doneDates.size / targetDays`, donde `targetDays` es la frecuencia semanal COMPLETA (no prorrateada al día actual de la semana). Al inicio de semana, el máximo posible es 1/targetDays → el estado colapsa aunque el usuario hizo el 100% de lo posible.
3. **Amortiguación inicial diseñada pero nunca implementada**: `docs/scoring/plan-tecnico-scoring.md` §2.1.1 define el contrato, pero `grep amortiz|provisional|firstWeek` en `domain/scoring/` da 0 resultados.

La intuición del dueño (que los días no se recalculen) **apunta al mismo dolor** que este
diagnóstico. **La conexión central**: los síntomas son el mismo problema de siempre, visto desde
otro ángulo. La sesión futura no puede ignorar #858; cualquier idea de agregación temporal debería
decir qué hace con esos tres root causes (los absorbe, los reemplaza o los deja aparte).

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
| assumedRanges filtra solo tracks activos | `domain/abstinence/AbstinenceRelapseMaterializationPolicy.kt` | 34 | `tracks.filter { it.active }` — los días de pausa de un track inactivo no quedan excluidos explícitamente |
| UserActivityConfigEntity SIN versionado de targets | `data/Entities.kt` | 217–235 | Solo `createdAt`/`updatedAt`; no hay tabla de historial de config |

---

## 8. Sugerencia de método: exploración abierta con números a mano

NO iniciar implementación. La sesión es una exploración (`/sdd-explore`) que **abre el espacio de
ideas** — no que elige entre opciones ya escritas:

1. **Caracterizá el problema con números reales**, calculados a mano, en los dos escenarios que le
   molestan al dueño:
   - Editar un target a mitad de semana (el ejemplo de §2).
   - Lunes nuevo vs fin de semana bueno (el reinicio del diagnóstico #858).
2. **Generá varias ideas** de cómo el motor podría agregar en el tiempo (la intuición del dueño del
   §4 es una de partida; buscá más, no te quedes en una). Para cada idea, mostrá los números de los
   escenarios de arriba.
3. **Evaluá costo/riesgo de cada idea** contra el motor actual (366 tests, 27 axiomas, modelo
   semanal): qué esquema Room toca, cuántos tests, si enmienda el contrato matemático
   (`modelo-matematico-nucleo-v1.md`).
4. **Evaluá si el Gap B** (recaídas asumidas en pausa) se puede resolver aparte, más chico.

Formato: ejemplos numéricos a mano, **sin código**, que el dueño revise y "marque con la panza"
antes de comprometerse con cualquier ruta. No cierres el espacio de ideas antes de mostrarle los
números.

---

## 9. Qué quedó FUERA de esta sesión (ya cerrado en el SDD de "hardening")

El **SDD de "hardening de operaciones de ciclo de vida"** YA está **COMPLETO y commiteado**
(2026-06-18/19): Lote 1 (candado de cobertura de anclas + cierre de las 3 puertas traseras
`deleteCustomActivity`/`toggleActivityArchive`/`deleteUserActivityConfig`, commit `d916011`) y
Lote 2 (preservar `createdAt`/`active`/`archived`/`sortOrder` al editar config + validar capa al
crear soporte custom + cleanup de letra muerta, commit `e309f2d`). 398 tests verdes. Branch
`feat/scoring-motor-nucleo-v1`, SIN push. **Importante:** ese SDD **NO borró ningún hecho** — el
dueño decidió firme que *los hechos no se borran nunca* (sostiene el anti-trampa de la ventana).

Los tres gaps del §5 (A/B/C) están **excluidos** de ese SDD porque su resolución depende del modelo
de agregación temporal que se decida en esta exploración. No se tocaron ahí aunque el código cambie
cerca.

Lo que tampoco se toca en los SDDs actuales (queda para acá o para más adelante):
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
