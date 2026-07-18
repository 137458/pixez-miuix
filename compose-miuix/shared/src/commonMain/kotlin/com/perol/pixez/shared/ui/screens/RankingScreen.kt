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
 * 排行榜页：与首页相同的瀑布流布局，M4 接入不同排序数据。
 */
@Composable
fun RankingScreen(
    onIllustClick: (Int) -> Unit,
) {
    val illusts = remember { FakeData.illusts(count = 24) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = "排行榜")
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
