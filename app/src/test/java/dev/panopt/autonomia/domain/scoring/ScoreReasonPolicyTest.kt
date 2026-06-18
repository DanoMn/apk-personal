package dev.panopt.autonomia.domain.scoring

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Razones del reporte semanal (señales en lenguaje del usuario). Reconstrucción adaptada al motor
 * núcleo: las señales ya no son las del modelo viejo, pero los 3 mensajes y sus umbrales se
 * preservan (peor capa < 0.60, sueño < 0.70, sobriedad activa < 0.70).
 *
 * Tono: adulto compasivo (AGENTS.md) — sin "fallaste"/"deberías".
 */
class ScoreReasonPolicyTest {

    private val worstLayerMsg = "La capa más baja es Cuerpo."
    private val sleepMsg = "El descanso bajo está afectando Cuerpo."
    private val sobrietyMsg = "Sobriedad está reduciendo Conducta esta semana."

    // ---------------- peor capa (umbral 0.60) ----------------

    @Test
    fun `peor capa por debajo de 0_60 incluye la razon de capa`() {
        val reasons = ScoreReasonPolicy.build(
            worstLayerName = "Cuerpo",
            worstLayerBaseEff = 0.5,
            sleepSignal = null,
            sobrietySignal = null,
            hasActiveSobriety = false,
        )
        assertTrue(reasons.contains(worstLayerMsg))
    }

    @Test
    fun `peor capa en 0_90 no incluye la razon de capa`() {
        val reasons = ScoreReasonPolicy.build(
            worstLayerName = "Cuerpo",
            worstLayerBaseEff = 0.9,
            sleepSignal = null,
            sobrietySignal = null,
            hasActiveSobriety = false,
        )
        assertFalse(reasons.contains(worstLayerMsg))
    }

    @Test
    fun `peor capa sin nombre no incluye la razon de capa`() {
        val reasons = ScoreReasonPolicy.build(
            worstLayerName = null,
            worstLayerBaseEff = 0.1,
            sleepSignal = null,
            sobrietySignal = null,
            hasActiveSobriety = false,
        )
        assertFalse(reasons.any { it.startsWith("La capa más baja") })
    }

    // ---------------- sueño (umbral 0.70) ----------------

    @Test
    fun `senal de sueno por debajo de 0_70 incluye la razon de descanso`() {
        val reasons = ScoreReasonPolicy.build(
            worstLayerName = null,
            worstLayerBaseEff = null,
            sleepSignal = 0.5,
            sobrietySignal = null,
            hasActiveSobriety = false,
        )
        assertTrue(reasons.contains(sleepMsg))
    }

    @Test
    fun `senal de sueno en 0_80 no incluye la razon de descanso`() {
        val reasons = ScoreReasonPolicy.build(
            worstLayerName = null,
            worstLayerBaseEff = null,
            sleepSignal = 0.8,
            sobrietySignal = null,
            hasActiveSobriety = false,
        )
        assertFalse(reasons.contains(sleepMsg))
    }

    @Test
    fun `sin senal de sueno no incluye la razon de descanso`() {
        val reasons = ScoreReasonPolicy.build(
            worstLayerName = null,
            worstLayerBaseEff = null,
            sleepSignal = null,
            sobrietySignal = null,
            hasActiveSobriety = false,
        )
        assertFalse(reasons.contains(sleepMsg))
    }

    // ---------------- sobriedad (umbral 0.70) ----------------

    @Test
    fun `sobriedad activa con senal por debajo de 0_70 incluye la razon de sobriedad`() {
        val reasons = ScoreReasonPolicy.build(
            worstLayerName = null,
            worstLayerBaseEff = null,
            sleepSignal = null,
            sobrietySignal = 0.5,
            hasActiveSobriety = true,
        )
        assertTrue(reasons.contains(sobrietyMsg))
    }

    @Test
    fun `sobriedad activa con senal en 0_80 no incluye la razon de sobriedad`() {
        val reasons = ScoreReasonPolicy.build(
            worstLayerName = null,
            worstLayerBaseEff = null,
            sleepSignal = null,
            sobrietySignal = 0.8,
            hasActiveSobriety = true,
        )
        assertFalse(reasons.contains(sobrietyMsg))
    }

    @Test
    fun `sin tracks de sobriedad no incluye la razon de sobriedad aunque la senal este baja`() {
        val reasons = ScoreReasonPolicy.build(
            worstLayerName = null,
            worstLayerBaseEff = null,
            sleepSignal = null,
            sobrietySignal = 0.5,
            hasActiveSobriety = false,
        )
        assertFalse(reasons.contains(sobrietyMsg))
    }

    // ---------------- combinación ----------------

    @Test
    fun `multiples deficits acumulan multiples razones`() {
        val reasons = ScoreReasonPolicy.build(
            worstLayerName = "Cuerpo",
            worstLayerBaseEff = 0.4,
            sleepSignal = 0.5,
            sobrietySignal = 0.5,
            hasActiveSobriety = true,
        )
        assertTrue(reasons.contains(worstLayerMsg))
        assertTrue(reasons.contains(sleepMsg))
        assertTrue(reasons.contains(sobrietyMsg))
    }

    @Test
    fun `todo en buen estado no produce razones`() {
        val reasons = ScoreReasonPolicy.build(
            worstLayerName = "Cuerpo",
            worstLayerBaseEff = 0.9,
            sleepSignal = 0.9,
            sobrietySignal = 0.9,
            hasActiveSobriety = true,
        )
        assertTrue(reasons.isEmpty())
    }
}
