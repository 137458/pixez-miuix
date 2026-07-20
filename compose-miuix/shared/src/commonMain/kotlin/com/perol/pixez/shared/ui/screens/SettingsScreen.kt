package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.AccountPersist
import com.perol.pixez.shared.data.repository.AccountRepository
import com.perol.pixez.shared.ui.AppInfo
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.utils.runCatchingNonCancel
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar

/**
 * 设置页：分组展示账号、主题、关于等入口。
 *
 * @param themeMode 当前主题模式：0 跟随系统，1 浅色，2 深色。
 * @param onThemeModeChange 主题模式变更回调，由外层 [RootContent] 应用到 [MiuixTheme]。
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAboutClick: () -> Unit,
    onLoginClick: () -> Unit,
    onDownloadHistoryClick: () -> Unit,
    themeMode: Int,
    onThemeModeChange: (Int) -> Unit,
    accountRepository: AccountRepository,
) {
    val coroutineScope = rememberCoroutineScope()

    // 当前账号信息，登出后手动置 null。
    var account by remember { mutableStateOf<AccountPersist?>(null) }
    var isLoggingOut by rememberSaveable { mutableStateOf(false) }

    // 页面进入时加载一次账号信息。
    LaunchedEffect(accountRepository) {
        account = runCatchingNonCancel { accountRepository.currentAccount() }.getOrNull()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "设置",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
                SmallTitle(text = "账号")
            }
            item {
                AccountSection(
                    account = account,
                    isLoggingOut = isLoggingOut,
                    onLoginClick = onLoginClick,
                    onLogoutClick = {
                        if (isLoggingOut) return@AccountSection
                        coroutineScope.launch {
                            try {
                                isLoggingOut = true
                                runCatchingNonCancel { accountRepository.logout() }
                                    .onSuccess { account = null }
                            } finally {
                                isLoggingOut = false
                            }
                        }
                    },
                )
            }
            item {
                SmallTitle(text = "主题")
            }
            item {
                ThemeModeSelector(
                    selected = themeMode,
                    onSelect = onThemeModeChange,
                )
            }
            item {
                SmallTitle(text = "下载")
            }
            item {
                BasicComponent(
                    title = "下载历史",
                    summary = "查看已下载的作品记录",
                    onClick = onDownloadHistoryClick,
                )
            }
            item {
                SmallTitle(text = "关于")
            }
            item {
                BasicComponent(
                    title = "关于 PixEz",
                    summary = "版本 ${AppInfo.VERSION_NAME}",
                    onClick = onAboutClick,
                )
            }
        }
    }
}

/**
 * 账号分组：已登录展示头像与账号信息，未登录提供登录入口。
 */
@Composable
private fun AccountSection(
    account: AccountPersist?,
    isLoggingOut: Boolean,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (account != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PixivAsyncImage(
                    model = account.userImage,
                    contentDescription = account.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = account.name,
                        style = top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles.title3,
                    )
                    Text(
                        text = "@${account.account}",
                        style = top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles.body2,
                    )
                    Text(
                        text = "ID: ${account.userId}",
                        style = top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles.footnote2,
                    )
                }
            }
            Button(
                onClick = onLogoutClick,
                enabled = !isLoggingOut,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = if (isLoggingOut) "退出中…" else "退出登录")
            }
        } else {
            Text(
                text = "未登录",
                style = top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles.body1,
            )
            Button(
                onClick = onLoginClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "去登录")
            }
        }
    }
}

@Composable
private fun ThemeModeSelector(
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    val options = listOf("跟随系统" to 0, "浅色" to 1, "深色" to 2)
    Column {
        options.forEach { (label, value) ->
            BasicComponent(
                title = label,
                onClick = { onSelect(value) },
                endActions = {
                    if (selected == value) {
                        Text(text = "✓")
                    }
                },
            )
        }
    }
}
