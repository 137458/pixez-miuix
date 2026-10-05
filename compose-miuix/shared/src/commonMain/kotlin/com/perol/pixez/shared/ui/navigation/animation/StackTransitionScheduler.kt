package com.perol.pixez.shared.ui.navigation.animation

import com.arkivanov.decompose.Child
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.animation.Direction
import com.arkivanov.decompose.router.stack.ChildStack

/**
 * 转场中的单层渲染条目：child 与它在本次转场中的方向。
 * 渲染顺序 = 列表顺序（先组合者在底层）。
 */
@ExperimentalDecomposeApi
internal data class StackLayer<out C : Any, out T : Any>(
    val child: Child.Created<C, T>,
    val direction: Direction,
)

/**
 * [StackTransitionScheduler.resolve] 的决策结果。
 *
 * 视觉进度 p ∈ [0,1] 语义：1 = 转场起始（新层未进入/旧层未退出），0 = 完成（全部落稳态）。
 * 任何转场 p 都从高到低收敛；各层 factor 由组合层按 (direction, p) 换算（同相：|factor| 互补）。
 */
@ExperimentalDecomposeApi
internal sealed interface StackTransitionDecision<out C : Any, out T : Any> {

    /** 稳态开新转场：p 先落 1 再动画到 0。 */
    data class Start<C : Any, T : Any>(val layers: List<StackLayer<C, T>>) : StackTransitionDecision<C, T>

    /**
     * 转场中反向打断（push 转场中 pop 回转场底层）：两层方向互换，视觉连续。
     * p 先 snapTo(startProgress)（值跳变但帧换算同步换向，视觉无跳变）再动画到 0。
     */
    data class Flip<C : Any, T : Any>(val layers: List<StackLayer<C, T>>, val startProgress: Float) : StackTransitionDecision<C, T>

    /** 直接落稳态单层，无动画（同 key instance 重建等无视觉变化场景）。 */
    data class Settle<C : Any, T : Any>(val layers: List<StackLayer<C, T>>) : StackTransitionDecision<C, T>

    /** 当前转场继续，新栈排队，转场完成后由组合层再次 resolve。 */
    data class Defer<C : Any, T : Any>(val pendingStack: ChildStack<C, T>) : StackTransitionDecision<C, T>

    /** 栈相对当前状态无变化。 */
    data object Idle : StackTransitionDecision<Nothing, Nothing>
}

/**
 * 页面栈转场调度器（纯函数）：把「新到来的栈」对照当前渲染状态解析为转场决策。
 *
 * 与 Decompose `AbstractStackAnimation` 的差异（本类存在的目的）：
 * 1. 转场中反向操作不再排队重播，而是 [StackTransitionDecision.Flip] 连续翻转；
 * 2. 排队语义显式化为 [StackTransitionDecision.Defer]，由组合层在转场完成后重新 resolve；
 * 3. 排队长度恒为 1（新事件覆盖旧 pending），与用户最新意图一致。
 *
 * pop 判定与 Decompose `getAnimationItems` 一致：
 * 新栈更浅且新 active 在旧栈 backStack 中。
 */
@ExperimentalDecomposeApi
internal object StackTransitionScheduler {

    fun <C : Any, T : Any> resolve(
        stableStack: ChildStack<C, T>,
        targetStack: ChildStack<C, T>,
        isTransitioning: Boolean,
        progress: Float,
        newStack: ChildStack<C, T>,
    ): StackTransitionDecision<C, T> {
        if (isTransitioning) {
            return resolveDuringTransition(stableStack, targetStack, progress, newStack)
        }
        if (isSameStack(stableStack, newStack)) return StackTransitionDecision.Idle
        // 同 key、实例重建：无视觉变化，直接落稳态单层（对应 Decompose 的 isInitial 分支）。
        if (newStack.active.key == stableStack.active.key) {
            return StackTransitionDecision.Settle(listOf(StackLayer(newStack.active, Direction.ENTER_FRONT)))
        }
        return if (isPop(stableStack, newStack)) {
            StackTransitionDecision.Start(
                listOf(
                    StackLayer(newStack.active, Direction.ENTER_BACK),
                    StackLayer(stableStack.active, Direction.EXIT_FRONT),
                ),
            )
        } else {
            StackTransitionDecision.Start(
                listOf(
                    StackLayer(stableStack.active, Direction.EXIT_BACK),
                    StackLayer(newStack.active, Direction.ENTER_FRONT),
                ),
            )
        }
    }

    private fun <C : Any, T : Any> resolveDuringTransition(
        stableStack: ChildStack<C, T>,
        targetStack: ChildStack<C, T>,
        progress: Float,
        newStack: ChildStack<C, T>,
    ): StackTransitionDecision<C, T> {
        if (isSameStack(targetStack, newStack)) return StackTransitionDecision.Idle
        val isPushTransition = targetStack.active.key != stableStack.active.key
        val popsBackToTransitionBackLayer =
            newStack.active.key == stableStack.active.key && newStack.items.size < targetStack.items.size
        if (isPushTransition && popsBackToTransitionBackLayer) {
            // push 转场中返回转场底层：两层方向互换，p 换算 1-p 保持两层帧视觉连续。
            return StackTransitionDecision.Flip(
                layers = listOf(
                    StackLayer(stableStack.active, Direction.ENTER_BACK),
                    StackLayer(targetStack.active, Direction.EXIT_FRONT),
                ),
                startProgress = 1f - progress.coerceIn(0f, 1f),
            )
        }
        return StackTransitionDecision.Defer(newStack)
    }

    private fun <C : Any, T : Any> isSameStack(left: ChildStack<C, T>, right: ChildStack<C, T>): Boolean =
        left.active.key == right.active.key &&
            left.active.instance == right.active.instance &&
            left.items.size == right.items.size

    private fun <C : Any, T : Any> isPop(stableStack: ChildStack<C, T>, newStack: ChildStack<C, T>): Boolean =
        newStack.items.size < stableStack.items.size && stableStack.backStack.any { it.key == newStack.active.key }
}
