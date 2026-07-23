package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.AccountPersist
import com.perol.pixez.shared.data.repository.AccountRepository
import com.perol.pixez.shared.platform.IllustClipboard
import com.perol.pixez.shared.platform.openBrowser
import com.perol.pixez.shared.ui.components.ToastMessage
import com.perol.pixez.shared.ui.utils.runCatchingNonCancel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*

/**
 * 简单邮箱格式校验正则，用于保存前的基础格式检查。
 */
private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

/**
 * 账号信息编辑页。
 *
 * 支持查看当前账号、修改邮箱与密码、复制 Refresh Token、跳转账号注销页面。
 *
 * @param onBack 返回上一级页面。
 * @param accountRepository 账号仓库，用于读取与编辑账号信息。
 */
@Composable
fun AccountEditScreen(
    onBack: () -> Unit,
    accountRepository: AccountRepository,
) {
    val coroutineScope = rememberCoroutineScope()
    val clipboard = remember { IllustClipboard() }

    // 当前账号信息，进入页面时从仓库异步加载。
    var account by remember { mutableStateOf<AccountPersist?>(null) }

    // 表单输入状态：当前密码、新密码、邮箱。
    // 密码使用 remember 而非 rememberSaveable，避免明文密码进入 Saved Instance State。
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    // 邮箱使用 remember 而非 rememberSaveable：邮箱属于账号敏感信息，不写入 Saved Instance State；
    // 配置变更后页面会重新加载当前账号并回填邮箱，避免信息泄露。
    var email by remember { mutableStateOf("") }

    // 密码框显示/隐藏切换状态。
    var currentPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var newPasswordVisible by rememberSaveable { mutableStateOf(false) }

    // 保存操作加载态，用于防止重复提交；使用 remember，配置变更后允许重新提交。
    var isSaving by remember { mutableStateOf(false) }

    // 账号注销二次确认栏显示状态。
    var showDeletionConfirm by rememberSaveable { mutableStateOf(false) }

    // Toast 提示文本，为 null 时不显示。
    var toastMessage by rememberSaveable { mutableStateOf<String?>(null) }

    // 页面进入时加载当前账号，并根据邮箱认证状态预填充当前密码：
    // 仅非邮箱认证账号才预填充已保存密码，与 Flutter 原版逻辑保持一致。
    LaunchedEffect(accountRepository) {
        runCatchingNonCancel { accountRepository.currentAccount() }
            .onSuccess { loaded ->
                account = loaded
                loaded?.let {
                    email = it.mailAddress
                    if (it.isMailAuthorized != 1) {
                        currentPassword = it.passWord
                    }
                }
            }
            .onFailure { e ->
                Napier.e("加载账号信息失败", e)
                toastMessage = "加载账号失败：${e.message}"
            }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "账号信息",
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    AccountInfoSection(
                        account = account,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }

                item {
                    SmallTitle(
                        text = "修改信息",
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                    )
                }

                item {
                    TextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = "当前密码",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        singleLine = true,
                        visualTransformation = if (currentPasswordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            PasswordVisibilityToggle(
                                visible = currentPasswordVisible,
                                onToggle = { currentPasswordVisible = !currentPasswordVisible },
                            )
                        },
                    )
                }

                item {
                    TextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = "新密码",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        singleLine = true,
                        visualTransformation = if (newPasswordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            PasswordVisibilityToggle(
                                visible = newPasswordVisible,
                                onToggle = { newPasswordVisible = !newPasswordVisible },
                            )
                        },
                    )
                }

                item {
                    TextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "邮箱",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        singleLine = true,
                    )
                }

                item {
                    Button(
                        onClick = {
                            // 校验阶段：若已有保存任务进行中，直接忽略本次点击，防止重复提交。
                            if (isSaving) return@Button

                            // 校验阶段：当前密码与邮箱不能为空，邮箱格式需基本合法。
                            if (currentPassword.isBlank()) {
                                toastMessage = "请输入当前密码"
                                return@Button
                            }
                            if (email.isBlank()) {
                                toastMessage = "请输入邮箱"
                                return@Button
                            }
                            if (!EMAIL_REGEX.matches(email)) {
                                toastMessage = "邮箱格式错误"
                                return@Button
                            }

                            coroutineScope.launch {
                                try {
                                    // 提交阶段：启用加载态，调用仓库接口提交账号修改。
                                    isSaving = true
                                    runCatchingNonCancel {
                                        accountRepository.editAccount(
                                            currentPassword = currentPassword,
                                            newPassword = newPassword.takeIf { it.isNotBlank() },
                                            newMailAddress = email.takeIf { it.isNotBlank() },
                                        )
                                    }.onSuccess {
                                        // 成功处理阶段：提示用户并清空密码框，降低误操作重复提交风险。
                                        toastMessage = "保存成功"
                                        currentPassword = ""
                                        newPassword = ""
                                    }.onFailure { e ->
                                        // 失败处理阶段：向用户展示错误信息并记录日志。
                                        toastMessage = "保存失败：${e.message}"
                                        Napier.e("保存账号信息失败", e)
                                    }
                                } finally {
                                    // 清理阶段：无论成功或失败，都重置加载态以允许再次提交。
                                    isSaving = false
                                }
                            }
                        },
                        enabled = !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Text(text = if (isSaving) "保存中…" else "保存")
                    }
                }

                // 仅已邮箱认证账号展示 Refresh Token 复制入口。
                if (account?.isMailAuthorized == 1) {
                    item {
                        SmallTitle(
                            text = "安全",
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                        )
                    }
                    item {
                        BasicComponent(
                            title = "复制 Refresh Token",
                            summary = "将当前账号的 refresh token 复制到剪贴板",
                            onClick = {
                                account?.refreshToken?.let { token ->
                                    try {
                                        clipboard.copy(token)
                                        toastMessage = "已复制到剪贴板"
                                    } catch (e: Exception) {
                                        toastMessage = "复制失败：${e.message}"
                                        Napier.e("复制 refresh token 失败", e)
                                    }
                                }
                            },
                        )
                    }
                }

                item {
                    SmallTitle(
                        text = "危险操作",
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                    )
                }
                item {
                    BasicComponent(
                        title = "账号注销",
                        summary = "跳转到 Pixiv 账号注销页面",
                        onClick = { showDeletionConfirm = true },
                    )
                }
            }

            // 账号注销二次确认栏：确认后使用系统浏览器打开注销页。
            if (showDeletionConfirm) {
                DeletionConfirmBar(
                    onConfirm = {
                        showDeletionConfirm = false
                        try {
                            openBrowser("https://www.pixiv.net/leave_pixiv.php")
                        } catch (e: Exception) {
                            toastMessage = "打开浏览器失败：${e.message}"
                            Napier.e("打开账号注销页面失败", e)
                        }
                    },
                    onCancel = { showDeletionConfirm = false },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }

            ToastMessage(
                message = toastMessage,
                onDismiss = { toastMessage = null },
            )
        }
    }
}

/**
 * 账号信息只读展示：账号名与 Pixiv ID。
 */
@Composable
private fun AccountInfoSection(
    account: AccountPersist?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = account?.name ?: "",
            style = MiuixTheme.textStyles.title3,
        )
        TextField(
            value = account?.account ?: "",
            onValueChange = { },
            label = "Pixiv ID",
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            readOnly = true,
        )
    }
}

/**
 * 密码可见性切换按钮。
 */
@Composable
private fun PasswordVisibilityToggle(
    visible: Boolean,
    onToggle: () -> Unit,
) {
    IconButton(onClick = onToggle) {
        Icon(
            imageVector = if (visible) MiuixIcons.Hide else MiuixIcons.Show,
            contentDescription = if (visible) "隐藏密码" else "显示密码",
        )
    }
}

/**
 * 账号注销二次确认栏：底部显示，避免引入 AlertDialog。
 */
@Composable
private fun DeletionConfirmBar(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "确定跳转到账号注销页面？",
            modifier = Modifier.weight(1f),
            style = MiuixTheme.textStyles.body1,
        )
        TextButton(
            text = "取消",
            onClick = onCancel,
        )
        Button(
            onClick = onConfirm,
        ) {
            Text("确定")
        }
    }
}
