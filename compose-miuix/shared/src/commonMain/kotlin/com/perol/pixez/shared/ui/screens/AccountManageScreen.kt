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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.perol.pixez.shared.data.model.AccountPersist
import com.perol.pixez.shared.data.repository.AccountRepository
import com.perol.pixez.shared.ui.components.BlurredBar
import com.perol.pixez.shared.ui.components.LocalBackdrop
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.components.ToastMessage
import com.perol.pixez.shared.ui.components.blurBackdropSource
import com.perol.pixez.shared.ui.components.rememberBlurBackdrop
import com.perol.pixez.shared.ui.i18n.LocalStrings
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
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
    accountRepository: AccountRepository,
    onBack: () -> Unit,
    onAddAccount: () -> Unit,
) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    var accounts by remember { mutableStateOf<List<AccountPersist>>(emptyList()) }
    var currentAccount by remember { mutableStateOf<AccountPersist?>(null) }
    var accountToDelete by remember { mutableStateOf<AccountPersist?>(null) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    fun refreshAccounts() {
        scope.launch {
            accounts = accountRepository.getAllAccounts()
            currentAccount = accountRepository.currentAccount()
        }
    }

    LaunchedEffect(Unit) {
        refreshAccounts()
    }

    val backdrop = LocalBackdrop.current ?: rememberBlurBackdrop()

    Scaffold(
        topBar = {
            BlurredBar(backdrop = backdrop) {
                TopAppBar(
                    title = strings.accountManageTitle,
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = strings.back,
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onAddAccount) {
                            Icon(
                                imageVector = MiuixIcons.Add,
                                contentDescription = strings.accountAdd,
                            )
                        }
                    },
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
                    .padding(horizontal = 16.dp),
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
                                    fontSize = 15.sp,
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
                                                    scope.launch {
                                                        accountRepository.switchAccount(account.userId)
                                                        refreshAccounts()
                                                        toastMessage = strings.accountSwitchSuccess
                                                    }
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
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.SemiBold,
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
                                                            fontSize = 10.sp,
                                                            color = MiuixTheme.colorScheme.primary,
                                                            fontWeight = FontWeight.Medium,
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "@${account.account} (${account.userId})",
                                                fontSize = 12.sp,
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
                                                    scope.launch {
                                                        accountRepository.switchAccount(account.userId)
                                                        refreshAccounts()
                                                        toastMessage = strings.accountSwitchSuccess
                                                    }
                                                },
                                                modifier = Modifier.height(32.dp),
                                            ) {
                                                Text(
                                                    text = strings.accountSwitch,
                                                    fontSize = 12.sp,
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        // 移除账号按钮
                                        IconButton(
                                            onClick = { accountToDelete = account },
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
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp)
                                                .height(0.5.dp)
                                                .background(MiuixTheme.colorScheme.dividerLine),
                                        )
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
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }

            // 移除账号确认弹窗
            val deleteTarget = accountToDelete
            if (deleteTarget != null) {
                OverlayDialog(
                    title = strings.accountDeleteConfirmTitle,
                    summary = "${strings.accountDeleteConfirmMsg}\n(${deleteTarget.name} - @${deleteTarget.account})",
                    show = true,
                    onDismissRequest = { accountToDelete = null },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        TextButton(
                            text = strings.cancel,
                            onClick = { accountToDelete = null },
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            text = strings.confirm,
                            onClick = {
                                val targetId = deleteTarget.userId
                                accountToDelete = null
                                scope.launch {
                                    accountRepository.deleteAccount(targetId)
                                    refreshAccounts()
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // Toast 消息
            ToastMessage(
                message = toastMessage,
                onDismiss = { toastMessage = null },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
            )
        }
    }
}
