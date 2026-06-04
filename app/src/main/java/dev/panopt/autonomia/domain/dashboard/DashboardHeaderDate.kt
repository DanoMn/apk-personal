package dev.panopt.autonomia.domain.dashboard

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month

/**
 * Formatea la fecha del header del dashboard en español ("Jueves 4 de junio").
 *
 * Determinístico y sin dependencia de `Locale` (los nombres están fijos), para que no
 * dependa de los datos de locale del dispositivo y sea testeable en JVM puro. Reemplaza
 * el texto que estaba hardcodeado en `TopBar` ("Miercoles 20 de mayo").
 */
internal object DashboardHeaderDate {
    private val DAY_NAMES = mapOf(
        DayOfWeek.MONDAY to "Lunes",
        DayOfWeek.TUESDAY to "Martes",
        DayOfWeek.WEDNESDAY to "Miércoles",
        DayOfWeek.THURSDAY to "Jueves",
        DayOfWeek.FRIDAY to "Viernes",
        DayOfWeek.SATURDAY to "Sábado",
        DayOfWeek.SUNDAY to "Domingo",
    )

    private val MONTH_NAMES = mapOf(
        Month.JANUARY to "enero",
        Month.FEBRUARY to "febrero",
        Month.MARCH to "marzo",
        Month.APRIL to "abril",
        Month.MAY to "mayo",
        Month.JUNE to "junio",
        Month.JULY to "julio",
        Month.AUGUST to "agosto",
        Month.SEPTEMBER to "septiembre",
        Month.OCTOBER to "octubre",
        Month.NOVEMBER to "noviembre",
        Month.DECEMBER to "diciembre",
    )

    fun format(date: LocalDate): String =
        "${DAY_NAMES.getValue(date.dayOfWeek)} ${date.dayOfMonth} de ${MONTH_NAMES.getValue(date.month)}"
}
