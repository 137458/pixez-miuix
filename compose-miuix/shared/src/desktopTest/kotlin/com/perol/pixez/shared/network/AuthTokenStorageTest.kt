package com.perol.pixez.shared.network

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.perol.pixez.shared.data.local.SqlDelightAuthTokenStorage
import com.perol.pixez.shared.data.local.account.AccountDatabase
import com.perol.pixez.shared.data.model.AccountResponse
import com.perol.pixez.shared.data.model.OAuthProfileImageUrls
import com.perol.pixez.shared.data.model.OAuthUser
import com.perol.pixez.shared.platform.PlatformTokenCipher
import com.perol.pixez.shared.platform.TokenCipherException
import com.perol.pixez.shared.ui.AppConstants
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 账号凭据存储测试（A-21）：真 SQLDelight 实现 + 内存驱动，
 * 验证 activeUserId 命中/回退、保存后缓存可见、切换/删除/更新 token/清空等外部行为。
 *
 * PlatformTokenCipher 在 Windows 上走真 DPAPI，非 Windows 回退明文分支，
 * 断言均不绑死密文形态。
 */
class AuthTokenStorageTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var storage: AuthTokenStorage
    private var persistedActiveUserId: String? = null

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AccountDatabase.Schema.create(driver)
        storage = SqlDelightAuthTokenStorage(
            driver = driver,
            getActiveUserId = { persistedActiveUserId },
            setActiveUserId = { persistedActiveUserId = it },
        )
    }

    @After
    fun tearDown() {
        driver.close()
    }

    private fun response(userId: String, accessToken: String = "at-$userId", refreshToken: String = "rt-$userId") =
        AccountResponse(
            accessToken = accessToken,
            expiresIn = 3600,
            tokenType = "bearer",
            scope = "",
            refreshToken = refreshToken,
            user = OAuthUser(
                profileImageUrls = OAuthProfileImageUrls(px16x16 = "a", px50x50 = "b", px170x170 = "c"),
                id = userId,
                name = "user-$userId",
                account = "acc-$userId",
                mailAddress = "m@$userId",
                isPremium = false,
                xRestrict = 0,
                isMailAuthorized = true,
            ),
        )

    @Test
    fun `saveAccount 后快速缓存与查询均可见且字段往返完整`() = runBlocking {
        storage.saveAccount(response("100"), password = "pwd", deviceToken = "dev")

        val cached = storage.getCachedAccountFast()
        assertEquals("100", cached?.user_id)
        assertEquals("at-100", cached?.access_token)
        assertEquals("dev", cached?.device_token)

        val current = storage.getCurrentAccount()
        assertEquals("100", current?.user_id)
        assertEquals("pwd", current?.password, "保存后密码应可被本实现读取（加解密对称）")
    }

    @Test
    fun `getCurrentAccount 优先 activeUserId 回退首条`() = runBlocking {
        storage.saveAccount(response("1"))
        storage.saveAccount(response("2"))
        persistedActiveUserId = "2"

        assertEquals("2", storage.getCurrentAccount()?.user_id, "应命中 activeUserId 指定的账号")
    }

    @Test
    fun `switchAccount 切换活跃账号并返回目标`() = runBlocking {
        storage.saveAccount(response("1"))
        storage.saveAccount(response("2"))

        val switched = storage.switchAccount("1")

        assertEquals("1", switched.user_id)
        assertEquals("1", storage.getCurrentAccount()?.user_id)
    }

    @Test
    fun `deleteAccount 删除后回退到剩余账号`() = runBlocking {
        storage.saveAccount(response("1"))
        storage.saveAccount(response("2"))
        storage.switchAccount("2")

        storage.deleteAccount("2")

        assertEquals("1", storage.getCurrentAccount()?.user_id, "删除当前账号后应回退到剩余账号")
    }

    @Test
    fun `updateTokens 只更新令牌不改其余字段`() = runBlocking {
        storage.saveAccount(response("5"))

        storage.updateTokens(accessToken = "new-at", refreshToken = "new-rt")

        val current = storage.getCurrentAccount()
        assertEquals("new-at", current?.access_token)
        assertEquals("new-rt", current?.refresh_token)
        assertEquals("user-5", current?.name, "其余字段不应被改动")
    }

    @Test
    fun `updateCurrentAccount 变换生效`() = runBlocking {
        storage.saveAccount(response("7"))

        storage.updateCurrentAccount { it?.copy(mail_address = "changed@x") }

        assertEquals("changed@x", storage.getCurrentAccount()?.mail_address)
    }

    @Test
    fun `clear 清空全部账号`() = runBlocking {
        storage.saveAccount(response("1"))
        storage.saveAccount(response("2"))

        storage.clear()

        assertNull(storage.getCachedAccountFast())
        assertNull(storage.getCurrentAccount())
        assertTrue(storage.getAllAccounts().isEmpty())
    }

    @Test
    fun `未保存任何账号时查询返回空`() = runBlocking {
        assertNull(storage.getCurrentAccount())
        assertTrue(storage.getAllAccounts().isEmpty())
    }

    @Test
    fun `解密失败按凭证失效清理保留账号轮廓且密文不再当凭证返回`() = runBlocking {
        storage.saveAccount(response("9"))
        // 直接改库注入损坏密文，模拟系统升级/恢复出厂后平台密钥丢失的持久化状态
        driver.execute(
            null,
            "UPDATE account SET access_token = ?, refresh_token = ?, password = ? WHERE user_id = ?",
            4,
        ) {
            bindString(0, "${AppConstants.Auth.TOKEN_ENCRYPTION_PREFIX}@@not-valid-base64@@")
            bindString(1, "${AppConstants.Auth.TOKEN_ENCRYPTION_PREFIX}AAAA")
            bindString(2, "${AppConstants.Auth.TOKEN_ENCRYPTION_PREFIX}####corrupted####")
            bindString(3, "9")
        }
        // 新建实例绕过内存缓存，从数据库读取损坏行
        val freshStorage = SqlDelightAuthTokenStorage(
            driver = driver,
            getActiveUserId = { persistedActiveUserId },
            setActiveUserId = { persistedActiveUserId = it },
        )

        val account = freshStorage.getCurrentAccount()

        assertEquals("9", account?.user_id, "账号轮廓应保留")
        assertEquals("user-9", account?.name, "账号轮廓应保留")
        assertEquals("", account?.access_token, "损坏密文不得被当作 access_token 原样返回")
        assertEquals("", account?.refresh_token, "损坏密文不得被当作 refresh_token 原样返回")
        // 清理需落库：再次从数据库读取不再触发解密失败，token 保持清空
        val reloaded = SqlDelightAuthTokenStorage(
            driver = driver,
            getActiveUserId = { persistedActiveUserId },
            setActiveUserId = { persistedActiveUserId = it },
        ).getCurrentAccount()
        assertEquals("", reloaded?.access_token, "清理应落库，重新读取不得再返回损坏密文")
        assertEquals("", reloaded?.refresh_token, "清理应落库，重新读取不得再返回损坏密文")
    }

    @Test
    fun `加密失败保存中断且明文不落库`() = runBlocking {
        val failingStorage = SqlDelightAuthTokenStorage(
            driver = driver,
            getActiveUserId = { persistedActiveUserId },
            setActiveUserId = { persistedActiveUserId = it },
            encryptFn = { throw TokenCipherException(TokenCipherException.STAGE_ENCRYPT, "模拟平台加密失败") },
            decryptFn = { PlatformTokenCipher.decrypt(it) },
        )

        val result = runCatching { failingStorage.saveAccount(response("10")) }

        val thrown = result.exceptionOrNull()
        assertTrue(thrown is TokenCipherException, "保存应因加密失败上抛 TokenCipherException，实际: $thrown")
        assertNull(failingStorage.getCurrentAccount(), "保存失败后不应有当前账号")
        // 用默认加解密直接读库，确认明文凭证未落库
        val verify = SqlDelightAuthTokenStorage(driver)
        assertNull(verify.getCurrentAccount(), "加密失败时凭证不得明文落库")
    }
}
