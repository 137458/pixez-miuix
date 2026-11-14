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
import androidx.compose.runtime.mutableIntStateOf
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
import com.perol.pixez.shared.ui.coui.component.CouiSelectRow
import com.perol.pixez.shared.ui.i18n.LocalStrings
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back

/**
 * COUI 画质设置页：固定顶栏 + 分组卡片内的下拉选择行。
 *
 * 四项画质偏好的取值索引与回写口径不变；COUI 库无下拉原语，
 * 选择行用锚点菜单实现（见共用 [CouiSelectRow]）。
 */
@Composable
internal fun CouiQualitySettingScreen(
    onBack: () -> Unit,
) {
    val settingsRepository = LocalSettingsRepository.current
    val strings = LocalStrings.current
    var feedPreviewQuality by remember { mutableIntStateOf(settingsRepository.feedPreviewQuality) }
    var pictureQuality by remember { mutableIntStateOf(settingsRepository.pictureQuality) }
    var mangaQuality by remember { mutableIntStateOf(settingsRepository.mangaQuality) }
    var zoomQuality by remember { mutableIntStateOf(settingsRepository.zoomQuality) }

    val feedPreviewQualityOptions = remember(strings) {
        listOf(strings.qualityMedium, strings.qualityLarge, strings.qualityLow)
    }
    val detailQualityOptions = remember(strings) {
        listOf(strings.qualityLarge, strings.qualityOriginal, strings.qualityMedium)
    }
    val zoomQualityOptions = remember(strings) {
        listOf(strings.qualityOriginal, strings.qualityLarge, strings.qualityMedium)
    }

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
                text = strings.settingQuality,
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
                item(key = "coui_quality", contentType = "section") {
                    CouiSettingSection(title = strings.settingQuality) {
                        CouiSelectRow(
                            title = strings.feedPreviewQuality,
                            items = feedPreviewQualityOptions,
                            selectedIndex = feedPreviewQuality.coerceIn(0, feedPreviewQualityOptions.lastIndex),
                            onSelect = { index ->
                                feedPreviewQuality = index
                                settingsRepository.feedPreviewQuality = index
                            },
                        )
                        CouiSelectRow(
                            title = strings.pictureQuality,
                            items = detailQualityOptions,
                            selectedIndex = pictureQuality.coerceIn(0, detailQualityOptions.lastIndex),
                            onSelect = { index ->
                                pictureQuality = index
                                settingsRepository.pictureQuality = index
                            },
                        )
                        CouiSelectRow(
                            title = strings.mangaQuality,
                            items = detailQualityOptions,
                            selectedIndex = mangaQuality.coerceIn(0, detailQualityOptions.lastIndex),
                            onSelect = { index ->
                                mangaQuality = index
                                settingsRepository.mangaQuality = index
                            },
                        )
                        CouiSelectRow(
                            title = strings.zoomQuality,
                            items = zoomQualityOptions,
                            selectedIndex = zoomQuality.coerceIn(0, zoomQualityOptions.lastIndex),
                            onSelect = { index ->
                                zoomQuality = index
                                settingsRepository.zoomQuality = index
                            },
                        )
                    }
                }
            }
        }
    }
}
