package com.perol.pixez.shared.ui.navigation.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.arkivanov.decompose.Child
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.animation.Direction
import com.arkivanov.decompose.extensions.compose.stack.animation.StackAnimation
import com.arkivanov.decompose.router.stack.ChildStack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** 简单可变容器：保存当前转场 Job（非组合状态，避免无关重组）。 */
private class TransitionJobRef {
    var value: Job? = null
}

/**
 * 自研可打断栈转场：替代 Decompose `AbstractStackAnimation` 的「排队 + 边界值重启」模型。
 *
 * 与 Decompose 的差异：
 * 1. **方向翻转连续**：push 转场中收到返回（pop 回转场底层），两层方向互换、
 *    视觉进度换算 1-p 后从当前位置连续反向，不排队、不从头重播（[StackTransitionScheduler.Flip]）。
 * 2. **其余转场中事件**显式排队（[StackTransitionScheduler.Defer]，队列长度 1、新事件覆盖），
 *    转场完成后立即消费，行为与 Decompose 排队一致。
 *
 * 驱动模型：全局单个 [Animatable] 持有视觉进度 p ∈ [0,1]（1=转场起始，0=稳态完成），
 * 各层 factor 由 (direction, p) 换算且两层严格同相（|factor| 互补）：
 * - ENTER_FRONT: factor = p
 * - EXIT_FRONT: factor = 1 - p
 * - ENTER_BACK: factor = -p
 * - EXIT_BACK: factor = p - 1
 * 转场弹簧 [MiuixTransitionSpec] 具备速度连续性，是打断转向平滑的物理基础。
 *
 * 帧渲染经 [frameFor] 委托给按 child 构造的 [MiuixTransitionFrame]（与 Decompose 驱动共用同一渲染实现，
 * 锚点解析、几何即时读取、registry 上报全部保持原语义）。
 *
 * @param frameFor 按 child 产出帧渲染闭包；捕获外部几何时必须以即时读取源（`() -> T`）形式捕获，
 * 严禁快照（渲染闭包可能被长期持有，快照会把冷启动首帧的 `Rect.Zero` 固化）。
 */
@ExperimentalDecomposeApi
internal class SeekableStackAnimation<C : Any, T : Any>(
    private val frameFor: (child: Child.Created<C, T>) -> MiuixTransitionFrame,
) : StackAnimation<C, T> {

    @Composable
    override fun invoke(stack: ChildStack<C, T>, modifier: Modifier, content: @Composable (Child.Created<C, T>) -> Unit) {
        var stableStack by remember { mutableStateOf(stack) }
        var targetStack by remember { mutableStateOf(stack) }
        var layers by remember { mutableStateOf(listOf(StackLayer(stack.active, Direction.ENTER_FRONT))) }
        var pendingStack by remember { mutableStateOf<ChildStack<C, T>?>(null) }
        var lastSeenStack by remember { mutableStateOf(stack) }
        val progress = remember { Animatable(0f) }
        val scope = rememberCoroutineScope()
        val transitionJob = remember { TransitionJobRef() }

        val childContent = remember(content) {
            movableContentOf<Child.Created<C, T>> { child ->
                key(child.key) {
                    content(child)
                }
            }
        }

        // 单次转场的执行与收尾：收敛稳态层后消费排队栈（存在则递归启动下一段转场）。
        suspend fun runTransitionAndDrain(newLayers: List<StackLayer<C, T>>, startProgress: Float) {
            layers = newLayers
            progress.snapTo(startProgress)
            progress.animateTo(0f, MiuixTransitionSpec)
            layers = listOf(StackLayer(targetStack.active, Direction.ENTER_FRONT))
            stableStack = targetStack
            val pending = pendingStack ?: return
            pendingStack = null
            lastSeenStack = pending
            when (val decision = StackTransitionScheduler.resolve(stableStack, stableStack, isTransitioning = false, progress = 0f, newStack = pending)) {
                is StackTransitionDecision.Start -> {
                    targetStack = pending
                    runTransitionAndDrain(decision.layers, startProgress = 1f)
                }
                is StackTransitionDecision.Settle -> {
                    targetStack = pending
                    stableStack = pending
                    layers = decision.layers
                }
                else -> Unit
            }
        }

        fun launchTransition(newLayers: List<StackLayer<C, T>>, startProgress: Float) {
            transitionJob.value?.cancel()
            transitionJob.value = scope.launch {
                runTransitionAndDrain(newLayers, startProgress)
            }
        }

        if (stack.active.key != lastSeenStack.active.key || stack.active.instance != lastSeenStack.active.instance) {
            lastSeenStack = stack
            when (val decision = StackTransitionScheduler.resolve(
                stableStack = stableStack,
                targetStack = targetStack,
                isTransitioning = layers.size > 1,
                progress = progress.value,
                newStack = stack,
            )) {
                is StackTransitionDecision.Start -> {
                    targetStack = stack
                    launchTransition(decision.layers, startProgress = 1f)
                }
                is StackTransitionDecision.Flip -> {
                    targetStack = stack
                    launchTransition(decision.layers, decision.startProgress)
                }
                is StackTransitionDecision.Settle -> {
                    transitionJob.value?.cancel()
                    transitionJob.value = null
                    targetStack = stack
                    stableStack = stack
                    layers = decision.layers
                }
                is StackTransitionDecision.Defer -> pendingStack = decision.pendingStack
                StackTransitionDecision.Idle -> Unit
            }
        }

        Box(modifier = modifier) {
            layers.forEach { layer ->
                key(layer.child.key) {
                    val factor = when (layer.direction) {
                        Direction.ENTER_FRONT -> progress.value
                        Direction.EXIT_FRONT -> 1f - progress.value
                        Direction.ENTER_BACK -> -progress.value
                        Direction.EXIT_BACK -> progress.value - 1f
                    }
                    frameFor(layer.child)(factor, layer.direction) { layerModifier ->
                        Box(modifier = layerModifier) {
                            childContent(layer.child)
                        }
                    }
                }
            }

            if (layers.size > 1) {
                TransitionInputConsumingOverlay(modifier = Modifier.matchParentSize())
            }
        }
    }
}

/**
 * 转场双层叠放期间的输入吞没层，防止转场中下层页面接收点击
 * （对应 Decompose `AbstractStackAnimation` 的 disableInputDuringAnimation 语义）。
 */
@Composable
private fun TransitionInputConsumingOverlay(modifier: Modifier) {
    Box(
        modifier = modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    event.changes.forEach { it.consume() }
                }
            }
        },
    )
}
