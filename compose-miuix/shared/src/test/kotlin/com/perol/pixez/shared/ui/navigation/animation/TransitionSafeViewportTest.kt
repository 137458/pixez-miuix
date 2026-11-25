package com.perol.pixez.shared.ui.navigation.animation

import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TransitionSafeViewportTest {
    @Test
    fun `computeSafeViewportBounds 从容器矩形中扣减顶栏与底栏遮挡区域`() {
        val safe = computeSafeViewportBounds(
            containerBounds = Rect(left = 240f, top = 0f, right = 1280f, bottom = 2400f),
            rootWidth = 1280f,
            rootHeight = 2400f,
            topOccluderBottomPx = 320f,
            bottomOccluderTopPx = 2120f,
        )

        assertEquals(Rect(left = 240f, top = 320f, right = 1280f, bottom = 2120f), safe)
    }

    @Test
    fun `computeSafeViewportBounds 在无外层容器时回退到窗口根节点尺寸`() {
        val safe = computeSafeViewportBounds(
            containerBounds = null,
            rootWidth = 1080f,
            rootHeight = 2400f,
            topOccluderBottomPx = 280f,
            bottomOccluderTopPx = null,
        )

        assertEquals(Rect(left = 0f, top = 280f, right = 1080f, bottom = 2400f), safe)
    }

    @Test
    fun `computeSafeViewportBounds 在遮挡交错导致区域退化时返回 null`() {
        assertNull(
            computeSafeViewportBounds(
                containerBounds = Rect(0f, 0f, 1080f, 2400f),
                rootWidth = 1080f,
                rootHeight = 2400f,
                topOccluderBottomPx = 1200f,
                bottomOccluderTopPx = 1000f,
            ),
        )
    }

    @Test
    fun `isRectContainedInViewport 精确拦截被顶栏底栏或侧栏部分遮挡的卡片`() {
        val safeViewport = Rect(left = 200f, top = 320f, right = 1080f, bottom = 2100f)

        // 完整落在安全可视区内（含 0.5px 浮点误差容忍）
        assertTrue(
            isRectContainedInViewport(
                rect = Rect(left = 220f, top = 319.5f, right = 600f, bottom = 900f),
                viewport = safeViewport,
            ),
        )

        // 上缘滚动到 TopAppBar 后方（如推荐页顶部右侧卡片）
        assertFalse(
            isRectContainedInViewport(
                rect = Rect(left = 550f, top = 210f, right = 1060f, bottom = 820f),
                viewport = safeViewport,
            ),
        )

        // 下缘伸入浮动底栏下方
        assertFalse(
            isRectContainedInViewport(
                rect = Rect(left = 220f, top = 1750f, right = 520f, bottom = 2200f),
                viewport = safeViewport,
            ),
        )

        // 左缘被侧边 NavigationRail 裁切
        assertFalse(
            isRectContainedInViewport(
                rect = Rect(left = 140f, top = 400f, right = 500f, bottom = 900f),
                viewport = safeViewport,
            ),
        )
    }

    @Test
    fun `resolveCardExpandViewportClip 在尾帧收束至安全视口并在展开稳态关闭裁切`() {
        val containerBounds = Rect(left = 0f, top = 0f, right = 1080f, bottom = 2400f)
        val safeViewport = Rect(left = 0f, top = 360f, right = 1080f, bottom = 2120f)

        // 稳态全屏不裁切
        assertNull(resolveCardExpandViewportClip(safeViewport, containerBounds, expansion = 1f))
        assertNull(resolveCardExpandViewportClip(safeViewport, containerBounds, expansion = 0.999f))
        assertNull(resolveCardExpandViewportClip(safeViewport = null, containerBounds = containerBounds, expansion = 0.2f))

        // 交接淡出阈值（0.05f）及以下完全收束到 TopBar/BottomBar 边界内
        assertEquals(
            safeViewport,
            resolveCardExpandViewportClip(safeViewport, containerBounds, expansion = 0.05f),
        )
        assertEquals(
            safeViewport,
            resolveCardExpandViewportClip(safeViewport, containerBounds, expansion = 0f),
        )

        // 中段平滑过渡
        val mid = resolveCardExpandViewportClip(safeViewport, containerBounds, expansion = 0.525f)
        assertNotNull(mid)
        assertEquals(180f, mid.top, absoluteTolerance = 0.5f)
        assertEquals(2260f, mid.bottom, absoluteTolerance = 0.5f)
    }

    @Test
    fun `TransitionViewportState 子作用域继承容器与底栏并隔离各自顶栏`() {
        val mainContainer = TransitionViewportState().apply {
            containerBounds = Rect(left = 0f, top = 0f, right = 1080f, bottom = 2400f)
            bottomOccluderTopPx = 2160f
        }
        val helloTab = TransitionViewportState(parent = mainContainer).apply {
            topOccluderBottomPx = 340f
        }
        val rankingTab = TransitionViewportState(parent = mainContainer).apply {
            topOccluderBottomPx = 480f
        }

        assertEquals(
            Rect(0f, 340f, 1080f, 2160f),
            helloTab.resolveSafeViewport(fallbackWidth = 1080f, fallbackHeight = 2400f),
        )
        assertEquals(
            Rect(0f, 480f, 1080f, 2160f),
            rankingTab.resolveSafeViewport(fallbackWidth = 1080f, fallbackHeight = 2400f),
        )
    }

    @Test
    fun `SharedBoundsRegistry 保存并回传目标卡片的安全视口`() {
        val registry = SharedBoundsRegistry()
        val target = TransitionTarget(TransitionTargetKind.Illust, 12345L)
        val container = Rect(0f, 0f, 1080f, 2400f)
        val safeViewport = Rect(0f, 320f, 1080f, 2140f)

        registry.putTarget(
            target = target,
            rect = Rect(540f, 200f, 1040f, 700f),
            cornerRadiusDp = 18f,
            safeViewport = safeViewport,
        )

        val anchor = registry.resolveTargetAnchor(listOf(target), container)
        assertNotNull(anchor)
        assertEquals(safeViewport, anchor.card.safeViewport)
    }

    @Test
    fun `TransitionViewportState 多遮挡源取最严边界且释放互不误清，CardBounds 支持动态刷新`() {
        val state = TransitionViewportState()
        val topBar = Any()
        val subTab = Any()
        state.updateTopOccluder(topBar, 240f)
        state.updateTopOccluder(subTab, 360f)
        assertEquals(360f, state.resolveTopOccluderBottomPx())

        state.updateTopOccluder(subTab, null)
        assertEquals(240f, state.resolveTopOccluderBottomPx())

        var latestSafe = Rect(0f, 360f, 1080f, 2100f)
        val card = IllustCardBounds(
            rect = Rect(0f, 400f, 500f, 900f),
            cornerRadiusDp = 16f,
            safeViewport = Rect(0f, 240f, 1080f, 2400f),
            safeViewportProvider = { latestSafe },
        )
        assertEquals(latestSafe, card.resolveSafeViewport())
        latestSafe = Rect(0f, 280f, 1080f, 2200f)
        assertEquals(latestSafe, card.resolveSafeViewport())
    }
}
