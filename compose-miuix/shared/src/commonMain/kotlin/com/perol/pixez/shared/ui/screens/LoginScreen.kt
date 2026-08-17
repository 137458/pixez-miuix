package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.repository.AccountRepository
import com.perol.pixez.shared.platform.openBrowser
import com.perol.pixez.shared.ui.utils.suspendRunCatchingNonCancel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 登录页：通过 OAuth2 授权码完成登录。
 *
 * 流程：
 * 1. 点击“使用浏览器登录”打开 Pixiv OAuth 授权页。
 * 2. 用户授权后，系统通过 DeepLink 自动回填或手动将 code 粘贴到输入框。
 * 3. 点击“登录”交换 token 并持久化账号。
 */
@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onLoginSuccess: () -> Unit,
    accountRepository: AccountRepository,
) {
    var code by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf("") }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(accountRepository) {
        accountRepository.loginEventFlow.collect {
            onLoginSuccess()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "登录",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues),
        ) {
            SmallTitle(text = "快捷登录")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "使用 Pixiv 账号登录",
                        style = MiuixTheme.textStyles.title4,
                    )
                    Text(
                        text = "点击下方按钮打开系统浏览器完成授权，授权完成后将自动回调登录。",
                        style = MiuixTheme.textStyles.body2,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                    )
                    Button(
                        onClick = {
                            try {
                                openBrowser(accountRepository.loginUrl())
                                errorMessage = ""
                            } catch (e: Exception) {
                                errorMessage = "打开浏览器失败：${e.message}"
                                Napier.e("打开登录浏览器失败", e)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("使用浏览器登录")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            try {
                                openBrowser(accountRepository.loginUrl(create = true))
                                errorMessage = ""
                            } catch (e: Exception) {
                                errorMessage = "打开浏览器失败：${e.message}"
                                Napier.e("打开创建账号浏览器失败", e)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("创建新账号")
                    }
                }
            }

            SmallTitle(text = "手动输入授权码")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "如未能自动回调，可手动粘贴回调 URL 中的 code：",
                        style = MiuixTheme.textStyles.body2,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    TextField(
                        value = code,
                        onValueChange = { code = it },
                        label = "授权码 (code)",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        colors = ButtonDefaults.buttonColorsPrimary(),
                        onClick = {
                            if (code.isBlank()) {
                                errorMessage = "请输入授权码"
                                return@Button
                            }
                            coroutineScope.launch {
                                try {
                                    isLoading = true
                                    errorMessage = ""
                                    suspendRunCatchingNonCancel {
                                        accountRepository.loginWithCode(code.trim())
                                    }.onSuccess {
                                        onLoginSuccess()
                                    }.onFailure { e ->
                                        errorMessage = "登录失败：${e.message}"
                                        Napier.e("登录失败", e)
                                    }
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (isLoading) "登录中…" else "登录")
                    }
                }
            }

            if (errorMessage.isNotBlank()) {
                Text(
                    text = errorMessage,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
