package com.perol.pixez.shared.ui.navigation.animation

import com.arkivanov.decompose.Child
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.animation.Direction
import com.arkivanov.decompose.router.stack.ChildStack
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalDecomposeApi::class)
class StackTransitionSchedulerTest {

    private fun stackOf(vararg configs: String): ChildStack<String, String> {
        val children = configs.map { Child.Created(configuration = it, instance = "$it-instance", key = it) }
        return ChildStack(active = children.last(), backStack = children.dropLast(1))
    }

    private fun activeKey(layer: StackLayer<String, String>): String = layer.child.configuration

    @Test
    fun testStableStackWithoutChangeIsIdle() {
        val decision = StackTransitionScheduler.resolve(
            stableStack = stackOf("main"),
            targetStack = stackOf("main"),
            isTransitioning = false,
            progress = 0f,
            newStack = stackOf("main"),
        )
        assertEquals(StackTransitionDecision.Idle, decision)
    }

    @Test
    fun testStablePushStartsTransitionWithBackLayerFirst() {
        val decision = StackTransitionScheduler.resolve(
            stableStack = stackOf("main"),
            targetStack = stackOf("main"),
            isTransitioning = false,
            progress = 0f,
            newStack = stackOf("main", "detail"),
        )
        val start = decision as StackTransitionDecision.Start<String, String>
        assertEquals(2, start.layers.size)
        assertEquals("main", activeKey(start.layers[0]))
        assertEquals(Direction.EXIT_BACK, start.layers[0].direction)
        assertEquals("detail", activeKey(start.layers[1]))
        assertEquals(Direction.ENTER_FRONT, start.layers[1].direction)
    }

    @Test
    fun testStablePopStartsTransitionWithRevealedLayerFirst() {
        val decision = StackTransitionScheduler.resolve(
            stableStack = stackOf("main", "detail"),
            targetStack = stackOf("main", "detail"),
            isTransitioning = false,
            progress = 0f,
            newStack = stackOf("main"),
        )
        val start = decision as StackTransitionDecision.Start<String, String>
        assertEquals(2, start.layers.size)
        assertEquals("main", activeKey(start.layers[0]))
        assertEquals(Direction.ENTER_BACK, start.layers[0].direction)
        assertEquals("detail", activeKey(start.layers[1]))
        assertEquals(Direction.EXIT_FRONT, start.layers[1].direction)
    }

    @Test
    fun testStableNavigateResetUsesPushSemantics() {
        val decision = StackTransitionScheduler.resolve(
            stableStack = stackOf("main", "detail"),
            targetStack = stackOf("main", "detail"),
            isTransitioning = false,
            progress = 0f,
            newStack = stackOf("main", "other"),
        )
        val start = decision as StackTransitionDecision.Start<String, String>
        assertEquals("detail", activeKey(start.layers[0]))
        assertEquals(Direction.EXIT_BACK, start.layers[0].direction)
        assertEquals("other", activeKey(start.layers[1]))
        assertEquals(Direction.ENTER_FRONT, start.layers[1].direction)
    }

    @Test
    fun testSameKeyWithNewInstanceSettlesWithoutAnimation() {
        val oldChild = Child.Created(configuration = "detail", instance = "detail-old", key = "detail")
        val newChild = Child.Created(configuration = "detail", instance = "detail-new", key = "detail")
        val decision = StackTransitionScheduler.resolve(
            stableStack = ChildStack(active = oldChild, backStack = emptyList()),
            targetStack = ChildStack(active = oldChild, backStack = emptyList()),
            isTransitioning = false,
            progress = 0f,
            newStack = ChildStack(active = newChild, backStack = emptyList()),
        )
        val settle = decision as StackTransitionDecision.Settle<String, String>
        assertEquals(1, settle.layers.size)
        assertEquals(Direction.ENTER_FRONT, settle.layers.single().direction)
    }

    @Test
    fun testPopDuringPushTransitionFlipsLayersContinuously() {
        val decision = StackTransitionScheduler.resolve(
            stableStack = stackOf("main"),
            targetStack = stackOf("main", "detail"),
            isTransitioning = true,
            progress = 0.4f,
            newStack = stackOf("main"),
        )
        val flip = decision as StackTransitionDecision.Flip<String, String>
        assertEquals(0.6f, flip.startProgress)
        assertEquals("main", activeKey(flip.layers[0]))
        assertEquals(Direction.ENTER_BACK, flip.layers[0].direction)
        assertEquals("detail", activeKey(flip.layers[1]))
        assertEquals(Direction.EXIT_FRONT, flip.layers[1].direction)
    }

    @Test
    fun testFlipStartProgressCoercesIntoUnitInterval() {
        val decision = StackTransitionScheduler.resolve(
            stableStack = stackOf("main"),
            targetStack = stackOf("main", "detail"),
            isTransitioning = true,
            progress = 0.9f,
            newStack = stackOf("main"),
        )
        val flip = decision as StackTransitionDecision.Flip<String, String>
        assertEquals(0.1f, flip.startProgress, 1e-6f)
    }

    @Test
    fun testRepeatedNotificationOfTargetStackDuringTransitionIsIdle() {
        val decision = StackTransitionScheduler.resolve(
            stableStack = stackOf("main"),
            targetStack = stackOf("main", "detail"),
            isTransitioning = true,
            progress = 0.4f,
            newStack = stackOf("main", "detail"),
        )
        assertEquals(StackTransitionDecision.Idle, decision)
    }

    @Test
    fun testPushDuringPushTransitionDefers() {
        val decision = StackTransitionScheduler.resolve(
            stableStack = stackOf("main"),
            targetStack = stackOf("main", "detail"),
            isTransitioning = true,
            progress = 0.4f,
            newStack = stackOf("main", "detail", "other"),
        )
        val defer = decision as StackTransitionDecision.Defer<String, String>
        assertEquals(listOf("main", "detail", "other"), defer.pendingStack.items.map { it.configuration })
    }

    @Test
    fun testPushDuringPopTransitionDefers() {
        val decision = StackTransitionScheduler.resolve(
            stableStack = stackOf("root", "main"),
            targetStack = stackOf("root", "main", "detail"),
            isTransitioning = true,
            progress = 0.4f,
            newStack = stackOf("root", "main", "detail", "other"),
        )
        assertEquals(StackTransitionDecision.Defer<String, String>(stackOf("root", "main", "detail", "other")), decision)
    }

    @Test
    fun testPopDuringPopTransitionDefers() {
        val decision = StackTransitionScheduler.resolve(
            stableStack = stackOf("root", "main"),
            targetStack = stackOf("root", "main", "detail"),
            isTransitioning = true,
            progress = 0.4f,
            newStack = stackOf("root"),
        )
        assertEquals(StackTransitionDecision.Defer<String, String>(stackOf("root")), decision)
    }

    @Test
    fun testResetToForeignStackDuringTransitionDefers() {
        val decision = StackTransitionScheduler.resolve(
            stableStack = stackOf("main"),
            targetStack = stackOf("main", "detail"),
            isTransitioning = true,
            progress = 0.4f,
            newStack = stackOf("settings"),
        )
        assertEquals(StackTransitionDecision.Defer<String, String>(stackOf("settings")), decision)
    }
}
