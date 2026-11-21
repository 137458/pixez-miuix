package com.perol.pixez.shared.ui.skin

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * COUI 官方浮动底栏的版式常量与几何推导。
 *
 * 下列数字逐条抄自 COUI-Kit 的 navigation/NavigationBar.kt。官方的浮动底栏是按「3 个条目 + 左右各一个
 * 圆形附件」设计的，胶囊宽度固定按 `可用宽度 - 136dp` 计算并给两个 56dp 附件预留位置，本工程 5 个
 * 等分条目、没有附件，因此底栏改为用库内公开的浮动玻璃原语自装配（androidMain 的 CouiBottomBar.kt），
 * 只沿用这里的度量：胶囊高、条目高、图标、内边距、间距、红点尺寸。
 *
 * 纯算术对象，不涉及组合、窗口特性与手势；NavigationBarBadgeMetricsTest 锁死这些取值，
 * 升级 COUI-Kit 后按此处复核官方栏是否仍是同一套度量。
 */
internal object CouiNavigationBarMetrics {

    /** 外框内边距：左、右、上各 16dp；底部同为 16dp，另加系统可点击区判断出的 0 或 8dp 留白。 */
    val contentPadding: Dp = 16.dp

    /** 底栏左右两个圆钮的宽度，单侧 56dp，菜单夹在两者之间。 */
    val sideButtonWidth: Dp = 56.dp

    /** 菜单两侧额外预留的宽度：左右圆钮各 56dp，再给菜单左右各 12dp 的呼吸空间。 */
    val innerExtraWidth: Dp = 136.dp

    /** 菜单行的行高 56dp。 */
    val rowHeight: Dp = 56.dp

    /** 条目高度 48dp，在 [rowHeight] 的行高里垂直居中。 */
    val itemHeight: Dp = 48.dp

    /** 条目间距 8dp。 */
    val itemSpacing: Dp = 8.dp

    /** 菜单的横向内边距，单侧 12dp；左右合计 24dp，即 [maximumMenuWidth] 里那个固定的 24。 */
    val menuHorizontalPadding: Dp = 12.dp

    /** 条目在菜单撑满时的目标宽度 70dp，与 [maximumMenuWidth] 里的 70n 对应。 */
    val itemTargetWidth: Dp = 70.dp

    /** 图标边长 24dp。 */
    val iconSize: Dp = 24.dp

    /** 条目内图标顶部留白 4dp，红点的纵向偏移沿用该值。 */
    val iconTopInset: Dp = 4.dp

    /** 未读红点直径 7dp。 */
    val badgeSize: Dp = 7.dp

    /**
     * 菜单在条目数为 [itemCount] 时的最大宽度：`24 + 70n + 8(n-1)`。
     *
     * 该上限让菜单撑满时每个条目恰好宽 [itemTargetWidth]（70dp）：扣掉左右内边距 24dp 与
     * 条目间距 8(n-1) 后正好剩 70n。库按条目数给出上限，超出部分由两侧圆钮的留白吸收。
     * [itemCount] 非正时返回 0dp，不抛异常——叠层只为真实存在的条目求值，防御性返回比崩溃更合适。
     */
    fun maximumMenuWidth(itemCount: Int): Dp {
        if (itemCount <= 0) return 0.dp
        return menuHorizontalPadding * 2 + itemTargetWidth * itemCount + itemSpacing * (itemCount - 1)
    }

    /**
     * 在可用宽度 [availableWidth] 下实际分给菜单的宽度：扣掉 [innerExtraWidth] 后的余量。
     *
     * 可用宽不足时收敛到 0dp 而非负数——Modifier.width 不接受负宽度。
     */
    fun menuWidth(availableWidth: Dp): Dp = (availableWidth - innerExtraWidth).coerceAtLeast(0.dp)

    /**
     * 菜单内单个条目的实际宽度：扣掉左右内边距与条目间距后按条目数均分。
     *
     * 对应布局里 Row 的 weight(1f) 分配结果；[itemCount] 非正或余量不足时返回 0dp。
     */
    fun itemWidth(menuWidth: Dp, itemCount: Int): Dp {
        if (itemCount <= 0) return 0.dp
        val remaining = menuWidth - menuHorizontalPadding * 2 - itemSpacing * (itemCount - 1)
        return (remaining / itemCount).coerceAtLeast(0.dp)
    }

    /**
     * 第 [itemIndex] 个条目的中心相对菜单栏左缘的 x 坐标。
     *
     * 菜单内是等宽条目按 [itemSpacing] 排布，故中心 = 内边距 + index × (条目宽 + 间距) + 条目宽 / 2。
     * 叠层把红点按 TopCenter 对齐到条目上，本函数用于校验红点落点；[itemIndex] 越界时按最近的有效
     * 条目取值，[itemCount] 非正时返回 0dp。
     */
    fun itemCenterX(itemIndex: Int, menuWidth: Dp, itemCount: Int): Dp {
        if (itemCount <= 0) return 0.dp
        val width = itemWidth(menuWidth, itemCount)
        val index = itemIndex.coerceIn(0, itemCount - 1)
        return menuHorizontalPadding + (width + itemSpacing) * index + width / 2
    }

    /**
     * 红点的横向偏移：图标 24dp 居中于条目，红点 7dp，要让红点右边贴住图标右边，中心须右移
     * `24 / 2 - 7 / 2 = 8.5dp`。
     */
    fun badgeOffsetX(): Dp = iconSize / 2 - badgeSize / 2

    /** 红点的纵向偏移：沿用条目内图标顶部留白 [iconTopInset]（4dp）。 */
    fun badgeOffsetY(): Dp = iconTopInset
}
