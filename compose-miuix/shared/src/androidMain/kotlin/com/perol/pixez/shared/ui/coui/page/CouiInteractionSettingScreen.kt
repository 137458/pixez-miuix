package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.ui.coui.component.CouiFixedTopBar
import com.perol.pixez.shared.ui.coui.component.CouiReadableColumn
import com.perol.pixez.shared.ui.coui.component.CouiSettingsLazyColumn
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.LocalSettingsRepository

/**
 * COUI 交互设置页：固定顶栏 + 开关分组卡片。
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CouiTheme.colorScheme.background),
    ) {
        CouiFixedTopBar(
            title = strings.interactionSettingTitle,
            onBack = onBack,
        )

        CouiReadableColumn(modifier = Modifier.fillMaxSize()) {
            CouiSettingsLazyColumn {
                item(key = "coui_interaction", contentType = "section") {
                    CouiSettingSection(title = strings.interactionSettingTitle) {
                        CouiSwitchRow(
                            title = strings.interactionSettingSwipeChange,
                            summary = if (swipeChangeArtwork) {
                                strings.interactionSettingSwipeChangeSummaryOn
                            } else {
                                strings.interactionSettingSwipeChangeSummaryOff
                            },
                            checked = swipeChangeArtwork,
                            onCheckedChange = { checked ->
                                swipeChangeArtwork = checked
                                settingsRepository.swipeChangeArtwork = checked
                            },
                        )
                        CouiSwitchRow(
                            title = strings.interactionSettingDoubleBackExit,
                            summary = if (isReturnAgainToExit) {
                                strings.interactionSettingDoubleBackExitSummaryOn
                            } else {
                                strings.interactionSettingDoubleBackExitSummaryOff
                            },
                            checked = isReturnAgainToExit,
                            onCheckedChange = { checked ->
                                isReturnAgainToExit = checked
                                settingsRepository.isReturnAgainToExit = checked
                            },
                        )
                    }
                }
            }
        }
    }
}
