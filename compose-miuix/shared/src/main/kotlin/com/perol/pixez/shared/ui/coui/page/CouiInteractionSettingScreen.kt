package com.perol.pixez.shared.ui.coui.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.coui.core.CouiRowDivider
import com.perol.pixez.shared.ui.coui.core.CouiSettingsScaffold
import com.perol.pixez.shared.ui.coui.core.CouiToggleRow
import com.perol.pixez.shared.ui.i18n.LocalStrings

/**
 * COUI 交互设置页：设置骨架 + 开关分组卡片。
 *
 * COUI 皮肤仅运行在 Android，保留滑动切换作品与双击返回退出两项；
 * 桌面专属的关闭到托盘、开机自启与快捷键速查不在本皮肤范围。
 */
@Composable
internal fun CouiInteractionSettingScreen(
    onBack: () -> Unit,
) {
    val settingsRepository = LocalSettingsRepository.current
    val strings = LocalStrings.current
    var isReturnAgainToExit by remember { mutableStateOf(settingsRepository.isReturnAgainToExit) }
    var swipeChangeArtwork by remember { mutableStateOf(settingsRepository.swipeChangeArtwork) }

    CouiSettingsScaffold(
        title = strings.interactionSettingTitle,
        onBack = onBack,
    ) {
        item(key = "coui_interaction", contentType = "section") {
            CouiSettingSection(title = strings.interactionSettingTitle) {
                CouiToggleRow(
                    title = strings.interactionSettingSwipeChange,
                    checked = swipeChangeArtwork,
                    onCheckedChange = { checked ->
                        swipeChangeArtwork = checked
                        settingsRepository.swipeChangeArtwork = checked
                    },
                    summary = if (swipeChangeArtwork) {
                        strings.interactionSettingSwipeChangeSummaryOn
                    } else {
                        strings.interactionSettingSwipeChangeSummaryOff
                    },
                )
                CouiRowDivider()
                CouiToggleRow(
                    title = strings.interactionSettingDoubleBackExit,
                    checked = isReturnAgainToExit,
                    onCheckedChange = { checked ->
                        isReturnAgainToExit = checked
                        settingsRepository.isReturnAgainToExit = checked
                    },
                    summary = if (isReturnAgainToExit) {
                        strings.interactionSettingDoubleBackExitSummaryOn
                    } else {
                        strings.interactionSettingDoubleBackExitSummaryOff
                    },
                )
            }
        }
    }
}
