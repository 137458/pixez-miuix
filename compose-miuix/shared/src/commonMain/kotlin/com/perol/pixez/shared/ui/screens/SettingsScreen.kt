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
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import com.perol.pixez.shared.data.model.AccountPersist
import com.perol.pixez.shared.data.repository.AccountRepository
import com.perol.pixez.shared.ui.AppInfo
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.components.ToastMessage
import com.perol.pixez.shared.ui.utils.runCatchingNonCancel
import kotlinx.coroutines.CancellationException
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
 * 设置页：分组展示账号、主题、下载、存储、关于等入口。
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAboutClick: () -> Unit,
    onShieldClick: () -> Unit,
    onLoginClick: () -> Unit,
    onDownloadHistoryClick: () -> Unit,
    onThemeSettingClick: () -> Unit,
    onNetworkSettingClick: () -> Unit,
    onDownloadSettingClick: () -> Unit,
    accountRepository: AccountRepository,
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalPlatformContext.current

    // 当前账号信息，登出后手动置 null。
    var account by remember { mutableStateOf<AccountPersist?>(null) }
    var isLoggingOut by rememberSaveable { mutableStateOf(false) }

    // 清除缓存的加载态与提示信息。
    var isClearingCache by remember { mutableStateOf(false) }
    var toastMessage by rememberSaveable { mutableStateOf<String?>(null) }

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
                BasicComponent(
                    title = "主题设置",
                    summary = "主题模式、AMOLED、动态颜色、种子色",
                    onClick = onThemeSettingClick,
                )
            }
            item {
                SmallTitle(text = "网络")
            }
            item {
                BasicComponent(
                    title = "网络设置",
                    summary = "网络模式、图片源",
                    onClick = onNetworkSettingClick,
                )
            }
            item {
                SmallTitle(text = "屏蔽")
            }
            item {
                BasicComponent(
                    title = "屏蔽设置",
                    summary = "标签、画师、AI 作品过滤",
                    onClick = onShieldClick,
                )
            }
            item {
                SmallTitle(text = "下载")
            }
            item {
                BasicComponent(
                    title = "下载设置",
                    summary = "保存路径、同时下载任务数、单文件夹模式",
                    onClick = onDownloadSettingClick,
                )
            }
            item {
                BasicComponent(
                    title = "下载历史",
                    summary = "查看已下载的作品记录",
                    onClick = onDownloadHistoryClick,
                )
            }
            item {
                SmallTitle(text = "存储")
            }
            item {
                BasicComponent(
                    title = "清除缓存",
                    summary = if (isClearingCache) "清理中…" else "释放图片缓存占用的空间",
                    onClick = {
                        if (isClearingCache) return@BasicComponent
                        coroutineScope.launch {
                            isClearingCache = true
                            val result = try {
                                val imageLoader = SingletonImageLoader.get(context)
                                imageLoader.memoryCache?.clear()
                                imageLoader.diskCache?.clear()
                                Result.success(Unit)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Throwable) {
                                Result.failure(e)
                            } finally {
                                isClearingCache = false
                            }
                            toastMessage = if (result.isSuccess) {
                                "缓存已清除"
                            } else {
                                "清除失败: ${result.exceptionOrNull()?.message}"
                            }
                        }
                    },
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

        ToastMessage(
            message = toastMessage,
            onDismiss = { toastMessage = null },
        )
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


