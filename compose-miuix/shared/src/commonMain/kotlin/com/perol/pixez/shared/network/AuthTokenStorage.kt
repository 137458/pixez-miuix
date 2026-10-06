package com.perol.pixez.shared.network

import com.perol.pixez.shared.data.local.account.Account
import com.perol.pixez.shared.data.model.AccountResponse

/** 旧 Flutter 表沿用的无密码占位值，保持非空约束兼容。 */
private const val DEFAULT_PASSWORD_PLACEHOLDER = "no more"

/**
 * 账号凭据存储契约（A-21 抽接口以便测试注入替身）。
 * 实现负责加解密、缓存与持久化。
 */
interface AuthTokenStorage {
    fun getCachedAccountFast(): Account?

    suspend fun getCurrentAccount(): Account?

    suspend fun saveAccount(
        account: AccountResponse,
        password: String = DEFAULT_PASSWORD_PLACEHOLDER,
        deviceToken: String = "",
    )

    suspend fun saveAccount(account: Account)

    suspend fun getAllAccounts(): List<Account>

    suspend fun switchAccount(userId: String): Account

    suspend fun deleteAccount(userId: String)

    suspend fun updateTokens(accessToken: String, refreshToken: String)

    suspend fun updateCurrentAccount(transform: suspend (Account?) -> Account?)

    suspend fun clear()
}
