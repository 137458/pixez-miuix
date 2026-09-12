package com.perol.pixez.shared.network

import app.cash.sqldelight.db.SqlDriver
import com.perol.pixez.shared.data.local.account.Account
import com.perol.pixez.shared.data.local.account.AccountDatabase
import com.perol.pixez.shared.data.model.AccountResponse
import com.perol.pixez.shared.platform.PlatformTokenCipher
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * 当前登录账号与 Token 的持久化存储。
 *
 * 直接复用旧 Flutter 的 account.db，字段与 SQLDelight 生成的 [Account] 保持一致。
 * 所有读写操作通过 [Mutex] 串行化，避免并发刷新 token 导致的数据竞争。
 * 并在底层接入 [PlatformTokenCipher]，对持久化到 SQLite 的 Token 与密码实施硬件级加密保护。
 */
class AuthTokenStorage(
    driver: SqlDriver,
    private val getActiveUserId: (() -> String?)? = null,
    private val setActiveUserId: ((String?) -> Unit)? = null,
) {
    private val database = AccountDatabase(driver)
    private val queries = database.accountQueries
    private val mutex = Mutex()

    @kotlin.concurrent.Volatile
    private var cachedAccount: Account? = null
    @kotlin.concurrent.Volatile
    private var isCacheInitialized = false

    /**
     * 快速同步获取已缓存的账号信息，若尚未初始化则返回 null。
     */
    fun getCachedAccountFast(): Account? = if (isCacheInitialized) cachedAccount else null

    /**
     * 读取当前活跃账号。
     *
     * 优先匹配当前保存的 activeUserId，若不存在则回退至第一条已登录账号。
     * 内部使用内存缓存，优先从缓存返回，避免首屏频繁查询 SQLite 阻塞。
     * 若数据库为空则返回 null；读取异常会向上抛出，避免静默掩盖数据库损坏。
     */
    suspend fun getCurrentAccount(): Account? = withContext(Dispatchers.Default) {
        if (isCacheInitialized) return@withContext cachedAccount
        mutex.withLock {
            if (isCacheInitialized) return@withLock cachedAccount
            try {
                val activeUid = getActiveUserId?.invoke()
                val rawAcc = if (!activeUid.isNullOrBlank()) {
                    queries.selectByUserId(activeUid).executeAsList().firstOrNull()
                        ?: queries.selectAll().executeAsList().firstOrNull()
                } else {
                    queries.selectAll().executeAsList().firstOrNull()
                }
                val acc = rawAcc?.decrypted()
                if (acc != null && activeUid != acc.user_id) {
                    setActiveUserId?.invoke(acc.user_id)
                }
                cachedAccount = acc
                isCacheInitialized = true
                acc
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Napier.e("读取当前账号失败", e)
                throw e
            }
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
    ) = withContext(Dispatchers.Default) {
        mutex.withLock {
            val user = account.user
            val existing = queries.selectByUserId(user.id).executeAsList().firstOrNull()
            queries.insertOrReplace(
                id = existing?.id,
                access_token = PlatformTokenCipher.encrypt(account.accessToken),
                refresh_token = PlatformTokenCipher.encrypt(account.refreshToken),
                device_token = deviceToken,
                user_id = user.id,
                user_image = user.profileImageUrls.px170x170,
                name = user.name,
                password = if (password.isNotBlank() && password != "no more") PlatformTokenCipher.encrypt(password) else password,
                account = user.account,
                mail_address = user.mailAddress,
                is_premium = boolToLong(user.isPremium),
                x_restrict = user.xRestrict.toLong(),
                is_mail_authorized = boolToLong(user.isMailAuthorized),
            )
            setActiveUserId?.invoke(user.id)
            cachedAccount = queries.selectByUserId(user.id).executeAsList().firstOrNull()?.decrypted()
            isCacheInitialized = true
        }
    }

    /**
     * 保存账号信息（直接使用已有的 [Account]）。
     */
    suspend fun saveAccount(account: Account) = withContext(Dispatchers.Default) {
        mutex.withLock {
            val enc = account.encrypted()
            queries.insertOrReplace(
                id = enc.id,
                access_token = enc.access_token,
                refresh_token = enc.refresh_token,
                device_token = enc.device_token,
                user_id = enc.user_id,
                user_image = enc.user_image,
                name = enc.name,
                password = enc.password,
                account = enc.account,
                mail_address = enc.mail_address,
                is_premium = enc.is_premium,
                x_restrict = enc.x_restrict,
                is_mail_authorized = enc.is_mail_authorized,
            )
            setActiveUserId?.invoke(account.user_id)
            cachedAccount = account.decrypted()
            isCacheInitialized = true
        }
    }

    /**
     * 获取本地所有已保存的账号列表。
     */
    suspend fun getAllAccounts(): List<Account> = withContext(Dispatchers.Default) {
        mutex.withLock {
            queries.selectAll().executeAsList().map { it.decrypted() }
        }
    }

    /**
     * 切换当前活跃账号。
     */
    suspend fun switchAccount(userId: String): Account = withContext(Dispatchers.Default) {
        mutex.withLock {
            val acc = queries.selectByUserId(userId).executeAsList().firstOrNull()?.decrypted()
                ?: throw IllegalArgumentException("未找到该账号: $userId")
            setActiveUserId?.invoke(userId)
            cachedAccount = acc
            isCacheInitialized = true
            acc
        }
    }

    /**
     * 移除指定账号。如果移除的是当前活跃账号，则自动切换到下一个可用账号。
     */
    suspend fun deleteAccount(userId: String) = withContext(Dispatchers.Default) {
        mutex.withLock {
            queries.deleteByUserId(userId)
            val current = cachedAccount
            if (current?.user_id == userId) {
                val next = queries.selectAll().executeAsList().firstOrNull()?.decrypted()
                setActiveUserId?.invoke(next?.user_id)
                cachedAccount = next
                isCacheInitialized = true
            }
        }
    }

    /**
     * 更新当前账号的 access_token / refresh_token，通常在 token 刷新后调用。
     *
     * @throws IllegalStateException 当本地没有登录账号时。
     */
    suspend fun updateTokens(accessToken: String, refreshToken: String) = withContext(Dispatchers.Default) {
        mutex.withLock {
            val activeUid = getActiveUserId?.invoke()
            val current = if (!activeUid.isNullOrBlank()) {
                queries.selectByUserId(activeUid).executeAsList().firstOrNull()
            } else {
                queries.selectAll().executeAsList().firstOrNull()
            } ?: throw IllegalStateException("没有登录账号，无法更新 token")
            val encAccess = PlatformTokenCipher.encrypt(accessToken)
            val encRefresh = PlatformTokenCipher.encrypt(refreshToken)
            queries.insertOrReplace(
                id = current.id,
                access_token = encAccess,
                refresh_token = encRefresh,
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
            cachedAccount = queries.selectByUserId(current.user_id).executeAsList().firstOrNull()?.decrypted()
            isCacheInitialized = true
        }
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
    suspend fun updateCurrentAccount(transform: suspend (Account?) -> Account?) = withContext(Dispatchers.Default) {
        mutex.withLock {
            val activeUid = getActiveUserId?.invoke()
            val rawCurrent = if (!activeUid.isNullOrBlank()) {
                queries.selectByUserId(activeUid).executeAsList().firstOrNull()
            } else {
                queries.selectAll().executeAsList().firstOrNull()
            }
            val current = rawCurrent?.decrypted()
            val updated = transform(current)
            if (updated != null) {
                val enc = updated.encrypted()
                queries.insertOrReplace(
                    id = enc.id,
                    access_token = enc.access_token,
                    refresh_token = enc.refresh_token,
                    device_token = enc.device_token,
                    user_id = enc.user_id,
                    user_image = enc.user_image,
                    name = enc.name,
                    password = enc.password,
                    account = enc.account,
                    mail_address = enc.mail_address,
                    is_premium = enc.is_premium,
                    x_restrict = enc.x_restrict,
                    is_mail_authorized = enc.is_mail_authorized,
                )
                cachedAccount = updated.decrypted()
                isCacheInitialized = true
            }
        }
    }

    /**
     * 清空所有账号信息，相当于登出。
     */
    suspend fun clear() = withContext(Dispatchers.Default) {
        mutex.withLock {
            queries.deleteAll()
            setActiveUserId?.invoke(null)
            cachedAccount = null
            isCacheInitialized = true
        }
    }

    private fun boolToLong(value: Boolean): Long = if (value) 1L else 0L

    private fun Account.decrypted(): Account {
        val decAccess = PlatformTokenCipher.decrypt(access_token)
        val decRefresh = PlatformTokenCipher.decrypt(refresh_token)
        val decPass = if (password.isNotBlank() && password != "no more") PlatformTokenCipher.decrypt(password) else password
        if (decAccess == access_token && decRefresh == refresh_token && decPass == password) {
            return this
        }
        return this.copy(
            access_token = decAccess,
            refresh_token = decRefresh,
            password = decPass,
        )
    }

    private fun Account.encrypted(): Account {
        val encAccess = PlatformTokenCipher.encrypt(access_token)
        val encRefresh = PlatformTokenCipher.encrypt(refresh_token)
        val encPass = if (password.isNotBlank() && password != "no more") PlatformTokenCipher.encrypt(password) else password
        if (encAccess == access_token && encRefresh == refresh_token && encPass == password) {
            return this
        }
        return this.copy(
            access_token = encAccess,
            refresh_token = encRefresh,
            password = encPass,
        )
    }
}
