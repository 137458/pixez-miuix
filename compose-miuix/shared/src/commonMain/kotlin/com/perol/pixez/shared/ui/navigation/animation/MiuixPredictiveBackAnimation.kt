package com.perol.pixez.shared.ui.navigation.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.arkivanov.decompose.Ref
import com.arkivanov.decompose.extensions.compose.stack.animation.StackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.PredictiveBackAnimatable
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.backhandler.BackEvent
import com.arkivanov.essenty.backhandler.BackHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * 手势跟随弹簧：高刚度无回弹。逐帧进度事件下几乎实时贴合手指；
 * OEM 派发进度稀疏或跳变（如澎湃 / MIUI 系统仅回调少量进度点）时平滑追赶，避免画面跳变。
 */
private val PredictiveFollowSpec = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = 3000f,
    visibilityThreshold = 0.0005f,
)

/** 手势确认后的收尾动画：复用转场的 HyperOS 减速曲线。 */
private val PredictiveFinishSpec = tween<Float>(durationMillis = 240, easing = HyperOSDecelerateEasing)

/** 手势取消后的回弹动画。 */
private val PredictiveCancelSpec = tween<Float>(durationMillis = 200, easing = HyperOSDecelerateEasing)

/**
 * 进度平滑的 [PredictiveBackAnimatable]：替代 Decompose 内置实现中的 snapTo 语义。
 *
 * Decompose 默认实现按系统事件原样跳变进度，在进度派发被系统阉割或稀疏的设备上
 * （典型如澎湃 / HyperOS：手势期间只有最终确认事件或个别进度点）页面会瞬间跳位；
 * 此实现以 [Animatable] 驱动进度，事件之间按弹簧连续补间。
 *
 * @param initialBackEvent 手势起始事件。
 * @param getExitModifier 由进度构造离场页（当前栈顶）修饰符。
 * @param getEnterModifier 由进度构造露出的下层页修饰符。
 */
@ExperimentalDecomposeApi
internal class MiuixSmoothedPredictiveBackAnimatable(
    initialBackEvent: BackEvent,
    private val getExitModifier: (progress: Float, edge: BackEvent.SwipeEdge) -> Modifier,
    private val getEnterModifier: (progress: Float, edge: BackEvent.SwipeEdge) -> Modifier,
) : PredictiveBackAnimatable {

    private val progressAnimatable = Animatable(initialValue = initialBackEvent.progress)
    private var swipeEdge by mutableStateOf(initialBackEvent.swipeEdge)

    override val exitModifier: Modifier
        get() = getExitModifier(progressAnimatable.value, swipeEdge)

    override val enterModifier: Modifier
        get() = getEnterModifier(progressAnimatable.value, swipeEdge)

    override suspend fun animate(event: BackEvent) {
        swipeEdge = event.swipeEdge
        progressAnimatable.animateTo(targetValue = event.progress, animationSpec = PredictiveFollowSpec)
    }

    override suspend fun finish() {
        progressAnimatable.animateTo(targetValue = 1f, animationSpec = PredictiveFinishSpec)
    }

    override suspend fun cancel() {
        progressAnimatable.animateTo(targetValue = 0f, animationSpec = PredictiveCancelSpec)
    }
}

/**
 * 构造针对 OEM 进度派发缺陷加固的预测性返回动画（移植自 Decompose `predictiveBackAnimation`）。
 *
 * 相比 Decompose 内置实现的两处行为修正：
 * 1. **取消动画期间收到确认返回时连续收起**：内置实现在 `onBackCancelled` 时立即把双层页面
 *    重置回单层，若系统随后才补发确认返回（澎湃 / MIUI 常见的乱序派发），页面会先回弹到全屏
 *    再整段重播收起动画，出现突兀的双重转场；此实现等取消动画完成后再重置，
 *    期间收到的确认返回从当前位置直接连续收起。
 * 2. **进度补间**：配合 [MiuixSmoothedPredictiveBackAnimatable]，进度事件按弹簧平滑跟随。
 *
 * @param backHandler 预测性返回事件源。
 * @param fallbackAnimation 常规出入栈转场（手势无进度事件直接确认时使用）。
 * @param selector 手势开始时选择 [PredictiveBackAnimatable]。
 * @param onBack 转场完成后回调（执行真正出栈）。
 */
@ExperimentalDecomposeApi
fun <C : Any, T : Any> miuixPredictiveBackAnimation(
    backHandler: BackHandler,
    fallbackAnimation: StackAnimation<C, T>,
    selector: (initialBackEvent: BackEvent, exitChild: Child.Created<C, T>, enterChild: Child.Created<C, T>) -> PredictiveBackAnimatable,
    onBack: () -> Unit,
): StackAnimation<C, T> =
    MiuixPredictiveBackAnimation(
        backHandler = backHandler,
        animation = fallbackAnimation,
        selector = selector,
        onBack = onBack,
    )

@OptIn(ExperimentalDecomposeApi::class, com.arkivanov.decompose.InternalDecomposeApi::class)
private class MiuixPredictiveBackAnimation<C : Any, T : Any>(
    private val backHandler: BackHandler,
    private val animation: StackAnimation<C, T>,
    private val selector: (BackEvent, exitChild: Child.Created<C, T>, enterChild: Child.Created<C, T>) -> PredictiveBackAnimatable,
    private val onBack: () -> Unit,
) : StackAnimation<C, T> {

    @Composable
    override fun invoke(stack: ChildStack<C, T>, modifier: Modifier, content: @Composable (child: Child.Created<C, T>) -> Unit) {
        val activeKeys = remember { HashSet<Any>() }
        val handler = rememberHandler(stack = stack, isGestureEnabled = { activeKeys.size == 1 })
        val anim = animation

        val childContent =
            remember(content) {
                movableContentOf<Child.Created<C, T>> { child ->
                    key(child.key) {
                        content(child)

                        DisposableEffect(Unit) {
                            activeKeys += child.key
                            onDispose { activeKeys -= child.key }
                        }
                    }
                }
            }

        Box(modifier = modifier) {
            handler.items.forEach { item ->
                key(item.key) {
                    anim(
                        stack = item.stack,
                        modifier = Modifier.fillMaxSize().then(item.modifier()),
                        content = childContent,
                    )
                }
            }

            if (handler.items.size > 1) {
                InputConsumingOverlay(modifier = Modifier.matchParentSize())
            }
        }

        if (stack.backStack.isNotEmpty()) {
            DisposableEffect(handler) {
                backHandler.register(handler)
                onDispose { backHandler.unregister(handler) }
            }
        }
    }

    @Composable
    private fun rememberHandler(stack: ChildStack<C, T>, isGestureEnabled: () -> Boolean): MiuixPredictiveBackGestureHandler<C, T> {
        val scope = key(stack) { rememberCoroutineScope() }

        return rememberWithLatest(stack) { previousHandler ->
            MiuixPredictiveBackGestureHandler(
                stack = stack,
                scope = scope,
                isGestureEnabled = isGestureEnabled,
                key = previousHandler?.items?.maxOf { it.key } ?: 0,
                selector = selector,
                onBack = onBack,
            )
        }
    }

    @Composable
    private fun <T> rememberWithLatest(key: Any, supplier: (T?) -> T): T {
        val ref = remember { Ref<T?>(null) }
        val v = remember(key) { supplier(ref.value) }
        ref.value = v

        return v
    }
}

/**
 * 预测性返回双层叠放期间的单层条目：页面栈快照、组合 key 与当帧修饰符。
 * 包装类与 [MiuixPredictiveBackGestureHandler] 共用。
 */
internal data class MiuixPredictiveBackGestureItem<out C : Any, out T : Any>(
    val stack: ChildStack<C, T>,
    val key: Int,
    val modifier: () -> Modifier = { Modifier },
)

/**
 * 预测性返回手势状态机（移植自 Decompose `PredictiveBackAnimation.Handler`，行为差异见
 * [miuixPredictiveBackAnimation] 文档）。internal 以便单元测试覆盖乱序事件序列。
 */
@OptIn(ExperimentalDecomposeApi::class)
internal class MiuixPredictiveBackGestureHandler<C : Any, T : Any>(
    private val stack: ChildStack<C, T>,
    private val scope: CoroutineScope,
    private val isGestureEnabled: () -> Boolean,
    private val key: Int,
    private val selector: (BackEvent, exitChild: Child.Created<C, T>, enterChild: Child.Created<C, T>) -> PredictiveBackAnimatable,
    private val onBack: () -> Unit,
) : BackCallback() {

    var items: List<MiuixPredictiveBackGestureItem<C, T>> by mutableStateOf(listOf(MiuixPredictiveBackGestureItem(stack = stack, key = key)))
        private set

    private var animatable: PredictiveBackAnimatable? = null
    private var initialBackEvent: BackEvent? = null
    private var cancelAnimationJob: Job? = null

    override fun onBackStarted(backEvent: BackEvent) {
        initialBackEvent = backEvent
    }

    override fun onBackProgressed(backEvent: BackEvent) {
        if (animatable == null) {
            if (!isGestureEnabled()) return
            // 部分系统（澎湃 / MIUI）可能跳过 Started 事件只派发进度：以首个进度事件兜底作为起始事件。
            val initial = initialBackEvent ?: backEvent
            initialBackEvent = null
            val created = selector(initial, stack.active, stack.backStack.last())
            animatable = created
            items =
                listOf(
                    MiuixPredictiveBackGestureItem(stack = stack.dropLast(), key = key + 1, modifier = created::enterModifier),
                    MiuixPredictiveBackGestureItem(stack = stack, key = key, modifier = created::exitModifier),
                )
            scope.launch { created.animate(backEvent) }
        } else {
            // 取消动画进行中手势被重新推进：终止回弹，复用同一动画器继续贴合手指。
            cancelAnimationJob?.cancel()
            cancelAnimationJob = null
            scope.launch { animatable?.animate(backEvent) }
        }
    }

    private fun <C : Any, T : Any> ChildStack<C, T>.dropLast(): ChildStack<C, T> =
        ChildStack(active = backStack.last(), backStack = backStack.dropLast(1))

    override fun onBack() {
        val current = animatable
        if (current == null) {
            onBack.invoke()
        } else {
            cancelAnimationJob?.cancel()
            cancelAnimationJob = null
            scope.launch {
                current.finish()
                animatable = null
                onBack.invoke()
            }
        }
    }

    override fun onBackCancelled() {
        val current = animatable ?: return
        cancelAnimationJob?.cancel()
        // 等回弹动画完成后再重置回单层：期间若系统乱序补发确认返回（onBack），
        // 动画器仍存在，可从当前位置连续收起，避免「回弹到全屏再整段重播收起」的双重转场。
        cancelAnimationJob = scope.launch {
            current.cancel()
            animatable = null
            items = listOf(MiuixPredictiveBackGestureItem(stack = stack, key = key))
        }
    }
}

/**
 * 预测性返回双层叠放期间的输入吞没层（移植自 Decompose internal `InputConsumingOverlay`），
 * 防止手势动画期间下层页面接收点击。
 */
@Composable
private fun InputConsumingOverlay(modifier: Modifier) {
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
