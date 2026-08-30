package com.perol.pixez.shared.ui.screens

import com.perol.pixez.shared.ui.components.FrostedTopAppBar

import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.perol.pixez.shared.ui.components.LocalBackdrop
import com.perol.pixez.shared.ui.components.topAppBarBlur
import com.perol.pixez.shared.ui.components.blurBackdropSource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.AccountPersist
import com.perol.pixez.shared.data.repository.AccountRepository
import com.perol.pixez.shared.platform.IllustClipboard
import com.perol.pixez.shared.platform.openBrowser
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.components.ToastMessage
import com.perol.pixez.shared.ui.utils.suspendRunCatchingNonCancel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import com.perol.pixez.shared.ui.i18n.LocalStrings

import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

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

    // 操作中的 loading 状态。
    var isSaving by remember { mutableStateOf(false) }

    // 是否显示注销确认对话框。
    var showDeletionConfirm by remember { mutableStateOf(false) }

    // 统一的 Toast 提示文本。
    var toastMessage by remember { mutableStateOf<String?>(null) }

    val strings = LocalStrings.current

    // 进入页面时异步加载当前账号。
    LaunchedEffect(Unit) {
        suspendRunCatchingNonCancel {
            accountRepository.currentAccount()
        }
            .onSuccess { active ->
                account = active
                email = active?.mailAddress.orEmpty()
            }
            .onFailure { e ->
                Napier.e("加载账号信息失败", e)
                toastMessage = "${strings.loadFailed}: ${e.message}"
            }
    }

    val scrollBehavior = top.yukonga.miuix.kmp.basic.MiuixScrollBehavior()
    val backdrop = rememberLayerBackdrop()
    val colorScheme = MiuixTheme.colorScheme

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            FrostedTopAppBar(
                title = strings.accountEditTitle,
                scrollBehavior = scrollBehavior,
                backdrop = backdrop,
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.surface)
                .layerBackdrop(backdrop),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                contentPadding = paddingValues,
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = AppConstants.Layout.TABLET_CONTENT_MAX_WIDTH_DP.dp)
                    .fillMaxWidth()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
            ) {
                item {
                    top.yukonga.miuix.kmp.basic.Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                        AccountInfoSection(
                            account = account,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }

                item {
                    SmallTitle(
                        text = strings.accountEditSectionInfo,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                    )
                    top.yukonga.miuix.kmp.basic.Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TextField(
                                value = currentPassword,
                                onValueChange = { currentPassword = it },
                                label = strings.accountEditCurrentPassword,
                                modifier = Modifier.fillMaxWidth(),
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
                            TextField(
                                value = newPassword,
                                onValueChange = { newPassword = it },
                                label = strings.accountEditNewPassword,
                                modifier = Modifier.fillMaxWidth(),
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
                            TextField(
                                value = email,
                                onValueChange = { email = it },
                                label = strings.accountEditEmail,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )
                            Button(
                                onClick = {
                                    if (isSaving) return@Button
                                    if (currentPassword.isBlank()) {
                                        toastMessage = strings.accountEditInputCurrentPassword
                                        return@Button
                                    }
                                    if (email.isBlank()) {
                                        toastMessage = strings.accountEditInputEmail
                                        return@Button
                                    }
                                    if (!EMAIL_REGEX.matches(email)) {
                                        toastMessage = strings.accountEditEmailFormatError
                                        return@Button
                                    }

                                    coroutineScope.launch {
                                        try {
                                            isSaving = true
                                            suspendRunCatchingNonCancel {
                                                accountRepository.editAccount(
                                                    currentPassword = currentPassword,
                                                    newPassword = newPassword.takeIf { it.isNotBlank() },
                                                    newMailAddress = email.takeIf { it.isNotBlank() },
                                                )
                                            }.onSuccess {
                                                toastMessage = strings.accountEditSaveSuccess
                                                currentPassword = ""
                                                newPassword = ""
                                            }.onFailure { e ->
                                                toastMessage = "${strings.accountEditSaveFailed}：${e.message}"
                                                Napier.e("保存账号信息失败", e)
                                            }
                                        } finally {
                                            isSaving = false
                                        }
                                    }
                                },
                                enabled = !isSaving,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(text = if (isSaving) strings.accountEditSaving else strings.accountEditSave)
                            }
                        }
                    }
                }

                // 仅已邮箱认证账号展示 Refresh Token 复制入口。
                if (account?.isMailAuthorized == 1) {
                    item {
                        SmallTitle(
                            text = strings.accountEditSectionSecurity,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                        )
                        top.yukonga.miuix.kmp.basic.Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            BasicComponent(
                                title = strings.accountEditCopyRefreshToken,
                                summary = strings.accountEditCopyRefreshTokenSummary,
                                onClick = {
                                    account?.refreshToken?.let { token ->
                                        try {
                                            clipboard.copy(token)
                                            toastMessage = strings.copiedToClipboard
                                        } catch (e: Exception) {
                                            toastMessage = "${strings.copy}${strings.loadFailed}：${e.message}"
                                            Napier.e("复制 refresh token 失败", e)
                                        }
                                    }
                                },
                            )
                        }
                    }
                }

                item {
                    SmallTitle(
                        text = strings.accountEditSectionDanger,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                    )
                    top.yukonga.miuix.kmp.basic.Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        BasicComponent(
                            title = strings.accountEditLogoutTitle,
                            summary = strings.accountEditLogoutSummary,
                            onClick = { showDeletionConfirm = true },
                        )
                    }
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
                            toastMessage = "${strings.loadFailed}：${e.message}"
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
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    IconButton(onClick = onToggle) {
        Icon(
            imageVector = if (visible) MiuixIcons.Hide else MiuixIcons.Show,
            contentDescription = if (visible) strings.accountEditHidePassword else strings.accountEditShowPassword,
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
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = strings.accountEditLogoutConfirm,
            modifier = Modifier.weight(1f),
            style = MiuixTheme.textStyles.body1,
        )
        TextButton(
            text = strings.cancel,
            onClick = onCancel,
        )
        Button(
            onClick = onConfirm,
        ) {
            Text(strings.confirm)
        }
    }
}
