package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.AccountPersist
import com.perol.pixez.shared.ui.LocalAccountRepository
import com.perol.pixez.shared.ui.components.BlurTopBarScaffold
import com.perol.pixez.shared.ui.components.DelayedClearEffect
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.components.ToastMessage
import com.perol.pixez.shared.ui.components.ToastType
import com.perol.pixez.shared.ui.components.blurBackdropSource
import com.perol.pixez.shared.ui.components.rememberBlurBackdrop
import com.perol.pixez.shared.ui.i18n.LocalStrings
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 多账号管理界面。
 *
 * 遵循 Xiaomi HyperOS / MIUIX 规范，支持查看所有已登录账号、一键切换当前活跃账号、移除指定账号以及添加新账号。
 */
@Composable
fun AccountManageScreen(
    onBack: () -> Unit,
    onAddAccount: () -> Unit,
) {
    // 中-9：数据仓储统一经 CompositionLocal 注入（RootContent 根部 provide）。
    val accountRepository = LocalAccountRepository.current
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    var accounts by remember { mutableStateOf<List<AccountPersist>>(emptyList()) }
    var currentAccount by remember { mutableStateOf<AccountPersist?>(null) }
    var accountToDelete by remember { mutableStateOf<AccountPersist?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<ToastData?>(null) }

    // 弹窗关闭后延迟清理引用，保证退场动画期间数据完整并不泄漏引用
    DelayedClearEffect(showDeleteDialog, accountToDelete) {
        accountToDelete = null
    }

    fun refreshAccounts() {
        scope.launch {
            accounts = accountRepository.getAllAccounts()
            currentAccount = accountRepository.currentAccount()
        }
    }

    fun performSwitchAccount(userId: String) {
        scope.launch {
            accountRepository.switchAccount(userId)
            refreshAccounts()
            toastMessage = ToastData(strings.accountSwitchSuccess, ToastType.Success)
        }
    }

    LaunchedEffect(Unit) {
        refreshAccounts()
    }

    val backdrop = rememberBlurBackdrop()
    val scrollBehavior = MiuixScrollBehavior()

    BlurTopBarScaffold(
        title = strings.accountManageTitle,
        backdrop = backdrop,
        scrollBehavior = scrollBehavior,
        onBack = onBack,
        actions = {
            IconButton(onClick = onAddAccount) {
                Icon(
                    imageVector = MiuixIcons.Add,
                    contentDescription = strings.accountAdd,
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .blurBackdropSource(backdrop),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Spacer(modifier = Modifier.height(6.dp)) }

                if (accounts.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = strings.accountEmpty,
                                    style = MiuixTheme.textStyles.body1,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = onAddAccount) {
                                    Text(strings.accountAdd)
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                accounts.forEachIndexed { index, account ->
                                    val isActive = account.userId == currentAccount?.userId
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (!isActive) {
                                                    performSwitchAccount(account.userId)
                                                }
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        // 用户头像
                                        PixivAsyncImage(
                                            model = account.userImage,
                                            contentDescription = account.name,
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(CircleShape),
                                        )

                                        Spacer(modifier = Modifier.width(14.dp))

                                        // 用户名与账号信息
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = account.name,
                                                    style = MiuixTheme.textStyles.body1.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MiuixTheme.colorScheme.onSurface,
                                                )
                                                if (isActive) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.15f))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                                    ) {
                                                        Text(
                                                            text = strings.accountCurrentActive,
                                                            style = MiuixTheme.textStyles.footnote2.copy(fontWeight = FontWeight.Medium),
                                                            color = MiuixTheme.colorScheme.primary,
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "@${account.account} (${account.userId})",
                                                style = MiuixTheme.textStyles.footnote1,
                                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // 操作按钮区
                                        if (isActive) {
                                            Icon(
                                                imageVector = MiuixIcons.Ok,
                                                contentDescription = strings.accountCurrentActive,
                                                tint = MiuixTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        } else {
                                            Button(
                                                onClick = {
                                                    performSwitchAccount(account.userId)
                                                },
                                                modifier = Modifier.height(32.dp),
                                            ) {
                                                Text(
                                                    text = strings.accountSwitch,
                                                    style = MiuixTheme.textStyles.footnote1,
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        // 移除账号按钮
                                        IconButton(
                                            onClick = {
                                                accountToDelete = account
                                                showDeleteDialog = true
                                            },
                                            modifier = Modifier.size(32.dp),
                                        ) {
                                            Icon(
                                                imageVector = MiuixIcons.Delete,
                                                contentDescription = strings.accountDelete,
                                                tint = MiuixTheme.colorScheme.error.copy(alpha = 0.8f),
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    }
                                    if (index < accounts.lastIndex) {
                                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = onAddAccount,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            color = MiuixTheme.colorScheme.surfaceContainer,
                            contentColor = MiuixTheme.colorScheme.primary,
                        ),
                    ) {
                        Text(
                            text = "+ ${strings.accountAdd}",
                            style = MiuixTheme.textStyles.body2.copy(fontWeight = FontWeight.Medium),
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }

            // 移除账号确认弹窗
            val deleteTarget = accountToDelete
            OverlayDialog(
                title = strings.accountDeleteConfirmTitle,
                summary = deleteTarget?.let { "${strings.accountDeleteConfirmMsg}\n(${it.name} - @${it.account})" }
                    ?: strings.accountDeleteConfirmMsg,
                show = showDeleteDialog && deleteTarget != null,
                onDismissRequest = { showDeleteDialog = false },
            ) {
                TextButton(
                    text = strings.cancel,
                    onClick = { showDeleteDialog = false },
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(
                    text = strings.confirm,
                    onClick = {
                        val targetId = deleteTarget?.userId
                        showDeleteDialog = false
                        if (targetId != null) {
                            scope.launch {
                                accountRepository.deleteAccount(targetId)
                                refreshAccounts()
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Toast 消息
            ToastMessage(
                toast = toastMessage,
                onDismiss = { toastMessage = null },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
            )
        }
    }
}
