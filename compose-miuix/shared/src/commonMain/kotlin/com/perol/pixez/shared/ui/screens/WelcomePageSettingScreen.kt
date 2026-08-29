package com.perol.pixez.shared.ui.screens

import com.perol.pixez.shared.ui.components.FrostedTopAppBar

import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.perol.pixez.shared.ui.components.LocalBackdrop
import com.perol.pixez.shared.ui.components.topAppBarBlur
import com.perol.pixez.shared.ui.components.blurBackdropSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.perol.pixez.shared.ui.AppConstants
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

/**
 * 欢迎页设置页：选择应用启动后默认进入的页面。
 *
 * 选项沿用旧 Flutter 版的字符串编码，写入 [SettingsRepository.welcomePageType]，
 * 由 [RootComponent] 在启动时解析为初始路由。
 *
 * @param settingsRepository 设置仓库，用于读写欢迎页类型。
 * @param onBack 返回上一级页面。
 */
@Composable
fun WelcomePageSettingScreen(
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
) {
    // 页面状态：从 SettingsRepository 读取当前欢迎页类型。
    var selectedType by remember { mutableStateOf(settingsRepository.welcomePageType) }

    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    val welcomePageOptions = remember(strings) {
        listOf(
            WelcomePageOption(type = "home", label = strings.tabRecommend),
            WelcomePageOption(type = "rank", label = strings.tabRanking),
            WelcomePageOption(type = "quick_view", label = strings.tabSpotlight),
            WelcomePageOption(type = "search", label = strings.tabSearch),
            WelcomePageOption(type = "setting", label = strings.tabSettings),
        )
    }

    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberLayerBackdrop()
    val colorScheme = MiuixTheme.colorScheme

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            FrostedTopAppBar(
                title = strings.settingWelcomePage,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = strings.back,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                contentPadding = paddingValues,
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = AppConstants.Layout.TABLET_CONTENT_MAX_WIDTH_DP.dp)
                    .fillMaxWidth()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
            ) {
                item {
                    SmallTitle(text = strings.settingSectionStartup)
                    top.yukonga.miuix.kmp.basic.Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        welcomePageOptions.forEach { option ->
                            RadioButtonPreference(
                                title = option.label,
                                selected = selectedType == option.type,
                                onClick = {
                                    selectedType = option.type
                                    settingsRepository.welcomePageType = option.type
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 欢迎页选项数据。
 */
private data class WelcomePageOption(
    val type: String,
    val label: String,
)

