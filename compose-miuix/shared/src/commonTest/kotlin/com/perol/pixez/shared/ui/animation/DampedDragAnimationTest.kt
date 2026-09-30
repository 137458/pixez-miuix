package com.perol.pixez.shared.ui.animation

import androidx.compose.runtime.BroadcastFrameClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DampedDragAnimationTest {

    private suspend fun TestScope.stepFrames(
        clock: BroadcastFrameClock,
        frames: Int = 80,
        deltaNanos: Long = 16_000_000L,
    ) {
        var time = 0L
        repeat(frames) {
            time += deltaNanos
            clock.sendFrame(time)
            testScheduler.advanceTimeBy(16)
            testScheduler.runCurrent()
        }
    }

    @Test
    fun testUpdateValueCoercesToRange() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(testDispatcher)
        val anim = DampedDragAnimation(
            animationScope = scope,
            initialValue = 0f,
            valueRange = 0f..3f,
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 1.39f,
            onDragStarted = {},
            onDragStopped = {},
            onDrag = { _, _ -> },
        )

        anim.updateValue(1.5f)
        scope.advanceUntilIdle()
        assertEquals(1.5f, anim.value, 0.001f)

        anim.updateValue(-1f)
        scope.advanceUntilIdle()
        assertEquals(0f, anim.value, 0.001f)

        anim.updateValue(5f)
        scope.advanceUntilIdle()
        assertEquals(3f, anim.value, 0.001f)
    }

    @Test
    fun testTabSwitchAnimationDoesNotTriggerPressLag() = runTest {
        val clock = BroadcastFrameClock()
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(testDispatcher + clock)

        withContext(clock) {
            val anim = DampedDragAnimation(
                animationScope = scope,
                initialValue = 0f,
                valueRange = 0f..3f,
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 1.39f,
                onDragStarted = {},
                onDragStopped = {},
                onDrag = { _, _ -> },
            )

            // 点击选项切换时指示器不应该膨胀变形（按压形变导致严重滞后）
            anim.animateToValue(2f, pressed = false)
            assertEquals(0f, anim.pressProgress, 0.001f)
            assertEquals(1f, anim.scaleX, 0.001f)

            stepFrames(clock, frames = 80)
            assertEquals(2f, anim.value, 0.05f)
            assertEquals(0f, anim.pressProgress, 0.001f)
        }
    }

    @Test
    fun testPressAndReleaseRestoresScale() = runTest {
        val clock = BroadcastFrameClock()
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(testDispatcher + clock)

        withContext(clock) {
            val anim = DampedDragAnimation(
                animationScope = scope,
                initialValue = 0f,
                valueRange = 0f..3f,
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 1.39f,
                onDragStarted = {},
                onDragStopped = {},
                onDrag = { _, _ -> },
            )

            anim.press()
            stepFrames(clock, frames = 60)
            assertTrue(anim.pressProgress > 0.9f)
            assertTrue(anim.scaleX > 1.3f)

            anim.release()
            stepFrames(clock, frames = 60)
            assertEquals(0f, anim.pressProgress, 0.05f)
            assertEquals(1f, anim.scaleX, 0.05f)
        }
    }
}
