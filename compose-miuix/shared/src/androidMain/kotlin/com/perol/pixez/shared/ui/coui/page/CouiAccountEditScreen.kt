package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.Dialog
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.data.model.AccountPersist
import com.perol.pixez.shared.platform.IllustClipboard
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.components.LocalToastSink
import com.perol.pixez.shared.ui.components.PixivApiErrorFormatter
import com.perol.pixez.shared.ui.components.runClipboardShare
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.components.ToastType
import com.perol.pixez.shared.ui.coui.component.couiDialogMaterialOrNull
import com.perol.pixez.shared.ui.coui.component.couiDialogPlacement
import com.perol.pixez.shared.ui.coui.component.couiPressFeedback
import com.perol.pixez.shared.ui.coui.component.CouiTextInput
import com.perol.pixez.shared.ui.coui.core.CouiActionRow
import com.perol.pixez.shared.ui.coui.core.CouiInfoRow
import com.perol.pixez.shared.ui.coui.core.CouiSettingsGroup
import com.perol.pixez.shared.ui.coui.core.CouiSettingsScaffold
import com.perol.pixez.shared.ui.coui.core.CouiTextButton
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.LocalAccountRepository
import com.perol.pixez.shared.ui.screens.EMAIL_REGEX
import com.perol.pixez.shared.ui.utils.openSafeUrl
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.icon.extended.Hide
import top.yukonga.miuix.kmp.icon.extended.Show
import top.yukonga.miuix.kmp.icon.MiuixIcons

/**
 * COUI 账号编辑页：账号信息、密码/邮箱修改、Refresh Token 复制与账号注销入口。
 *
 * 校验顺序、成功/失败提示、密码可见性切换与敏感字段不写入 Saved Instance State 的约定
 * 都与 MIUIX 版一致；差异只有表现层：骨架收敛到 [CouiSettingsScaffold]，只读的 Pixiv ID
 * 改用信息行，保存按钮改用胶囊文字按钮，注销二次确认沿用 COUI `Dialog`。
 */
@Composable
internal fun CouiAccountEditScreen(
    onBack: () -> Unit,
) {
    val accountRepository = LocalAccountRepository.current
    val strings = LocalStrings.current
    val toastSink = LocalToastSink.current
    val coroutineScope = rememberCoroutineScope()
    val clipboard = remember { IllustClipboard() }

    var account by remember { mutableStateOf<AccountPersist?>(null) }

    // 密码使用 remember：避免明文密码进入 Saved Instance State。
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    // 邮箱属于账号敏感信息，同样不写入 Saved Instance State。
    var email by remember { mutableStateOf("") }
    var currentPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var newPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var showDeletionConfirm by remember { mutableStateOf(false) }

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
                toastSink(ToastData(PixivApiErrorFormatter.format(e, strings), ToastType.Error))
            }
    }

    fun save() {
        if (isSaving) return
        if (currentPassword.isBlank()) {
            toastSink(ToastData(strings.accountEditInputCurrentPassword, ToastType.Error))
            return
        }
        if (email.isBlank()) {
            toastSink(ToastData(strings.accountEditInputEmail, ToastType.Error))
            return
        }
        if (!EMAIL_REGEX.matches(email)) {
            toastSink(ToastData(strings.accountEditEmailFormatError, ToastType.Error))
            return
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
                    toastSink(ToastData(strings.accountEditSaveSuccess, ToastType.Success))
                    currentPassword = ""
                    newPassword = ""
                }.onFailure { e ->
                    toastSink(
                        ToastData(
                            "${strings.accountEditSaveFailed}：${PixivApiErrorFormatter.format(e, strings)}",
                            ToastType.Error,
                        ),
                    )
                    Napier.e("保存账号信息失败", e)
                }
            } finally {
                isSaving = false
            }
        }
    }

    CouiSettingsScaffold(
        title = strings.accountEditTitle,
        onBack = onBack,
    ) {
        item(key = "account_info") {
            // 账号昵称作分组标题、Pixiv ID 作只读信息行，替代原先的禁用输入框。
            CouiSettingsGroup(title = account?.name.orEmpty()) {
                CouiInfoRow(title = strings.pixivId, value = account?.account.orEmpty())
            }
        }

        item(key = "account_form") {
            CouiSettingSection(title = strings.accountEditSectionInfo) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CouiAccountEditPasswordField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        placeholder = strings.accountEditCurrentPassword,
                        visible = currentPasswordVisible,
                        onToggleVisibility = { currentPasswordVisible = !currentPasswordVisible },
                    )
                    CouiAccountEditPasswordField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        placeholder = strings.accountEditNewPassword,
                        visible = newPasswordVisible,
                        onToggleVisibility = { newPasswordVisible = !newPasswordVisible },
                    )
                    CouiAccountEditTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = strings.accountEditEmail,
                    )
                    CouiTextButton(
                        text = if (isSaving) strings.accountEditSaving else strings.accountEditSave,
                        onClick = ::save,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving,
                    )
                }
            }
        }

        // 仅已邮箱认证账号展示 Refresh Token 复制入口。
        if (account?.isMailAuthorized == 1) {
            item(key = "account_security") {
                CouiSettingSection(title = strings.accountEditSectionSecurity) {
                    CouiActionRow(
                        title = strings.accountEditCopyRefreshToken,
                        onClick = {
                            account?.refreshToken?.let { token ->
                                runClipboardShare(
                                    success = ToastData(strings.copiedToClipboard, ToastType.Success),
                                    failurePrefix = "${strings.copy}${strings.failedSuffix}",
                                    onToast = toastSink,
                                ) { clipboard.copy(token) }.onFailure { e ->
                                    Napier.e("复制 refresh token 失败", e)
                                }
                            }
                        },
                        summary = strings.accountEditCopyRefreshTokenSummary,
                    )
                }
            }
        }

        item(key = "account_danger") {
            CouiSettingSection(title = strings.accountEditSectionDanger) {
                CouiActionRow(
                    title = strings.accountEditLogoutTitle,
                    onClick = { showDeletionConfirm = true },
                    summary = strings.accountEditLogoutSummary,
                    destructive = true,
                )
            }
        }
    }

    if (showDeletionConfirm) {
        Dialog(
            onDismissRequest = { showDeletionConfirm = false },
            visible = true,
            material = couiDialogMaterialOrNull(),
            title = strings.accountEditLogoutTitle,
            message = strings.accountEditLogoutConfirm,
            placement = couiDialogPlacement(),
            confirmAction = CouiDialogAction(
                text = strings.confirm,
                style = CouiDialogActionStyle.Primary,
                onClick = {
                    showDeletionConfirm = false
                    openSafeUrl(AppConstants.Urls.PIXIV_LEAVE_ACCOUNT, strings, onError = { message ->
                        toastSink(ToastData(message, ToastType.Error))
                    })
                },
            ),
            dismissAction = CouiDialogAction(
                text = strings.cancel,
                onClick = { showDeletionConfirm = false },
            ),
        )
    }
}

/** 普通文本输入框：统一走 CouiTextInput，与全仓其它输入框同观感。 */
@Composable
private fun CouiAccountEditTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    CouiTextInput(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        textStyle = TextStyle(color = CouiTheme.colorScheme.onSurface, fontSize = 14.sp),
        modifier = Modifier.fillMaxWidth(),
        minHeight = 48.dp,
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 14.dp),
    )
}

/** 密码输入框：CouiTextInput 承载视觉变换，可见性切换走尾部插槽。 */
@Composable
private fun CouiAccountEditPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    visible: Boolean,
    onToggleVisibility: () -> Unit,
) {
    val strings = LocalStrings.current
    val scheme = CouiTheme.colorScheme
    // 可见性按钮作为尾部插槽，命中区 32dp 与其它图标按钮统一。
    val visibilityToggle: @Composable () -> Unit = {
        val pressInteraction = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(50))
                .couiPressFeedback(pressInteraction)
                .clickable(
                    interactionSource = pressInteraction,
                    indication = null,
                    onClick = onToggleVisibility,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = rememberVectorPainter(if (visible) MiuixIcons.Hide else MiuixIcons.Show),
                contentDescription = if (visible) {
                    strings.accountEditHidePassword
                } else {
                    strings.accountEditShowPassword
                },
                colorFilter = ColorFilter.tint(scheme.onSurfaceVariant),
                modifier = Modifier.size(18.dp),
            )
        }
    }
    CouiTextInput(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        singleLine = true,
        textStyle = TextStyle(color = scheme.onSurface, fontSize = 14.sp),
        visualTransformation = if (visible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        modifier = Modifier.fillMaxWidth(),
        minHeight = 48.dp,
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 14.dp),
        trailing = visibilityToggle,
    )
}
