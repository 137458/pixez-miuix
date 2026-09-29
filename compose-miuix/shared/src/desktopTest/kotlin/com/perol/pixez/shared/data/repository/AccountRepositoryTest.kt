package com.perol.pixez.shared.data.repository

import com.perol.pixez.shared.data.model.Account
import com.perol.pixez.shared.data.model.OAuthProfileImageUrls
import com.perol.pixez.shared.data.model.OAuthUser
import com.perol.pixez.shared.data.model.AccountResponse
import com.perol.pixez.shared.data.local.account.Account as AccountRow
import com.perol.pixez.shared.network.AuthTokenStorage
import com.perol.pixez.shared.network.OAuthClient
import com.perol.pixez.shared.network.OAuthClient.PkcePair
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.FormDataContent
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodedPath
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import kotlin.test.fail

/**
 * 账号仓库测试（A-21）：以接口注入的 fake 双依赖 + MockEngine 打桩外部网络，
 * 验证登录分发矩阵、降级语义、事件发布、账号切换委托与账号编辑请求契约。
 */
class AccountRepositoryTest {

    private data class CapturedRequest(
        val path: String,
        val formParams: Map<String, String>,
    )

    private class FakeOAuthClient : OAuthClient {
        var exchangeResult: Account? = null
        var exchangeError: Throwable? = null
        var refreshResult: Account? = null
        var refreshError: Throwable? = null
        val receivedCodes = mutableListOf<String>()
        val receivedTokens = mutableListOf<String>()

        override val lastCodeVerifier: String? = null
        override fun generatePkcePair(): PkcePair = PkcePair("verifier", "challenge")
        override fun buildLoginUrl(create: Boolean): String = "https://login-test"
        override suspend fun exchangeCodeForToken(code: String, codeVerifier: String?): Account {
            receivedCodes.add(code)
            exchangeError?.let { throw it }
            return exchangeResult ?: error("未设置 exchangeResult")
        }

        override suspend fun refreshToken(refreshToken: String): Account {
            receivedTokens.add(refreshToken)
            refreshError?.let { throw it }
            return refreshResult ?: error("未设置 refreshResult")
        }
    }

    private class FakeTokenStorage : AuthTokenStorage {
        val rows = mutableListOf<AccountRow>()
        var activeUserId: String? = null
        val savedResponses = mutableListOf<AccountResponse>()
        var updateTokensCalls = 0
        var updateCurrentAccountCalls = 0
        var transformAppliedTo: String? = null

        private fun row(userId: String, accessToken: String = "at", refreshToken: String = "rt") = AccountRow(
            id = rows.size.toLong() + 1,
            access_token = accessToken,
            refresh_token = refreshToken,
            device_token = "",
            user_id = userId,
            user_image = "img",
            name = "user-$userId",
            password = "",
            account = "acc-$userId",
            mail_address = "m@$userId",
            is_premium = 0,
            x_restrict = 0,
            is_mail_authorized = 0,
        )

        override fun getCachedAccountFast(): AccountRow? = rows.lastOrNull()
        override suspend fun getCurrentAccount(): AccountRow? =
            rows.lastOrNull { it.user_id == activeUserId } ?: rows.lastOrNull()

        override suspend fun saveAccount(account: AccountResponse, password: String, deviceToken: String) {
            savedResponses.add(account)
            rows.add(row(account.user.id, accessToken = account.accessToken, refreshToken = account.refreshToken))
            if (activeUserId == null) activeUserId = account.user.id
        }

        override suspend fun saveAccount(account: AccountRow) {
            rows.add(account)
        }

        override suspend fun getAllAccounts(): List<AccountRow> = rows.toList()
        override suspend fun switchAccount(userId: String): AccountRow {
            activeUserId = userId
            return rows.last { it.user_id == userId }
        }

        override suspend fun deleteAccount(userId: String) {
            rows.removeAll { it.user_id == userId }
        }

        override suspend fun updateTokens(accessToken: String, refreshToken: String) {
            updateTokensCalls++
        }

        override suspend fun updateCurrentAccount(transform: suspend (AccountRow?) -> AccountRow?) {
            updateCurrentAccountCalls++
            val latest = getCurrentAccount()
            val transformed = transform(latest)
            transformAppliedTo = transformed?.user_id
            if (transformed != null && latest != null) {
                rows[rows.indexOf(latest)] = transformed
            }
        }

        override suspend fun clear() {
            rows.clear()
            activeUserId = null
        }
    }

    private lateinit var oauth: FakeOAuthClient
    private lateinit var storage: FakeTokenStorage
    private lateinit var repository: AccountRepository

    private var capturedPath: String? = null
    private var editResponseBody: String = """{"error":false,"message":"","body":{"is_succeed":true,"validation_errors":{}}}"""
    private lateinit var accountClient: HttpClient

    @Before
    fun setUp() {
        oauth = FakeOAuthClient()
        storage = FakeTokenStorage()
        accountClient = HttpClient(MockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true })
            }
            engine {
                addHandler { request ->
                    val params = LinkedHashMap<String, String>()
                    (request.body as? FormDataContent)?.formData?.entries()?.forEach { entry ->
                        params[entry.key] = entry.value.joinToString(",")
                    }
                    capturedPath = request.url.encodedPath
                    capturedForm = params
                    respond(editResponseBody, HttpStatusCode.OK, headersOf("Content-Type", ContentType.Application.Json.toString()))
                }
            }
        }
        repository = AccountRepository(oauth, storage, accountClient)
    }

    private var capturedForm: Map<String, String> = emptyMap()

    private fun oauthEnvelope(userId: String = "100") = Account(
        response = AccountResponse(
            accessToken = "access-$userId",
            expiresIn = 3600,
            tokenType = "bearer",
            scope = "",
            refreshToken = "refresh-$userId",
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
        ),
    )

    private suspend fun awaitEvent(deferred: CompletableDeferred<Unit>): Boolean =
        withTimeoutOrNull(1_000) { deferred.await() } != null

    @Test
    fun `login 回调 URL 自动提取 code 走授权码分支`() = runBlocking {
        oauth.exchangeResult = oauthEnvelope()

        repository.login("https://app-api.pixiv.net/web/v1/users/auth/pixiv/callback?code=abc123")

        assertEquals(listOf("abc123"), oauth.receivedCodes)
        assertTrue(oauth.receivedTokens.isEmpty())
    }

    @Test
    fun `login JSON 凭证走 token 分支`() = runBlocking {
        oauth.refreshResult = oauthEnvelope()

        repository.login("""{"refresh_token":"tok123"}""")

        assertEquals(listOf("tok123"), oauth.receivedTokens)
        assertTrue(oauth.receivedCodes.isEmpty())
    }

    @Test
    fun `login 剥离 Bearer 与全角冒号前缀后走 token 分支`() = runBlocking {
        oauth.refreshResult = oauthEnvelope()

        repository.login("Bearer tok-1")
        repository.login("token：tok-2")
        repository.login("refresh_token: tok-3")

        assertEquals(listOf("tok-1", "tok-2", "tok-3"), oauth.receivedTokens)
    }

    @Test
    fun `login 空白凭证抛非法参数异常`() = runBlocking<Unit> {
        assertFailsWith<IllegalArgumentException> { repository.login("   ") }
    }

    @Test
    fun `login token 分支失败时降级尝试授权码`() = runBlocking {
        oauth.refreshError = IllegalStateException("token 无效")
        oauth.exchangeResult = oauthEnvelope()

        repository.login("not-json-and-not-url")

        assertEquals(listOf("not-json-and-not-url"), oauth.receivedCodes, "应降级为授权码分支")
    }

    @Test
    fun `login 双分支均失败时抛出来自 token 分支的异常`() = runBlocking {
        oauth.refreshError = IllegalStateException("token 错误")
        oauth.exchangeError = IllegalStateException("code 错误")

        try {
            repository.login("raw-credential")
            fail("应抛出异常")
        } catch (e: IllegalStateException) {
            assertEquals("token 错误", e.message, "双失败时应抛出来自 token 分支的原异常")
        }
    }

    @Test
    fun `loginWithCode 保存响应并发出登录事件`() = runBlocking {
        oauth.exchangeResult = oauthEnvelope(userId = "77")
        val events = CompletableDeferred<Unit>()
        val job = launch(start = CoroutineStart.UNDISPATCHED) { repository.loginEventFlow.collect { events.complete(Unit) } }

        repository.loginWithCode("code-1")

        assertTrue(awaitEvent(events), "登录成功应发出 loginEventFlow 事件")
        assertEquals(1, storage.savedResponses.size)
        assertEquals("77", storage.savedResponses.single().user.id)
        job.cancel()
    }

    @Test
    fun `switchAccount 与 logout 委托存储并各发一次事件`() = runBlocking {
        storage.rows.add(rowFixture("1"))
        storage.rows.add(rowFixture("2"))
        storage.activeUserId = "1"

        val e1 = CompletableDeferred<Unit>()
        val job = launch(start = CoroutineStart.UNDISPATCHED) { repository.loginEventFlow.collect { e1.complete(Unit) } }
        repository.switchAccount("2")
        assertTrue(awaitEvent(e1))
        assertEquals("2", storage.activeUserId)

        val e2 = CompletableDeferred<Unit>()
        val job2 = launch(start = CoroutineStart.UNDISPATCHED) { repository.loginEventFlow.collect { e2.complete(Unit) } }
        repository.logout()
        assertTrue(awaitEvent(e2))
        assertTrue(storage.rows.isEmpty())
        job.cancel()
        job2.cancel()
    }

    private fun rowFixture(userId: String) = AccountRow(
        id = userId.toLong(),
        access_token = "at",
        refresh_token = "rt",
        device_token = "",
        user_id = userId,
        user_image = "img",
        name = "user-$userId",
        password = "",
        account = "acc",
        mail_address = "m",
        is_premium = 0,
        x_restrict = 0,
        is_mail_authorized = 0,
    )

    @Test
    fun `currentAccount 未登录返回 null`() = runBlocking {
        assertNull(repository.currentAccount())
    }

    @Test
    fun `editAccount 空当前密码抛非法参数异常`() = runBlocking<Unit> {
        assertFailsWith<IllegalArgumentException> { repository.editAccount("", null, null) }
    }

    @Test
    fun `editAccount 未登录抛非法状态异常`() = runBlocking {
        try {
            repository.editAccount("pwd", null, null)
            fail("未登录应抛出异常")
        } catch (e: IllegalStateException) {
            assertEquals("没有登录账号，无法编辑账号信息", e.message)
        }
    }

    @Test
    fun `editAccount 成功路径请求端点与表单正确并回写本地`() = runBlocking {
        storage.rows.add(rowFixture("9"))
        storage.activeUserId = "9"

        repository.editAccount(currentPassword = "old", newPassword = "new", newMailAddress = "new@m")

        assertEquals("/api/account/edit", capturedPath)
        assertEquals("old", capturedForm["current_password"])
        assertEquals("new", capturedForm["new_password"])
        assertEquals("new@m", capturedForm["new_mail_address"])
        assertEquals("9", storage.transformAppliedTo, "编辑成功后应回写当前账号缓存")
    }
}
