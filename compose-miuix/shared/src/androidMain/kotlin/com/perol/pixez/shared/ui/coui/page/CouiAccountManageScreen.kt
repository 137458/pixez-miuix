package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.Dialog
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.data.model.AccountPersist
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.components.LocalToastSink
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.components.ToastType
import com.perol.pixez.shared.ui.coui.component.CouiActionButton
import com.perol.pixez.shared.ui.coui.component.CouiCard
import com.perol.pixez.shared.ui.coui.component.couiDialogMaterialOrNull
import com.perol.pixez.shared.ui.coui.component.couiDialogPlacement
import com.perol.pixez.shared.ui.coui.component.CouiFixedTopBar
import com.perol.pixez.shared.ui.coui.component.couiPressFeedback
import com.perol.pixez.shared.ui.coui.component.couiPrimaryActionColors
import com.perol.pixez.shared.ui.coui.component.CouiReadableColumn
import com.perol.pixez.shared.ui.coui.component.CouiTextAction
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.LocalAccountRepository
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.MiuixIcons

/**
 * COUI 账号管理页：固定顶栏 + 账号卡片。
 *
 * 账号列表、当前活跃标识、切换账号、移除账号确认与添加账号全部保留；
 * 差异只有皮肤层：切换/移除改用 COUI 文本动作与图标按钮，
 * 移除确认改用 COUI `Dialog`。
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CouiTheme.colorScheme.background),
    ) {
        CouiFixedTopBar(
            title = strings.accountManageTitle,
            onBack = onBack,
        ) {
            CouiActionButton(
                icon = MiuixIcons.Add,
                contentDescription = strings.accountAdd,
                onClick = onAddAccount,
            )
        }

        CouiReadableColumn(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    top = 8.dp,
                    end = 12.dp,
                    bottom = LocalBottomBarContentPadding.current,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                overscrollEffect = rememberCouiOverscrollEffect(),
            ) {
                item(key = "coui_account_list", contentType = "section") {
                    CouiCard(modifier = Modifier.padding(top = 4.dp)) {
                        if (accounts.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    text = strings.accountEmpty,
                                    kind = CouiTextKind.ItemSummary,
                                    color = CouiTheme.colorScheme.onSurfaceVariant,
                                )
                                CouiTextAction(
                                    text = strings.accountAdd,
                                    color = CouiTheme.colorScheme.accent,
                                    onClick = onAddAccount,
                                )
                            }
                        } else {
                            accounts.forEach { account ->
                                val isActive = account.userId == currentAccount?.userId
                                CouiAccountRow(
                                    account = account,
                                    isActive = isActive,
                                    onSwitch = { performSwitchAccount(account.userId) },
                                    onDelete = { accountToDelete = account },
                                )
                            }
                        }
                    }
                }

                item(key = "coui_account_add", contentType = "action") {
                    CouiCard {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clickable(onClick = onAddAccount),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "+ ${strings.accountAdd}",
                                kind = CouiTextKind.ItemTitle,
                                color = CouiTheme.colorScheme.accent,
                            )
                        }
                    }
                }
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

/** 账号行：头像 + 昵称/账号，尾部活跃标识或切换动作，以及移除按钮。 */
@Composable
private fun CouiAccountRow(
    account: AccountPersist,
    isActive: Boolean,
    onSwitch: () -> Unit,
    onDelete: () -> Unit,
) {
    val strings = LocalStrings.current
    val scheme = CouiTheme.colorScheme
    val pressInteraction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .couiPressFeedback(pressInteraction, enabled = !isActive)
            .clickable(
                interactionSource = pressInteraction,
                indication = null,
                enabled = !isActive,
                onClick = onSwitch,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PixivAsyncImage(
            model = account.userImage,
            contentDescription = account.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = account.name,
                kind = CouiTextKind.ItemTitle,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "@${account.account} (${account.userId})",
                kind = CouiTextKind.ItemSummary,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
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
                color = scheme.accent,
                onClick = onSwitch,
            )
        }
        CouiActionButton(
            icon = MiuixIcons.Delete,
            contentDescription = strings.accountDelete,
            onClick = onDelete,
        )
    }
}
