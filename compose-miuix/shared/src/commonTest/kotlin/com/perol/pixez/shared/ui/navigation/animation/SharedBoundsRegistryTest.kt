package com.perol.pixez.shared.ui.navigation.animation

import androidx.compose.ui.geometry.Rect
import com.arkivanov.decompose.extensions.compose.stack.animation.Direction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * [SharedBoundsRegistry] 的登记 / 读取 / 注销行为验证。
 *
 * 该登记表是卡片展开转场的几何来源，若残留陈旧矩形，
 * 详情页就会从「上一次点击的卡片」或已销毁卡片的旧位置错误地展开。
 */
class SharedBoundsRegistryTest {

    private val card = Rect(left = 10f, top = 20f, right = 110f, bottom = 220f)

    @Test
    fun `登记后可按作品 ID 取回矩形`() {
        val registry = SharedBoundsRegistry()

        registry.put(illustId = 42L, rect = card)

        assertEquals(card, registry.get(42))
    }

    @Test
    fun `未登记的作品 ID 返回空`() {
        val registry = SharedBoundsRegistry()

        assertNull(registry.get(42))
    }

    @Test
    fun `重复登记以最后一次为准`() {
        val registry = SharedBoundsRegistry()
        val moved = Rect(left = 300f, top = 400f, right = 500f, bottom = 700f)

        registry.put(illustId = 42L, rect = card)
        registry.put(illustId = 42L, rect = moved)

        assertEquals(moved, registry.get(42))
    }

    @Test
    fun `登记空矩形等价于注销`() {
        val registry = SharedBoundsRegistry()
        registry.put(illustId = 42L, rect = card)

        registry.put(illustId = 42L, rect = null)

        assertNull(registry.get(42))
    }

    @Test
    fun `零尺寸矩形视为无效并不予登记`() {
        val registry = SharedBoundsRegistry()

        registry.put(illustId = 42L, rect = Rect(left = 5f, top = 5f, right = 5f, bottom = 5f))

        assertNull(registry.get(42))
    }

    @Test
    fun `注销只影响目标作品不影响其他登记`() {
        val registry = SharedBoundsRegistry()
        registry.put(illustId = 1L, rect = card)
        registry.put(illustId = 2L, rect = card)

        registry.remove(1)

        assertNull(registry.get(1))
        assertEquals(card, registry.get(2))
    }

    @Test
    fun `清空移除全部登记`() {
        val registry = SharedBoundsRegistry()
        registry.put(illustId = 1L, rect = card)
        registry.put(illustId = 2L, rect = card)

        registry.clear()

        assertNull(registry.get(1))
        assertNull(registry.get(2))
    }

    @Test
    fun `详情页左右滑动切换作品后优先使用当前实际展示的作品 ID 匹配收回卡片`() {
        val registry = SharedBoundsRegistry()
        registry.activeDetailIllustId = 99

        assertEquals(99, registry.resolveEffectiveIllustId(routeIllustId = 42))
        registry.activeDetailIllustId = null
        assertEquals(42, registry.resolveEffectiveIllustId(routeIllustId = 42))
    }

    @Test
    fun `滚出容器可视范围超过阈值的卡片返回空以回退为侧滑动画`() {
        val registry = SharedBoundsRegistry()
        val container = Rect(left = 0f, top = 0f, right = 1080f, bottom = 2400f)
        // 完全在容器内的卡片
        val visibleCard = Rect(left = 16f, top = 200f, right = 520f, bottom = 800f)
        // 绝大部分已滚出容器顶部的卡片（仅露出底部 20px，总高 600px，可见占比 < 5%）
        val mostlyScrolledOutCard = Rect(left = 16f, top = -580f, right = 520f, bottom = 20f)

        registry.put(illustId = 1L, rect = visibleCard)
        registry.put(illustId = 2L, rect = mostlyScrolledOutCard)

        assertEquals(visibleCard, registry.getVisibleInContainer(1, container)?.rect)
        assertNull(registry.getVisibleInContainer(2, container))
    }

    @Test
    fun `转场动画进行中禁止用被缩放的后台列表坐标覆盖卡片静止态真实坐标`() {
        val registry = SharedBoundsRegistry()
        val stationaryBounds = Rect(left = 24f, top = 300f, right = 524f, bottom = 950f)
        val shrunkBoundsFromBackdrop = Rect(left = 38f, top = 320f, right = 518f, bottom = 944f)

        registry.put(illustId = 42L, rect = stationaryBounds)
        // 转场开始（expansion = 0.6f），底层列表被 0.96x 缩放触发 onGloballyPositioned
        registry.updateTransitionState(illustId = 42L, expansion = 0.6f)
        registry.put(illustId = 42L, rect = shrunkBoundsFromBackdrop)

        // 取出退出终点坐标时，必须保持未缩放的静止态真实坐标 stationaryBounds
        assertEquals(stationaryBounds, registry.get(42))

        // 转场结束后（expansion = 0f），允许正常列表滚动更新坐标
        registry.updateTransitionState(illustId = 42L, expansion = 0f)
        val scrolledBounds = Rect(left = 24f, top = 180f, right = 524f, bottom = 830f)
        registry.put(illustId = 42L, rect = scrolledBounds)
        assertEquals(scrolledBounds, registry.get(42))
    }

    @Test
    fun `视差侧滑期间指派的卡片坐标按位移逆变换归一为静止态坐标`() {
        val registry = SharedBoundsRegistry()
        // 侧滑期间底层列表整体左移 250px（25% 视差），卡片上报的是平移后的瞬时坐标
        registry.updateListTranslation(-250f)

        registry.put(illustId = 7L, rect = Rect(left = 90f, top = 500f, right = 490f, bottom = 900f))

        assertEquals(
            Rect(left = 340f, top = 500f, right = 740f, bottom = 900f),
            registry.get(7),
            "侧滑期首次登记的坐标必须按位移逆变换回静止态，否则收回终点会整体错位",
        )

        // 位移植零（底层回到静止态）后恢复正常登记
        registry.updateListTranslation(0f)
        val stationary = Rect(left = 20f, top = 100f, right = 420f, bottom = 500f)
        registry.put(illustId = 8L, rect = stationary)
        assertEquals(stationary, registry.get(8))
    }

    @Test
    fun `侧滑期间已登记卡片的静止态坐标不被瞬时坐标覆盖`() {
        val registry = SharedBoundsRegistry()
        val stationary = Rect(left = 100f, top = 400f, right = 500f, bottom = 800f)
        registry.put(illustId = 7L, rect = stationary)

        registry.updateListTranslation(-250f)
        registry.put(illustId = 7L, rect = Rect(left = -150f, top = 400f, right = 250f, bottom = 800f))

        assertEquals(stationary, registry.get(7))
    }

    @Test
    fun `锚点优先当前展示的作品其次本次打开的作品`() {
        val registry = SharedBoundsRegistry()
        val container = Rect(left = 0f, top = 0f, right = 1080f, bottom = 2400f)
        val displayedCard = Rect(left = 16f, top = 200f, right = 520f, bottom = 800f)
        val openedCard = Rect(left = 560f, top = 1200f, right = 1064f, bottom = 1800f)
        registry.put(illustId = 99L, rect = displayedCard, cornerRadiusDp = 16f)
        registry.put(illustId = 42L, rect = openedCard, cornerRadiusDp = 12f)

        // 详情页内滑到作品 99：其卡片在列表里，优先收回它
        registry.activeDetailIllustId = 99
        val displayedAnchor = registry.resolveAnchor(listOf(99, 42), container)
        assertEquals(99L, displayedAnchor?.illustId)
        assertEquals(displayedCard, displayedAnchor?.card?.rect)

        // 滑到的作品不在列表（未登记）时，退回本次打开的作品 42
        registry.activeDetailIllustId = 88
        val fallbackAnchor = registry.resolveAnchor(listOf(88, 42), container)
        assertEquals(42L, fallbackAnchor?.illustId)
        assertEquals(openedCard, fallbackAnchor?.card?.rect)
        assertEquals(12f, fallbackAnchor?.card?.cornerRadiusDp, "退回锚点必须带上该卡片自身的圆角")
    }

    @Test
    fun `候选卡片均不可用时锚点为空中止卡片展开`() {
        val registry = SharedBoundsRegistry()
        val container = Rect(left = 0f, top = 0f, right = 1080f, bottom = 2400f)
        // 已滚出容器：可见高度占比不足阈值
        registry.put(illustId = 42L, rect = Rect(left = 16f, top = -580f, right = 520f, bottom = 20f))

        assertNull(registry.resolveAnchor(listOf(88, 42), container))
        assertNull(registry.resolveAnchor(listOf(null, null), container))
    }

    @Test
    fun `横屏容器下竖图卡片锚点正常解析不回退侧滑`() {
        val registry = SharedBoundsRegistry()
        val landscapeContainer = Rect(left = 0f, top = 0f, right = 1600f, bottom = 1000f)
        val portraitCard = Rect(left = 300f, top = 220f, right = 600f, bottom = 790f)
        registry.put(illustId = 42L, rect = portraitCard)

        val anchor = registry.resolveAnchor(listOf(42), landscapeContainer)
        assertEquals(42L, anchor?.illustId)
        assertEquals(portraitCard, anchor?.card?.rect)
    }

    @Test
    fun `登记时记录卡片圆角并随可视矩形一起返回`() {
        val registry = SharedBoundsRegistry()
        val container = Rect(left = 0f, top = 0f, right = 1080f, bottom = 2400f)

        registry.put(illustId = 42L, rect = card, cornerRadiusDp = 12f)

        val visible = registry.getVisibleInContainer(42, container)
        assertEquals(12f, visible?.cornerRadiusDp, "转场终点圆角必须取卡片自身圆角（如历史卡 12dp）")
        assertEquals(card, visible?.rect)
    }

    @Test
    fun `卡片转场激活期底层列表保持无缩放原尺寸登记且已登记卡片允许实时更新滚动位置`() {
        val registry = SharedBoundsRegistry()
        val container = Rect(left = 0f, top = 0f, right = 1000f, bottom = 2000f)
        val sourceCard = Rect(left = 100f, top = 400f, right = 500f, bottom = 800f)
        registry.put(illustId = 42L, rect = sourceCard)
        registry.updateTransitionState(
            illustId = 42L,
            expansion = 0.5f,
            sourceBounds = sourceCard,
            containerBounds = container,
        )

        val newCard = Rect(left = 200f, top = 500f, right = 600f, bottom = 900f)
        registry.put(illustId = 7L, rect = newCard, cornerRadiusDp = 12f)
        assertEquals(
            newCard,
            registry.get(7),
            "底层列表在卡片转场期保持 1.0x 不缩放，新进入组合的卡片直接为静止态真实坐标",
        )

        // 快速进出期间用户在上一张 (42) 收回尚未结束时滚动列表并点击另一张已登记卡片 (7)，该卡片的新坐标必须实时刷新而非丢弃
        val scrolledCard = Rect(left = 200f, top = 260f, right = 600f, bottom = 660f)
        registry.put(illustId = 7L, rect = scrolledCard)
        assertEquals(scrolledCard, registry.get(7))
    }

    @Test
    fun `转场结束后登记坐标不再做逆变换`() {
        val registry = SharedBoundsRegistry()
        val container = Rect(left = 0f, top = 0f, right = 1000f, bottom = 2000f)
        val sourceCard = Rect(left = 100f, top = 400f, right = 500f, bottom = 800f)
        registry.updateTransitionState(illustId = 42L, expansion = 0.5f, sourceBounds = sourceCard, containerBounds = container)
        registry.updateTransitionState(illustId = null, expansion = 0f)

        val stationary = Rect(left = 202f, top = 502f, right = 594f, bottom = 894f)
        registry.put(illustId = 7L, rect = stationary)

        assertEquals(stationary, registry.get(7))
    }

    @Test
    fun `单参 StackAnimation 下 push 与滑页后 pop 的前后层均能解析一致的作品 ID`() {
        val registry = SharedBoundsRegistry()
        // 1. 初始在 Main 列表页
        registry.syncActiveRouteIllustId(null)

        // 2. Push 进入 IllustDetail(42)
        registry.syncActiveRouteIllustId(42)
        assertEquals(42, registry.resolveTransitionIllustId(rawIllustId = 42, direction = Direction.ENTER_FRONT))
        assertEquals(42, registry.resolveTransitionIllustId(rawIllustId = null, direction = Direction.EXIT_BACK))

        // 3. 在详情页内左右滑切到作品 99
        registry.updateDisplayedIllustId(originIllustId = 42, displayedIllustId = 99)

        // 4. Pop 返回 Main（即使详情页 DisposableEffect 先将 activeDetailIllustId 置空，退出动画仍须保持 99）
        registry.syncActiveRouteIllustId(null)
        registry.onDetailDisposed(originIllustId = 42)
        assertEquals(99, registry.resolveTransitionIllustId(rawIllustId = 42, direction = Direction.EXIT_FRONT))
        assertEquals(99, registry.resolveTransitionIllustId(rawIllustId = null, direction = Direction.ENTER_BACK))
    }

    @Test
    fun `详情页滑切到列表外作品后返回时顶层与底层均能回退到本次打开的作品锚点`() {
        val registry = SharedBoundsRegistry()
        val container = Rect(left = 0f, top = 0f, right = 1080f, bottom = 2400f)
        val openedCard = Rect(left = 24f, top = 300f, right = 524f, bottom = 950f)
        registry.put(illustId = 42L, rect = openedCard, cornerRadiusDp = 16f)

        // 1. 从列表点击作品 42 进入详情页
        registry.syncActiveRouteIllustId(42)
        // 2. 在详情页内右滑切到不在列表中的关联作品 99
        registry.updateDisplayedIllustId(originIllustId = 42, displayedIllustId = 99)
        // 3. 按返回出栈回到 Main（rawIllustId = null）
        registry.syncActiveRouteIllustId(null)
        registry.onDetailDisposed(originIllustId = 42)

        val frontCandidates = registry.resolveTransitionIllustIdCandidates(rawIllustId = 42, direction = Direction.EXIT_FRONT)
        val backCandidates = registry.resolveTransitionIllustIdCandidates(rawIllustId = null, direction = Direction.ENTER_BACK)

        val frontAnchor = registry.resolveAnchor(frontCandidates, container)
        val backAnchor = registry.resolveAnchor(backCandidates, container)

        assertEquals(42L, frontAnchor?.illustId, "顶层 EXIT_FRONT 应回退到打开时的卡片 42")
        assertEquals(42L, backAnchor?.illustId, "底层 ENTER_BACK (rawIllustId=null) 也必须一致回退到卡片 42，防止底层误走侧滑")
    }

    @Test
    fun `快速多次进入退出不同作品详情页时旧页面的延迟回调与收尾帧绝不污染新页面的返回锚点`() {
        val registry = SharedBoundsRegistry()
        val container = Rect(left = 0f, top = 0f, right = 1080f, bottom = 2400f)
        val cardA = Rect(left = 24f, top = 200f, right = 524f, bottom = 800f)
        val cardB = Rect(left = 556f, top = 200f, right = 1056f, bottom = 800f)
        registry.put(illustId = 101L, rect = cardA)
        registry.put(illustId = 202L, rect = cardB)

        // 1. 打开作品 A (101)
        registry.syncActiveRouteIllustId(101)
        registry.updateDisplayedIllustId(originIllustId = 101, displayedIllustId = 101)

        // 2. 快速返回（此时作品 101 仍在执行 340ms EXIT_FRONT 动画，尚未从组合树销毁）
        registry.syncActiveRouteIllustId(null)

        // 3. 在 101 尚未销毁前，用户立即点击打开作品 B (202)
        registry.syncActiveRouteIllustId(202)
        registry.updateTransitionState(illustId = 202L, expansion = 0.4f, sourceBounds = cardB, containerBounds = container)

        // 4. 此时旧页面 101 的异步关联列表或 DisposableEffect.onDispose 延迟触发，甚至其收尾帧上报 expansion = 0f
        registry.updateDisplayedIllustId(originIllustId = 101, displayedIllustId = 101)
        registry.onDetailDisposed(originIllustId = 101)
        registry.updateTransitionState(illustId = 101L, expansion = 0f)

        // 断言：202 的激活态不得被旧页面 101 的收尾帧清空
        assertEquals(202, registry.activeTransitionIllustId, "旧页面 101 的收尾帧不得清空当前正在展开的 202")

        // 5. 用户再次快速从作品 B (202) 返回，即使 202 的 LaunchedEffect 尚未执行，返回位置也必须精准落在卡片 B (202) 而非卡片 A (101)
        registry.syncActiveRouteIllustId(null)
        val candidatesB = registry.resolveTransitionIllustIdCandidates(rawIllustId = 202, direction = Direction.EXIT_FRONT)
        val backCandidatesB = registry.resolveTransitionIllustIdCandidates(rawIllustId = null, direction = Direction.ENTER_BACK)

        assertEquals(202, registry.resolveAnchor(candidatesB, container)?.illustId, "作品 202 必须收回卡片 202，绝不能误收回 101")
        assertEquals(202, registry.resolveAnchor(backCandidatesB, container)?.illustId, "底层 ENTER_BACK 同样必须定位到 202")
    }

    @Test
    fun `视差侧滑到达起点或终点时自动归零位移以防后续页面卡片坐标冻结或偏移`() {
        val registry = SharedBoundsRegistry()
        val initialRect = Rect(left = 24f, top = 300f, right = 524f, bottom = 950f)
        registry.put(illustId = 42L, rect = initialRect)

        // 侧滑中段（fraction = 0.5）记录视差位移
        registry.updateSlideParallax(widthPx = 1000f, fraction = 0.5f)
        // 侧滑入栈到达终点（fraction = 1.0，例如进入搜索页/画师页）必须释放位移状态
        registry.updateSlideParallax(widthPx = 1000f, fraction = 1.0f)

        // 释放后在二级页新登记的卡片不得被叠加视差偏移，已有卡片滚动后也必须能正常刷新坐标
        val newCardRect = Rect(left = 100f, top = 200f, right = 500f, bottom = 600f)
        registry.put(illustId = 88L, rect = newCardRect)
        assertEquals(newCardRect, registry.get(88), "侧滑入栈结束后新卡片不得被叠加陈旧视差偏移")

        val scrolledRect = Rect(left = 24f, top = 120f, right = 524f, bottom = 770f)
        registry.put(illustId = 42L, rect = scrolledRect)
        assertEquals(scrolledRect, registry.get(42), "侧滑结束后已登记卡片在滚动时必须允许更新坐标")
    }

    @Test
    fun `从作品详情页进入非详情页及从非详情页返回详情页时前后层均解析为空以回退侧滑`() {
        val registry = SharedBoundsRegistry()
        registry.syncActiveRouteIllustId(42)
        registry.updateDisplayedIllustId(originIllustId = 42, displayedIllustId = 99)

        // 从详情页 (42) Push 进入画师页 (null)
        registry.syncActiveRouteIllustId(null)
        assertNull(registry.resolveTransitionIllustId(rawIllustId = null, direction = Direction.ENTER_FRONT))
        assertNull(registry.resolveTransitionIllustId(rawIllustId = 42, direction = Direction.EXIT_BACK))

        // 从画师页 (null) Pop 返回详情页 (42)
        registry.syncActiveRouteIllustId(42)
        assertNull(registry.resolveTransitionIllustId(rawIllustId = null, direction = Direction.EXIT_FRONT))
        assertNull(registry.resolveTransitionIllustId(rawIllustId = 42, direction = Direction.ENTER_BACK))
    }
}
