package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.repository.AccountRepository
import com.perol.pixez.shared.platform.IllustClipboard
import com.perol.pixez.shared.platform.openBrowser
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.AppInfo
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.utils.suspendRunCatchingNonCancel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import pixez_miuix.shared.generated.resources.Res
import pixez_miuix.shared.generated.resources.ic_pixez_logo
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 登录页：支持通过系统浏览器 OAuth 授权或直接输入 Pixiv Refresh Token 完成登录。
 *
 * 视觉与交互特性：
 * 1. 顶部品牌 Hero 区域：展示 PixEz Squircle 应用图标与产品版本标题。
 * 2. 快捷 OAuth 授权卡片：推荐路径，调用系统浏览器完成安全免密授权。
 * 3. 凭证/Token 登录卡片：支持 Refresh Token、授权回调链接与 JSON 凭证，提供一键从剪贴板粘贴与清空能力。
 * 4. 网络诊断与排障引导：提示大陆用户无法打开登录页面时的代理与 SNI 绕过配置。
 * 5. 服务条款与隐私政策：合规外链跳转。
 * 6. 大屏响应式自适应：限制平板与桌面端最大宽度并自动居中。
 */
@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onLoginSuccess: () -> Unit,
    accountRepository: AccountRepository,
    onNetworkSettingClick: (() -> Unit)? = null,
) {
    var tokenInput by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf("") }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val clipboard = remember { IllustClipboard() }

    LaunchedEffect(accountRepository) {
        accountRepository.loginEventFlow.collect {
            onLoginSuccess()
        }
    }

    val strings = LocalStrings.current
    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = strings.goLogin,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = strings.back,
                        )
                    }
                },
                actions = {
                    if (onNetworkSettingClick != null) {
                        IconButton(onClick = onNetworkSettingClick) {
                            Icon(
                                imageVector = MiuixIcons.Settings,
                                contentDescription = strings.loginGoNetworkSetting,
                            )
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = AppConstants.Layout.TABLET_CONTENT_MAX_WIDTH_DP.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // 1. 顶部品牌 Hero 区域
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(MiuixTheme.colorScheme.surfaceContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(Res.drawable.ic_pixez_logo),
                        contentDescription = "PixEz Logo",
                        modifier = Modifier.size(64.dp),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "PixEz MIUIX",
                    style = MiuixTheme.textStyles.title2,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onSurface,
                )

                Row(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(MiuixTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "v${AppInfo.VERSION_NAME}",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = strings.loginPixivPrompt,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 2. 快捷 OAuth 授权卡片（推荐）
                SmallTitle(text = strings.loginQuick)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = strings.loginWithPixiv,
                                style = MiuixTheme.textStyles.title4,
                                fontWeight = FontWeight.Bold,
                                color = MiuixTheme.colorScheme.onSurface,
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = strings.loginPixivPrompt,
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            colors = ButtonDefaults.buttonColorsPrimary(),
                            onClick = {
                                try {
                                    openBrowser(accountRepository.loginUrl())
                                    errorMessage = ""
                                } catch (e: Exception) {
                                    errorMessage = "${strings.openInBrowser}: ${e.message ?: ""}"
                                    Napier.e("打开登录浏览器失败", e)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(strings.loginWithBrowser)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            colors = ButtonDefaults.buttonColors(
                                color = MiuixTheme.colorScheme.surfaceContainerHigh,
                            ),
                            onClick = {
                                try {
                                    openBrowser(accountRepository.loginUrl(create = true))
                                    errorMessage = ""
                                } catch (e: Exception) {
                                    errorMessage = "${strings.openInBrowser}: ${e.message ?: ""}"
                                    Napier.e("打开创建账号浏览器失败", e)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = strings.loginCreateAccount,
                                color = MiuixTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Token / 凭证手动登录卡片
                SmallTitle(text = strings.loginToken)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = strings.loginTokenPrompt,
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        TextField(
                            value = tokenInput,
                            onValueChange = {
                                tokenInput = it
                                if (errorMessage.isNotEmpty()) errorMessage = ""
                            },
                            label = strings.loginInputTokenHint,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                if (tokenInput.isNotEmpty()) {
                                    IconButton(onClick = { tokenInput = "" }) {
                                        Icon(
                                            imageVector = MiuixIcons.Close,
                                            contentDescription = strings.actionClear,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                            },
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(
                                text = strings.loginPasteFromClipboard,
                                onClick = {
                                    try {
                                        val text = clipboard.getText()?.trim()
                                        if (!text.isNullOrBlank()) {
                                            tokenInput = text
                                            errorMessage = ""
                                        }
                                    } catch (e: Exception) {
                                        Napier.e("剪贴板读取失败", e)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            )

                            Button(
                                colors = ButtonDefaults.buttonColorsPrimary(),
                                enabled = !isLoading,
                                onClick = {
                                    if (tokenInput.isBlank()) {
                                        errorMessage = strings.loginTokenLabel
                                        return@Button
                                    }
                                    coroutineScope.launch {
                                        try {
                                            isLoading = true
                                            errorMessage = ""
                                            suspendRunCatchingNonCancel {
                                                accountRepository.login(tokenInput.trim())
                                            }.onFailure { e ->
                                                errorMessage = "${strings.loadFailed}: ${e.message ?: strings.clearFailed}"
                                                Napier.e("登录失败", e)
                                            }
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                if (isLoading) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                    ) {
                                        InfiniteProgressIndicator(modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(strings.loginLoggingIn)
                                    }
                                } else {
                                    Text(strings.goLogin)
                                }
                            }
                        }
                    }
                }

                // 4. 网络诊断与排障卡片
                if (onNetworkSettingClick != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        BasicComponent(
                            title = strings.loginTroubleTip,
                            summary = strings.loginTroubleDesc,
                            onClick = onNetworkSettingClick,
                            endActions = {
                                Icon(
                                    imageVector = MiuixIcons.Settings,
                                    contentDescription = strings.loginGoNetworkSetting,
                                    tint = MiuixTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                        )
                    }
                }

                // 5. 错误提示卡片
                if (errorMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = errorMessage,
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.error,
                            modifier = Modifier.padding(14.dp),
                        )
                    }
                }

                // 6. 服务条款与隐私政策
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${strings.loginTermsAndPrivacy} ",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    Text(
                        text = strings.loginTerms,
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            try {
                                openBrowser(AppConstants.Urls.PIXIV_TERMS)
                            } catch (e: Exception) {
                                Napier.e("打开服务条款失败", e)
                            }
                        },
                    )
                    Text(
                        text = " & ",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    Text(
                        text = strings.loginPrivacy,
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            try {
                                openBrowser(AppConstants.Urls.PIXIV_PRIVACY)
                            } catch (e: Exception) {
                                Napier.e("打开隐私政策失败", e)
                            }
                        },
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
