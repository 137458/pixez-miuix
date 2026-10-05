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

    private class FakePredictiveBackAnimatable : MiuixSnapBackAnimatable {
        val animated = mutableListOf<Float>()
        val snapLog = mutableListOf<Float>()
        var finishCount = 0
        var cancelCount = 0
        var cancelGate: CompletableDeferred<Unit>? = null

        override val exitModifier: Modifier get() = Modifier
        override val enterModifier: Modifier get() = Modifier

        override suspend fun animate(event: BackEvent) {
            animated += event.progress
        }

        override suspend fun snap(event: BackEvent) {
            snapLog += event.progress
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
        val backInvoked: MutableList<Int>,
    )

    private fun runFixture(gestureEnabled: Boolean = true, block: suspend Fixture.() -> Unit) = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val animatables = mutableListOf<FakePredictiveBackAnimatable>()
        val backInvoked = mutableListOf<Int>()
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
            onBack = { backInvoked += 1 },
        )
        Fixture(scope, stack, handler, animatables, backInvoked).block()
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
    fun testProgressWithoutStartedIsIgnored() = runFixture {
        // 对齐上游 Decompose：必须先收到 onBackStarted 才激活预测性返回双层结构。
        // 乱序/杂散进度事件（澎湃 / HyperOS 转场期间或稳态下的补发）不得凭空启动假手势，
        // 否则双层渲染会与常规转场争抢页面层级，导致重影、侧滑样式错乱与列表绘制损坏。
        handler.onBackProgressed(event(0.3f))
        scope.advanceUntilIdle()
        assertTrue(animatables.isEmpty(), "selector must not run without a preceding onBackStarted")
        assertEquals(1, handler.items.size)
    }

    @Test
    fun testProgressAfterCancelledGestureDoesNotReactivateWithoutNewStarted() = runFixture {
        handler.onBackStarted(event(0f))
        handler.onBackProgressed(event(0.5f))
        scope.advanceUntilIdle()
        assertEquals(1, animatables.size)

        handler.onBackCancelled()
        scope.advanceUntilIdle()
        assertEquals(1, handler.items.size)

        // 取消后手势生命周期已终止：后续杂散进度事件不得重新激活假手势。
        handler.onBackProgressed(event(0.6f))
        scope.advanceUntilIdle()

        assertEquals(1, animatables.size, "cancelled gesture must not reactivate from a stray progress event")
        assertEquals(1, handler.items.size)
    }

    @Test
    fun testNewStartedAfterCancelledGestureEngagesFreshly() = runFixture {
        handler.onBackStarted(event(0f))
        handler.onBackProgressed(event(0.5f))
        scope.advanceUntilIdle()
        handler.onBackCancelled()
        scope.advanceUntilIdle()

        // 用户重新发起返回手势：系统重新派发 Started，随后进度正常激活新手势。
        handler.onBackStarted(event(0f))
        handler.onBackProgressed(event(0.4f))
        scope.advanceUntilIdle()

        assertEquals(2, animatables.size)
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

    @Test
    fun testLateProgressAfterConfirmedBackIsIgnored() = runFixture {
        // 模拟澎湃 / HyperOS 乱序派发：确认返回（onBack）已触发 pop，系统随后才补发进度事件。
        // 状态机必须忽略补发进度，否则会在出栈转场进行中误启动新手势，叠加双层渲染导致重影与列表绘制损坏。
        handler.onBack()
        scope.advanceUntilIdle()

        handler.onBackStarted(event(0f))
        handler.onBackProgressed(event(0.6f))
        handler.onBackProgressed(event(0.9f))
        scope.advanceUntilIdle()

        assertTrue(animatables.isEmpty(), "selector must not run after back is confirmed")
        assertEquals(1, handler.items.size, "items must stay single-layer after back is confirmed")
    }

    @Test
    fun testLateEventsAfterConfirmedBackWithEngagedGestureDoNotReactivate() = runFixture {
        handler.onBackStarted(event(0f))
        handler.onBackProgressed(event(0.4f))
        scope.advanceUntilIdle()
        val engaged = animatables.single()

        handler.onBack()
        scope.advanceUntilIdle()
        val finishCountAfterBack = engaged.finishCount
        assertEquals(1, finishCountAfterBack)

        // 确认后补发的进度/起始事件不得复活动画器或重建双层结构。
        handler.onBackStarted(event(0f))
        handler.onBackProgressed(event(0.8f))
        handler.onBackCancelled()
        scope.advanceUntilIdle()

        assertEquals(1, animatables.size)
        assertSame(engaged, animatables.single())
        assertEquals(listOf(0.4f), engaged.animated)
        assertEquals(finishCountAfterBack, engaged.finishCount)
        assertEquals(0, engaged.cancelCount)
    }

    // ---- 应用内拖拽返回（批次 2）：与系统手势流互斥，跟手期 snap 直驱 ----

    @Test
    fun testDragStartedAndProgressedEngageItemsWithSnapFollow() = runFixture {
        handler.onDragStarted(event(0f))
        handler.onDragProgressed(event(0.4f))
        scope.advanceUntilIdle()

        assertEquals(1, animatables.size)
        assertEquals(listOf(0.4f), animatables.single().snapLog, "drag follow must drive progress via snap (1:1)")
        assertEquals(2, handler.items.size)
    }

    @Test
    fun testDragIsIgnoredWhileSystemGestureIsPending() = runFixture {
        // 系统手势流已持有起始事件（onBackStarted 已到、尚未激活）时，拖拽不得接管。
        handler.onBackStarted(event(0f))
        handler.onDragStarted(event(0f))
        handler.onDragProgressed(event(0.5f))
        scope.advanceUntilIdle()

        assertTrue(animatables.isEmpty(), "drag must not take over while a system gesture start event is pending")
        assertEquals(1, handler.items.size)
    }

    @Test
    fun testSystemEventsAreIgnoredDuringDragSession() = runFixture {
        handler.onDragStarted(event(0f))
        handler.onDragProgressed(event(0.4f))
        scope.advanceUntilIdle()
        val engaged = animatables.single()

        // 模拟澎湃乱序补发：拖拽会话中系统手势事件不得重复激活或干扰跟手动画。
        handler.onBackStarted(event(0f))
        handler.onBackProgressed(event(0.9f))
        scope.advanceUntilIdle()

        assertEquals(1, animatables.size)
        assertSame(engaged, animatables.single())
        assertEquals(listOf(0.4f), engaged.snapLog)
        assertEquals(2, handler.items.size)
    }

    @Test
    fun testDragCommitFinishesAndPops() = runFixture {
        handler.onDragStarted(event(0f))
        handler.onDragProgressed(event(0.6f))
        scope.advanceUntilIdle()
        handler.onDragCommit()
        scope.advanceUntilIdle()

        assertEquals(1, animatables.single().finishCount)
        assertEquals(listOf(1), backInvoked)

        // 确认后系统补发事件被门闩拦截，不得复活动画器。
        handler.onBackStarted(event(0f))
        handler.onBackProgressed(event(0.9f))
        scope.advanceUntilIdle()
        assertEquals(1, animatables.size)
    }

    @Test
    fun testDragCancelResetsToSingleLayerAfterCancelAnimation() = runFixture {
        handler.onDragStarted(event(0f))
        handler.onDragProgressed(event(0.5f))
        scope.advanceUntilIdle()
        handler.onDragCancel()
        scope.advanceUntilIdle()

        assertEquals(1, animatables.single().cancelCount)
        assertEquals(1, handler.items.size)
    }

    @Test
    fun testDragProgressDuringCancelResumesSnapFollow() = runFixture {
        handler.onDragStarted(event(0f))
        handler.onDragProgressed(event(0.5f))
        scope.advanceUntilIdle()
        val first = animatables.single()

        val gate = CompletableDeferred<Unit>()
        first.cancelGate = gate
        handler.onDragCancel()
        scope.runCurrent()

        // 回弹动画进行中用户重新按压边缘拖拽（新拖拽会话）：
        // 复用仍在渲染双层的同一动画器继续 snap 跟手，不重新选择、不提前重置。
        handler.onDragStarted(event(0f))
        handler.onDragProgressed(event(0.8f))
        scope.advanceUntilIdle()
        gate.complete(Unit)
        scope.advanceUntilIdle()

        assertEquals(1, animatables.size)
        assertSame(first, animatables.single())
        assertEquals(listOf(0.5f, 0.8f), first.snapLog)
        assertEquals(2, handler.items.size)
    }

    @Test
    fun testDragGestureDisabledBlocksEngagement() = runFixture(gestureEnabled = false) {
        handler.onDragStarted(event(0f))
        handler.onDragProgressed(event(0.4f))
        scope.advanceUntilIdle()

        assertTrue(animatables.isEmpty())
        assertEquals(1, handler.items.size)
    }
}
