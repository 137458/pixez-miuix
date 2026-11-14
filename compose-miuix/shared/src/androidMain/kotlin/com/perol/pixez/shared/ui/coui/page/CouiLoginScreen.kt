package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itos.coui.dialog.CouiDialogDefaults
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.AppInfo
import com.perol.pixez.shared.platform.IllustClipboard
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.LocalAccountRepository
import com.perol.pixez.shared.ui.components.PixivApiErrorFormatter
import com.perol.pixez.shared.ui.coui.component.CouiActionButton
import com.perol.pixez.shared.ui.coui.component.CouiCard
import com.perol.pixez.shared.ui.coui.component.CouiReadableColumn
import com.perol.pixez.shared.ui.coui.component.couiPressFeedback
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.utils.openSafeUrl
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import pixez_miuix.shared.generated.resources.Res
import pixez_miuix.shared.generated.resources.ic_pixez_logo
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Settings

/**
 * COUI 登录页：品牌区 + 快捷 OAuth 授权 + Refresh Token 登录 + 排障入口 + 条款链接。
 *
 * OAuth 跳转、Token 登录、剪贴板粘贴、错误提示与登录事件回调均与 MIUIX 版一致；
 * COUI 没有文本输入与填充按钮原语，输入框与主/次按钮按 COUI 令牌自绘胶囊，
 * 品牌区与各卡片收敛到平板可读宽度，不做下拉刷新。
 */
@Composable
internal fun CouiLoginScreen(
    onBack: () -> Unit,
    onLoginSuccess: () -> Unit,
    onNetworkSettingClick: (() -> Unit)? = null,
) {
    val accountRepository = LocalAccountRepository.current
    val strings = LocalStrings.current
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

    fun openUrl(url: String) {
        errorMessage = ""
        openSafeUrl(url, strings, onError = { message ->
            errorMessage = "${strings.openInBrowser}: $message"
        })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CouiTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CouiTheme.colorScheme.background)
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CouiActionButton(
                icon = MiuixIcons.Back,
                contentDescription = strings.back,
                onClick = onBack,
            )
            Text(
                text = strings.goLogin,
                kind = CouiTextKind.ItemTitle,
                color = CouiTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (onNetworkSettingClick != null) {
                CouiActionButton(
                    icon = MiuixIcons.Settings,
                    contentDescription = strings.loginGoNetworkSetting,
                    onClick = onNetworkSettingClick,
                )
            }
        }

        CouiReadableColumn(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // 1. 品牌区
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(CouiTheme.colorScheme.card),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(Res.drawable.ic_pixez_logo),
                        contentDescription = strings.logoContentDescription,
                        modifier = Modifier.size(64.dp),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "PixEz MIUIX",
                    kind = CouiTextKind.ItemTitle,
                    color = CouiTheme.colorScheme.onSurface,
                )
                Text(
                    text = "v${AppInfo.VERSION_NAME}",
                    kind = CouiTextKind.ItemSummary,
                    color = CouiTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(CouiTheme.colorScheme.card)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
                Text(
                    text = strings.loginPixivPrompt,
                    kind = CouiTextKind.ItemSummary,
                    color = CouiTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp),
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 2. 快捷 OAuth 授权
                CouiLoginSectionTitle(strings.loginQuick)
                CouiCard(modifier = Modifier.padding(horizontal = 12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = strings.loginWithPixiv,
                            kind = CouiTextKind.ItemTitle,
                            color = CouiTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = strings.loginPixivPrompt,
                            kind = CouiTextKind.ItemSummary,
                            color = CouiTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        CouiLoginButton(
                            text = strings.loginWithBrowser,
                            primary = true,
                            onClick = { openUrl(accountRepository.loginUrl()) },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        CouiLoginButton(
                            text = strings.loginCreateAccount,
                            primary = false,
                            onClick = { openUrl(accountRepository.loginUrl(create = true)) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Token / 凭证登录
                CouiLoginSectionTitle(strings.loginToken)
                CouiCard(modifier = Modifier.padding(horizontal = 12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = strings.loginTokenPrompt,
                            kind = CouiTextKind.ItemSummary,
                            color = CouiTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
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
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CouiLoginButton(
                                text = strings.loginPasteFromClipboard,
                                primary = false,
                                modifier = Modifier.weight(1f),
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
                            )
                            CouiLoginButton(
                                text = if (isLoading) strings.loginLoggingIn else strings.goLogin,
                                primary = true,
                                enabled = !isLoading,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (tokenInput.isBlank()) {
                                        errorMessage = strings.loginTokenLabel
                                        return@CouiLoginButton
                                    }
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
                                },
                            )
                        }
                    }
                }

                // 4. 网络诊断与排障
                if (onNetworkSettingClick != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CouiCard(modifier = Modifier.padding(horizontal = 12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onNetworkSettingClick)
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.loginTroubleTip,
                                    kind = CouiTextKind.ItemTitle,
                                    color = CouiTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = strings.loginTroubleDesc,
                                    kind = CouiTextKind.ItemSummary,
                                    color = CouiTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                            Image(
                                painter = rememberVectorPainter(MiuixIcons.Settings),
                                contentDescription = strings.loginGoNetworkSetting,
                                colorFilter = ColorFilter.tint(CouiTheme.colorScheme.accent),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }

                // 5. 错误提示
                if (errorMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    CouiCard(modifier = Modifier.padding(horizontal = 12.dp)) {
                        Text(
                            text = errorMessage,
                            kind = CouiTextKind.ItemSummary,
                            color = CouiDialogDefaults.colors(CouiTheme.isDark).destructive,
                            modifier = Modifier.padding(14.dp),
                        )
                    }
                }

                // 6. 条款与隐私
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
    }
}

/** 分组小标题，对应 MIUIX 版 `SmallTitle`。 */
@Composable
private fun CouiLoginSectionTitle(text: String) {
    Text(
        text = text,
        kind = CouiTextKind.ItemSummary,
        color = CouiTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 6.dp),
    )
}

/** 填充/次要按钮：COUI 无按钮原语，按令牌自绘胶囊。 */
@Composable
private fun CouiLoginButton(
    text: String,
    primary: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val scheme = CouiTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val background = when {
        !enabled -> scheme.container
        primary -> scheme.accent
        else -> scheme.container
    }
    val contentColor = when {
        !enabled -> scheme.onSurfaceVariant
        primary -> scheme.onAccent
        else -> scheme.onSurface
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(50))
            .background(background)
            .couiPressFeedback(interactionSource, enabled = enabled, list = false)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            kind = CouiTextKind.ItemTitle,
            color = contentColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.container)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    kind = CouiTextKind.ItemSummary,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(color = scheme.onSurface, fontSize = 14.sp),
                cursorBrush = SolidColor(scheme.accent),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
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
                    painter = rememberVectorPainter(MiuixIcons.Close),
                    contentDescription = clearDescription,
                    colorFilter = ColorFilter.tint(scheme.onSurfaceVariant),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
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
