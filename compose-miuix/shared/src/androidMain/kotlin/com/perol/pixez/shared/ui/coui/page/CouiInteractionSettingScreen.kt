package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.coui.component.CouiActionButton
import com.perol.pixez.shared.ui.coui.component.CouiReadableColumn
import com.perol.pixez.shared.ui.i18n.LocalStrings
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back

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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CouiTheme.colorScheme.background)
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CouiActionButton(
                icon = MiuixIcons.Back,
                contentDescription = strings.back,
                onClick = onBack,
            )
            Text(
                text = strings.interactionSettingTitle,
                kind = CouiTextKind.ItemTitle,
                color = CouiTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

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
