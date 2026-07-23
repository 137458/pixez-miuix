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
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*

/**
 * 交互设置页：管理异形屏适配、H 内容过滤、再次返回退出、滑动切换作品等开关。
 *
 * @param settingsRepository 设置仓库，用于读写交互相关偏好。
 * @param onBack 返回上一级页面。
 */
@Composable
fun InteractionSettingScreen(
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
) {
    // 页面状态：从 SettingsRepository 读取当前交互相关设置，用于驱动 Switch 的显示与回写。
    var isBangs by remember { mutableStateOf(settingsRepository.isBangs) }
    var hIsNotAllow by remember { mutableStateOf(settingsRepository.hIsNotAllow) }
    var isReturnAgainToExit by remember { mutableStateOf(settingsRepository.isReturnAgainToExit) }
    var swipeChangeArtwork by remember { mutableStateOf(settingsRepository.swipeChangeArtwork) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "交互设置",
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
                SmallTitle(text = "交互行为")
            }

            // 异形屏适配开关：开启后会对刘海/挖孔屏进行适配。
            item {
                BasicComponent(
                    title = "异形屏适配",
                    summary = if (isBangs) "已开启刘海/挖孔屏适配" else "未开启异形屏适配",
                    endActions = {
                        Switch(
                            checked = isBangs,
                            onCheckedChange = { checked ->
                                isBangs = checked
                                settingsRepository.isBangs = checked
                            },
                        )
                    },
                )
            }

            // H 是不行的开关：开启后过滤 H 内容。
            item {
                BasicComponent(
                    title = "H 是不行的",
                    summary = if (hIsNotAllow) "已开启 H 内容过滤" else "未开启 H 内容过滤",
                    endActions = {
                        Switch(
                            checked = hIsNotAllow,
                            onCheckedChange = { checked ->
                                hIsNotAllow = checked
                                settingsRepository.hIsNotAllow = checked
                            },
                        )
                    },
                )
            }

            // 再次返回退出开关：开启后需要连续返回两次才会退出应用。
            item {
                BasicComponent(
                    title = "再次返回退出",
                    summary = if (isReturnAgainToExit) "连续返回两次后退出应用" else "按一次返回键即退出应用",
                    endActions = {
                        Switch(
                            checked = isReturnAgainToExit,
                            onCheckedChange = { checked ->
                                isReturnAgainToExit = checked
                                settingsRepository.isReturnAgainToExit = checked
                            },
                        )
                    },
                )
            }

            // 滑动切换作品开关：开启后可在插画详情页左右滑动切换作品。
            item {
                BasicComponent(
                    title = "滑动切换作品",
                    summary = if (swipeChangeArtwork) "插画详情页左右滑动可切换作品" else "插画详情页不通过滑动切换作品",
                    endActions = {
                        Switch(
                            checked = swipeChangeArtwork,
                            onCheckedChange = { checked ->
                                swipeChangeArtwork = checked
                                settingsRepository.swipeChangeArtwork = checked
                            },
                        )
                    },
                )
            }
        }
    }
}
