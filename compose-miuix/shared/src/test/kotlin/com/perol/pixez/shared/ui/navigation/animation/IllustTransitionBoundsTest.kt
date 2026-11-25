package com.perol.pixez.shared.ui.navigation.animation

import androidx.compose.ui.geometry.Size
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IllustTransitionBoundsTest {

    @Test
    fun `SystemCardLayerCaptureGuard freezes layer recording while pressed and during release settle`() {
        val guard = SystemCardLayerCaptureGuard()
        val cardSize = Size(320f, 120f)

        // Initial frame must record even if called cleanly before any touch.
        assertTrue(guard.shouldRecord(cardSize, nowUptimeMs = 100L))
        guard.markRecorded(cardSize)
        assertTrue(guard.shouldRecord(cardSize, nowUptimeMs = 200L))

        // Pointer down locks the unpressed snapshot even across size changes until release.
        guard.onPointerDown()
        assertFalse(guard.shouldRecord(cardSize, nowUptimeMs = 210L))
        assertFalse(guard.shouldRecord(Size(300f, 120f), nowUptimeMs = 220L))

        // Immediately after pointer up (during press feedback decay), snapshot stays frozen.
        guard.onPointerUpOrCancel(nowUptimeMs = 300L)
        assertFalse(guard.shouldRecord(cardSize, nowUptimeMs = 301L))
        assertFalse(guard.shouldRecord(cardSize, nowUptimeMs = 749L))

        // Once settle window elapses, resting frames record again.
        assertTrue(guard.shouldRecord(cardSize, nowUptimeMs = 750L))
    }

    @Test
    fun `SystemCardLayerCaptureGuard records after size change when pointer is not down`() {
        val guard = SystemCardLayerCaptureGuard()
        val initial = Size(320f, 120f)
        val resized = Size(320f, 160f)

        assertTrue(guard.shouldRecord(initial, nowUptimeMs = 100L))
        guard.markRecorded(initial)

        guard.onPointerDown()
        guard.onPointerUpOrCancel(nowUptimeMs = 200L)

        assertTrue(guard.shouldRecord(resized, nowUptimeMs = 210L))
    }
}
