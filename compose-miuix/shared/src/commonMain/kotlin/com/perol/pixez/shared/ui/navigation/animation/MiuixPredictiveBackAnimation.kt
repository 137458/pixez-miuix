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
import com.perol.pixez.shared.platform.rememberDragBackGestureEnabled
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
 * 支持跟手直驱（snap）的预测性返回动画器：应用内拖拽路径用 [snap] 1:1 贴合手指，
 * 系统手势路径仍走 [animate] 弹簧跟随。
 */
@ExperimentalDecomposeApi
internal interface MiuixSnapBackAnimatable : PredictiveBackAnimatable {
    /** 手势跟手期直驱：进度立即落位，无补间。 */
    suspend fun snap(event: BackEvent)
}

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
) : MiuixSnapBackAnimatable {

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

    override suspend fun snap(event: BackEvent) {
        swipeEdge = event.swipeEdge
        progressAnimatable.snapTo(event.progress)
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
 * 手势激活条件与上游严格一致：仅当先收到 [BackCallback.onBackStarted] 后的进度事件才激活
 * 双层结构。系统随后乱序补发的进度 / 起始事件（澎湃 / HyperOS 在确认返回后仍会补发）一律忽略；
 * 若放宽为「任意进度事件都激活」，杂散事件会在常规转场或稳态下凭空启动假手势，
 * 双层渲染与转场争抢 movableContent，导致重影、返回样式错乱与列表绘制空白。
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
 * @param dragBackEligible 应用内拖拽返回的页面判定：非 null 且平台开关开启、当前稳态单层的
 *   active 页面命中时，挂载左缘拖拽手势（事件经 [MiuixPredictiveBackGestureHandler] 与系统流互斥）。
 */
@ExperimentalDecomposeApi
fun <C : Any, T : Any> miuixPredictiveBackAnimation(
    backHandler: BackHandler,
    fallbackAnimation: StackAnimation<C, T>,
    selector: (initialBackEvent: BackEvent, exitChild: Child.Created<C, T>, enterChild: Child.Created<C, T>) -> PredictiveBackAnimatable,
    onBack: () -> Unit,
    dragBackEligible: ((T) -> Boolean)? = null,
): StackAnimation<C, T> =
    MiuixPredictiveBackAnimation(
        backHandler = backHandler,
        animation = fallbackAnimation,
        selector = selector,
        onBack = onBack,
        dragBackEligible = dragBackEligible,
    )

@OptIn(ExperimentalDecomposeApi::class, com.arkivanov.decompose.InternalDecomposeApi::class)
private class MiuixPredictiveBackAnimation<C : Any, T : Any>(
    private val backHandler: BackHandler,
    private val animation: StackAnimation<C, T>,
    private val selector: (BackEvent, exitChild: Child.Created<C, T>, enterChild: Child.Created<C, T>) -> PredictiveBackAnimatable,
    private val onBack: () -> Unit,
    private val dragBackEligible: ((T) -> Boolean)? = null,
) : StackAnimation<C, T> {

    @Composable
    override fun invoke(stack: ChildStack<C, T>, modifier: Modifier, content: @Composable (child: Child.Created<C, T>) -> Unit) {
        val activeKeys = remember { HashSet<Any>() }
        val handler = rememberHandler(stack = stack, isGestureEnabled = { activeKeys.size == 1 })
        val anim = animation
        NavTransitionLog.d("invoke") { "stack=[${stack.items.joinToString { it.configuration.toString() }}] items=${handler.items.size} animHash=${anim.hashCode()} handlerHash=${handler.hashCode()}" }

        val childContent =
            remember(content) {
                movableContentOf<Child.Created<C, T>> { child ->
                    key(child.key) {
                        content(child)

                        DisposableEffect(Unit) {
                            NavTransitionLog.d("invoke") { "activeKeys + ${child.key} -> ${activeKeys.size + 1}" }
                            activeKeys += child.key
                            onDispose {
                                NavTransitionLog.d("invoke") { "activeKeys - ${child.key} -> ${activeKeys.size - 1}" }
                                activeKeys -= child.key
                            }
                        }
                    }
                }
            }

        // 应用内拖拽返回：挂在最外层 Box（手势会话中 handler.items 变双层，
        // 若挂在内层会被组合移除而中断手势事件流）。激活判定只在按下阶段进行。
        val dragBackActive = dragBackEligible != null &&
            rememberDragBackGestureEnabled() &&
            handler.items.size == 1 &&
            dragBackEligible.invoke(handler.items.single().stack.active.instance)

        Box(
            modifier = if (dragBackActive) {
                modifier.then(
                    Modifier.miuixDragBackGesture(
                        isEligible = { true },
                        onStart = handler::onDragStarted,
                        onProgress = handler::onDragProgressed,
                        onCommit = handler::onDragCommit,
                        onCancel = handler::onDragCancel,
                    ),
                )
            } else {
                modifier
            },
        ) {
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

    /**
     * 本次返回是否已被确认（[onBack] 已触发）。
     *
     * 部分系统（澎湃 / MIUI / HyperOS）在返回确认后仍会乱序补发 [onBackStarted] / [onBackProgressed]，
     * 若不拦截，状态机会把补发进度误判为一次新手势，在出栈转场进行中强行叠加预测性返回双层渲染，
     * 与回退转场争抢 movableContent，导致页面重影与返回后列表绘制空白。
     */
    private var isBackConfirmed = false

    /** 应用内拖拽会话进行中：与系统手势流互斥（一方激活时另一方的事件被忽略）。 */
    private var isDragSession = false

    override fun onBackStarted(backEvent: BackEvent) {
        NavTransitionLog.d("gesture") { "onBackStarted progress=${backEvent.progress} confirmed=$isBackConfirmed animatable=${animatable != null}" }
        if (isBackConfirmed) return
        if (isDragSession) return
        initialBackEvent = backEvent
    }

    /** 应用内拖拽起始（[Modifier.miuixDragBackGesture] 边缘起手时调用）。 */
    fun onDragStarted(backEvent: BackEvent) {
        NavTransitionLog.d("gesture") { "onDragStarted progress=${backEvent.progress} confirmed=$isBackConfirmed animatable=${animatable != null}" }
        if (isBackConfirmed) return
        if (isDragSession) return
        // 系统手势流已持有起始事件（Started 已到、尚未激活）时拖拽不得接管，避免双事件源争抢。
        if (initialBackEvent != null) return
        initialBackEvent = backEvent
        isDragSession = true
    }

    /** 应用内拖拽推进（跟手期 snap 直驱）。 */
    fun onDragProgressed(backEvent: BackEvent) {
        NavTransitionLog.d("gesture") { "onDragProgressed progress=${backEvent.progress} confirmed=$isBackConfirmed drag=$isDragSession animatable=${animatable != null}" }
        if (isBackConfirmed) return
        if (!isDragSession) return
        progressed(backEvent, snapToFollow = true)
    }

    /** 应用内拖拽确认（位移/速度越阈值松手）：与 [onBack] 同路径收尾并出栈。 */
    fun onDragCommit() {
        onBack()
    }

    /** 应用内拖拽取消（未达阈值松手）：与 [onBackCancelled] 同路径回弹。 */
    fun onDragCancel() {
        onBackCancelled()
    }

    override fun onBackProgressed(backEvent: BackEvent) {
        NavTransitionLog.d("gesture") { "onBackProgressed progress=${backEvent.progress} confirmed=$isBackConfirmed animatable=${animatable != null} gestureEnabled=${isGestureEnabled()}" }
        if (isBackConfirmed) return
        if (isDragSession) return
        progressed(backEvent, snapToFollow = false)
    }

    /**
     * 手势进度共用路径：未激活时以 [initialBackEvent] 激活双层结构，已激活时继续贴合。
     * 系统手势流用弹簧跟随（OEM 进度稀疏时平滑追赶），拖拽会话用 snap 1:1 直驱。
     */
    private fun progressed(backEvent: BackEvent, snapToFollow: Boolean) {
        val initial = initialBackEvent
        if (initial != null && animatable == null) {
            if (!isGestureEnabled()) {
                // 激活失败（转场中/非稳态）：丢弃会话，防止门闩卡死后续手势。
                initialBackEvent = null
                isDragSession = false
                return
            }
            // 与上游 Decompose 一致：必须先收到 Started（或拖拽 Start）才激活双层结构。
            // 杂散/乱序进度事件（澎湃 / HyperOS 确认返回后的补发等）不得凭空启动假手势，
            // 否则双层渲染会与常规转场争抢 movableContent，导致重影与列表绘制空白。
            initialBackEvent = null
            val created = selector(initial, stack.active, stack.backStack.last())
            animatable = created
            items =
                listOf(
                    MiuixPredictiveBackGestureItem(stack = stack.dropLast(), key = key + 1, modifier = created::enterModifier),
                    MiuixPredictiveBackGestureItem(stack = stack, key = key, modifier = created::exitModifier),
                )
            NavTransitionLog.d("gesture") { "ENGAGE snap=$snapToFollow handlerKey=$key exit=${stack.active.configuration} enter=${stack.backStack.last().configuration}" }
            scope.launch {
                val snapped = created as? MiuixSnapBackAnimatable
                if (snapToFollow && snapped != null) snapped.snap(backEvent) else created.animate(backEvent)
            }
        } else {
            // 取消动画进行中手势被重新推进：终止回弹，复用同一动画器继续贴合手指。
            cancelAnimationJob?.cancel()
            cancelAnimationJob = null
            val current = animatable
            scope.launch {
                val snapped = current as? MiuixSnapBackAnimatable
                if (snapToFollow && snapped != null) snapped.snap(backEvent) else current?.animate(backEvent)
            }
        }
    }

    private fun <C : Any, T : Any> ChildStack<C, T>.dropLast(): ChildStack<C, T> =
        ChildStack(active = backStack.last(), backStack = backStack.dropLast(1))

    override fun onBack() {
        NavTransitionLog.d("gesture") { "onBack confirmed (was=$isBackConfirmed) animatable=${animatable != null} drag=$isDragSession" }
        isBackConfirmed = true
        isDragSession = false
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
        NavTransitionLog.d("gesture") { "onBackCancelled confirmed=$isBackConfirmed animatable=${animatable != null} drag=$isDragSession" }
        if (isBackConfirmed) return
        // 手势生命周期已终止：丢弃尚未消费的起始事件，防止取消后杂散进度凭空激活假手势。
        initialBackEvent = null
        isDragSession = false
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
