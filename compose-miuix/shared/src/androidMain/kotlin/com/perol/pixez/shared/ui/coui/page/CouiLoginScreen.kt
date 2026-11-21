package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itos.coui.dialog.CouiDialogDefaults
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.AppInfo
import com.perol.pixez.shared.platform.IllustClipboard
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.components.PixivApiErrorFormatter
import com.perol.pixez.shared.ui.coui.component.couiPressFeedback
import com.perol.pixez.shared.ui.coui.component.CouiTextInput
import com.perol.pixez.shared.ui.coui.core.CouiItemSummary
import com.perol.pixez.shared.ui.coui.core.CouiItemTitle
import com.perol.pixez.shared.ui.coui.core.CouiScrollableScaffold
import com.perol.pixez.shared.ui.coui.core.CouiSettingsGroup
import com.perol.pixez.shared.ui.coui.core.CouiTextButton
import com.perol.pixez.shared.ui.coui.core.CouiTopBarIconButton
import com.perol.pixez.shared.ui.coui.core.CouiValueRow
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.LocalAccountRepository
import com.perol.pixez.shared.ui.utils.openSafeUrl
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import androidx.compose.ui.res.painterResource
import com.perol.pixez.shared.R
import com.perol.pixez.shared.ui.coui.icon.CouiIcons

/**
 * COUI 登录页：品牌区 + 快捷 OAuth 授权 + Refresh Token 登录 + 排障入口 + 条款链接。
 *
 * OAuth 跳转、Token 登录、剪贴板粘贴、错误提示与登录事件回调均与 MIUIX 版一致；
 * 差异只有表现层：骨架收敛到 [CouiScrollableScaffold]，输入框保留 [CouiTextInput]，
 * 按钮改用 [CouiTextButton]，分组统一用官方 Group 版式的 [CouiSettingsGroup]。
 */
@Composable
internal fun CouiLoginScreen(
    onBack: () -> Unit,
    onLoginSuccess: () -> Unit,
    onNetworkSettingClick: (() -> Unit)? = null,
) {
    val accountRepository = LocalAccountRepository.current
    val strings = LocalStrings.current
    // 长期凭证不进 savedInstanceState：token 落盘会随系统进程快照一起被读取。
    var tokenInput by remember { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf("") }
    // 加载态不持久化：进程恢复后协程不会恢复，存了就会让登录按钮永久禁用。
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val clipboard = remember { IllustClipboard() }

    LaunchedEffect(accountRepository) {
        accountRepository.loginEventFlow.collect {
            onLoginSuccess()
        }
    }

    fun openUrl(url: String) {
        errorMessage = ""
        openSafeUrl(url, strings, onError = { message ->
            errorMessage = "${strings.openInBrowser}: $message"
        })
    }

    CouiScrollableScaffold(
        title = strings.goLogin,
        onBack = onBack,
        actions = {
            if (onNetworkSettingClick != null) {
                CouiTopBarIconButton(
                    onClick = onNetworkSettingClick,
                    icon = CouiIcons.Settings,
                    contentDescription = strings.loginGoNetworkSetting,
                )
            }
        },
    ) {
        // 1. 品牌区
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(CouiTheme.colorScheme.card),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_pixez_logo),
                    contentDescription = strings.logoContentDescription,
                    modifier = Modifier.size(64.dp),
                )
            }
            CouiItemTitle(text = "PixEz MIUIX")
            Text(
                text = "v${AppInfo.VERSION_NAME}",
                kind = CouiTextKind.ItemSummary,
                color = CouiTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(CouiTheme.colorScheme.card)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
            Text(
                text = strings.loginPixivPrompt,
                kind = CouiTextKind.ItemSummary,
                color = CouiTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
        }

        // 2. 快捷 OAuth 授权
        CouiSettingsGroup(title = strings.loginQuick) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CouiItemTitle(text = strings.loginWithPixiv)
                CouiItemSummary(text = strings.loginPixivPrompt)
                CouiTextButton(
                    text = strings.loginWithBrowser,
                    onClick = { openUrl(accountRepository.loginUrl()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                )
                CouiTextButton(
                    text = strings.loginCreateAccount,
                    onClick = { openUrl(accountRepository.loginUrl(create = true)) },
                    modifier = Modifier.fillMaxWidth(),
                    filled = false,
                )
            }
        }

        // 3. Token / 凭证登录
        CouiSettingsGroup(title = strings.loginToken) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CouiItemSummary(text = strings.loginTokenPrompt)
                CouiLoginTokenField(
                    value = tokenInput,
                    onValueChange = {
                        tokenInput = it
                        if (errorMessage.isNotEmpty()) errorMessage = ""
                    },
                    placeholder = strings.loginInputTokenHint,
                    clearDescription = strings.actionClear,
                    onClear = { tokenInput = "" },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CouiTextButton(
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
                        filled = false,
                    )
                    CouiTextButton(
                        text = if (isLoading) strings.loginLoggingIn else strings.goLogin,
                        onClick = {
                            if (tokenInput.isBlank()) {
                                errorMessage = strings.loginTokenLabel
                            } else {
                                coroutineScope.launch {
                                    try {
                                        isLoading = true
                                        errorMessage = ""
                                        suspendRunCatchingNonCancel {
                                            accountRepository.login(tokenInput.trim())
                                        }.onFailure { e ->
                                            errorMessage = PixivApiErrorFormatter.format(e, strings)
                                            Napier.e("登录失败", e)
                                        }
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading,
                    )
                }
            }
        }

        // 4. 网络诊断与排障
        if (onNetworkSettingClick != null) {
            CouiSettingsGroup {
                CouiValueRow(
                    title = strings.loginTroubleTip,
                    summary = strings.loginTroubleDesc,
                    onClick = onNetworkSettingClick,
                )
            }
        }

        // 5. 错误提示
        if (errorMessage.isNotBlank()) {
            CouiSettingsGroup {
                Text(
                    text = errorMessage,
                    kind = CouiTextKind.ItemSummary,
                    color = CouiDialogDefaults.colors(CouiTheme.isDark).destructive,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                )
            }
        }

        // 6. 条款与隐私
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${strings.loginTermsAndPrivacy} ",
                kind = CouiTextKind.ItemSummary,
                color = CouiTheme.colorScheme.onSurfaceVariant,
            )
            CouiLoginLink(
                text = strings.loginTerms,
                onClick = { openUrl(AppConstants.Urls.PIXIV_TERMS) },
            )
            Text(
                text = " · ",
                kind = CouiTextKind.ItemSummary,
                color = CouiTheme.colorScheme.onSurfaceVariant,
            )
            CouiLoginLink(
                text = strings.loginPrivacy,
                onClick = { openUrl(AppConstants.Urls.PIXIV_PRIVACY) },
            )
        }
    }
}

/** Token 输入框：容器胶囊 + 单行输入 + 清空按钮。 */
@Composable
private fun CouiLoginTokenField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    clearDescription: String,
    onClear: () -> Unit,
) {
    val scheme = CouiTheme.colorScheme
    // 有输入才给清空按钮，避免空态下留一块不可点区域。
    val trailingSlot: (@Composable () -> Unit)? = if (value.isEmpty()) {
        null
    } else {
        {
            val pressInteraction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(50))
                    .couiPressFeedback(pressInteraction)
                    .clickable(
                        interactionSource = pressInteraction,
                        indication = null,
                        onClick = onClear,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = rememberVectorPainter(CouiIcons.Close),
                    contentDescription = clearDescription,
                    colorFilter = ColorFilter.tint(scheme.onSurfaceVariant),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
    CouiTextInput(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        textStyle = TextStyle(color = scheme.onSurface, fontSize = 14.sp),
        modifier = Modifier.fillMaxWidth(),
        minHeight = 48.dp,
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 14.dp),
        trailing = trailingSlot,
    )
}

/** 条款链接：强调色文字 + 按压反馈。 */
@Composable
private fun CouiLoginLink(
    text: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Text(
        text = text,
        kind = CouiTextKind.ItemSummary,
        color = CouiTheme.colorScheme.accent,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .couiPressFeedback(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 2.dp, vertical = 2.dp),
    )
}
