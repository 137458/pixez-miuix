package com.perol.pixez.shared.network

import app.cash.sqldelight.db.SqlDriver
import com.perol.pixez.shared.data.local.account.Account
import com.perol.pixez.shared.data.local.account.AccountDatabase
import com.perol.pixez.shared.data.model.AccountResponse
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 当前登录账号与 Token 的持久化存储。
 *
 * 直接复用旧 Flutter 的 account.db，字段与 SQLDelight 生成的 [Account] 保持一致。
 * 所有读写操作通过 [Mutex] 串行化，避免并发刷新 token 导致的数据竞争。
 */
class AuthTokenStorage(
    driver: SqlDriver,
) {
    private val database = AccountDatabase(driver)
    private val queries = database.accountQueries
    private val mutex = Mutex()

    /**
     * 读取当前最新账号（按数据库主键降序，取第一条）。
     *
     * 若数据库为空则返回 null；读取异常会向上抛出，避免静默掩盖数据库损坏。
     */
    suspend fun getCurrentAccount(): Account? = mutex.withLock {
        try {
            queries.selectAll().executeAsList().firstOrNull()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.e("读取当前账号失败", e)
            throw e
        }
    }

    /**
     * 保存或更新登录账号。
     *
     * @param account OAuth 登录响应。
     * @param password 原账号密码；新授权码登录无密码时传空字符串占位，保持与旧表非空约束兼容。
     * @param deviceToken 设备 token，旧 Flutter 登录后通常为空字符串。
     */
    suspend fun saveAccount(
        account: AccountResponse,
        password: String = "no more",
        deviceToken: String = "",
    ) = mutex.withLock {
        val user = account.user
        val existing = queries.selectByUserId(user.id).executeAsList().firstOrNull()
        queries.insertOrReplace(
            id = existing?.id,
            access_token = account.accessToken,
            refresh_token = account.refreshToken,
            device_token = deviceToken,
            user_id = user.id,
            user_image = user.profileImageUrls.px170x170,
            name = user.name,
            password = password,
            account = user.account,
            mail_address = user.mailAddress,
            is_premium = boolToLong(user.isPremium),
            x_restrict = user.xRestrict.toLong(),
            is_mail_authorized = boolToLong(user.isMailAuthorized),
        )
    }

    /**
     * 保存账号信息（直接使用已有的 [Account]）。
     */
    suspend fun saveAccount(account: Account) = mutex.withLock {
        queries.insertOrReplace(
            id = account.id,
            access_token = account.access_token,
            refresh_token = account.refresh_token,
            device_token = account.device_token,
            user_id = account.user_id,
            user_image = account.user_image,
            name = account.name,
            password = account.password,
            account = account.account,
            mail_address = account.mail_address,
            is_premium = account.is_premium,
            x_restrict = account.x_restrict,
            is_mail_authorized = account.is_mail_authorized,
        )
    }

    /**
     * 更新当前账号的 access_token / refresh_token，通常在 token 刷新后调用。
     *
     * @throws IllegalStateException 当本地没有登录账号时。
     */
    suspend fun updateTokens(accessToken: String, refreshToken: String) = mutex.withLock {
        val current = queries.selectAll().executeAsList().firstOrNull()
            ?: throw IllegalStateException("没有登录账号，无法更新 token")
        queries.insertOrReplace(
            id = current.id,
            access_token = accessToken,
            refresh_token = refreshToken,
            device_token = current.device_token,
            user_id = current.user_id,
            user_image = current.user_image,
            name = current.name,
            password = current.password,
            account = current.account,
            mail_address = current.mail_address,
            is_premium = current.is_premium,
            x_restrict = current.x_restrict,
            is_mail_authorized = current.is_mail_authorized,
        )
    }

    /**
     * 原子更新当前账号。
     *
     * 在 Mutex 保护下读取当前账号，调用 [transform] 生成新账号对象，再将结果写回数据库。
     * 读取、transform、写入整个流程串行化，避免并发操作导致的数据覆盖或丢失。
     * [transform] 允许挂起，因此可在其中执行网络请求后再返回更新后的账号。
     *
     * @param transform 接收当前账号（未登录时为 null），返回更新后的账号；返回 null 表示不写入。
     */
    suspend fun updateCurrentAccount(transform: suspend (Account?) -> Account?) = mutex.withLock {
        val current = queries.selectAll().executeAsList().firstOrNull()
        val updated = transform(current)
        if (updated != null) {
            queries.insertOrReplace(
                id = updated.id,
                access_token = updated.access_token,
                refresh_token = updated.refresh_token,
                device_token = updated.device_token,
                user_id = updated.user_id,
                user_image = updated.user_image,
                name = updated.name,
                password = updated.password,
                account = updated.account,
                mail_address = updated.mail_address,
                is_premium = updated.is_premium,
                x_restrict = updated.x_restrict,
                is_mail_authorized = updated.is_mail_authorized,
            )
        }
    }

    /**
     * 清空所有账号信息，相当于登出。
     */
    suspend fun clear() = mutex.withLock {
        queries.deleteAll()
    }

    private fun boolToLong(value: Boolean): Long = if (value) 1L else 0L
}
