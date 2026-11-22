package com.perol.pixez.android

/**
 * 屏幕可用显示模式候选摘要，便于与 Android Framework `Display.Mode` 解耦做纯逻辑单测。
 */
internal data class DisplayModeCandidate(
    val modeId: Int,
    val refreshRate: Float,
    val physicalWidth: Int,
    val physicalHeight: Int,
)

/**
 * Window 显示模式选择结果：
 * - `modeId == 0` 且 `preferredRefreshRate == 0f` 表示交由系统默认调度（跟随系统）。
 */
internal data class DisplayModeSelection(
    val modeId: Int,
    val preferredRefreshRate: Float,
)

internal object DisplayModePolicy {
    const val DISPLAY_MODE_SYSTEM = 0
    const val DISPLAY_MODE_LIMIT_60HZ = 1
    const val DISPLAY_MODE_HIGH_REFRESH = 2

    private val REFRESH_RATE_60HZ_RANGE = 58f..62f
    private const val REFRESH_RATE_HIGH_MIN = 88f

    /**
     * 根据用户选择的 [displayMode] 从 [modes] 中挑选目标显示模式：
     * - `0`（默认/跟随系统）：不锁定模式或刷新率，返回 `(0, 0f)` 让系统按需调度（含 LTPO 变频）；
     * - `1`（60Hz）：优先锁定 58~62Hz 且物理分辨率最高的模式，`preferredRefreshRate = 60f`；
     * - `2`（高刷新率）：优先锁定 `>= 88Hz` 且刷新率最高的模式，否则回退最大刷新率模式。
     */
    fun select(
        displayMode: Int,
        modes: List<DisplayModeCandidate>,
    ): DisplayModeSelection {
        return when (displayMode) {
            DISPLAY_MODE_LIMIT_60HZ -> {
                val target60 = modes
                    .filter { it.refreshRate in REFRESH_RATE_60HZ_RANGE }
                    .maxByOrNull { it.physicalWidth.toLong() * it.physicalHeight }
                DisplayModeSelection(
                    modeId = target60?.modeId ?: 0,
                    preferredRefreshRate = 60f,
                )
            }

            DISPLAY_MODE_HIGH_REFRESH -> {
                val maxMode = modes.maxByOrNull { it.refreshRate }
                val highMode = modes
                    .filter { it.refreshRate >= REFRESH_RATE_HIGH_MIN }
                    .maxByOrNull { it.refreshRate }
                    ?: maxMode
                DisplayModeSelection(
                    modeId = highMode?.modeId ?: 0,
                    preferredRefreshRate = highMode?.refreshRate ?: maxMode?.refreshRate ?: 60f,
                )
            }

            else -> DisplayModeSelection(
                modeId = 0,
                preferredRefreshRate = 0f,
            )
        }
    }
}
