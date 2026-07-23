package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.components.CheckIndicator
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*

/**
 * 桌面小组件推荐类型设置页：选择小部件展示的内容来源。
 *
 * 选项沿用旧 Flutter 版的字符串编码，写入 [SettingsRepository.widgetIllustType]，
 * 供桌面小部件在刷新时读取并决定请求哪类插画列表。
 *
 * @param settingsRepository 设置仓库，用于读写小组件推荐类型。
 * @param onBack 返回上一级页面。
 */
@Composable
fun WidgetRecommendSettingScreen(
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
) {
    // 页面状态：从 SettingsRepository 读取当前小组件推荐类型。
    var selectedType by remember { mutableStateOf(settingsRepository.widgetIllustType) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "小组件推荐",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = "返回",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues,
        ) {
            item {
                SmallTitle(text = "推荐内容")
            }
            // 遍历三个互斥选项，渲染为带单选指示器的行。
            items(WIDGET_ILLUST_OPTIONS.size) { index ->
                val option = WIDGET_ILLUST_OPTIONS[index]
                BasicComponent(
                    title = option.label,
                    onClick = {
                        // 更新本地选中状态，并同步写入设置仓库。
                        selectedType = option.type
                        settingsRepository.widgetIllustType = option.type
                    },
                    endActions = {
                        CheckIndicator(selected = selectedType == option.type)
                    },
                )
            }
        }
    }
}

/**
 * 小组件推荐选项数据。
 */
private data class WidgetIllustOption(
    val type: String,
    val label: String,
)

/**
 * 可选的小组件推荐类型与展示文案，顺序与旧 Flutter 应用保持一致。
 * - recom：推荐
 * - rank：排行榜
 * - news：关注
 */
private val WIDGET_ILLUST_OPTIONS = listOf(
    WidgetIllustOption(type = "recom", label = "推荐"),
    WidgetIllustOption(type = "rank", label = "排行榜"),
    WidgetIllustOption(type = "news", label = "关注"),
)
