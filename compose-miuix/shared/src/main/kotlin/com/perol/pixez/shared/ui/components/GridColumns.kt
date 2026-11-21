package com.perol.pixez.shared.ui.components

import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.AppConstants

/**
 * 网格列数参数（架构审查 UI-9）：自适应最小卡宽与固定列数二选一。
 */
internal data class GridColumns(
    /** 自适应模式的最小卡宽（dp）；null 表示固定列数。 */
    val adaptiveMinWidthDp: Int? = null,
    /** 固定模式的列数；null 表示自适应。 */
    val fixedCount: Int? = null,
)

/**
 * 列数策略差异（架构审查 UI-9）：插画瀑布流与特辑网格的换算共同点收敛到
 * [resolveGridColumns]，差异全部显式化为策略字段，数值落在 [AppConstants.Layout]。
 */
internal data class GridColumnPolicy(
    /** 自适应最小宽相对偏好宽度的倍率。 */
    val adaptiveWidthScale: Float = 1f,
    /** 自适应最小宽下限（dp）。 */
    val minWidthDp: Int,
    /** 自适应最小宽上限（dp）。 */
    val maxWidthDp: Int,
    /** 固定列数上限。 */
    val maxFixedColumns: Int,
    /** 固定列数等于该值时改走自适应（null 表示不启用）。 */
    val adaptiveFixedCount: Int? = null,
    /** [adaptiveFixedCount] 命中时使用的自适应卡宽（dp）。 */
    val adaptiveFixedWidthDp: Int = 0,
)

/** 插画瀑布流的列数策略：自适应按布局设置的偏好宽度原样收敛，固定列数收敛到 1-8。 */
internal val IllustGridColumnPolicy = GridColumnPolicy(
    minWidthDp = SettingsRepository.CROSS_ADAPTER_WIDTH_MIN,
    maxWidthDp = AppConstants.Layout.GRID_ADAPTIVE_MAX_WIDTH_DP,
    maxFixedColumns = AppConstants.Layout.GRID_MAX_FIXED_COLUMNS,
)

/** 特辑卡片网格的列数策略：自适应按偏好宽度放大 1.2 倍后收敛，固定 2 列改走 240dp 自适应。 */
internal val SpotlightGridColumnPolicy = GridColumnPolicy(
    adaptiveWidthScale = AppConstants.Layout.SPOTLIGHT_CARD_WIDTH_SCALE,
    minWidthDp = AppConstants.Layout.SPOTLIGHT_CARD_MIN_WIDTH_DP,
    maxWidthDp = AppConstants.Layout.SPOTLIGHT_CARD_MAX_WIDTH_DP,
    maxFixedColumns = AppConstants.Layout.SPOTLIGHT_GRID_MAX_COLUMNS,
    adaptiveFixedCount = 2,
    adaptiveFixedWidthDp = AppConstants.Layout.SPOTLIGHT_CARD_ADAPTIVE_WIDTH_DP,
)

/**
 * 把布局设置快照换算成网格列数参数（架构审查 UI-9）：
 * 自适应模式按偏好宽度 × [GridColumnPolicy.adaptiveWidthScale] 收敛到上下限；
 * 固定模式收敛到 1-[GridColumnPolicy.maxFixedColumns]，命中
 * [GridColumnPolicy.adaptiveFixedCount] 时改走 [GridColumnPolicy.adaptiveFixedWidthDp]。
 */
internal fun resolveGridColumns(
    adapt: Boolean,
    adapterWidthDp: Int,
    configuredCount: Int,
    policy: GridColumnPolicy,
): GridColumns {
    if (adapt) {
        val minWidth = (adapterWidthDp * policy.adaptiveWidthScale).toInt()
            .coerceIn(policy.minWidthDp, policy.maxWidthDp)
        return GridColumns(adaptiveMinWidthDp = minWidth)
    }
    if (policy.adaptiveFixedCount != null && configuredCount == policy.adaptiveFixedCount) {
        return GridColumns(adaptiveMinWidthDp = policy.adaptiveFixedWidthDp)
    }
    return GridColumns(fixedCount = configuredCount.coerceIn(1, policy.maxFixedColumns))
}

/** 把列数参数落成 Compose 的 [StaggeredGridCells]。 */
internal fun GridColumns.toStaggeredGridCells(): StaggeredGridCells =
    adaptiveMinWidthDp?.let { StaggeredGridCells.Adaptive(it.dp) }
        ?: StaggeredGridCells.Fixed(fixedCount ?: 1)
