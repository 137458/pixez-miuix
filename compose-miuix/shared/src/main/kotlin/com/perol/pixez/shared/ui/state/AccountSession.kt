package com.perol.pixez.shared.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.perol.pixez.shared.data.model.AccountPersist
import com.perol.pixez.shared.data.model.isLoggedIn
import com.perol.pixez.shared.ui.LocalAccountRepository
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel

/**
 * 当前账号身份：[user] 是最近一次读到的账号（未登录为 null），[isLoggedIn] 为 null 表示尚未读取完成。
 *
 * 拆成两个值是因为页面用途不同：判断「该拉首屏了吗」需要三态（未知 / 已登录 / 未登录），
 * 而切号作废只需比较 [user] 身份。
 */
internal data class AccountSession(val user: AccountPersist?, val isLoggedIn: Boolean?)

/**
 * 订阅当前账号身份：首次进入读一次 [com.perol.pixez.shared.data.repository.AccountRepository.currentAccount]，
 * 之后跟随 [com.perol.pixez.shared.data.repository.AccountRepository.loginEventFlow] 刷新。
 *
 * 各列表页此前逐字复制这段「读当前账号 + 收集登录事件」的样板（COUI 与 MIUIX 两套页面各一份），
 * 一旦有人漏包 suspendRunCatchingNonCancel 或忘了跟随登录事件，就会出现切号后仍显示上个账号数据的
 * 幽灵列表。这里收敛成单一实现：
 * - 读取走 [suspendRunCatchingNonCancel]：账号存储异常（如密钥丢失）不会把异常抛给 Recomposer 令页面崩溃；
 * - 不落 rememberSaveable：进程恢复后必须重新读取，否则会沿用被杀进程前的过期账号。
 */
@Composable
internal fun rememberAccountSession(): AccountSession {
    val accountRepository = LocalAccountRepository.current
    var user by remember(accountRepository) { mutableStateOf<AccountPersist?>(null) }
    var isLoggedIn by remember(accountRepository) { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(accountRepository) {
        suspend fun refresh() {
            user = suspendRunCatchingNonCancel { accountRepository.currentAccount() }.getOrNull()
            isLoggedIn = user.isLoggedIn
        }
        refresh()
        accountRepository.loginEventFlow.collect { refresh() }
    }

    return AccountSession(user = user, isLoggedIn = isLoggedIn)
}
