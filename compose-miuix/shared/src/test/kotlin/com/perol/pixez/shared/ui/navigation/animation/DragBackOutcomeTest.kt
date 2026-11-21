package com.perol.pixez.shared.ui.navigation.animation

import kotlin.test.Test
import kotlin.test.assertEquals

class DragBackOutcomeTest {

    @Test
    fun testProgressAtThresholdCommits() {
        assertEquals(DragBackOutcome.COMMIT, resolveDragBackOutcome(progress = 0.35f, velocityPxPerSec = 0f))
    }

    @Test
    fun testProgressBelowThresholdCancels() {
        assertEquals(DragBackOutcome.CANCEL, resolveDragBackOutcome(progress = 0.34f, velocityPxPerSec = 0f))
    }

    @Test
    fun testVelocityAtThresholdCommitsEvenWithSmallProgress() {
        assertEquals(DragBackOutcome.COMMIT, resolveDragBackOutcome(progress = 0.1f, velocityPxPerSec = 2000f))
    }

    @Test
    fun testVelocityBelowThresholdCancelsWithSmallProgress() {
        assertEquals(DragBackOutcome.CANCEL, resolveDragBackOutcome(progress = 0.1f, velocityPxPerSec = 1999f))
    }

    @Test
    fun testZeroDragCancels() {
        assertEquals(DragBackOutcome.CANCEL, resolveDragBackOutcome(progress = 0f, velocityPxPerSec = 0f))
    }

    @Test
    fun testFullDragCommits() {
        assertEquals(DragBackOutcome.COMMIT, resolveDragBackOutcome(progress = 1f, velocityPxPerSec = 0f))
    }
}
