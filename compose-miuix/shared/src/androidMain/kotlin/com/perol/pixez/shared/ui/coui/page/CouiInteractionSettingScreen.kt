package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.coui.component.CouiFixedTopBar
import com.perol.pixez.shared.ui.coui.component.CouiReadableColumn
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.LocalSettingsRepository
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.MiuixIcons

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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    top = 8.dp,
                    end = 12.dp,
                    bottom = LocalBottomBarContentPadding.current,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                overscrollEffect = rememberCouiOverscrollEffect(),
            ) {
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
