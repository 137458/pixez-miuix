package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.Dialog
import com.itos.coui.layout.CouiPageDefaults
import com.itos.coui.layout.Item
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.data.model.AccountPersist
import com.perol.pixez.shared.ui.components.LocalToastSink
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.components.ToastType
import com.perol.pixez.shared.ui.coui.component.CouiActionButton
import com.perol.pixez.shared.ui.coui.component.CouiTextAction
import com.perol.pixez.shared.ui.coui.component.couiDialogMaterialOrNull
import com.perol.pixez.shared.ui.coui.component.couiDialogPlacement
import com.perol.pixez.shared.ui.coui.component.couiPrimaryActionColors
import com.perol.pixez.shared.ui.coui.core.CouiItemSummary
import com.perol.pixez.shared.ui.coui.core.CouiItemTitle
import com.perol.pixez.shared.ui.coui.core.CouiPlaceholder
import com.perol.pixez.shared.ui.coui.core.CouiRowDivider
import com.perol.pixez.shared.ui.coui.core.CouiSettingsGroup
import com.perol.pixez.shared.ui.coui.core.CouiSettingsScaffold
import com.perol.pixez.shared.ui.coui.core.CouiTopBarIconButton
import com.perol.pixez.shared.ui.coui.core.CouiValueRow
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.LocalAccountRepository
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.MiuixIcons

/** 账号行头像尺寸：官方 Item 的图标槽按此尺寸让出入口。 */
private val CouiAccountAvatarSize = 40.dp

/** 带图标的分隔线缩进：行内边距 + 头像 + 图标间距，与官方 Item 的图标槽对齐。 */
private val CouiAccountDividerIndent =
    CouiPageDefaults.rowPadding + CouiAccountAvatarSize + CouiPageDefaults.iconSpacing

/**
 * COUI 账号管理页：设置页骨架 + 账号分组。
 *
 * 账号列表、当前活跃标识、切换账号、移除账号确认与添加账号全部保留；
 * 差异只有表现层：骨架收敛到 [CouiSettingsScaffold]，行改用官方 Group/Item 版式，
 * 移除确认沿用 COUI `Dialog`。
 */
@Composable
internal fun CouiAccountManageScreen(
    onBack: () -> Unit,
    onAddAccount: () -> Unit,
) {
    val accountRepository = LocalAccountRepository.current
    val strings = LocalStrings.current
    val toastSink = LocalToastSink.current
    val coroutineScope = rememberCoroutineScope()

    var accounts by remember { mutableStateOf<List<AccountPersist>>(emptyList()) }
    var currentAccount by remember { mutableStateOf<AccountPersist?>(null) }
    var accountToDelete by remember { mutableStateOf<AccountPersist?>(null) }

    fun refreshAccounts() {
        coroutineScope.launch {
            accounts = accountRepository.getAllAccounts()
            currentAccount = accountRepository.currentAccount()
        }
    }

    fun performSwitchAccount(userId: String) {
        coroutineScope.launch {
            accountRepository.switchAccount(userId)
            refreshAccounts()
            toastSink(ToastData(strings.accountSwitchSuccess, ToastType.Success))
        }
    }

    LaunchedEffect(Unit) {
        refreshAccounts()
    }

    CouiSettingsScaffold(
        title = strings.accountManageTitle,
        onBack = onBack,
        actions = {
            CouiTopBarIconButton(
                onClick = onAddAccount,
                icon = MiuixIcons.Add,
                contentDescription = strings.accountAdd,
            )
        },
    ) {
        if (accounts.isEmpty()) {
            item(key = "coui_account_empty") {
                CouiPlaceholder(
                    message = strings.accountEmpty,
                    actionText = strings.accountAdd,
                    onAction = onAddAccount,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                )
            }
        } else {
            item(key = "coui_account_list", contentType = "section") {
                CouiSettingsGroup {
                    accounts.forEachIndexed { index, account ->
                        if (index > 0) {
                            CouiRowDivider(startIndent = CouiAccountDividerIndent)
                        }
                        CouiAccountRow(
                            account = account,
                            isActive = account.userId == currentAccount?.userId,
                            onSwitch = { performSwitchAccount(account.userId) },
                            onDelete = { accountToDelete = account },
                        )
                    }
                }
            }
        }

        item(key = "coui_account_add", contentType = "action") {
            CouiSettingsGroup {
                CouiValueRow(title = strings.accountAdd, onClick = onAddAccount)
            }
        }
    }

    accountToDelete?.let { target ->
        Dialog(
            onDismissRequest = { accountToDelete = null },
            visible = true,
            material = couiDialogMaterialOrNull(),
            title = strings.accountDeleteConfirmTitle,
            message = "${strings.accountDeleteConfirmMsg}\n(${target.name} - @${target.account})",
            placement = couiDialogPlacement(),
            confirmAction = CouiDialogAction(
                text = strings.confirm,
                style = CouiDialogActionStyle.Primary,
                colors = couiPrimaryActionColors(danger = true),
                onClick = {
                    accountToDelete = null
                    coroutineScope.launch {
                        accountRepository.deleteAccount(target.userId)
                        refreshAccounts()
                    }
                },
            ),
            dismissAction = CouiDialogAction(
                text = strings.cancel,
                onClick = { accountToDelete = null },
            ),
        )
    }
}

/** 账号行：头像 + 昵称/账号，尾部活跃标识（点击整行切换）与移除按钮。 */
@Composable
private fun CouiAccountRow(
    account: AccountPersist,
    isActive: Boolean,
    onSwitch: () -> Unit,
    onDelete: () -> Unit,
) {
    val strings = LocalStrings.current
    val scheme = CouiTheme.colorScheme
    Item(
        icon = {
            PixivAsyncImage(
                model = account.userImage,
                contentDescription = account.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(CouiAccountAvatarSize)
                    .clip(CircleShape),
            )
        },
        iconSize = CouiAccountAvatarSize,
        title = { CouiItemTitle(text = account.name) },
        summary = { CouiItemSummary(text = "@${account.account} (${account.userId})") },
        // 当前账号不再切回自己，因此不给点击语义。
        onClick = if (isActive) null else onSwitch,
        trailing = {
            // 当前账号只给状态标识；其它账号给显式的「切换」动作，避免只能靠整行点击发现。
            if (isActive) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(scheme.accent.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = strings.accountCurrentActive,
                        kind = CouiTextKind.ItemSummary,
                        color = scheme.accent,
                    )
                }
            } else {
                CouiTextAction(
                    text = strings.accountSwitch,
                    onClick = onSwitch,
                    color = scheme.accent,
                )
            }
            CouiActionButton(
                icon = MiuixIcons.Delete,
                contentDescription = strings.accountDelete,
                onClick = onDelete,
            )
        },
    )
}
