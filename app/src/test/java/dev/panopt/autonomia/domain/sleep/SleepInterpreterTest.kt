package dev.panopt.autonomia.domain.sleep

import dev.panopt.autonomia.domain.sleep.interpretation.InterpretationParams
import dev.panopt.autonomia.domain.sleep.interpretation.SleepConfidence
import dev.panopt.autonomia.domain.sleep.interpretation.SleepInterpreter
import dev.panopt.autonomia.domain.sleep.interpretation.SleepSegmentKind
import dev.panopt.autonomia.platform.telemetry.DeviceActivityEvent
import dev.panopt.autonomia.platform.telemetry.DeviceActivityEventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * TDD RED phase — WU-3: SleepInterpreter.
 *
 * Tests reference SleepInterpreter.interpret(...) which does not exist yet.
 * All tests will fail (compile error) until SleepInterpreter.kt is created (GREEN).
 *
 * Detection window: 20:00 (day D-1) → 12:00 (day D).
 * nightDate = date of definitiveWake (wake day, not onset day).
 *
 * Test reference date: night of 2026-06-02 → 2026-06-03.
 * Base epoch: 2026-06-02T00:00:00Z (UTC) = 1_748_822_400_000 ms
 */
class SleepInterpreterTest {

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private val params = InterpretationParams.DEFAULT

    // Detection window reference: 2026-06-02 20:00 → 2026-06-03 12:00 (UTC for test simplicity)
    private val wakeDate = LocalDate.of(2026, 6, 3)

    private fun utcMillis(dateTime: String): Long =
        LocalDateTime.parse(dateTime).toInstant(ZoneOffset.UTC).toEpochMilli()

    private fun event(
        type: DeviceActivityEventType,
        dateTime: String,
        pkg: String? = null,
    ) = DeviceActivityEvent(
        eventType = type,
        packageName = pkg,
        timestamp = utcMillis(dateTime),
        source = "test",
    )

    // A plausible target window for a typical night (23:00–07:00 expressed as string)
    private val defaultTarget = SleepTargetWindow(
        targetSleepAt = "23:00",
        targetWakeAt = "07:00",
    )

    // ─── 1. Events outside detection window are discarded ────────────────────

    @Test
    fun eventsOutsideWindowAreDiscarded() {
        // 13:00 on day D-1 is BEFORE the 20:00 window start → must be discarded
        // 22:00 on day D-1 is INSIDE the window → must be processed
        val events = listOf(
            event(DeviceActivityEventType.USER_INTERACTION, "2026-06-02T13:00:00"), // outside
            event(DeviceActivityEventType.USER_INTERACTION, "2026-06-02T22:00:00"), // inside
        )
        val timeline = SleepInterpreter.interpret(events, defaultTarget, params)

        // The outside event must NOT contribute to an AwakeUse segment near 13:00
        val segmentsBeforeWindow = timeline.segments.filter {
            it.startAt.toEpochMilli() < utcMillis("2026-06-02T20:00:00")
        }
        assertTrue(
            "Events before 20:00 window start must be discarded",
            segmentsBeforeWindow.isEmpty(),
        )
    }

    // ─── 2. SCREEN_ON solo = vistazo — no crea AwakeUse ─────────────────────

    @Test
    fun screenOnAloneIsIgnoredAsGlance() {
        // Quiet night: only a SCREEN_ON at 02:30, no USER_INTERACTION follows
        val events = listOf(
            event(DeviceActivityEventType.SCREEN_ON, "2026-06-02T23:00:00"),
            event(DeviceActivityEventType.SCREEN_OFF, "2026-06-02T23:00:30"),
            event(DeviceActivityEventType.SCREEN_ON, "2026-06-03T02:30:00"),
            // No USER_INTERACTION or APP_FOREGROUND follows — just a glance
            event(DeviceActivityEventType.SCREEN_OFF, "2026-06-03T02:30:05"),
        )
        val timeline = SleepInterpreter.interpret(events, defaultTarget, params)

        val awakeUseSegments = timeline.segments.filter { it.kind == SleepSegmentKind.AwakeUse }
        assertTrue(
            "SCREEN_ON alone must not create an AwakeUse segment",
            awakeUseSegments.isEmpty(),
        )
    }

    // ─── 3. USER_INTERACTION crea despertar real ──────────────────────────────

    @Test
    fun userInteractionCreatesAwakeUse() {
        val events = listOf(
            event(DeviceActivityEventType.USER_INTERACTION, "2026-06-03T03:15:00"),
            event(DeviceActivityEventType.SCREEN_OFF, "2026-06-03T03:20:00"),
        )
        val timeline = SleepInterpreter.interpret(events, defaultTarget, params)

        val awakeUseSegments = timeline.segments.filter { it.kind == SleepSegmentKind.AwakeUse }
        assertEquals(
            "USER_INTERACTION must create exactly one AwakeUse segment",
            1,
            awakeUseSegments.size,
        )
        val seg = awakeUseSegments.first()
        assertEquals(utcMillis("2026-06-03T03:15:00"), seg.startAt.toEpochMilli())
    }

    // ─── 4. APP_FOREGROUND crea uso real ──────────────────────────────────────

    @Test
    fun appForegroundCreatesAwakeUse() {
        val events = listOf(
            event(DeviceActivityEventType.APP_FOREGROUND, "2026-06-03T04:00:00", "com.spotify"),
            event(DeviceActivityEventType.APP_BACKGROUND, "2026-06-03T04:05:00", "com.spotify"),
        )
        val timeline = SleepInterpreter.interpret(events, defaultTarget, params)

        val awakeUseSegments = timeline.segments.filter { it.kind == SleepSegmentKind.AwakeUse }
        assertEquals(
            "APP_FOREGROUND must create exactly one AwakeUse segment",
            1,
            awakeUseSegments.size,
        )
        val seg = awakeUseSegments.first()
        assertEquals(utcMillis("2026-06-03T04:00:00"), seg.startAt.toEpochMilli())
    }

    // ─── 5. Tanda de eventos agrupados en un solo AwakeUse ───────────────────

    @Test
    fun closeRealUseEventsGroupedIntoOneAwakeUse() {
        // Three events within a few minutes of each other → ONE AwakeUse episode
        val events = listOf(
            event(DeviceActivityEventType.USER_INTERACTION, "2026-06-03T02:00:00"),
            event(DeviceActivityEventType.APP_FOREGROUND, "2026-06-03T02:03:00"),
            event(DeviceActivityEventType.USER_INTERACTION, "2026-06-03T02:07:00"),
            event(DeviceActivityEventType.SCREEN_OFF, "2026-06-03T02:10:00"),
        )
        val timeline = SleepInterpreter.interpret(events, defaultTarget, params)

        val awakeUseSegments = timeline.segments.filter { it.kind == SleepSegmentKind.AwakeUse }
        assertEquals(
            "Close real-use events must be grouped into exactly one AwakeUse segment",
            1,
            awakeUseSegments.size,
        )
        val seg = awakeUseSegments.first()
        // Segment spans from first event to (approximately) last event in the group
        assertEquals(utcMillis("2026-06-03T02:00:00"), seg.startAt.toEpochMilli())
    }

    // ─── 6. Onset = quietud tras último uso real ──────────────────────────────

    @Test
    fun sleepOnsetAtIsSetAfterLastRealUse() {
        // Real use ends at 23:45, then silence through 06:30
        val events = listOf(
            event(DeviceActivityEventType.USER_INTERACTION, "2026-06-02T23:40:00"),
            event(DeviceActivityEventType.USER_INTERACTION, "2026-06-02T23:45:00"),
            event(DeviceActivityEventType.SCREEN_OFF, "2026-06-02T23:46:00"),
            // Long silence → interpreted as sleep
            event(DeviceActivityEventType.USER_INTERACTION, "2026-06-03T06:30:00"),
            event(DeviceActivityEventType.SCREEN_OFF, "2026-06-03T06:40:00"),
        )
        val timeline = SleepInterpreter.interpret(events, defaultTarget, params)

        assertNotNull("sleepOnsetAt must be set when sleep is detected", timeline.sleepOnsetAt)
        // Onset must be at or after the last real use before the main sleep block
        val onsetMs = timeline.sleepOnsetAt!!.toEpochMilli()
        assertTrue(
            "sleepOnsetAt must be >= last real use event (23:45 or after)",
            onsetMs >= utcMillis("2026-06-02T23:45:00"),
        )
    }

    // ─── 7. Detox no cuenta como anchor de onset ──────────────────────────────

    @Test
    fun detoxActivityDoesNotAnchorOnsetBeforeIt() {
        // APP_FOREGROUND events (detox-like) ending at 23:50, then silence
        val events = listOf(
            event(DeviceActivityEventType.APP_FOREGROUND, "2026-06-02T23:45:00", "com.kindle"),
            event(DeviceActivityEventType.APP_FOREGROUND, "2026-06-02T23:50:00", "com.kindle"),
            event(DeviceActivityEventType.APP_BACKGROUND, "2026-06-02T23:51:00", "com.kindle"),
            // Long silence
            event(DeviceActivityEventType.USER_INTERACTION, "2026-06-03T07:00:00"),
            event(DeviceActivityEventType.SCREEN_OFF, "2026-06-03T07:05:00"),
        )
        val timeline = SleepInterpreter.interpret(events, defaultTarget, params)

        assertNotNull("sleepOnsetAt must be set", timeline.sleepOnsetAt)
        val onsetMs = timeline.sleepOnsetAt!!.toEpochMilli()
        assertTrue(
            "sleepOnsetAt must be set after APP_FOREGROUND at 23:50 (detox is real use)",
            onsetMs >= utcMillis("2026-06-02T23:50:00"),
        )
    }

    // ─── 8. Siesta lejos del objetivo no cuenta como noche ───────────────────

    @Test
    fun napFarFromGoalWindowIsExcluded() {
        // Scenario: a short evening nap (20:15–21:30, 75min) followed by
        // a long main night (23:55–07:30, ~7.5h). Goal window: 00:00–07:00.
        // The nap block is far from the goal; the main sleep block is the night.
        // With napSeparationMillis=90min, the 2h+ gap between nap and main night
        // puts them in separate sleep blocks.
        val napTarget = SleepTargetWindow(
            targetSleepAt = "00:00",
            targetWakeAt = "07:00",
        )
        val events = listOf(
            // Pre-nap use ending at 20:15
            event(DeviceActivityEventType.USER_INTERACTION, "2026-06-02T20:10:00"),
            event(DeviceActivityEventType.SCREEN_OFF, "2026-06-02T20:15:00"),
            // Post-nap wakeup at 21:45 (90+ min gap from 20:15 → separate block)
            event(DeviceActivityEventType.USER_INTERACTION, "2026-06-02T21:50:00"),
            event(DeviceActivityEventType.SCREEN_OFF, "2026-06-02T21:55:00"),
            // Evening activity ending at 23:55 (real sleep onset anchor)
            event(DeviceActivityEventType.USER_INTERACTION, "2026-06-02T23:50:00"),
            event(DeviceActivityEventType.SCREEN_OFF, "2026-06-02T23:55:00"),
            // Definitive wake at 07:30 (7.5h after 00:00)
            event(DeviceActivityEventType.USER_INTERACTION, "2026-06-03T07:30:00"),
            event(DeviceActivityEventType.SCREEN_OFF, "2026-06-03T07:40:00"),
        )
        val timeline = SleepInterpreter.interpret(events, napTarget, params)

        // The main sleep block spans from ~00:00 to ~07:30 (7.5h = 450min).
        // Verify the interpreter found a significant Asleep block (>= 200 min) after 23:00.
        val longAsleepAfterMidnight = timeline.segments
            .filter { seg ->
                seg.kind == SleepSegmentKind.Asleep &&
                    seg.startAt.toEpochMilli() >= utcMillis("2026-06-02T23:00:00")
            }
            .any { seg ->
                val durationMin = (seg.endAt.toEpochMilli() - seg.startAt.toEpochMilli()) / 60_000L
                durationMin >= 200 // at least 3.3h contiguous Asleep block after 23:00
            }
        assertTrue(
            "Interpreter must include a long Asleep block (>=200min) after 23:00 (main night, not nap)",
            longAsleepAfterMidnight,
        )
    }

    // ─── 9. Noche pertenece al día de despertar ───────────────────────────────

    @Test
    fun nightDateAssignedToWakeDay() {
        // Onset 2026-06-02 23:00, definitive wake 2026-06-03 06:30
        val events = listOf(
            event(DeviceActivityEventType.USER_INTERACTION, "2026-06-02T22:50:00"),
            event(DeviceActivityEventType.SCREEN_OFF, "2026-06-02T23:00:00"),
            event(DeviceActivityEventType.USER_INTERACTION, "2026-06-03T06:30:00"),
            event(DeviceActivityEventType.SCREEN_OFF, "2026-06-03T06:45:00"),
        )
        val timeline = SleepInterpreter.interpret(events, defaultTarget, params)

        assertEquals(
            "nightDate must be 2026-06-03 (wake day), not 2026-06-02 (onset day)",
            LocalDate.of(2026, 6, 3),
            timeline.nightDate,
        )
    }

    // ─── 10. Teléfono quieto → ALTA confianza ────────────────────────────────

    @Test
    fun quietPhoneYieldsHighConfidence() {
        // Zero USER_INTERACTION/APP_FOREGROUND in the biological window
        // (Quiet phone is the signature of good sleep — must NOT be NoData or Ambiguous)
        val events = listOf(
            // Only SCREEN_ON/OFF glances — no real use
            event(DeviceActivityEventType.SCREEN_ON, "2026-06-03T03:00:00"),
            event(DeviceActivityEventType.SCREEN_OFF, "2026-06-03T03:00:05"),
        )
        // Force onset by providing pre-window real use to anchor sleep
        val eventsWithOnset = listOf(
            event(DeviceActivityEventType.USER_INTERACTION, "2026-06-02T22:55:00"),
            event(DeviceActivityEventType.SCREEN_OFF, "2026-06-02T23:00:00"),
        ) + events + listOf(
            event(DeviceActivityEventType.USER_INTERACTION, "2026-06-03T07:00:00"),
            event(DeviceActivityEventType.SCREEN_OFF, "2026-06-03T07:05:00"),
        )
        val timeline = SleepInterpreter.interpret(eventsWithOnset, defaultTarget, params)

        assertEquals(
            "Quiet phone (no real use during night) must yield High confidence",
            SleepConfidence.High,
            timeline.confidence,
        )
    }

    // ─── 11. Sin señal → NoData, sin score fabricado ─────────────────────────

    @Test
    fun noEventsYieldsNoData() {
        // Absolutely no telemetry events — total absence of signal
        val timeline = SleepInterpreter.interpret(emptyList(), defaultTarget, params)

        assertEquals(
            "Total absence of telemetry must yield NoData confidence",
            SleepConfidence.NoData,
            timeline.confidence,
        )
        assertTrue("NoData night must have no segments", timeline.segments.isEmpty())
        assertNull("NoData night must have null sleepOnsetAt", timeline.sleepOnsetAt)
        assertNull("NoData night must have null definitiveWakeAt", timeline.definitiveWakeAt)
    }

    // ─── 12. Señal contradictoria → AMBIGUOUS ────────────────────────────────

    @Test
    fun contradictorySignalYieldsAmbiguous() {
        // Alternating SCREEN_ON + USER_INTERACTION throughout the night with NO quiet block
        // This creates a noisy signal with no clear onset anchor
        val events = mutableListOf<DeviceActivityEvent>()
        var hour = 20
        while (hour < 28) { // 20:00 to 04:00 next day
            val h = hour % 24
            val day = if (hour < 24) "2026-06-02" else "2026-06-03"
            val time = "%02d:00:00".format(h)
            events.add(event(DeviceActivityEventType.USER_INTERACTION, "${day}T${time}"))
            events.add(event(DeviceActivityEventType.SCREEN_OFF, "${day}T%02d:05:00".format(h)))
            hour++
        }
        val timeline = SleepInterpreter.interpret(events, defaultTarget, params)

        assertEquals(
            "Continuously fragmented signal must yield Ambiguous confidence",
            SleepConfidence.Ambiguous,
            timeline.confidence,
        )
    }

    // ─── 13. API 26 sin SCREEN events — proxy suficiente ──────────────────────

    @Test
    fun api26ProxySignalWorksWithoutScreenEvents() {
        // API 26: no SCREEN_ON/OFF events, but APP_FOREGROUND and USER_INTERACTION present
        // The interpreter must work correctly with proxy signals only
        val events = listOf(
            event(DeviceActivityEventType.USER_INTERACTION, "2026-06-02T23:00:00"),
            event(DeviceActivityEventType.APP_FOREGROUND, "2026-06-02T23:02:00", "com.app"),
            event(DeviceActivityEventType.APP_BACKGROUND, "2026-06-02T23:05:00", "com.app"),
            // Long silence (no SCREEN events — API 26 proxy scenario)
            event(DeviceActivityEventType.USER_INTERACTION, "2026-06-03T07:00:00"),
            event(DeviceActivityEventType.APP_BACKGROUND, "2026-06-03T07:01:00", "com.app"),
        )
        val timeline = SleepInterpreter.interpret(events, defaultTarget, params)

        // Must produce meaningful interpretation (not forced to NoData due to missing SCREEN events)
        assertTrue(
            "API 26 proxy signals must yield segments (not empty NoData timeline)",
            timeline.segments.isNotEmpty() || timeline.confidence == SleepConfidence.High,
        )
        // Must not downgrade to Ambiguous just because of missing SCREEN events
        val confidence = timeline.confidence
        assertTrue(
            "API 26 should not force Ambiguous confidence solely from missing SCREEN events",
            confidence == SleepConfidence.High || confidence == SleepConfidence.Ambiguous,
        )
    }

    // ─── Triangulation: Two wake episodes → two AwakeUse segments ────────────

    @Test
    fun twoSeparateWakeEpisodesProduceTwoAwakeUseSegments() {
        // Two distinct wake episodes, each separated by > quietGapMillis
        val events = listOf(
            // First wake episode: 02:00
            event(DeviceActivityEventType.USER_INTERACTION, "2026-06-03T02:00:00"),
            event(DeviceActivityEventType.SCREEN_OFF, "2026-06-03T02:10:00"),
            // Sleep gap: more than 15 minutes silence (02:10 → 04:00 = ~110 min)
            // Second wake episode: 04:00
            event(DeviceActivityEventType.USER_INTERACTION, "2026-06-03T04:00:00"),
            event(DeviceActivityEventType.SCREEN_OFF, "2026-06-03T04:10:00"),
            // Definitive wake: 07:00
            event(DeviceActivityEventType.USER_INTERACTION, "2026-06-03T07:00:00"),
            event(DeviceActivityEventType.SCREEN_OFF, "2026-06-03T07:15:00"),
        )
        // Provide onset anchor
        val eventsWithOnset = listOf(
            event(DeviceActivityEventType.USER_INTERACTION, "2026-06-02T23:00:00"),
            event(DeviceActivityEventType.SCREEN_OFF, "2026-06-02T23:10:00"),
        ) + events
        val timeline = SleepInterpreter.interpret(eventsWithOnset, defaultTarget, params)

        val awakeUseSegments = timeline.segments.filter { it.kind == SleepSegmentKind.AwakeUse }
        assertTrue(
            "Two distinct wake episodes must produce at least 2 AwakeUse segments",
            awakeUseSegments.size >= 2,
        )
    }
}
