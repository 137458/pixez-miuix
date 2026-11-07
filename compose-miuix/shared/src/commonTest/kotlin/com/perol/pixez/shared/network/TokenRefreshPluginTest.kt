package com.perol.pixez.shared.network

import com.perol.pixez.shared.data.local.account.Account as AccountRow
import com.perol.pixez.shared.data.model.Account
import com.perol.pixez.shared.data.model.AccountResponse
import com.perol.pixez.shared.data.model.OAuthProfileImageUrls
import com.perol.pixez.shared.data.model.OAuthUser
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodedPath
import io.ktor.http.headersOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

private const val API_HOST = "https://app-api.pixiv.net"

private const val STALE_ACCESS_TOKEN = "stale-access-token"
private const val STALE_REFRESH_TOKEN = "stale-refresh-token"
private const val FRESH_ACCESS_TOKEN = "fresh-access-token"
private const val FRESH_REFRESH_TOKEN = "fresh-refresh-token"

/** 401 响应体：含 OAuth 特征，命中 TokenRefreshPlugin 的宽匹配刷新条件。 */
private const val OAUTH_EXPIRED_BODY =
    """{"error":"invalid_grant","message":"OAuth access token expired"}"""

/** 不含 OAuth/invalid_grant/Access Token/token 任一特征的普通 400 错误体。 */
private const val PLAIN_BAD_REQUEST_BODY =
    """{"error":"validation_failed","message":"invalid parameter: limit"}"""

/** 业务性 400 错误体：错误消息含 "token" 字样但与 OAuth 凭证无关。 */
private const val BUSINESS_TOKEN_BODY =
    """{"error":"malformed_request","message":"invalid request: target token is not accessible"}"""

/** Pixiv OAuth 过期 400 错误体：invalid_grant 专属标识（TokenRefreshPlugin.kt:90 已知行为）。 */
private const val INVALID_GRANT_BODY =
    """{"error":"invalid_grant","error_description":"Invalid refresh token or expired access token"}"""

private const val OK_BODY = """{"error":false,"message":"","body":{}}"""

private val jsonHeaders = headersOf("Content-Type", ContentType.Application.Json.toString())

private fun accountFixture(accessToken: String, refreshToken: String) = Account(
    response = AccountResponse(
        accessToken = accessToken,
        expiresIn = 3600,
        tokenType = "bearer",
        scope = "",
        refreshToken = refreshToken,
        user = OAuthUser(
            profileImageUrls = OAuthProfileImageUrls(
                px16x16 = "https://i.pximg.net/16.jpg",
                px50x50 = "https://i.pximg.net/50.jpg",
                px170x170 = "https://i.pximg.net/170.jpg",
            ),
            id = "100",
            name = "tester",
            account = "tester-account",
            mailAddress = "tester@example.com",
            isPremium = false,
            xRestrict = 0,
            isMailAuthorized = true,
        ),
    ),
)

/**
 * 内存 Map 版 [AuthTokenStorage] 替身：以 user_id 为 key 维护 [AccountRow]，仅跟踪当前活跃账号；
 * [onGetCurrentAccount] 钩子供并发用例把读取时机钉死在刷新互斥窗口内。
 */
private class FakeAuthTokenStorage : AuthTokenStorage {
    private val accounts = LinkedHashMap<String, AccountRow>()
    private var activeUserId: String? = null
    var onGetCurrentAccount: (() -> Unit)? = null

    private fun rowFixture(userId: String, accessToken: String, refreshToken: String) = AccountRow(
        id = 1L,
        access_token = accessToken,
        refresh_token = refreshToken,
        device_token = "",
        user_id = userId,
        user_image = "https://i.pximg.net/170.jpg",
        name = "tester",
        password = "",
        account = "tester-account",
        mail_address = "tester@example.com",
        is_premium = 0,
        x_restrict = 0,
        is_mail_authorized = 1,
    )

    fun login(accessToken: String, refreshToken: String) {
        accounts[TEST_USER_ID] = rowFixture(TEST_USER_ID, accessToken, refreshToken)
        activeUserId = TEST_USER_ID
    }

    /** 预置第二个账号行，供跨账号切换用例构造多账号存储状态。 */
    fun addUser(userId: String, accessToken: String, refreshToken: String) {
        accounts[userId] = rowFixture(userId, accessToken, refreshToken)
    }

    /** 按账号行读取凭证字段（测试断言用，绕过 activeUserId 语义）。 */
    fun rowOf(userId: String): AccountRow? = accounts[userId]

    override suspend fun getCurrentAccount(): AccountRow? {
        onGetCurrentAccount?.invoke()
        return accounts[activeUserId]
    }

    override suspend fun saveAccount(account: AccountResponse, password: String, deviceToken: String) {
        accounts[account.user.id] = rowFixture(account.user.id, account.accessToken, account.refreshToken)
        activeUserId = account.user.id
    }

    override suspend fun getAllAccounts(): List<AccountRow> = accounts.values.toList()

    override suspend fun switchAccount(userId: String): AccountRow {
        val target = accounts.getValue(userId)
        activeUserId = userId
        return target
    }

    override suspend fun deleteAccount(userId: String) {
        accounts.remove(userId)
        if (activeUserId == userId) activeUserId = null
    }

    override suspend fun updateTokensForUser(userId: String, accessToken: String, refreshToken: String) {
        val current = accounts[userId] ?: throw IllegalStateException("未找到账号 $userId，无法更新 token")
        accounts[userId] = current.copy(access_token = accessToken, refresh_token = refreshToken)
    }

    val invalidatedUserIds = mutableListOf<String>()

    override suspend fun invalidateCredentials(userId: String) {
        invalidatedUserIds += userId
        accounts[userId]?.let { accounts[userId] = it.copy(access_token = "", refresh_token = "") }
    }

    override suspend fun updateCurrentAccount(transform: suspend (AccountRow?) -> AccountRow?) {
        val updated = transform(accounts[activeUserId]) ?: return
        accounts[updated.user_id] = updated
    }

    override suspend fun clear() {
        accounts.clear()
        activeUserId = null
    }

    private companion object {
        const val TEST_USER_ID = "100"
    }
}

/** [OAuthClient] 替身：只实现 refreshToken 分支，其余接口本测试不触达。 */
private class FakeOAuthClient : OAuthClient {
    var refreshResult: Account? = null
    var refreshError: Throwable? = null

    /** 刷新发生时回调（在互斥锁内执行），供并发用例挂起以钉死时序。 */
    var onRefreshing: (suspend () -> Unit)? = null

    var refreshCalls = 0
        private set
    val receivedRefreshTokens = mutableListOf<String>()

    override val lastCodeVerifier: String? = null
    override fun generatePkcePair(): OAuthClient.PkcePair = OAuthClient.PkcePair("verifier", "challenge")
    override fun buildLoginUrl(create: Boolean): String = "https://login-test.invalid"
    override suspend fun exchangeCodeForToken(code: String, codeVerifier: String?): Account =
        error("本测试不触达授权码换 token 分支")

    override suspend fun refreshToken(refreshToken: String): Account {
        refreshCalls++
        receivedRefreshTokens.add(refreshToken)
        onRefreshing?.invoke()
        refreshError?.let { throw it }
        return refreshResult ?: error("未设置 refreshResult")
    }
}

/**
 * 鉴权续期插件（[TokenRefreshPlugin]）五条路径的行为固化：
 * ① Bearer 注入与 walkthrough 豁免；② 401+OAuth 响应体刷新重放；
 * ③ 并发 401 仅一次刷新并复用前置凭证；④ 刷新失败原样上抛不重试；
 * ⑤ 400 响应体不含 OAuth 特征不触发刷新（宽匹配边界）。
 * 打桩全部文件内自包含：Fake 存储与 OAuth 客户端 + MockEngine 请求捕获。
 */
class TokenRefreshPluginTest {

    private fun buildClient(
        storage: FakeAuthTokenStorage,
        oauth: FakeOAuthClient,
        authExpirySignal: AuthExpirySignal? = null,
        handler: MockRequestHandler,
    ): HttpClient = HttpClient(MockEngine) {
        install(TokenRefreshPlugin) {
            this.tokenStorage = storage
            this.oAuthClient = oauth
            this.authExpirySignal = authExpirySignal
        }
        engine { addHandler(handler) }
    }

    /** 记录每次请求的 (路径, Authorization) 并对旧 token 回 401+OAuth 体，其余一律 200。 */
    private fun recordingHandler(
        requests: MutableList<Pair<String, String?>>,
    ): MockRequestHandler = { request ->
        val auth = request.headers["Authorization"]
        requests.add(request.url.encodedPath to auth)
        when (auth) {
            "Bearer $STALE_ACCESS_TOKEN" -> respond(OAUTH_EXPIRED_BODY, HttpStatusCode.Unauthorized, jsonHeaders)
            else -> respond(OK_BODY, HttpStatusCode.OK, jsonHeaders)
        }
    }

    @Test
    fun `普通请求注入 Bearer 且 walkthrough 路径豁免注入`() {
        val storage = FakeAuthTokenStorage().apply { login(STALE_ACCESS_TOKEN, STALE_REFRESH_TOKEN) }
        val oauth = FakeOAuthClient()
        val requests = mutableListOf<Pair<String, String?>>()
        // 注入断言与 401 无关，恒回 200，避免 fixture 误触刷新路径。
        val client = buildClient(storage, oauth) { request ->
            val auth = request.headers["Authorization"]
            requests.add(request.url.encodedPath to auth)
            respond(OK_BODY, HttpStatusCode.OK, jsonHeaders)
        }

        client.use {
            runBlocking {
                client.get("$API_HOST/v1/illust/detail")
                client.get("$API_HOST/v1/walkthrough/illusts")
            }
        }

        assertEquals(
            listOf(
                "/v1/illust/detail" to "Bearer $STALE_ACCESS_TOKEN",
                "/v1/walkthrough/illusts" to null,
            ),
            requests,
            "普通请求应注入 Bearer 凭证，walkthrough 匿名接口不应注入 Authorization",
        )
        assertEquals(0, oauth.refreshCalls, "200 响应不应触发刷新")
    }

    @Test
    fun `401 且 OAuth 响应体触发一次刷新并以新 token 重放`() {
        val storage = FakeAuthTokenStorage().apply { login(STALE_ACCESS_TOKEN, STALE_REFRESH_TOKEN) }
        val oauth = FakeOAuthClient().apply {
            refreshResult = accountFixture(FRESH_ACCESS_TOKEN, FRESH_REFRESH_TOKEN)
        }
        val requests = mutableListOf<Pair<String, String?>>()
        val client = buildClient(storage, oauth, handler = recordingHandler(requests))

        val response = client.use {
            runBlocking { client.get("$API_HOST/v1/illust/detail").status }
        }

        assertEquals(HttpStatusCode.OK, response, "刷新后重放应成功")
        assertEquals(2, requests.size, "应恰为一次 401 请求加一次重放")
        assertEquals(
            "Bearer $FRESH_ACCESS_TOKEN",
            requests.last().second,
            "重放请求应携带刷新后的 access_token",
        )
        assertEquals(1, oauth.refreshCalls, "应仅触发一次刷新")
        assertEquals(listOf(STALE_REFRESH_TOKEN), oauth.receivedRefreshTokens, "刷新应使用存储中的旧 refresh_token")
        assertEquals(FRESH_ACCESS_TOKEN, runBlocking { storage.getCurrentAccount() }?.access_token, "新 token 应写回存储")
        assertEquals(FRESH_REFRESH_TOKEN, runBlocking { storage.getCurrentAccount() }?.refresh_token, "新 refresh_token 应写回存储")
    }

    @Test
    fun `两个并发 401 仅触发一次刷新且复用前置凭证重放`() {
        val storage = FakeAuthTokenStorage().apply { login(STALE_ACCESS_TOKEN, STALE_REFRESH_TOKEN) }
        val refreshStarted = CompletableDeferred<Unit>()
        val secondStaleRead = CompletableDeferred<Unit>()
        var readsAfterRefreshStarted = 0
        storage.onGetCurrentAccount = {
            if (refreshStarted.isCompleted) {
                readsAfterRefreshStarted++
                // 第 1 次是第二个请求的 Bearer 注入读取，第 2 次是其刷新入口的旧 token 读取：
                // 此刻它尚未进入互斥锁，等待期间第一个请求完成刷新，即构成"复用前置凭证"窗口。
                if (readsAfterRefreshStarted == 2) secondStaleRead.complete(Unit)
            }
        }
        val oauth = FakeOAuthClient().apply {
            refreshResult = accountFixture(FRESH_ACCESS_TOKEN, FRESH_REFRESH_TOKEN)
            onRefreshing = {
                refreshStarted.complete(Unit)
                // 挂起在互斥锁内，直到第二个请求完成旧 token 读取并在锁上排队，再让刷新生效。
                withTimeoutOrNull(5_000) { secondStaleRead.await() }
                    ?: error("时序未按预期推进：第二个请求的旧 token 读取未在刷新完成前发生")
            }
        }
        val requests = mutableListOf<Pair<String, String?>>()
        val statuses = mutableMapOf<String, HttpStatusCode>()
        val client = buildClient(storage, oauth, handler = recordingHandler(requests))

        client.use {
            runBlocking {
                val first = launch(start = CoroutineStart.UNDISPATCHED) {
                    statuses["/a"] = client.get("$API_HOST/a").status
                }
                withTimeoutOrNull(5_000) { refreshStarted.await() }
                    ?: error("时序未按预期推进：第一个请求未进入刷新流程")
                val second = launch {
                    statuses["/b"] = client.get("$API_HOST/b").status
                }
                first.join()
                second.join()
            }
        }

        assertEquals(HttpStatusCode.OK, statuses["/a"], "第一个请求刷新后应重放成功")
        assertEquals(HttpStatusCode.OK, statuses["/b"], "第二个请求应复用前置刷新结果重放成功")
        assertEquals(1, oauth.refreshCalls, "两个并发 401 应只触发一次刷新")
        assertEquals(
            listOf(STALE_REFRESH_TOKEN),
            oauth.receivedRefreshTokens,
            "仅第一个请求持锁真正刷新，第二个请求复用凭证",
        )
        val authsByPath = requests.groupBy({ it.first }, { it.second })
        assertEquals(
            listOf("Bearer $STALE_ACCESS_TOKEN", "Bearer $FRESH_ACCESS_TOKEN"),
            authsByPath["/a"],
            "/a 应先带旧 token 得 401，再带新 token 重放",
        )
        assertEquals(
            listOf("Bearer $STALE_ACCESS_TOKEN", "Bearer $FRESH_ACCESS_TOKEN"),
            authsByPath["/b"],
            "/b 应先带旧 token 得 401，再复用前置刷新出的新 token 重放",
        )
        assertEquals(FRESH_ACCESS_TOKEN, runBlocking { storage.getCurrentAccount() }?.access_token, "刷新结果应写回存储")
    }

    @Test
    fun `刷新失败时异常原样上抛且不重试`() {
        val storage = FakeAuthTokenStorage().apply { login(STALE_ACCESS_TOKEN, STALE_REFRESH_TOKEN) }
        val refreshError = IllegalStateException("refresh boom")
        val oauth = FakeOAuthClient().apply { this.refreshError = refreshError }
        val requests = mutableListOf<Pair<String, String?>>()
        val client = buildClient(storage, oauth, handler = recordingHandler(requests))

        val thrown = client.use {
            runBlocking {
                assertFailsWith<IllegalStateException> { client.get("$API_HOST/v1/illust/detail") }
            }
        }

        assertSame(refreshError, thrown, "刷新失败的异常应原样向上抛出")
        assertEquals(1, oauth.refreshCalls, "刷新失败后不应再次刷新")
        assertEquals(1, requests.size, "刷新失败后不应重放请求")
    }

    @Test
    fun `400 响应体不含 OAuth 特征时不触发刷新`() {
        val storage = FakeAuthTokenStorage().apply { login(STALE_ACCESS_TOKEN, STALE_REFRESH_TOKEN) }
        val oauth = FakeOAuthClient()
        val requests = mutableListOf<Pair<String, String?>>()
        val client = buildClient(storage, oauth) { request ->
            val auth = request.headers["Authorization"]
            requests.add(request.url.encodedPath to auth)
            respond(PLAIN_BAD_REQUEST_BODY, HttpStatusCode.BadRequest, jsonHeaders)
        }

        val response = client.use {
            runBlocking { client.get("$API_HOST/v1/illust/detail").status }
        }

        assertEquals(HttpStatusCode.BadRequest, response, "不含 OAuth 特征的 400 应原样返回给调用方")
        assertEquals(0, oauth.refreshCalls, "响应体无 OAuth 特征时不应触发刷新")
        assertEquals(1, requests.size, "不触发刷新即不应重放请求")
    }

    @Test
    fun `业务 400 响应体含 token 字样不触发刷新`() {
        val storage = FakeAuthTokenStorage().apply { login(STALE_ACCESS_TOKEN, STALE_REFRESH_TOKEN) }
        val oauth = FakeOAuthClient().apply {
            // 刷新若被误触发必须在此显式失败为业务断言（refreshCalls 计数），而非 Fake 未配置异常。
            refreshResult = accountFixture(FRESH_ACCESS_TOKEN, FRESH_REFRESH_TOKEN)
        }
        val requests = mutableListOf<Pair<String, String?>>()
        val client = buildClient(storage, oauth) { request ->
            val auth = request.headers["Authorization"]
            requests.add(request.url.encodedPath to auth)
            if (auth == "Bearer $STALE_ACCESS_TOKEN") {
                respond(BUSINESS_TOKEN_BODY, HttpStatusCode.BadRequest, jsonHeaders)
            } else {
                respond(OK_BODY, HttpStatusCode.OK, jsonHeaders)
            }
        }

        val response = client.use {
            runBlocking { client.get("$API_HOST/v1/illust/detail").status }
        }

        assertEquals(HttpStatusCode.BadRequest, response, "业务性 400 应原样返回给调用方，不得被吞掉")
        assertEquals(0, oauth.refreshCalls, "业务 400 含 token 字样不是凭证过期，不应触发 OAuth 刷新")
        assertEquals(1, requests.size, "不触发刷新即不应重放请求")
    }

    @Test
    fun `刷新期间活跃账号切换时刷新结果不得写入切换后的账号`() {
        val storage = FakeAuthTokenStorage().apply { login(STALE_ACCESS_TOKEN, STALE_REFRESH_TOKEN) }
        // 预置切换目标账号 200：模拟刷新网络往返期间用户完成 switchAccount 的并发窗口。
        storage.addUser("200", "at-200", "rt-200")
        val oauth = FakeOAuthClient().apply {
            refreshResult = accountFixture(FRESH_ACCESS_TOKEN, FRESH_REFRESH_TOKEN)
            onRefreshing = { storage.switchAccount("200") }
        }
        val requests = mutableListOf<Pair<String, String?>>()
        val client = buildClient(storage, oauth, handler = recordingHandler(requests))

        val response = client.use {
            runBlocking { client.get("$API_HOST/v1/illust/detail").status }
        }

        assertEquals(
            "at-200",
            storage.rowOf("200")?.access_token,
            "旧账号刷新出的新 token 严禁写入切换后的账号 200（跨账号凭证污染）",
        )
        assertEquals(
            "rt-200",
            storage.rowOf("200")?.refresh_token,
            "旧账号刷新出的新 refresh_token 严禁写入切换后的账号 200",
        )
        assertEquals(
            FRESH_ACCESS_TOKEN,
            storage.rowOf("100")?.access_token,
            "刷新结果应写回发起刷新的账号 100",
        )
        assertEquals(FRESH_REFRESH_TOKEN, storage.rowOf("100")?.refresh_token, "刷新结果应写回发起刷新的账号 100")
        assertEquals(HttpStatusCode.OK, response, "刷新重放请求不应因并发切换而失败")
    }

    @Test
    fun `刷新判定失效时清理凭证发事件并对同账号熔断`() {
        val storage = FakeAuthTokenStorage().apply { login(STALE_ACCESS_TOKEN, STALE_REFRESH_TOKEN) }
        val signal = AuthExpirySignal()
        val oauth = FakeOAuthClient().apply {
            refreshError = PixivApiException(statusCode = 400, message = "invalid_grant")
        }
        val events = mutableListOf<AuthExpiredEvent>()
        val client = buildClient(
            storage,
            oauth,
            signal,
        ) { respond(OAUTH_EXPIRED_BODY, HttpStatusCode.Unauthorized, jsonHeaders) }

        client.use {
            runBlocking {
                val collector = launch(start = CoroutineStart.UNDISPATCHED) {
                    signal.events.collect { events += it }
                }
                val first = assertFailsWith<AuthExpiredException> {
                    client.get("$API_HOST/first")
                }
                assertEquals("100", first.userId, "失效事件应携带当前账号 userId")
                val second = assertFailsWith<AuthExpiredException> {
                    client.get("$API_HOST/second")
                }
                assertEquals("100", second.userId, "熔断窗口内应直接快速失败")
                collector.cancel()
            }
        }

        assertEquals(1, oauth.refreshCalls, "同一账号 30 秒内不应重复发起 OAuth 刷新")
        assertEquals(listOf("100"), storage.invalidatedUserIds, "失效后应清空该账号凭证")
        assertTrue(events.isNotEmpty(), "凭证失效必须发出 authExpired 事件")
        assertTrue(storage.rowOf("100")?.access_token.isNullOrEmpty(), "失效后 access_token 应为空")
        assertTrue(storage.rowOf("100")?.refresh_token.isNullOrEmpty(), "失效后 refresh_token 应为空")
    }

    @Test
    fun `refresh token 为空时直接判失效不发网络`() {
        val storage = FakeAuthTokenStorage().apply { login(STALE_ACCESS_TOKEN, "") }
        val signal = AuthExpirySignal()
        val oauth = FakeOAuthClient()
        val client = buildClient(
            storage,
            oauth,
            signal,
        ) { respond(OAUTH_EXPIRED_BODY, HttpStatusCode.Unauthorized, jsonHeaders) }

        client.use {
            runBlocking {
                // 以 async 挂载一次性收集并 await 结果：事件在 tryEmit 中只是排队恢复收集协程，
                // 若像旧实现那样在异常抛出后立即 cancel()，排队中的协程会被取消、事件丢失。
                val collected = async(start = CoroutineStart.UNDISPATCHED) {
                    signal.events.first()
                }
                assertFailsWith<AuthExpiredException> { client.get("$API_HOST/no-refresh-token") }
                assertEquals(
                    AuthExpiredEvent("100"),
                    collected.await(),
                    "无 refresh_token 也必须发出失效事件",
                )
            }
        }

        assertEquals(0, oauth.refreshCalls, "无 refresh_token 时不应发起注定失败的刷新请求")
        assertEquals(listOf("100"), storage.invalidatedUserIds)
    }

    @Test
    fun `网络异常不清理凭证也不发失效事件`() {
        val storage = FakeAuthTokenStorage().apply { login(STALE_ACCESS_TOKEN, STALE_REFRESH_TOKEN) }
        val signal = AuthExpirySignal()
        val oauth = FakeOAuthClient().apply {
            refreshError = RuntimeException("offline")
        }
        val events = mutableListOf<AuthExpiredEvent>()
        val client = buildClient(
            storage,
            oauth,
            signal,
        ) { respond(OAUTH_EXPIRED_BODY, HttpStatusCode.Unauthorized, jsonHeaders) }

        client.use {
            runBlocking {
                val collector = launch(start = CoroutineStart.UNDISPATCHED) {
                    signal.events.collect { events += it }
                }
                val thrown = assertFailsWith<RuntimeException> { client.get("$API_HOST/offline") }
                assertEquals("offline", thrown.message, "网络异常应原样抛出")
                collector.cancel()
            }
        }

        assertEquals(1, oauth.refreshCalls, "网络异常不应触发额外刷新")
        assertTrue(storage.invalidatedUserIds.isEmpty(), "网络异常不得清理凭证，否则离线用户会被误登出")
        assertTrue(events.isEmpty(), "网络异常不得发出失效事件")
        assertEquals(STALE_ACCESS_TOKEN, storage.rowOf("100")?.access_token, "网络异常后凭证应保持不变")
        assertEquals(STALE_REFRESH_TOKEN, storage.rowOf("100")?.refresh_token, "网络异常后 refresh_token 应保持不变")
    }

    @Test
    fun `400 invalid_grant 过期 token 仍触发刷新并以新 token 重放`() {
        val storage = FakeAuthTokenStorage().apply { login(STALE_ACCESS_TOKEN, STALE_REFRESH_TOKEN) }
        val oauth = FakeOAuthClient().apply {
            refreshResult = accountFixture(FRESH_ACCESS_TOKEN, FRESH_REFRESH_TOKEN)
        }
        val requests = mutableListOf<Pair<String, String?>>()
        val client = buildClient(storage, oauth) { request ->
            val auth = request.headers["Authorization"]
            requests.add(request.url.encodedPath to auth)
            if (auth == "Bearer $STALE_ACCESS_TOKEN") {
                respond(INVALID_GRANT_BODY, HttpStatusCode.BadRequest, jsonHeaders)
            } else {
                respond(OK_BODY, HttpStatusCode.OK, jsonHeaders)
            }
        }

        val response = client.use {
            runBlocking { client.get("$API_HOST/v1/illust/detail").status }
        }

        assertEquals(HttpStatusCode.OK, response, "invalid_grant 400 刷新后重放应成功")
        assertEquals(1, oauth.refreshCalls, "invalid_grant 表示凭证过期，应触发恰好一次刷新")
        assertEquals(
            listOf(STALE_REFRESH_TOKEN),
            oauth.receivedRefreshTokens,
            "刷新应使用存储中的旧 refresh_token",
        )
        assertEquals(2, requests.size, "应恰为一次 400 请求加一次重放")
        assertEquals(
            "Bearer $FRESH_ACCESS_TOKEN",
            requests.last().second,
            "重放请求应携带刷新后的 access_token",
        )
    }
}
