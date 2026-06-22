package dev.panopt.autonomia.domain.scoring

import dev.panopt.autonomia.AbstinenceLog
import dev.panopt.autonomia.AbstinenceStatus
import dev.panopt.autonomia.AbstinenceTrack
import dev.panopt.autonomia.ActivityLog
import dev.panopt.autonomia.ActivitySurface
import dev.panopt.autonomia.ContributionRole
import dev.panopt.autonomia.DailyActivityStatus
import dev.panopt.autonomia.Task
import dev.panopt.autonomia.TaskStatus
import dev.panopt.autonomia.domain.activity.ActiveTargetVersionRule
import dev.panopt.autonomia.domain.activity.ActivityDefinition
import dev.panopt.autonomia.domain.activity.ActivityTargetVersion
import dev.panopt.autonomia.domain.sleep.SleepNightScore
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * PR-E — el ADAPTER del modelo de núcleo v1: la ÚNICA pieza que conoce la forma cruda de los
 * hechos Room. Transforma la ventana semanal (ya recolectada y deduplicada en
 * [WeeklyScoringContext]) en las formas que el motor consume:
 *
 * - por **ancla**: [AnchorWindow]`(f, t, mins)` — `mins[día] = actualValue` (Minutes-only, PR-D);
 * - por **soporte**: `días_sostenidos` en la ventana indulgente de 4 días (UX inversa);
 * - por **capa**: `n_tasks_hoy` (tasks completadas HOY con esa capa, efímero);
 * - por **track**: `días_recaída` en la ventana de 7 días → señal `M_sobr` ([OptInPolicy]);
 * - **sueño**: señal `M ∈ [0,1]` (promedio de noches con dato; `null` si no hay).
 *
 * NO recalcula scoring — solo adapta forma. Dominio puro JVM: no importa Room ni Compose; consume
 * los modelos de dominio ([ActivityLog], [Task], [AbstinenceLog], [SleepNightScore]). Toda la
 * aritmética de fechas usa la zona local del dispositivo (la fecha del log es `LocalDate` del
 * cierre diario). Es la única fuente de formas crudas del motor núcleo v1 (el pipeline viejo se
 * eliminó en PR-F); lo invoca el orquestador [ScoreEngine].
 *
 * Spec: `openspec/changes/scoring-motor-nucleo-v1/specs/scoring-facts-adapter/spec.md`.
 */
internal object ScoringFactsAdapter {

    /** Ventana indulgente de soportes (días). El motor convierte a `s = min(días/SUPPORT_WINDOW, 1)`. */
    const val SUPPORT_WINDOW_DAYS = 4

    /**
     * Construye la forma cruda `(f, t, mins)` de UN ancla desde sus logs (ya deduplicados por
     * `"activityId:date"` en el builder; aquí se deduplica defensivamente por fecha por si el caller
     * pasa logs crudos). Invariante Minutes-only (PR-D): `mins[día] = actualValue`.
     *
     * Reglas por log:
     * - `Omitted` → se excluye (no cuenta como día con actividad ni penaliza).
     * - `NotDone` / `actualValue` nulo o `0` → día sin actividad (no aporta minuto positivo).
     * - cualquier otro con `actualValue > 0` → aporta `actualValue` minutos ese día.
     *
     * Solo los días con minuto `> 0` entran a `mins` (el modelo Best-F filtra `m > 0`).
     */
    fun anchorWindow(
        def: ActivityDefinition,
        logs: List<ActivityLog>,
        versions: List<ActivityTargetVersion> = emptyList(),
        windowStart: LocalDate? = null,
    ): AnchorWindow {
        val minsByDate = LinkedHashMap<String, Int>()
        for (log in logs) {
            if (log.status == DailyActivityStatus.Omitted) continue
            val minutes = minutesOf(log)
            if (minutes <= 0) continue
            // Dedup defensivo por fecha: el primer día con actividad gana (el builder ya dedup).
            minsByDate.putIfAbsent(log.date, minutes)
        }
        val mins = minsByDate.values.toList()

        // Camino legacy (sin versiones): config actual para todos los días — comportamiento previo.
        if (versions.isEmpty()) {
            return AnchorWindow(f = def.targetDays(), t = def.targetDailyValue(), mins = mins)
        }

        // FASE 2: cada día con la meta de MINUTOS que regía ESE día; la FRECUENCIA es la vigente en
        // el día más viejo de la ventana ("entra a los 7 días"). Fallback a la config actual para
        // fechas sin versión (anteriores a la primera). Ver cambios-config-en-el-tiempo-v1.md.
        val dayRatios = minsByDate.entries.map { (dateStr, m) ->
            val date = runCatching { LocalDate.parse(dateStr) }.getOrNull()
            val targetMin = date?.let { ActiveTargetVersionRule.resolve(versions, it)?.targetMinutes }
                ?: def.targetDailyValue()
            m.toDouble() / targetMin.toDouble()
        }
        val oldest = windowStart
            ?: minsByDate.keys.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.minOrNull()
        val f = oldest?.let { ActiveTargetVersionRule.resolve(versions, it)?.targetDays }
            ?: def.targetDays()

        return AnchorWindow(f = f, t = def.targetDailyValue(), mins = mins, dayRatios = dayRatios)
    }

    /**
     * Minutos del día de un log de ancla. Invariante Minutes-only: el mapeo es directo
     * (`actualValue`). Un `Done` sin `actualValue` cargado NO asume cumplimiento de tiempo → 0.
     */
    private fun minutesOf(log: ActivityLog): Int {
        if (log.status == DailyActivityStatus.NotDone) return 0
        return (log.actualValue ?: 0).coerceAtLeast(0)
    }

    /**
     * `días_sostenidos` de un soporte en la ventana de [SUPPORT_WINDOW_DAYS] días (UX inversa):
     * **sin registro del día = sostenido** (la ausencia de dato NO penaliza). Solo las OMISIONES
     * (registros que `countsAsDone`, i.e. el usuario marcó que SÍ ocurrió lo que el soporte evita)
     * dentro de la ventana restan días sostenidos. Días de omisión distintos se cuentan una vez.
     *
     * @param logs logs del soporte en la semana (se filtran a la ventana de 4 días).
     * @param today día de referencia (zona local).
     * @return `días_sostenidos ∈ [0, SUPPORT_WINDOW_DAYS]`.
     */
    fun sustainedSupportDays(logs: List<ActivityLog>, today: LocalDate): Int {
        val windowStart = today.minusDays((SUPPORT_WINDOW_DAYS - 1).toLong())
        val omittedDates = logs
            .filter { it.countsAsDone() }
            .mapNotNull { it.dateAsLocalDate() }
            .filter { it in windowStart..today }
            .distinct()
            .count()
        return (SUPPORT_WINDOW_DAYS - omittedDates).coerceIn(0, SUPPORT_WINDOW_DAYS)
    }

    /**
     * `n_tasks_hoy` por capa: tasks completadas HOY (zona local) con `layerId` no nulo y no
     * `Neutral`. Efímero diario: las tasks de días anteriores NO cuentan (mañana se resetea).
     *
     * @return mapa `layerId → conteo` (solo capas con al menos una task de hoy).
     */
    fun tasksTodayByLayer(tasks: List<Task>, today: LocalDate): Map<String, Int> =
        tasks
            .filter { it.isCompletedTodayWithLayer(today) }
            .groupingBy { it.layerId!! }
            .eachCount()

    private fun Task.isCompletedTodayWithLayer(today: LocalDate): Boolean {
        if (status != TaskStatus.Done) return false
        if (layerId == null || contributionRole == ContributionRole.Neutral) return false
        val completed = completedAt ?: return false
        val completedDate = Instant.ofEpochMilli(completed)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        return completedDate == today
    }

    /**
     * `días_recaída` de un track en la ventana semanal: días con `status == Relapse`.
     * El motor lo convierte a `M_sobr = Π_tracks (1−A)^días`. Un track limpio (0 recaídas)
     * produce factor `1` (invisible, no diluye).
     */
    fun relapseDaysByTrack(
        track: AbstinenceTrack,
        logs: List<AbstinenceLog>,
        weekDates: List<LocalDate>,
    ): Int {
        val weekDateStrings = weekDates.mapTo(HashSet()) { it.toString() }
        return logs
            .filter { it.trackId == track.id && it.status == AbstinenceStatus.Relapse }
            .filter { it.date in weekDateStrings }
            .map { it.date }
            .distinct()
            .count()
    }

    /**
     * Señal `M` de sueño: promedio de las noches CON dato (`sleepScore != null`); las `NoData` se
     * excluyen. Sin ninguna noche con dato → `null` (opt-in de sueño inactivo: el motor no aplica
     * término-sombra de sueño). El refinamiento a 4 componentes está fuera de alcance.
     */
    fun sleepSignal(nights: List<SleepNightScore>): Double? {
        val scored = nights.mapNotNull { it.sleepScore }
        if (scored.isEmpty()) return null
        return scored.map { it.toDouble() }.average()
    }

    /**
     * Compone las [LayerFacts] de cada capa activa desde el [WeeklyScoringContext] + las tasks de
     * la semana. Para cada capa con actividades visibles agrupa sus anclas y soportes; añade
     * `n_tasks_hoy` de esa capa. El cableado fino del opt-in (sueño/sobriedad → `optIn` de la capa
     * que corresponda) lo cierra el orquestador en PR-F; aquí `optIn` queda `null` salvo que el
     * caller lo provea — esta función expone las FORMAS, no decide la política de capa↔opt-in.
     *
     * @param context ventana semanal ya recolectada/deduplicada.
     * @param tasks tasks (para `n_tasks_hoy`); se filtran a HOY por capa internamente.
     * @param today día de referencia (zona local).
     */
    fun buildLayerFacts(
        context: WeeklyScoringContext,
        tasks: List<Task>,
        today: LocalDate,
    ): List<LayerFacts> {
        val tasksToday = tasksTodayByLayer(tasks, today)
        val activitiesByLayer = context.visibleActivities.groupBy { it.layerId }

        return context.activeLayers.mapNotNull { layer ->
            val layerActivities = activitiesByLayer[layer.id].orEmpty()
            val anchors = layerActivities
                .filter { it.activityType == ActivitySurface.Anchor }
                .map { def -> anchorWindow(def, context.weeklyLogsByActivity[def.id].orEmpty()) }
            val supportDays = layerActivities
                .filter { it.activityType == ActivitySurface.Support }
                .map { def ->
                    sustainedSupportDays(context.weeklyLogsByActivity[def.id].orEmpty(), today)
                }
            val nTasksToday = tasksToday[layer.id] ?: 0

            // Capa sin ninguna forma (ni anclas, ni soportes, ni tasks) no aporta término.
            if (anchors.isEmpty() && supportDays.isEmpty() && nTasksToday == 0) {
                null
            } else {
                LayerFacts(
                    anchors = anchors,
                    supportDays = supportDays,
                    nTasksToday = nTasksToday,
                )
            }
        }
    }
}
