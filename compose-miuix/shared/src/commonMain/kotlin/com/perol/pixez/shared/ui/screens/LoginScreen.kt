package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.repository.AccountRepository
import com.perol.pixez.shared.platform.openBrowser
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 登录页：通过 OAuth2 授权码完成登录。
 *
 * 流程：
 * 1. 点击“使用浏览器登录”打开 Pixiv OAuth 授权页。
 * 2. 用户授权后，从回调 URL 中提取 `code` 并粘贴到输入框。
 * 3. 点击“登录”交换 token 并持久化账号。
 */
@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onLoginSuccess: () -> Unit,
    accountRepository: AccountRepository,
) {
    var code by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "登录",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                .padding(paddingValues)
                .padding(16.dp),
        ) {
            Text(
                text = "使用 Pixiv 账号登录",
                style = MiuixTheme.textStyles.title1,
            )
            Text(
                text = "点击下方按钮打开浏览器授权，然后将回调 URL 中的 code 粘贴到输入框。",
                style = MiuixTheme.textStyles.body2,
                modifier = Modifier.padding(top = 8.dp),
            )

            TextButton(
                text = "使用浏览器登录",
                onClick = {
                    try {
                        openBrowser(accountRepository.loginUrl())
                        errorMessage = ""
                    } catch (e: Exception) {
                        errorMessage = "打开浏览器失败：${e.message}"
                        Napier.e("打开登录浏览器失败", e)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            )

            TextButton(
                text = "创建新账号",
                onClick = {
                    try {
                        openBrowser(accountRepository.loginUrl(create = true))
                        errorMessage = ""
                    } catch (e: Exception) {
                        errorMessage = "打开浏览器失败：${e.message}"
                        Napier.e("打开创建账号浏览器失败", e)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )

            TextField(
                value = code,
                onValueChange = { code = it },
                label = "授权码 (code)",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            )

            TextButton(
                text = if (isLoading) "登录中…" else "登录",
                onClick = {
                    if (code.isBlank()) {
                        errorMessage = "请输入授权码"
                        return@TextButton
                    }
                    coroutineScope.launch {
                        isLoading = true
                        errorMessage = ""
                        try {
                            accountRepository.loginWithCode(code.trim())
                            onLoginSuccess()
                        } catch (e: Exception) {
                            errorMessage = "登录失败：${e.message}"
                            Napier.e("登录失败", e)
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            )

            if (errorMessage.isNotBlank()) {
                Text(
                    text = errorMessage,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }
}
