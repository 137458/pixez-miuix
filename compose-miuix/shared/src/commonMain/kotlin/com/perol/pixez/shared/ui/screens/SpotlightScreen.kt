package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.perol.pixez.shared.ui.FakeData
import com.perol.pixez.shared.ui.components.IllustStaggeredGrid
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar

/**
 * Spotlight 精选页：M4 接入 Pixiv 官方 Spotlight 数据，当前用推荐插画占位。
 */
@Composable
fun SpotlightScreen(
    onIllustClick: (Int) -> Unit,
) {
    val illusts = remember { FakeData.illusts(count = 16) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = "Spotlight")
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
