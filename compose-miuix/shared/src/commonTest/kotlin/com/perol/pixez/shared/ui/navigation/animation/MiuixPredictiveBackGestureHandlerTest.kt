package com.perol.pixez.shared.ui.navigation.animation

import androidx.compose.ui.Modifier
import com.arkivanov.decompose.Child
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.PredictiveBackAnimatable
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.essenty.backhandler.BackEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalDecomposeApi::class)
class MiuixPredictiveBackGestureHandlerTest {

    private class FakePredictiveBackAnimatable : PredictiveBackAnimatable {
        val animated = mutableListOf<Float>()
        var finishCount = 0
        var cancelCount = 0
        var cancelGate: CompletableDeferred<Unit>? = null

        override val exitModifier: Modifier get() = Modifier
        override val enterModifier: Modifier get() = Modifier

        override suspend fun animate(event: BackEvent) {
            animated += event.progress
        }

        override suspend fun finish() {
            finishCount++
        }

        override suspend fun cancel() {
            cancelCount++
            cancelGate?.await()
        }
    }

    private data class Fixture(
        val scope: TestScope,
        val stack: ChildStack<String, String>,
        val handler: MiuixPredictiveBackGestureHandler<String, String>,
        val animatables: MutableList<FakePredictiveBackAnimatable>,
    )

    private fun runFixture(gestureEnabled: Boolean = true, block: suspend Fixture.() -> Unit) = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val animatables = mutableListOf<FakePredictiveBackAnimatable>()
        val stack = ChildStack(
            active = Child.Created(configuration = "detail", instance = "detail-instance", key = "detail"),
            backStack = listOf(Child.Created(configuration = "main", instance = "main-instance", key = "main")),
        )
        val handler = MiuixPredictiveBackGestureHandler<String, String>(
            stack = stack,
            scope = scope,
            isGestureEnabled = { gestureEnabled },
            key = 0,
            selector = { _, _, _ ->
                FakePredictiveBackAnimatable().also { animatables += it }
            },
            onBack = {},
        )
        Fixture(scope, stack, handler, animatables).block()
    }

    private fun event(progress: Float): BackEvent = BackEvent(progress = progress, swipeEdge = BackEvent.SwipeEdge.LEFT)

    @Test
    fun testBackWithoutProgressPopsImmediatelyWithoutEngagement() = runFixture {
        var backInvoked = 0
        val plainHandler = MiuixPredictiveBackGestureHandler<String, String>(
            stack = stack,
            scope = scope,
            isGestureEnabled = { true },
            key = 0,
            selector = { _, _, _ -> error("selector must not be called without progress events") },
            onBack = { backInvoked++ },
        )
        plainHandler.onBack()
        scope.advanceUntilIdle()
        assertEquals(1, backInvoked)
        assertEquals(1, plainHandler.items.size)
    }

    @Test
    fun testProgressEngagesPredictiveItems() = runFixture {
        handler.onBackStarted(event(0f))
        handler.onBackProgressed(event(0.4f))
        scope.advanceUntilIdle()

        assertEquals(1, animatables.size)
        assertEquals(listOf(0.4f), animatables.single().animated)
        assertEquals(2, handler.items.size)
        assertEquals(1, handler.items[0].key)
        assertEquals(0, handler.items[1].key)
        assertEquals("main", handler.items[0].stack.active.configuration)
        assertEquals("detail", handler.items[1].stack.active.configuration)
    }

    @Test
    fun testProgressWithoutStartedEngagesWithProgressEventAsInitial() = runFixture {
        handler.onBackProgressed(event(0.3f))
        scope.advanceUntilIdle()
        assertEquals(1, animatables.size)
        assertEquals(2, handler.items.size)
    }

    @Test
    fun testGestureDisabledBlocksEngagement() = runFixture(gestureEnabled = false) {
        handler.onBackStarted(event(0f))
        handler.onBackProgressed(event(0.4f))
        scope.advanceUntilIdle()
        assertTrue(animatables.isEmpty())
        assertEquals(1, handler.items.size)
    }

    @Test
    fun testCancelledResetsItemsOnlyAfterCancelAnimationCompletes() = runFixture {
        handler.onBackStarted(event(0f))
        handler.onBackProgressed(event(0.5f))
        scope.advanceUntilIdle()
        val animatable = animatables.single()

        val gate = CompletableDeferred<Unit>()
        animatable.cancelGate = gate
        handler.onBackCancelled()
        scope.runCurrent()

        // 回弹动画未完成前保持双层结构，避免系统乱序补发确认返回时出现双重转场。
        assertEquals(2, handler.items.size)
        assertEquals(1, animatable.cancelCount)

        gate.complete(Unit)
        scope.advanceUntilIdle()
        assertEquals(1, handler.items.size)
    }

    @Test
    fun testBackDuringCancelRoutesThroughFinishPath() = runFixture {
        handler.onBackStarted(event(0f))
        handler.onBackProgressed(event(0.5f))
        scope.advanceUntilIdle()
        val animatable = animatables.single()

        val gate = CompletableDeferred<Unit>()
        animatable.cancelGate = gate
        handler.onBackCancelled()
        scope.runCurrent()

        // 模拟澎湃乱序派发：取消动画中途系统确认返回，应从当前位置连续收起而非重播转场。
        handler.onBack()
        scope.runCurrent()
        assertEquals(1, animatable.finishCount)

        gate.complete(Unit)
        scope.advanceUntilIdle()
        assertEquals(1, animatable.finishCount)
        assertEquals(1, animatable.cancelCount)
    }

    @Test
    fun testProgressDuringCancelReusesSameAnimatable() = runFixture {
        handler.onBackStarted(event(0f))
        handler.onBackProgressed(event(0.5f))
        scope.advanceUntilIdle()
        val first = animatables.single()

        val gate = CompletableDeferred<Unit>()
        first.cancelGate = gate
        handler.onBackCancelled()
        scope.runCurrent()

        handler.onBackProgressed(event(0.7f))
        scope.advanceUntilIdle()
        gate.complete(Unit)
        scope.advanceUntilIdle()

        // 取消动画进行中手势重新推进：复用同一动画器继续跟随，不重新选择、不提前重置。
        assertEquals(1, animatables.size)
        assertSame(first, animatables.single())
        assertEquals(listOf(0.5f, 0.7f), first.animated)
        assertEquals(2, handler.items.size)
    }
}
