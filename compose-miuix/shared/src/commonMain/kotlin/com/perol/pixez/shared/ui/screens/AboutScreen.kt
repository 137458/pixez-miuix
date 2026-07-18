package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.ui.AppInfo
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 关于页：展示应用名称、版本与开源信息。
 */
@Composable
fun AboutScreen(
    onBack: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "关于",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "PixEz",
                style = MiuixTheme.textStyles.title1,
            )
            Text(
                text = AppInfo.VERSION_NAME,
                style = MiuixTheme.textStyles.body1,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = "使用 MIUIX + Compose Multiplatform 重构的 PixEz 第三方客户端。",
                style = MiuixTheme.textStyles.body2,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = "原应用功能保留中，M3 阶段为基础页面移植。",
                style = MiuixTheme.textStyles.footnote1,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
