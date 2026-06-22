package dev.panopt.autonomia.domain.activity

/**
 * Decide si configurar un ancla debe registrar una **versión nueva** de la vara: solo cuando
 * cambia la meta de minutos por sesión o la de frecuencia (días/semana). Editar otros campos
 * (nombre, descripción) NO crea versión. Al crear (sin valores previos) siempre se registra la
 * versión inicial. Dominio puro JVM.
 */
object TargetVersionDecisionRule {

    /**
     * @param previousMinutes meta de minutos previa, o `null` si el ancla se está creando.
     * @param previousDays meta de frecuencia previa, o `null` si se está creando.
     * @return `true` si hay que registrar una versión nueva.
     */
    fun shouldRecordVersion(
        previousMinutes: Int?,
        previousDays: Int?,
        newMinutes: Int?,
        newDays: Int?,
    ): Boolean {
        // Creación (sin previa): registrar la versión inicial.
        if (previousMinutes == null && previousDays == null) return true
        // Edición: solo si cambió alguna de las dos varas.
        return previousMinutes != newMinutes || previousDays != newDays
    }
}
