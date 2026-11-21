package com.perol.pixez.shared.ui.skin

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 库底栏未读红点的几何守护。
 *
 * 版式数字来自 COUI-Kit 0.0.1-alpha11 的 navigation/NavigationBar.kt：库底栏的条目只接受图标与
 * 标题、没有内容槽，红点只能由叠层（androidMain 的 UiSkinNavigationBar.kt）按
 * [CouiNavigationBarMetrics] 复算位置，因此这里锁住公式的具体数值与边界。升级库或调整公式时，
 * 先从 Expected 与 Received 的差值看出叠层会偏多少。
 */
class NavigationBarBadgeMetricsTest {

    /** Dp 以 Float 装帧，等比推导带浮点误差，按 0.001dp 容差核对具体落点。 */
    private fun assertDpNear(expected: Dp, actual: Dp, tolerance: Float = 0.001f) {
        val delta = actual.value - expected.value
        assertTrue(
            delta <= tolerance && delta >= -tolerance,
            "期望 ${expected.value}dp（容差 ${tolerance}dp），实际 ${actual.value}dp",
        )
    }

    @Test
    fun `版式常量等于 COUI-Kit 的版式取值`() {
        val metrics = CouiNavigationBarMetrics
        assertEquals(16.dp, metrics.contentPadding)
        assertEquals(56.dp, metrics.sideButtonWidth)
        assertEquals(136.dp, metrics.innerExtraWidth)
        assertEquals(56.dp, metrics.rowHeight)
        assertEquals(48.dp, metrics.itemHeight)
        assertEquals(8.dp, metrics.itemSpacing)
        assertEquals(12.dp, metrics.menuHorizontalPadding)
        assertEquals(70.dp, metrics.itemTargetWidth)
        assertEquals(24.dp, metrics.iconSize)
        assertEquals(4.dp, metrics.iconTopInset)
        assertEquals(7.dp, metrics.badgeSize)
    }

    @Test
    fun `maximumMenuWidth 按 24 + 70n + 8(n-1) 给出上限`() {
        val metrics = CouiNavigationBarMetrics
        assertEquals(94.dp, metrics.maximumMenuWidth(1))
        assertEquals(172.dp, metrics.maximumMenuWidth(2))
        assertEquals(250.dp, metrics.maximumMenuWidth(3))
        assertEquals(328.dp, metrics.maximumMenuWidth(4))
        assertEquals(406.dp, metrics.maximumMenuWidth(5))
    }

    @Test
    fun `maximumMenuWidth 每多一个条目增长 78dp`() {
        val metrics = CouiNavigationBarMetrics
        // 增量 = 条目目标宽 70 加条目间距 8，与条目数无关。
        for (count in 2..6) {
            val delta = metrics.maximumMenuWidth(count) - metrics.maximumMenuWidth(count - 1)
            assertEquals(78.dp, delta, "itemCount=$count 的增量")
        }
    }

    @Test
    fun `maximumMenuWidth 在条目数非正时返回 0dp`() {
        assertEquals(0.dp, CouiNavigationBarMetrics.maximumMenuWidth(0))
        assertEquals(0.dp, CouiNavigationBarMetrics.maximumMenuWidth(-1))
        assertEquals(0.dp, CouiNavigationBarMetrics.maximumMenuWidth(-3))
    }

    @Test
    fun `最大菜单宽下每个条目恰好 70dp`() {
        val metrics = CouiNavigationBarMetrics
        for (count in 1..6) {
            val menuWidth = metrics.maximumMenuWidth(count)
            assertEquals(70.dp, metrics.itemWidth(menuWidth, count), "itemCount=$count 的条目宽")
        }
    }

    @Test
    fun `menuWidth 扣掉两侧圆钮预留的 136dp`() {
        val metrics = CouiNavigationBarMetrics
        assertEquals(364.dp, metrics.menuWidth(500.dp))
        assertEquals(214.dp, metrics.menuWidth(350.dp))
        assertEquals(164.dp, metrics.menuWidth(300.dp))
        assertEquals(94.dp, metrics.menuWidth(230.dp))
    }

    @Test
    fun `menuWidth 在可用宽不足时收敛到 0dp`() {
        val metrics = CouiNavigationBarMetrics
        assertEquals(0.dp, metrics.menuWidth(136.dp))
        assertEquals(0.dp, metrics.menuWidth(100.dp))
        assertEquals(0.dp, metrics.menuWidth(0.dp))
    }

    @Test
    fun `itemWidth 在余量不足时收敛到 0dp`() {
        val metrics = CouiNavigationBarMetrics
        assertEquals(26.dp, metrics.itemWidth(50.dp, 1))
        assertEquals(0.dp, metrics.itemWidth(20.dp, 3))
        assertEquals(0.dp, metrics.itemWidth(0.dp, 3))
        assertEquals(0.dp, metrics.itemWidth(250.dp, 0))
        assertEquals(0.dp, metrics.itemWidth(250.dp, -2))
    }

    @Test
    fun `itemCenterX 在最大菜单宽下左右对称`() {
        val metrics = CouiNavigationBarMetrics
        // 菜单撑满时条目正好 70dp：中心 = 12 + 78 * index + 35，首尾关于菜单中线镜像。
        val datasets = listOf(
            3 to listOf(47.dp, 125.dp, 203.dp),
            4 to listOf(47.dp, 125.dp, 203.dp, 281.dp),
            5 to listOf(47.dp, 125.dp, 203.dp, 281.dp, 359.dp),
        )
        for ((count, expectedCenters) in datasets) {
            val menuWidth = metrics.maximumMenuWidth(count)
            val centers = (0 until count).map { index -> metrics.itemCenterX(index, menuWidth, count) }
            assertEquals(expectedCenters, centers, "itemCount=$count 的条目中心")
            assertEquals(menuWidth, centers.first() + centers.last(), "itemCount=$count 的首尾中心应镜像")
            for (index in 1 until centers.size) {
                assertEquals(78.dp, centers[index] - centers[index - 1], "itemCount=$count 相邻中心间距")
            }
        }
    }

    @Test
    fun `itemCenterX 在 294dp 菜单宽下按条目等分`() {
        val metrics = CouiNavigationBarMetrics
        val menuWidth = 294.dp
        val count = 3
        // 条目宽 = (294 - 24 - 16) / 3 = 84.667dp，中心落在 54.333 / 147 / 239.667。
        assertDpNear(54.333f.dp, metrics.itemCenterX(0, menuWidth, count))
        assertDpNear(147f.dp, metrics.itemCenterX(1, menuWidth, count))
        assertDpNear(239.667f.dp, metrics.itemCenterX(2, menuWidth, count))

        val first = metrics.itemCenterX(0, menuWidth, count)
        val last = metrics.itemCenterX(count - 1, menuWidth, count)
        assertDpNear(menuWidth, first + last)

        // 首尾中心到菜单边缘的距离相等：内边距 12 加半个条目宽。
        val halfItem = metrics.itemWidth(menuWidth, count) / 2
        assertDpNear(12.dp + halfItem, first)
        assertDpNear(menuWidth - 12.dp - halfItem, last)
    }

    @Test
    fun `itemCenterX 对非正条目数与越界索引做防御`() {
        val metrics = CouiNavigationBarMetrics
        assertEquals(0.dp, metrics.itemCenterX(0, 250.dp, 0))
        assertEquals(0.dp, metrics.itemCenterX(2, 250.dp, -1))
        // 越界索引按最近的有效条目取值，不抛异常。
        assertEquals(47.dp, metrics.itemCenterX(-5, 250.dp, 3))
        assertEquals(203.dp, metrics.itemCenterX(7, 250.dp, 3))
    }

    @Test
    fun `badgeOffsetX 等于图标与红点的半径差`() {
        val metrics = CouiNavigationBarMetrics
        assertEquals(8.5f.dp, metrics.badgeOffsetX())
        assertEquals(metrics.iconSize / 2 - metrics.badgeSize / 2, metrics.badgeOffsetX())
        // 中心右移半个图标半径减半个红点后，红点右边缘正好贴住图标右边缘。
        assertEquals(metrics.iconSize / 2, metrics.badgeOffsetX() + metrics.badgeSize / 2)
    }

    @Test
    fun `badgeOffsetY 沿用条目内图标顶部留白`() {
        val metrics = CouiNavigationBarMetrics
        assertEquals(4.dp, metrics.badgeOffsetY())
        assertEquals(metrics.iconTopInset, metrics.badgeOffsetY())
    }
}
