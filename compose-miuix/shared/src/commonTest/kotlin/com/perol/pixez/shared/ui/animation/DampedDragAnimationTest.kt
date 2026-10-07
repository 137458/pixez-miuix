package com.perol.pixez.shared.ui.animation

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.snapshots.Snapshot
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
            Snapshot.sendApplyNotifications()
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
    fun testTabSwitchAnimationTriggersScaleAndRestores() = runTest {
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

            // 切换 tab 时指示器应该先膨胀放大（呼吸弹性质感），并在到位后平滑恢复
            anim.animateToValue(2f)
            stepFrames(clock, frames = 10)
            assertTrue(anim.pressProgress > 0.5f, "起步阶段 pressProgress 应当放大生效")
            assertTrue(anim.scaleX > 1.2f, "起步阶段 scaleX 应当放大")

            stepFrames(clock, frames = 100)
            assertEquals(2f, anim.value, 0.05f)
            assertEquals(0f, anim.pressProgress, 0.05f)
            assertEquals(1f, anim.scaleX, 0.05f)
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
