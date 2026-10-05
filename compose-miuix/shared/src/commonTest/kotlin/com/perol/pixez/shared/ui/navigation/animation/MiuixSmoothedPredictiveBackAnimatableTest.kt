package com.perol.pixez.shared.ui.navigation.animation

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.essenty.backhandler.BackEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalDecomposeApi::class)
class MiuixSmoothedPredictiveBackAnimatableTest {

    private fun TestScope.launchAnimatable(
        clock: BroadcastFrameClock,
        block: suspend MiuixSmoothedPredictiveBackAnimatable.() -> Unit,
    ): MiuixSmoothedPredictiveBackAnimatable {
        val animatable = MiuixSmoothedPredictiveBackAnimatable(
            initialBackEvent = BackEvent(progress = 0f),
            getExitModifier = { _, _ -> Modifier },
            getEnterModifier = { _, _ -> Modifier },
        )
        launch(StandardTestDispatcher(testScheduler) + clock) {
            animatable.block()
        }
        return animatable
    }

    // 帧时间跨调用单调递增：弹簧动画按帧差推进，时间戳重复会导致动画零推进。
    private var frameTimeNanos = 0L

    private fun TestScope.stepFrames(clock: BroadcastFrameClock, frames: Int = 60) {
        repeat(frames) {
            frameTimeNanos += 16_000_000L
            clock.sendFrame(frameTimeNanos)
            testScheduler.advanceTimeBy(16)
            testScheduler.runCurrent()
        }
    }

    private fun recorderAnimatable(onProgress: (Float) -> Unit): MiuixSmoothedPredictiveBackAnimatable =
        MiuixSmoothedPredictiveBackAnimatable(
            initialBackEvent = BackEvent(progress = 0f),
            getExitModifier = { progress, _ ->
                onProgress(progress)
                Modifier
            },
            getEnterModifier = { _, _ -> Modifier },
        )

    @Test
    fun testAnimateEventConvergesToEventProgress() = runTest {
        val clock = BroadcastFrameClock()
        var observed = -1f
        val animatable = MiuixSmoothedPredictiveBackAnimatable(
            initialBackEvent = BackEvent(progress = 0f),
            getExitModifier = { progress, _ ->
                observed = progress
                Modifier
            },
            getEnterModifier = { _, _ -> Modifier },
        )
        launch(StandardTestDispatcher(testScheduler) + clock) {
            animatable.animate(BackEvent(progress = 0.5f))
        }
        stepFrames(clock)
        animatable.exitModifier
        assertEquals(0.5f, observed, 0.01f)
    }

    @Test
    fun testJumpyProgressIsSmoothedInsteadOfSnapped() = runTest {
        val clock = BroadcastFrameClock()
        val seen = mutableListOf<Float>()
        val animatable = recorderAnimatable { progress -> seen += progress }
        launch(StandardTestDispatcher(testScheduler) + clock) {
            animatable.animate(BackEvent(progress = 0f))
            animatable.animate(BackEvent(progress = 0.9f))
        }
        // snapTo 语义下进度只会出现 0 与 0.9；平滑语义下逐帧采样必然经过中间值。
        repeat(30) {
            stepFrames(clock, frames = 1)
            animatable.exitModifier
        }
        val intermediates = seen.filter { it > 0.05f && it < 0.85f }
        assertTrue(intermediates.isNotEmpty(), "expected intermediate progress values, seen=$seen")
    }

    @Test
    fun testFinishConvergesToOne() = runTest {
        val clock = BroadcastFrameClock()
        var observed = -1f
        val animatable = MiuixSmoothedPredictiveBackAnimatable(
            initialBackEvent = BackEvent(progress = 0f),
            getExitModifier = { progress, _ ->
                observed = progress
                Modifier
            },
            getEnterModifier = { _, _ -> Modifier },
        )
        launch(StandardTestDispatcher(testScheduler) + clock) {
            animatable.animate(BackEvent(progress = 0.3f))
            animatable.finish()
        }
        stepFrames(clock, frames = 90)
        animatable.exitModifier
        assertEquals(1f, observed, 0.01f)
    }

    @Test
    fun testCancelConvergesToZero() = runTest {
        val clock = BroadcastFrameClock()
        var observed = -1f
        val animatable = MiuixSmoothedPredictiveBackAnimatable(
            initialBackEvent = BackEvent(progress = 0f),
            getExitModifier = { progress, _ ->
                observed = progress
                Modifier
            },
            getEnterModifier = { _, _ -> Modifier },
        )
        launch(StandardTestDispatcher(testScheduler) + clock) {
            animatable.animate(BackEvent(progress = 0.7f))
            animatable.cancel()
        }
        stepFrames(clock, frames = 90)
        animatable.exitModifier
        assertEquals(0f, observed, 0.01f)
    }

    @Test
    fun testFinishDuringCancelConvergesToOne() = runTest {
        val clock = BroadcastFrameClock()
        var observed = -1f
        val animatable = MiuixSmoothedPredictiveBackAnimatable(
            initialBackEvent = BackEvent(progress = 0f),
            getExitModifier = { progress, _ ->
                observed = progress
                Modifier
            },
            getEnterModifier = { _, _ -> Modifier },
        )
        launch(StandardTestDispatcher(testScheduler) + clock) {
            animatable.animate(BackEvent(progress = 0.6f))
            // 取消动画启动并推进数帧后收到确认返回：应平滑转向 finish 收敛到 1，而非回弹到 0。
            launch { animatable.cancel() }
            stepFrames(clock, frames = 2)
            animatable.finish()
        }
        stepFrames(clock, frames = 120)
        animatable.exitModifier
        assertEquals(1f, observed, 0.02f)
    }

    @Test
    fun testSwipeEdgeIsForwardedToModifiers() = runTest {
        val clock = BroadcastFrameClock()
        var observedEdge: BackEvent.SwipeEdge? = null
        val animatable = MiuixSmoothedPredictiveBackAnimatable(
            initialBackEvent = BackEvent(progress = 0f),
            getExitModifier = { _, edge ->
                observedEdge = edge
                Modifier
            },
            getEnterModifier = { _, _ -> Modifier },
        )
        launch(StandardTestDispatcher(testScheduler) + clock) {
            animatable.animate(BackEvent(progress = 0.4f, swipeEdge = BackEvent.SwipeEdge.LEFT))
        }
        stepFrames(clock)
        animatable.exitModifier
        assertEquals(BackEvent.SwipeEdge.LEFT, observedEdge)
    }

    @Test
    fun testSnapUpdatesProgressImmediatelyWithoutTweenFrames() = runTest {
        val clock = BroadcastFrameClock()
        val seen = mutableListOf<Float>()
        val animatable = recorderAnimatable { progress -> seen += progress }
        launch(StandardTestDispatcher(testScheduler) + clock) {
            animatable.snap(BackEvent(progress = 0.4f))
        }
        testScheduler.runCurrent()
        animatable.exitModifier
        // snap 直驱（应用内拖拽跟手）：进度 1:1 立即落位，无弹簧补间。
        assertEquals(0.4f, seen.last(), 1e-4f)
        assertTrue(seen.none { it in 0.01f..0.39f }, "snap must not tween through intermediate values, seen=$seen")
    }
}
