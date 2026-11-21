package com.perol.pixez.shared.ui.components

import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.AppConstants
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 架构审查 UI-9：网格列数策略纯函数。
 *
 * 两处网格（插画瀑布流 / 特辑卡片）共用同一换算，差异只体现在策略参数上：
 * 自适应模式换算并收敛到上下限、固定模式收敛列数、特辑固定 2 列改走自适应卡宽。
 */
class GridColumnsTest {

    @Test
    fun illustAdaptiveClampsPreferredWidthToPolicyBounds() {
        // 布局设置偏好宽度落在区间内：原样作为自适应最小宽
        assertEquals(
            200,
            resolveGridColumns(
                adapt = true,
                adapterWidthDp = 200,
                configuredCount = 3,
                policy = IllustGridColumnPolicy,
            ).adaptiveMinWidthDp,
        )
        // 低于下限：收敛到设置允许的最小宽度
        assertEquals(
            SettingsRepository.CROSS_ADAPTER_WIDTH_MIN,
            resolveGridColumns(
                adapt = true,
                adapterWidthDp = SettingsRepository.CROSS_ADAPTER_WIDTH_MIN - 10,
                configuredCount = 3,
                policy = IllustGridColumnPolicy,
            ).adaptiveMinWidthDp,
        )
        // 高于上限：收敛到 AppConstants 的上限，避免超大屏一列撑满
        assertEquals(
            AppConstants.Layout.GRID_ADAPTIVE_MAX_WIDTH_DP,
            resolveGridColumns(
                adapt = true,
                adapterWidthDp = AppConstants.Layout.GRID_ADAPTIVE_MAX_WIDTH_DP + 500,
                configuredCount = 3,
                policy = IllustGridColumnPolicy,
            ).adaptiveMinWidthDp,
        )
    }

    @Test
    fun illustFixedClampsColumnCountWithoutAdaptiveSwitch() {
        assertEquals(
            3,
            resolveGridColumns(false, adapterWidthDp = 200, configuredCount = 3, policy = IllustGridColumnPolicy).fixedCount,
        )
        assertEquals(
            1,
            resolveGridColumns(false, adapterWidthDp = 200, configuredCount = 0, policy = IllustGridColumnPolicy).fixedCount,
        )
        assertEquals(
            AppConstants.Layout.GRID_MAX_FIXED_COLUMNS,
            resolveGridColumns(false, adapterWidthDp = 200, configuredCount = 20, policy = IllustGridColumnPolicy).fixedCount,
        )
        // 瀑布流没有「固定 2 列改自适应」策略：2 列就是 2 列
        val twoColumns = resolveGridColumns(false, adapterWidthDp = 200, configuredCount = 2, policy = IllustGridColumnPolicy)
        assertEquals(2, twoColumns.fixedCount)
        assertNull(twoColumns.adaptiveMinWidthDp)
    }

    @Test
    fun spotlightAdaptiveScalesPreferredWidth() {
        // 偏好 300 × 1.2 = 360，落在 160-600 区间内
        assertEquals(
            360,
            resolveGridColumns(true, adapterWidthDp = 300, configuredCount = 1, policy = SpotlightGridColumnPolicy).adaptiveMinWidthDp,
        )
        // 偏好 100 × 1.2 = 120，低于下限 160
        assertEquals(
            AppConstants.Layout.SPOTLIGHT_CARD_MIN_WIDTH_DP,
            resolveGridColumns(true, adapterWidthDp = 100, configuredCount = 1, policy = SpotlightGridColumnPolicy).adaptiveMinWidthDp,
        )
        // 偏好 1000 × 1.2 = 1200，高于上限 600
        assertEquals(
            AppConstants.Layout.SPOTLIGHT_CARD_MAX_WIDTH_DP,
            resolveGridColumns(true, adapterWidthDp = 1000, configuredCount = 1, policy = SpotlightGridColumnPolicy).adaptiveMinWidthDp,
        )
    }

    @Test
    fun spotlightFixedTwoColumnsFallsBackToAdaptiveCardWidth() {
        val twoColumns = resolveGridColumns(false, adapterWidthDp = 180, configuredCount = 2, policy = SpotlightGridColumnPolicy)
        assertEquals(AppConstants.Layout.SPOTLIGHT_CARD_ADAPTIVE_WIDTH_DP, twoColumns.adaptiveMinWidthDp)
        assertNull(twoColumns.fixedCount)

        assertEquals(
            3,
            resolveGridColumns(false, adapterWidthDp = 180, configuredCount = 3, policy = SpotlightGridColumnPolicy).fixedCount,
        )
        assertEquals(
            AppConstants.Layout.SPOTLIGHT_GRID_MAX_COLUMNS,
            resolveGridColumns(false, adapterWidthDp = 180, configuredCount = 9, policy = SpotlightGridColumnPolicy).fixedCount,
        )
    }
}
