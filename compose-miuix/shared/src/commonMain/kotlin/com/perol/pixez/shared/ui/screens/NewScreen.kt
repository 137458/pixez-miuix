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
 * 最新作品页：展示最新上传的插画，M4 接入 /v1/illust/new 数据。
 */
@Composable
fun NewScreen(
    onIllustClick: (Int) -> Unit,
) {
    val illusts = remember { FakeData.illusts(count = 24) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = "最新")
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
