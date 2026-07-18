package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.perol.pixez.shared.ui.FakeData
import com.perol.pixez.shared.ui.components.IllustStaggeredGrid
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar

/**
 * 首页/推荐页：顶部标题栏 + 插画瀑布流。
 */
@Composable
fun HelloScreen(
    onIllustClick: (Int) -> Unit,
    onSettingsClick: () -> Unit,
) {
    // M3 阶段缓存假数据，避免重组时重新生成导致列表状态丢失。
    val illusts = remember { FakeData.illusts() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "首页",
                actions = {
                    IconButton(
                        onClick = onSettingsClick,
                    ) {
                        top.yukonga.miuix.kmp.basic.Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "设置",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        IllustStaggeredGrid(
            illusts = illusts,
            onIllustClick = onIllustClick,
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues,
        )
    }
}
