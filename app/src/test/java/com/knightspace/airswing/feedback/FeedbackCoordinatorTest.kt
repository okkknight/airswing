package com.knightspace.airswing.feedback

import com.knightspace.airswing.recognition.VirtualImpactEvent
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeedbackCoordinatorTest {
    @Test
    fun `audio preload becomes ready only after every sample succeeds`() {
        val tracker = AudioLoadTracker(expectedSamples = 3)

        tracker.onLoadComplete(sampleId = 10, loadStatus = 0)
        tracker.onLoadComplete(sampleId = 11, loadStatus = 0)
        assertEquals(FeedbackStatus.LOADING, tracker.status)

        tracker.onLoadComplete(sampleId = 12, loadStatus = 0)
        assertEquals(FeedbackStatus.READY, tracker.status)
    }

    @Test
    fun `one failed preload permanently reports feedback error`() {
        val tracker = AudioLoadTracker(expectedSamples = 3)
        tracker.onLoadComplete(sampleId = 10, loadStatus = 1)
        tracker.onLoadComplete(sampleId = 11, loadStatus = 0)
        tracker.onLoadComplete(sampleId = 12, loadStatus = 0)

        assertEquals(FeedbackStatus.ERROR, tracker.status)
    }

    @Test
    fun `future impact timestamp becomes a bounded feedback delay`() {
        assertEquals(55L, feedbackDelayMs(eventTimestampNs = 155_000_000, nowNs = 100_000_000))
        assertEquals(0L, feedbackDelayMs(eventTimestampNs = 90_000_000, nowNs = 100_000_000))
        assertEquals(100L, feedbackDelayMs(eventTimestampNs = 500_000_000, nowNs = 100_000_000, maxDelayMs = 100))
    }

    @Test
    fun `one impact requests audio and haptic exactly once`() {
        val audio = FakeAudio(FeedbackStatus.READY, playSucceeds = true)
        val haptic = FakeHaptic()
        val coordinator = FeedbackCoordinator(audio, haptic)

        val accepted = coordinator.dispatch(VirtualImpactEvent(1, 2f, 3f))

        assertTrue(accepted)
        assertEquals(1, audio.calls)
        assertEquals(1, haptic.calls)
    }

    @Test
    fun `loading or failed audio never accepts an impact`() {
        val loadingAudio = FakeAudio(FeedbackStatus.LOADING, playSucceeds = true)
        val failedAudio = FakeAudio(FeedbackStatus.READY, playSucceeds = false)

        assertFalse(FeedbackCoordinator(loadingAudio, FakeHaptic()).dispatch(event()))
        assertFalse(FeedbackCoordinator(failedAudio, FakeHaptic()).dispatch(event()))
        assertEquals(0, loadingAudio.calls)
    }

    private fun event() = VirtualImpactEvent(1, 2f, 3f)

    private class FakeAudio(
        override val status: FeedbackStatus,
        private val playSucceeds: Boolean,
    ) : ImpactAudio {
        var calls = 0
        override fun play(event: VirtualImpactEvent): Boolean {
            calls++
            return playSucceeds
        }
    }

    private class FakeHaptic : ImpactHaptic {
        var calls = 0
        override fun play(): Boolean {
            calls++
            return true
        }
    }
}
