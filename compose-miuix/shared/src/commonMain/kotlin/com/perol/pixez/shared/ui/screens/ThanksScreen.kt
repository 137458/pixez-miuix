package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.THANKS_PEOPLES
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*

/**
 * 致谢页：展示项目感谢人员名单。
 *
 * @param onBack 返回上一级页面。
 */
@Composable
fun ThanksScreen(
    onBack: () -> Unit,
) {
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = strings.settingThanks,
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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues,
        ) {
            item {
                Text(
                    text = strings.aboutThanksDesc,
                    modifier = Modifier.padding(16.dp),
                    style = top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles.body2,
                )
            }
            items(THANKS_PEOPLES.size) { index ->
                Text(
                    text = THANKS_PEOPLES[index],
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    style = top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles.body1,
                )
            }
        }
    }
}
