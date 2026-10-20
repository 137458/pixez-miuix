package com.perol.pixez.shared.data.local

import app.cash.sqldelight.db.SqlDriver
import com.perol.pixez.shared.data.local.account.Account
import com.perol.pixez.shared.data.local.account.AccountDatabase
import com.perol.pixez.shared.data.model.AccountResponse
import com.perol.pixez.shared.network.AuthTokenStorage
import com.perol.pixez.shared.platform.PlatformTokenCipher
import com.perol.pixez.shared.platform.TokenCipherException
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** 旧 Flutter 表沿用的无密码占位值，保持非空约束兼容。 */
private const val DEFAULT_PASSWORD_PLACEHOLDER = "no more"

/**
 * 当前登录账号与 Token 的持久化存储。
 *
 * 直接复用旧 Flutter 的 account.db，字段与 SQLDelight 生成的 [Account] 保持一致。
 * 所有读写操作通过 [Mutex] 串行化，避免并发刷新 token 导致的数据竞争。
 * 并在底层接入 [PlatformTokenCipher]，对持久化到 SQLite 的 Token 与密码实施硬件级加密保护。
 */
/**
 * 默认实现：SQLDelight 持久化 + PlatformTokenCipher 加解密 + 内存缓存。
 *
 * @param encryptFn / decryptFn 平台加解密注入缝，生产使用默认 [PlatformTokenCipher]；
 *   desktopTest 注入故障以验证「加密失败不得明文落库」等 fail-fast 行为（中-17）。
 */
class SqlDelightAuthTokenStorage(
    driver: SqlDriver,
    private val getActiveUserId: (() -> String?)? = null,
    private val setActiveUserId: ((String?) -> Unit)? = null,
    private val encryptFn: (String) -> String = { PlatformTokenCipher.encrypt(it) },
    private val decryptFn: (String) -> String = { PlatformTokenCipher.decrypt(it) },
) : AuthTokenStorage {
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
    override fun getCachedAccountFast(): Account? = if (isCacheInitialized) cachedAccount else null

    /**
     * 读取当前活跃账号。
     *
     * 优先匹配当前保存的 activeUserId，若不存在则回退至第一条已登录账号。
     * 内部使用内存缓存，优先从缓存返回，避免首屏频繁查询 SQLite 阻塞。
     * 若数据库为空则返回 null；读取异常会向上抛出，避免静默掩盖数据库损坏；
     * 凭证解密失败不在此列——按「凭证失效」清理后返回仅含账号轮廓的记录（见 [decrypted]）。
     */
    override suspend fun getCurrentAccount(): Account? = withContext(Dispatchers.IO) {
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
    override suspend fun saveAccount(
        account: AccountResponse,
        password: String,
        deviceToken: String,
    ) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val user = account.user
            val existing = queries.selectByUserId(user.id).executeAsList().firstOrNull()
            queries.insertOrReplace(
                id = existing?.id,
                access_token = encryptFn(account.accessToken),
                refresh_token = encryptFn(account.refreshToken),
                device_token = deviceToken,
                user_id = user.id,
                user_image = user.profileImageUrls.px170x170,
                name = user.name,
                password = encryptPassword(password),
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
    override suspend fun saveAccount(account: Account) = withContext(Dispatchers.IO) {
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
    override suspend fun getAllAccounts(): List<Account> = withContext(Dispatchers.IO) {
        mutex.withLock {
            queries.selectAll().executeAsList().map { it.decrypted() }
        }
    }

    /**
     * 切换当前活跃账号。
     */
    override suspend fun switchAccount(userId: String): Account = withContext(Dispatchers.IO) {
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
    override suspend fun deleteAccount(userId: String) = withContext(Dispatchers.IO) {
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
    override suspend fun updateTokens(accessToken: String, refreshToken: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val activeUid = getActiveUserId?.invoke()
            val current = if (!activeUid.isNullOrBlank()) {
                queries.selectByUserId(activeUid).executeAsList().firstOrNull()
            } else {
                queries.selectAll().executeAsList().firstOrNull()
            } ?: throw IllegalStateException("没有登录账号，无法更新 token")
            val encAccess = encryptFn(accessToken)
            val encRefresh = encryptFn(refreshToken)
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
     * 按 [userId] 指定账号更新 access_token / refresh_token（token 刷新专用）。
     *
     * 刷新结果只写回 [userId] 对应的账号行；内存缓存仅在缓存指向该账号时同步刷新，
     * 防止刷新网络往返期间切换活跃账号导致新凭证被写入切换后的账号（跨账号凭证污染）。
     *
     * @throws IllegalStateException 当 [userId] 对应账号不存在时（含刷新期间账号被登出/删除）。
     */
    override suspend fun updateTokensForUser(userId: String, accessToken: String, refreshToken: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val target = queries.selectByUserId(userId).executeAsList().firstOrNull()
                ?: throw IllegalStateException("未找到账号 $userId，无法更新 token")
            queries.insertOrReplace(
                id = target.id,
                access_token = encryptFn(accessToken),
                refresh_token = encryptFn(refreshToken),
                device_token = target.device_token,
                user_id = target.user_id,
                user_image = target.user_image,
                name = target.name,
                password = target.password,
                account = target.account,
                mail_address = target.mail_address,
                is_premium = target.is_premium,
                x_restrict = target.x_restrict,
                is_mail_authorized = target.is_mail_authorized,
            )
            if (cachedAccount?.user_id == userId) {
                cachedAccount = queries.selectByUserId(userId).executeAsList().firstOrNull()?.decrypted()
                isCacheInitialized = true
            }
        }
    }

    /**
     * 原子更新当前账号。
     *
     * 在 Mutex 保护下读取当前账号，调用 [transform] 生成新账号对象，再将结果写回数据库。
     * 读取、transform、写入整个流程串行化，避免并发操作导致的数据覆盖或丢失。
     *
     * 锁边界约束：[transform] 在 Mutex 内被串行调用，其中严禁执行网络请求或任何挂起的
     * 外部 IO（项目约定见 CONTEXT.md：「网络请求不得在 token storage 的 Mutex 内执行」）。
     * [transform] 长时间挂起会阻塞所有账号读写，与锁外的 token 刷新网络往返相互等待即构成
     * 死锁入口；[transform] 只应做纯内存变换后立即返回。
     *
     * @param transform 接收当前账号（未登录时为 null），返回更新后的账号；返回 null 表示不写入。
     */
    override suspend fun updateCurrentAccount(transform: suspend (Account?) -> Account?) = withContext(Dispatchers.IO) {
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
    override suspend fun clear() = withContext(Dispatchers.IO) {
        mutex.withLock {
            queries.deleteAll()
            setActiveUserId?.invoke(null)
            cachedAccount = null
            isCacheInitialized = true
        }
    }

    private fun boolToLong(value: Boolean): Long = if (value) 1L else 0L

    private fun encryptPassword(password: String): String =
        if (password.isNotBlank() && password != DEFAULT_PASSWORD_PLACEHOLDER) {
            encryptFn(password)
        } else {
            password
        }

    private fun decryptPassword(password: String): String =
        if (password.isNotBlank() && password != DEFAULT_PASSWORD_PLACEHOLDER) {
            decryptFn(password)
        } else {
            password
        }

    /**
     * 解密账号凭证字段；解密失败（[TokenCipherException]，平台密钥丢失或密文损坏）时
     * 按「凭证失效」清理该账号（中-17 fail-fast）：清空 token 与密码、保留账号轮廓并落库，
     * 密文不再被当作凭证原样返回给网络层，UI 侧可凭空 token 引导重新登录。
     * 必须在持有 [mutex] 的上下文中调用（清理需要写库）。
     */
    private fun Account.decrypted(): Account {
        return try {
            val decAccess = decryptFn(access_token)
            val decRefresh = decryptFn(refresh_token)
            val decPass = decryptPassword(password)
            if (decAccess == access_token && decRefresh == refresh_token && decPass == password) {
                this
            } else {
                this.copy(
                    access_token = decAccess,
                    refresh_token = decRefresh,
                    password = decPass,
                )
            }
        } catch (e: TokenCipherException) {
            Napier.e("账号($user_id)凭证解密失败，按凭证失效清理：清空 token 并保留账号轮廓，等待重新登录", e)
            invalidateCredentials()
        }
    }

    /**
     * 将当前账号的凭证字段清空并落库（access/refresh token 置空、密码恢复占位值），
     * 返回清理后的账号对象；调用方持有 [mutex]，后续读取不再触发解密失败。
     */
    private fun Account.invalidateCredentials(): Account {
        val sanitized = this.copy(
            access_token = "",
            refresh_token = "",
            password = DEFAULT_PASSWORD_PLACEHOLDER,
        )
        queries.insertOrReplace(
            id = sanitized.id,
            access_token = sanitized.access_token,
            refresh_token = sanitized.refresh_token,
            device_token = sanitized.device_token,
            user_id = sanitized.user_id,
            user_image = sanitized.user_image,
            name = sanitized.name,
            password = sanitized.password,
            account = sanitized.account,
            mail_address = sanitized.mail_address,
            is_premium = sanitized.is_premium,
            x_restrict = sanitized.x_restrict,
            is_mail_authorized = sanitized.is_mail_authorized,
        )
        return sanitized
    }

    private fun Account.encrypted(): Account {
        // 与 saveAccount/updateTokens 一致，统一走注入的 encryptFn，保证故障注入测试能覆盖本路径。
        val encAccess = encryptFn(access_token)
        val encRefresh = encryptFn(refresh_token)
        val encPass = encryptPassword(password)
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
