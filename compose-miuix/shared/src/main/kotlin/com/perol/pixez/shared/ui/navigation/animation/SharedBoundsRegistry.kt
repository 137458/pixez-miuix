package com.perol.pixez.shared.ui.navigation.animation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/** 转场锚点类型：区分不同业务实体，防止共用同一数字 ID 时互相覆盖。 */
enum class TransitionTargetKind {
    Illust,
    Novel,
    Spotlight,
    User,
    Comments,
    NovelComments,
}

/** 一次卡片展开转场的唯一目标标识。 */
@Stable
data class TransitionTarget(
    val kind: TransitionTargetKind,
    val id: Long,
)

/**
 * 作品列表卡片到详情页「卡片展开式」转场的几何信息源（支持插画与小说）。
 *
 * 列表中的作品卡片在布局完成后把自己的窗口坐标矩形登记进来，
 * 转场动画据此把详情页从卡片位置放大展开、返回时收回卡片；
 * 未登记（如分享链接直达、进程重建恢复）或已滚出容器可视范围时回退为视差侧滑。
 */
@Stable
class SharedBoundsRegistry {

    private val bounds = mutableStateMapOf<TransitionTarget, IllustCardBounds>()

    private var _activeDetailTarget: TransitionTarget? by mutableStateOf(null)

    /** 按详情页打开时的初始路由目标记录该页面内当前实际展示的目标（隔离多实例快速进出时的生命周期延迟回调）。 */
    private val displayedTargetByOrigin = mutableMapOf<TransitionTarget, TransitionTarget>()

    /**
     * 当前插画详情页实际展示的作品 ID（支持详情页内左右滑动切换作品后按真实展示的作品定位收回卡片）。
     */
    var activeDetailIllustId: Long?
        get() = _activeDetailTarget?.takeIf { it.kind == TransitionTargetKind.Illust }?.id
        set(value) {
            val target = value?.let { TransitionTarget(TransitionTargetKind.Illust, it) }
            _activeDetailTarget = target
            if (target != null) {
                lastActiveDetailTarget = target
                val origin = currentRouteTarget
                if (origin != null) {
                    displayedTargetByOrigin[origin] = target
                }
            }
        }

    /**
     * 登记指定详情页（初始路由为 [originTarget]）当前正在展示的目标 [displayedTarget]。
     */
    fun updateDisplayedTarget(originTarget: TransitionTarget, displayedTarget: TransitionTarget) {
        displayedTargetByOrigin[originTarget] = displayedTarget
        if (currentRouteTarget == originTarget) {
            _activeDetailTarget = displayedTarget
            lastActiveDetailTarget = displayedTarget
        } else if (exitingOriginRouteTarget == originTarget && currentRouteTarget == null) {
            exitingFrontTarget = displayedTarget
        }
    }

    /** 兼容插画专用调用：[updateDisplayedTarget] 的 [TransitionTargetKind.Illust] 便捷封装。 */
    fun updateDisplayedIllustId(originIllustId: Long, displayedIllustId: Long) {
        updateDisplayedTarget(
            originTarget = TransitionTarget(TransitionTargetKind.Illust, originIllustId),
            displayedTarget = TransitionTarget(TransitionTargetKind.Illust, displayedIllustId),
        )
    }

    /**
     * 当初始路由为 [originTarget] 的详情页从组合树销毁时调用。
     *
     * 仅当当前栈顶仍为该页面时才清空 [_activeDetailTarget]，避免旧页面退场销毁时误清空新打开页面的状态。
     */
    fun onDetailTargetDisposed(originTarget: TransitionTarget) {
        if (currentRouteTarget == originTarget) {
            _activeDetailTarget = null
        }
        displayedTargetByOrigin.remove(originTarget)
    }

    /** 兼容插画专用调用：[onDetailTargetDisposed] 的 [TransitionTargetKind.Illust] 便捷封装。 */
    fun onDetailDisposed(originIllustId: Long) {
        onDetailTargetDisposed(TransitionTarget(TransitionTargetKind.Illust, originIllustId))
    }

    /** 最近一次在详情页内确认展示的目标（供详情页出栈时 DisposableEffect 清空后仍能定位目标卡片）。 */
    private var lastActiveDetailTarget: TransitionTarget? = null

    /** 最近一次同步的栈顶路由目标（非详情页时为 null）。 */
    private var currentRouteTarget: TransitionTarget? = null

    /** 当前入栈转场的前层目标（仅当新栈顶为详情页时非空）。 */
    private var enteringFrontTarget: TransitionTarget? = null

    /** 当前出栈转场的前层目标（仅当刚退出的原栈顶为详情页时非空）。 */
    private var exitingFrontTarget: TransitionTarget? = null

    /** 当前出栈的详情页最初打开时的路由目标（详情页内滑切到其他作品后，供底层 ENTER_BACK 同样回退到打开时的卡片）。 */
    private var exitingOriginRouteTarget: TransitionTarget? = null

    /** 当前正在执行卡片展开/收回转场的通用目标（插画或小说）。 */
    var activeTransitionTarget: TransitionTarget? by mutableStateOf(null)
        private set

    /**
     * 当前正在执行卡片展开/收回转场的目标插画作品 ID（兼容既有调用点）。
     */
    val activeTransitionIllustId: Long?
        get() = activeTransitionTarget?.takeIf { it.kind == TransitionTargetKind.Illust }?.id

    /**
     * 当前正在执行的卡片展开度（0f..1f），用于驱动列表源卡片交叉淡出避免分身重影。
     */
    var activeTransitionExpansion: Float by mutableFloatStateOf(0f)
        private set

    /** 当前转场是否为出栈退场（pop/返回列表）。退场期列表源卡片保持可见，避免退场动画中途卡片隐身。 */
    var isExitingTransition: Boolean by mutableStateOf(false)
        private set

    /** 转场激活期底层纵深缩放的窗口坐标锚点（= 源卡片中心）。 */
    private var backdropPivot = Offset.Zero

    /** 转场激活期底层纵深缩放的当前比例；非激活态保持 1f。 */
    private var backdropScale = 1f

    /** 转场激活期底层页面因视差侧滑产生的水平位移；非激活态保持 0f。 */
    private var listTranslationX = 0f

    /**
     * 登记某个目标卡片的窗口坐标矩形与卡片圆角。
     */
    fun putTarget(
        target: TransitionTarget,
        rect: Rect?,
        cornerRadiusDp: Float = DEFAULT_CARD_CORNER_RADIUS_DP,
    ) {
        if (rect == null || rect.width <= 0f || rect.height <= 0f) {
            return
        }
        // 正在执行展开/收回动画的目标卡片自身锁定静止态坐标，确保单次动画全程终点锚点恒定不漂移。
        if (target == activeTransitionTarget && bounds.containsKey(target)) {
            return
        }
        val isListTransformed = listTranslationX != 0f || (backdropScale > 0f && backdropScale < 0.999f)
        if (isListTransformed) {
            if (bounds.containsKey(target)) {
                return
            }
            bounds[target] = IllustCardBounds(rect = rect.toStationaryBounds(), cornerRadiusDp = cornerRadiusDp)
            return
        }
        bounds[target] = IllustCardBounds(rect = rect, cornerRadiusDp = cornerRadiusDp)
    }

    /** 登记某个插画卡片的窗口坐标矩形与卡片圆角。 */
    fun put(illustId: Long, rect: Rect?, cornerRadiusDp: Float = DEFAULT_CARD_CORNER_RADIUS_DP) {
        putTarget(TransitionTarget(TransitionTargetKind.Illust, illustId), rect, cornerRadiusDp)
    }

    /** 取出某个目标卡片最近一次登记的窗口坐标矩形与圆角。 */
    fun getTarget(target: TransitionTarget): IllustCardBounds? = bounds[target]

    /** 取出某个插画卡片最近一次登记的窗口坐标矩形。 */
    fun get(illustId: Long): Rect? = bounds[TransitionTarget(TransitionTargetKind.Illust, illustId)]?.rect

    /**
     * 取出目标卡片在当前容器可视区域内的有效登记；
     * 若卡片大部分（超过 75%）已滚出容器可视范围则返回 null，使转场优雅回退为视差侧滑。
     */
    fun getVisibleTargetInContainer(target: TransitionTarget, containerBounds: Rect): IllustCardBounds? {
        val card = bounds[target] ?: return null
        val rect = card.rect
        if (rect.width <= 0f || rect.height <= 0f) return null
        if (containerBounds.width <= 0f || containerBounds.height <= 0f) {
            return card
        }

        val intersectLeft = maxOf(rect.left, containerBounds.left)
        val intersectTop = maxOf(rect.top, containerBounds.top)
        val intersectRight = minOf(rect.right, containerBounds.right)
        val intersectBottom = minOf(rect.bottom, containerBounds.bottom)

        val visibleWidth = (intersectRight - intersectLeft).coerceAtLeast(0f)
        val visibleHeight = (intersectBottom - intersectTop).coerceAtLeast(0f)

        val widthVisibleRatio = visibleWidth / rect.width
        val heightVisibleRatio = visibleHeight / rect.height

        return card.takeIf {
            widthVisibleRatio >= MIN_VISIBLE_RATIO && heightVisibleRatio >= MIN_VISIBLE_RATIO
        }
    }

    /** 兼容插画专用调用：[getVisibleTargetInContainer] 的 [TransitionTargetKind.Illust] 便捷封装。 */
    fun getVisibleInContainer(illustId: Long, containerBounds: Rect): IllustCardBounds? =
        getVisibleTargetInContainer(TransitionTarget(TransitionTargetKind.Illust, illustId), containerBounds)

    /** 解析转场应该使用的有效目标（优先取详情页内切换后的当前目标）。 */
    fun resolveEffectiveTarget(routeTarget: TransitionTarget?): TransitionTarget? {
        if (routeTarget == null) return null
        return displayedTargetByOrigin[routeTarget]
            ?: _activeDetailTarget?.takeIf { it.kind == routeTarget.kind }
            ?: routeTarget
    }

    /** 解析转场应该使用的有效插画作品 ID（优先取详情页内滑动切换后的当前作品 ID）。 */
    fun resolveEffectiveIllustId(routeIllustId: Long?): Long? =
        resolveEffectiveTarget(routeIllustId?.let { TransitionTarget(TransitionTargetKind.Illust, it) })?.id

    /** 同步当前页面栈栈顶路由的目标（栈顶非详情页/阅读器时传入 null）。 */
    fun syncActiveRouteTarget(routeTarget: TransitionTarget?) {
        if (routeTarget != currentRouteTarget) {
            val previousOrigin = currentRouteTarget
            if (previousOrigin != null) {
                exitingOriginRouteTarget = previousOrigin
                exitingFrontTarget = displayedTargetByOrigin[previousOrigin]
                    ?: _activeDetailTarget?.takeIf { it.kind == previousOrigin.kind }
                    ?: lastActiveDetailTarget?.takeIf { it.kind == previousOrigin.kind }
                    ?: previousOrigin
            } else {
                exitingOriginRouteTarget = null
                exitingFrontTarget = null
            }
            currentRouteTarget = routeTarget
            if (routeTarget != null) {
                val displayed = displayedTargetByOrigin[routeTarget] ?: routeTarget
                displayedTargetByOrigin[routeTarget] = displayed
                _activeDetailTarget = displayed
                lastActiveDetailTarget = displayed
            } else {
                _activeDetailTarget = null
            }
            listTranslationX = 0f
        }
        enteringFrontTarget = routeTarget
    }

    /** 兼容插画专用调用：同步当前页面栈栈顶路由的插画作品 ID（非插画详情页传 null）。 */
    fun syncActiveRouteIllustId(routeIllustId: Long?) {
        syncActiveRouteTarget(routeIllustId?.let { TransitionTarget(TransitionTargetKind.Illust, it) })
    }

    /** 按当前层自身的 [rawTarget] 与转场方向 [direction] 解析本次转场对应的目标。 */
    fun resolveTransitionTarget(
        rawTarget: TransitionTarget?,
        direction: com.arkivanov.decompose.extensions.compose.stack.animation.Direction,
    ): TransitionTarget? = when (direction) {
        com.arkivanov.decompose.extensions.compose.stack.animation.Direction.ENTER_FRONT -> rawTarget
        com.arkivanov.decompose.extensions.compose.stack.animation.Direction.EXIT_FRONT -> if (rawTarget != null) {
            displayedTargetByOrigin[rawTarget]
                ?: _activeDetailTarget?.takeIf { it.kind == rawTarget.kind }
                ?: exitingFrontTarget?.takeIf { it.kind == rawTarget.kind }
                ?: lastActiveDetailTarget?.takeIf { it.kind == rawTarget.kind }
                ?: rawTarget
        } else {
            null
        }
        com.arkivanov.decompose.extensions.compose.stack.animation.Direction.EXIT_BACK -> enteringFrontTarget
        com.arkivanov.decompose.extensions.compose.stack.animation.Direction.ENTER_BACK -> {
            val origin = exitingOriginRouteTarget
            if (origin != null) {
                displayedTargetByOrigin[origin] ?: exitingFrontTarget ?: origin
            } else {
                exitingFrontTarget
            }
        }
    }

    /** 按当前层自身的 [rawIllustId] 与转场方向 [direction] 解析本次转场对应的目标作品 ID。 */
    fun resolveTransitionIllustId(
        rawIllustId: Long?,
        direction: com.arkivanov.decompose.extensions.compose.stack.animation.Direction,
    ): Long? = resolveTransitionTarget(
        rawTarget = rawIllustId?.let { TransitionTarget(TransitionTargetKind.Illust, it) },
        direction = direction,
    )?.takeIf { it.kind == TransitionTargetKind.Illust }?.id

    /** 按转场方向解析本次转场的锚点目标候选序列（当前展示的目标 -> 本次打开的目标）。 */
    fun resolveTransitionTargetCandidates(
        rawTarget: TransitionTarget?,
        direction: com.arkivanov.decompose.extensions.compose.stack.animation.Direction,
    ): List<TransitionTarget?> = when (direction) {
        com.arkivanov.decompose.extensions.compose.stack.animation.Direction.ENTER_FRONT -> listOf(rawTarget)
        com.arkivanov.decompose.extensions.compose.stack.animation.Direction.EXIT_FRONT -> if (rawTarget != null) {
            listOf(
                displayedTargetByOrigin[rawTarget]
                    ?: (if (exitingOriginRouteTarget == rawTarget) exitingFrontTarget else null)
                    ?: rawTarget,
                rawTarget,
            ).distinct()
        } else {
            emptyList()
        }
        com.arkivanov.decompose.extensions.compose.stack.animation.Direction.EXIT_BACK -> listOf(enteringFrontTarget)
        com.arkivanov.decompose.extensions.compose.stack.animation.Direction.ENTER_BACK -> {
            val origin = exitingOriginRouteTarget
            listOf(
                origin?.let { displayedTargetByOrigin[it] } ?: exitingFrontTarget,
                origin,
            ).distinct()
        }
    }

    /** 按转场方向解析本次转场的锚点插画作品 ID 候选序列。 */
    fun resolveTransitionIllustIdCandidates(
        rawIllustId: Long?,
        direction: com.arkivanov.decompose.extensions.compose.stack.animation.Direction,
    ): List<Long?> = resolveTransitionTargetCandidates(
        rawTarget = rawIllustId?.let { TransitionTarget(TransitionTargetKind.Illust, it) },
        direction = direction,
    ).map { candidate ->
        if (candidate == null) null
        else if (candidate.kind == TransitionTargetKind.Illust) candidate.id
        else null
    }.let { mapped ->
        if (direction == com.arkivanov.decompose.extensions.compose.stack.animation.Direction.EXIT_FRONT && rawIllustId == null) {
            emptyList()
        } else {
            mapped
        }
    }

    /**
     * 按 Miuix 默认侧滑进度更新底层页面的视差位移；当进度处于起点（<= 0.001f）或终点（>= 0.999f）时自动归零。
     */
    fun updateSlideParallax(
        widthPx: Float,
        fraction: Float,
        parallaxFraction: Float = MIUIX_DEFAULT_COVER_PARALLAX_FRACTION,
    ) {
        val clamped = fraction.coerceIn(0f, 1f)
        if (widthPx <= 0f || parallaxFraction <= 0f || clamped <= 0.001f || clamped >= 0.999f) {
            listTranslationX = 0f
        } else {
            listTranslationX = -widthPx * parallaxFraction * clamped
        }
    }

    /** 更新通用转场激活状态（支持插画与小说）；展开度到端点（0 或 1）时释放激活标记。 */
    fun updateTargetTransitionState(
        target: TransitionTarget?,
        expansion: Float,
        sourceBounds: Rect? = null,
        containerBounds: Rect = Rect.Zero,
        isExiting: Boolean = false,
    ) {
        val clamped = expansion.coerceIn(0f, 1f)
        val expectedOwner = currentRouteTarget
            ?.let { displayedTargetByOrigin[it] ?: it }
            ?: exitingOriginRouteTarget?.let { displayedTargetByOrigin[it] ?: it }

        if (target == null || clamped <= 0.001f || clamped >= 0.999f) {
            if (target == null || activeTransitionTarget == null || activeTransitionTarget == target) {
                activeTransitionTarget = null
                activeTransitionExpansion = 0f
                isExitingTransition = false
                backdropScale = 1f
            }
        } else {
            if (activeTransitionTarget != null &&
                activeTransitionTarget != target &&
                expectedOwner != null &&
                target != expectedOwner &&
                target != currentRouteTarget
            ) {
                return
            }
            activeTransitionTarget = target
            activeTransitionExpansion = clamped
            isExitingTransition = isExiting
            listTranslationX = 0f
            if (sourceBounds != null && containerBounds.width > 0f && containerBounds.height > 0f) {
                backdropPivot = sourceBounds.center
                backdropScale = lerp(1f, BACKDROP_MIN_SCALE, clamped)
            } else {
                backdropScale = 1f
            }
        }
    }

    /**
     * 更新当前转场的激活状态；当展开度到达端点（0 或 1）时自动释放激活标记。
     */
    fun updateTransitionState(
        illustId: Long?,
        expansion: Float,
        sourceBounds: Rect? = null,
        containerBounds: Rect = Rect.Zero,
        isExiting: Boolean = false,
    ) {
        updateTargetTransitionState(
            target = illustId?.let { TransitionTarget(TransitionTargetKind.Illust, it) },
            expansion = expansion,
            sourceBounds = sourceBounds,
            containerBounds = containerBounds,
            isExiting = isExiting,
        )
    }

    /** 解析本次转场应收回的通用目标卡片锚点。 */
    fun resolveTargetAnchor(
        targetCandidates: List<TransitionTarget?>,
        containerBounds: Rect,
    ): ResolvedTargetAnchor? =
        targetCandidates.filterNotNull().distinct().firstNotNullOfOrNull { target ->
            getVisibleTargetInContainer(target, containerBounds)
                ?.takeIf { hasUsableCardExpandGeometry(it.rect, containerBounds) }
                ?.let { ResolvedTargetAnchor(target = target, card = it) }
        }

    /**
     * 解析本次转场应收回的卡片锚点。
     */
    fun resolveAnchor(illustIdCandidates: List<Long?>, containerBounds: Rect): Anchor? =
        resolveTargetAnchor(
            targetCandidates = illustIdCandidates.map { it?.let { id -> TransitionTarget(TransitionTargetKind.Illust, id) } },
            containerBounds = containerBounds,
        )?.let { resolved ->
            Anchor(illustId = resolved.target.id, card = resolved.card)
        }

    /**
     * 更新底层页面当前的水平位移（视差侧滑期间由底层页面层逐帧上报，归零表示底层已回到静止态）。
     */
    fun updateListTranslation(translationX: Float) {
        listTranslationX = if (kotlin.math.abs(translationX) <= 0.5f) 0f else translationX
    }

    /** 移除某个目标卡片的登记，避免已销毁页面的陈旧几何信息被复用。 */
    fun removeTarget(target: TransitionTarget) {
        bounds.remove(target)
    }

    /**
     * 移除某个插画卡片的登记，避免已销毁页面的陈旧几何信息被复用。
     */
    fun remove(illustId: Long) {
        removeTarget(TransitionTarget(TransitionTargetKind.Illust, illustId))
    }

    /**
     * 清空全部登记（用于整栈重置等场景）。
     */
    fun clear() {
        bounds.clear()
        displayedTargetByOrigin.clear()
        _activeDetailTarget = null
        lastActiveDetailTarget = null
        currentRouteTarget = null
        enteringFrontTarget = null
        exitingFrontTarget = null
        exitingOriginRouteTarget = null
        activeTransitionTarget = null
        activeTransitionExpansion = 0f
        isExitingTransition = false
        backdropScale = 1f
        listTranslationX = 0f
    }

    /**
     * 把转场激活期上报的瞬时坐标按底层页面的当前变换逆变换回静止态坐标。
     *
     * 底层页面围绕源卡片中心（窗口坐标 [backdropPivot]）以 [backdropScale] 缩放，
     * 视差侧滑时还会整体平移 [listTranslationX]；
     * 观测坐标 q 与静止坐标 p 满足 q = pivot + (p - pivot) * scale + translation，反解即得 p。
     */
    private fun Rect.toStationaryBounds(): Rect {
        val untranslated = if (listTranslationX != 0f) {
            Rect(
                left = left - listTranslationX,
                top = top,
                right = right - listTranslationX,
                bottom = bottom,
            )
        } else {
            this
        }
        if (backdropScale >= 1f || backdropScale <= 0f) return untranslated
        val pivot = backdropPivot
        return Rect(
            left = pivot.x + (untranslated.left - pivot.x) / backdropScale,
            top = pivot.y + (untranslated.top - pivot.y) / backdropScale,
            right = pivot.x + (untranslated.right - pivot.x) / backdropScale,
            bottom = pivot.y + (untranslated.bottom - pivot.y) / backdropScale,
        )
    }

    private companion object {
        /** 卡片在容器内仍可执行展开/收回动画的最小可见宽高比例。 */
        const val MIN_VISIBLE_RATIO = 0.25f
    }
}

/** 未显式上报圆角时的卡片默认圆角（dp），与 MIUIX Card 默认圆角一致。 */
internal const val DEFAULT_CARD_CORNER_RADIUS_DP = 16f

/** 线性插值。 */
private fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction

/** 通用目标锚点（支持插画与小说卡片收回定位）。 */
@Stable
data class ResolvedTargetAnchor(
    val target: TransitionTarget,
    val card: IllustCardBounds,
)

/**
 * 一次转场应收回的卡片锚点：作品 ID 与其卡片静止态几何。
 *
 * @param illustId 锚点作品 ID，供底层源卡片交叉淡出时精确隐藏对应卡片。
 * @param card 锚点卡片的静止态矩形与圆角。
 */
@Stable
data class Anchor(
    val illustId: Long,
    val card: IllustCardBounds,
)

/**
 * 一次卡片展开/收回转场所依据的单张卡片静止态几何。
 *
 * @param rect 卡片在窗口坐标系下的静止态矩形。
 * @param cornerRadiusDp 卡片自身视觉圆角（dp）：列表卡片 16dp（MIUIX Card 默认）、
 * 浏览历史卡片 12dp，转场收回终点按各自圆角做像素级对齐。
 */
@Stable
data class IllustCardBounds(
    val rect: Rect,
    val cornerRadiusDp: Float,
)

/**
 * 提供当前组合树使用的 [SharedBoundsRegistry]。
 *
 * 默认值为一个游离实例，保证在未显式提供（如预览、测试）时调用方不会崩溃，
 * 只是永远取不到卡片矩形、从而回退为默认转场。
 */
val LocalSharedBoundsRegistry = compositionLocalOf { SharedBoundsRegistry() }
