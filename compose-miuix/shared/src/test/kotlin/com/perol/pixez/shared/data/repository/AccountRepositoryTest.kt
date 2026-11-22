package com.perol.pixez.shared.data.repository

import com.perol.pixez.shared.data.model.Account
import com.perol.pixez.shared.data.model.OAuthProfileImageUrls
import com.perol.pixez.shared.data.model.OAuthUser
import com.perol.pixez.shared.data.model.AccountResponse
import com.perol.pixez.shared.data.local.account.Account as AccountRow
import com.perol.pixez.shared.data.cache.SessionCache
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.network.AuthExpirySignal
import com.perol.pixez.shared.network.AuthTokenStorage
import com.perol.pixez.shared.network.PixivApiException
import com.perol.pixez.shared.network.OAuthClient
import com.perol.pixez.shared.network.OAuthClient.PkcePair
import com.perol.pixez.shared.network.TokenRefreshPlugin
import com.perol.pixez.shared.testutil.InMemorySettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodedPath
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
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
        var onRefreshing: (suspend () -> Unit)? = null
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
            onRefreshing?.invoke()
            refreshError?.let { throw it }
            return refreshResult ?: error("未设置 refreshResult")
        }
    }

    private class FakeTokenStorage : AuthTokenStorage {
        val rows = mutableListOf<AccountRow>()
        var activeUserId: String? = null
        val savedResponses = mutableListOf<AccountResponse>()
        var updateTokensCalls = 0
        var updateTokensError: Throwable? = null
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

        override suspend fun getCurrentAccount(): AccountRow? =
            rows.lastOrNull { it.user_id == activeUserId } ?: rows.lastOrNull()

        override suspend fun saveAccount(account: AccountResponse, password: String, deviceToken: String) {
            savedResponses.add(account)
            rows.add(row(account.user.id, accessToken = account.accessToken, refreshToken = account.refreshToken))
            if (activeUserId == null) activeUserId = account.user.id
        }

        override suspend fun getAllAccounts(): List<AccountRow> = rows.toList()
        override suspend fun switchAccount(userId: String): AccountRow {
            activeUserId = userId
            return rows.last { it.user_id == userId }
        }

        override suspend fun deleteAccount(userId: String) {
            rows.removeAll { it.user_id == userId }
        }

        override suspend fun updateTokensForUser(userId: String, accessToken: String, refreshToken: String) {
            updateTokensCalls++
            updateTokensError?.let { throw it }
            rows.replaceAll {
                if (it.user_id == userId) it.copy(access_token = accessToken, refresh_token = refreshToken) else it
            }
        }

        val invalidatedUserIds = mutableListOf<String>()

        override suspend fun invalidateCredentials(userId: String) {
            invalidatedUserIds += userId
            rows.replaceAll {
                if (it.user_id == userId) it.copy(access_token = "", refresh_token = "") else it
            }
        }

        override suspend fun expireAccessToken(userId: String) {
            rows.replaceAll {
                if (it.user_id == userId) it.copy(access_token = "") else it
            }
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
    private lateinit var sessionCache: SessionCache
    private lateinit var illustRepository: IllustRepository
    private lateinit var pixivisionRepository: PixivisionRepository
    private lateinit var pixivisionWebClient: HttpClient

    private var capturedPath: String? = null
    private var editResponseBody: String = """{"error":false,"message":"","body":{"is_succeed":true,"validation_errors":{}}}"""
    private lateinit var accountClient: HttpClient

    @Before
    fun setUp() {
        TokenRefreshPlugin.resetRefreshBreaker()
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
        val sharedSettings = InMemorySettings()
        val sessionCache = SessionCache().also { this.sessionCache = it }
        illustRepository = IllustRepository(
            apiClient = HttpClient(),
            settingsRepository = SettingsRepository(sharedSettings),
            sessionCache = sessionCache,
        )
        // 中-7：Spotlight 特辑缓存随拆分迁至 PixivisionRepository，
        // 用 MockEngine 打桩网页抓取以验证登出/切换账号时的缓存失效。
        pixivisionWebClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    respond(
                        """<html><head><meta property="og:title" content="t"></head><body></body></html>""",
                        HttpStatusCode.OK,
                        headersOf("Content-Type", ContentType.Text.Html.toString()),
                    )
                }
            }
        }
        pixivisionRepository = PixivisionRepository(
            apiClient = HttpClient(),
            webClient = pixivisionWebClient,
            settingsRepository = SettingsRepository(sharedSettings),
            sessionCache = sessionCache,
        )
        repository = AccountRepository(oauth, storage, accountClient, sessionCache)
    }

    private var capturedForm: Map<String, String> = emptyMap()

    private companion object {
        const val SPOTLIGHT_DETAIL_URL = "https://www.pixivision.net/zh/a/9001"
    }

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

    @Test
    fun `logout 清理作品内存缓存`() = runBlocking {
        illustRepository.cacheIllust(cachedIllustFixture(301L))
        pixivisionRepository.getSpotlightArticleDetail(SPOTLIGHT_DETAIL_URL)
        assertEquals("t", pixivisionRepository.getCachedSpotlightDetail(SPOTLIGHT_DETAIL_URL)?.title)

        repository.logout()

        assertNull(illustRepository.getCachedIllust(301L), "登出必须失效作品内存缓存（高-5 缓存清理缺失修复）")
        assertNull(
            pixivisionRepository.getCachedSpotlightDetail(SPOTLIGHT_DETAIL_URL),
            "登出必须失效 Spotlight 特辑缓存（中-7 拆分后保持原清缓存语义）",
        )
    }

    @Test
    fun `switchAccount 清理作品内存缓存`() = runBlocking {
        storage.rows.add(rowFixture("1"))
        storage.rows.add(rowFixture("2"))
        storage.activeUserId = "1"
        illustRepository.cacheIllust(cachedIllustFixture(302L))
        pixivisionRepository.getSpotlightArticleDetail(SPOTLIGHT_DETAIL_URL)

        repository.switchAccount("2")

        assertNull(illustRepository.getCachedIllust(302L), "切换账号必须失效作品内存缓存，防止跨账号读到旧收藏状态")
        assertNull(
            pixivisionRepository.getCachedSpotlightDetail(SPOTLIGHT_DETAIL_URL),
            "切换账号必须失效 Spotlight 特辑缓存（中-7 拆分后保持原清缓存语义）",
        )
    }

    @Test
    fun `loginWithCode 失效会话缓存以防跨账号串数据`() = runBlocking {
        oauth.exchangeResult = oauthEnvelope(userId = "78")
        var invalidations = 0
        sessionCache.register { invalidations++ }

        repository.loginWithCode("code-cache")

        assertEquals(1, invalidations, "登录成功后必须失效全部会话缓存")
    }

    @Test
    fun `loginWithToken 失效会话缓存以防跨账号串数据`() = runBlocking {
        oauth.refreshResult = oauthEnvelope(userId = "79")
        var invalidations = 0
        sessionCache.register { invalidations++ }

        repository.loginWithToken("token-cache")

        assertEquals(1, invalidations, "token 登录成功后必须失效全部会话缓存")
    }

    @Test
    fun `deleteAccount 失效会话缓存`() = runBlocking {
        storage.rows.add(rowFixture("1"))
        storage.rows.add(rowFixture("2"))
        storage.activeUserId = "1"
        var invalidations = 0
        sessionCache.register { invalidations++ }

        repository.deleteAccount("2")

        assertEquals(1, invalidations, "删除账号后必须失效全部会话缓存，避免残留被删账号的数据")
    }

    @Test
    fun `refreshSessionState 在账号凭证为空时发出失效事件`() = runBlocking {
        storage.rows.add(rowFixture("1").copy(access_token = "", refresh_token = ""))
        storage.activeUserId = "1"
        val event = CompletableDeferred<String>()
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            repository.authExpiredFlow.collect { event.complete(it.userId) }
        }

        repository.refreshSessionState()

        assertEquals("1", withTimeoutOrNull(1_000) { event.await() }, "凭证为空的账号应触发重新登录提示")
        job.cancel()
    }

    @Test
    fun `refreshSessionState 在凭证有效时不发失效事件`() = runBlocking {
        storage.rows.add(rowFixture("1"))
        storage.activeUserId = "1"
        var events = 0
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            repository.authExpiredFlow.collect { events++ }
        }

        repository.refreshSessionState()

        assertEquals(0, events, "有效凭证不应触发失效提示")
        job.cancel()
    }

    @Test
    fun `refreshSessionState 在 access 为空但 refresh 可用时静默恢复且不发失效事件`() = runBlocking {
        storage.rows.add(rowFixture("1").copy(access_token = ""))
        storage.activeUserId = "1"
        oauth.refreshResult = oauthEnvelope("1")
        var events = 0
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            repository.authExpiredFlow.collect { events++ }
        }
        val loginEvent = async(start = CoroutineStart.UNDISPATCHED) { repository.loginEventFlow.first() }

        repository.refreshSessionState()

        assertEquals(listOf("rt"), oauth.receivedTokens, "静默恢复必须使用保留的 refresh_token")
        assertEquals(0, events, "静默恢复成功不得提示重新登录")
        assertEquals("access-1", storage.rows.single().access_token, "恢复后应写回新的 access_token")
        assertEquals("refresh-1", storage.rows.single().refresh_token, "恢复后应写回轮换后的 refresh_token")
        assertTrue(withTimeoutOrNull(1_000) { loginEvent.await() } == Unit, "静默恢复后应通知 UI 重新读取账号")
        job.cancel()
    }

    @Test
    fun `refreshSessionState 在静默恢复被服务端拒绝时发出失效事件`() = runBlocking {
        storage.rows.add(rowFixture("1").copy(access_token = ""))
        storage.activeUserId = "1"
        oauth.refreshError = PixivApiException(statusCode = 400, message = "invalid_grant")
        val event = CompletableDeferred<String>()
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            repository.authExpiredFlow.collect { event.complete(it.userId) }
        }

        repository.refreshSessionState()

        assertEquals("1", withTimeoutOrNull(1_000) { event.await() }, "静默恢复被拒必须提示重新登录")
        job.cancel()
    }

    @Test
    fun `refreshSessionState 在静默恢复遇到网络异常时不发失效事件`() = runBlocking {
        storage.rows.add(rowFixture("1").copy(access_token = ""))
        storage.activeUserId = "1"
        oauth.refreshError = RuntimeException("offline")
        var events = 0
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            repository.authExpiredFlow.collect { events++ }
        }

        repository.refreshSessionState()

        assertEquals(0, events, "离线玩家不得被误判为登录失效")
        assertEquals("", storage.rows.single().access_token, "网络异常不得改动本地凭证")
        assertEquals("rt", storage.rows.single().refresh_token, "网络异常必须保留 refresh_token 供下次启动重试")
        job.cancel()
    }

    @Test
    fun `currentAccount 的 hasCredentials 反映凭证是否被清空`() = runBlocking {
        storage.rows.add(rowFixture("1").copy(access_token = "", refresh_token = ""))
        storage.activeUserId = "1"

        assertEquals(false, repository.currentAccount()?.hasCredentials, "token 被清空后 hasCredentials 必须为 false")

        storage.rows.clear()
        storage.rows.add(rowFixture("1"))
        storage.activeUserId = "1"

        assertEquals(true, repository.currentAccount()?.hasCredentials, "完整 token 的账号 hasCredentials 应为 true")
    }

    @Test
    fun `login 在 token 分支被取消时不吞掉取消也不降级调用 loginWithCode`() = runBlocking {
        oauth.refreshError = kotlinx.coroutines.CancellationException("cancelled")
        oauth.exchangeResult = oauthEnvelope()

        assertFailsWith<kotlinx.coroutines.CancellationException> {
            repository.login("plain-token-or-code")
        }
        assertTrue(oauth.receivedCodes.isEmpty(), "CancellationException 不得被降级为 loginWithCode")
    }

    private fun cachedIllustFixture(id: Long): com.perol.pixez.shared.data.model.Illust =
        com.perol.pixez.shared.data.model.Illust(
            id = id,
            title = "Illust $id",
            type = "illust",
            imageUrls = com.perol.pixez.shared.data.model.ImageUrls(squareMedium = "", medium = "", large = ""),
            caption = "",
            restrict = 0,
            user = com.perol.pixez.shared.data.model.IllustUser(
                id = 1L,
                name = "Artist",
                account = "artist",
                profileImageUrls = com.perol.pixez.shared.data.model.IllustProfileImageUrls(medium = ""),
            ),
            tags = emptyList(),
            tools = emptyList(),
            createDate = "2026-10-11T00:00:00+09:00",
            pageCount = 1,
            width = 1000,
            height = 1000,
            sanityLevel = 2,
            xRestrict = 0,
            metaSinglePage = com.perol.pixez.shared.data.model.MetaSinglePage(originalImageUrl = ""),
            metaPages = emptyList(),
            totalView = 0,
            totalBookmarks = 0,
            isBookmarked = false,
            visible = true,
            isMuted = false,
            totalComments = 0,
            illustAIType = 0,
            illustBookStyle = 0,
        )

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

    @Test
    fun `并发调用 refreshSessionState 仅触发一次 refreshToken`() = runBlocking {
        storage.rows.add(rowFixture("1").copy(access_token = "", refresh_token = "rt-1"))
        storage.activeUserId = "1"
        oauth.refreshResult = oauthEnvelope("1")
        val refreshStarted = CompletableDeferred<Unit>()
        val allowFinish = CompletableDeferred<Unit>()
        oauth.onRefreshing = {
            refreshStarted.complete(Unit)
            withTimeoutOrNull(5_000) { allowFinish.await() }
                ?: error("时序超时：allowFinish 未触发")
        }

        val first = launch(start = CoroutineStart.UNDISPATCHED) { repository.refreshSessionState() }
        withTimeoutOrNull(5_000) { refreshStarted.await() } ?: error("首次 refreshSessionState 未进入刷新")
        val second = launch { repository.refreshSessionState() }
        kotlinx.coroutines.delay(50)
        allowFinish.complete(Unit)
        first.join()
        second.join()

        assertEquals(listOf("rt-1"), oauth.receivedTokens, "并发静默恢复应共享互斥锁，仅消耗一次 refresh_token")
        assertEquals("access-1", storage.rows.single().access_token)
        assertEquals("refresh-1", storage.rows.single().refresh_token)
    }

    @Test
    fun `refreshSessionState 与 TokenRefreshPlugin 并发刷新共享互斥锁且仅消耗一次 refresh_token`() = runBlocking {
        storage.rows.add(rowFixture("1").copy(access_token = "stale-at", refresh_token = "rt-1"))
        storage.activeUserId = "1"
        oauth.refreshResult = oauthEnvelope("1")
        val refreshStarted = CompletableDeferred<Unit>()
        val allowFinish = CompletableDeferred<Unit>()
        oauth.onRefreshing = {
            refreshStarted.complete(Unit)
            withTimeoutOrNull(5_000) { allowFinish.await() }
                ?: error("时序超时：allowFinish 未触发")
        }
        val signal = AuthExpirySignal()
        val sharedRepo = AccountRepository(oauth, storage, accountClient, sessionCache, signal)
        lateinit var concurrentJob: kotlinx.coroutines.Job
        val apiClient = HttpClient(MockEngine) {
            install(TokenRefreshPlugin) {
                tokenStorage = storage
                oAuthClient = oauth
                authExpirySignal = signal
            }
            engine {
                addHandler { request ->
                    when (request.headers["Authorization"]) {
                        "Bearer stale-at" -> {
                            // 请求已携带旧 token 发出；响应返回前另一协程触发软失效并启动静默恢复。
                            storage.expireAccessToken("1")
                            concurrentJob = launch { sharedRepo.refreshSessionState() }
                            withTimeoutOrNull(5_000) { refreshStarted.await() }
                                ?: error("静默恢复未进入刷新窗口")
                            respond(
                                """{"error":"invalid_grant","message":"OAuth access token expired"}""",
                                HttpStatusCode.Unauthorized,
                                headersOf("Content-Type", ContentType.Application.Json.toString()),
                            )
                        }
                        "Bearer access-1" -> respond(
                            """{"ok":true}""",
                            HttpStatusCode.OK,
                            headersOf("Content-Type", ContentType.Application.Json.toString()),
                        )
                        else -> respond(
                            """{"error":"missing"}""",
                            HttpStatusCode.Unauthorized,
                            headersOf("Content-Type", ContentType.Application.Json.toString()),
                        )
                    }
                }
            }
        }

        val status = apiClient.use {
            val requestJob = async(start = CoroutineStart.UNDISPATCHED) {
                apiClient.get("https://app-api.pixiv.net/v1/illust/detail").status
            }
            withTimeoutOrNull(5_000) { refreshStarted.await() } ?: error("refreshStarted 未就绪")
            kotlinx.coroutines.delay(50)
            allowFinish.complete(Unit)
            val code = requestJob.await()
            concurrentJob.join()
            code
        }

        assertEquals(HttpStatusCode.OK, status, "等待静默恢复完成后应复用新凭证重放成功")
        assertEquals(listOf("rt-1"), oauth.receivedTokens, "AccountRepository 与 TokenRefreshPlugin 必须共享互斥锁，仅刷新一次")
        assertEquals("access-1", storage.rows.single().access_token)
    }

    @Test
    fun `refreshSessionState 在 refresh_token 格式非法时发出失效事件`() = runBlocking {
        storage.rows.add(rowFixture("1").copy(access_token = "", refresh_token = "bad-rt"))
        storage.activeUserId = "1"
        oauth.refreshError = IllegalArgumentException("invalid refresh token format")
        val event = CompletableDeferred<String>()
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            repository.authExpiredFlow.collect { event.complete(it.userId) }
        }

        repository.refreshSessionState()

        assertEquals("1", withTimeoutOrNull(1_000) { event.await() }, "refresh_token 格式非法应发出失效事件")
        job.cancel()
    }

    @Test
    fun `refreshSessionState 本地写库异常时不得误发失效事件也不得假通知恢复`() = runBlocking {
        storage.rows.add(rowFixture("1").copy(access_token = "", refresh_token = "rt-1"))
        storage.activeUserId = "1"
        oauth.refreshResult = oauthEnvelope("1")
        storage.updateTokensError = RuntimeException("sqlite disk error")
        var authEvents = 0
        var loginEvents = 0
        val job1 = launch(start = CoroutineStart.UNDISPATCHED) {
            repository.authExpiredFlow.collect { authEvents++ }
        }
        val job2 = launch(start = CoroutineStart.UNDISPATCHED) {
            repository.loginEventFlow.collect { loginEvents++ }
        }

        repository.refreshSessionState()

        assertEquals(0, authEvents, "本地写库失败不是凭证被拒，不得发射失效事件")
        assertEquals(0, loginEvents, "本地写库失败未持久化新凭证，不得通知 UI 会话已恢复")
        assertEquals("", storage.rows.single().access_token)
        assertEquals("rt-1", storage.rows.single().refresh_token)
        job1.cancel()
        job2.cancel()
    }

    @Test
    fun `isRefreshingSession 在静默恢复期间为 true 并在结束后复位为 false`() = runBlocking {
        storage.rows.add(rowFixture("1").copy(access_token = "", refresh_token = "rt-1"))
        storage.activeUserId = "1"
        oauth.refreshResult = oauthEnvelope("1")
        val refreshStarted = CompletableDeferred<Unit>()
        val allowFinish = CompletableDeferred<Unit>()
        oauth.onRefreshing = {
            refreshStarted.complete(Unit)
            withTimeoutOrNull(5_000) { allowFinish.await() }
                ?: error("时序超时：allowFinish 未触发")
        }

        assertEquals(false, repository.isRefreshingSession.value, "初始不应处于刷新中")
        val job = launch(start = CoroutineStart.UNDISPATCHED) { repository.refreshSessionState() }
        withTimeoutOrNull(5_000) { refreshStarted.await() } ?: error("refreshSessionState 未进入刷新")
        assertEquals(true, repository.isRefreshingSession.value, "静默恢复执行期间必须暴露 isRefreshingSession = true")

        allowFinish.complete(Unit)
        job.join()
        assertEquals(false, repository.isRefreshingSession.value, "静默恢复结束后必须复位为 false")
    }

    @Test
    fun `cachedCurrentAccount 与 hasCachedAccount 在读取 登录 登出后同步更新`() = runBlocking {
        assertEquals(false, repository.hasCachedAccount, "初始未读取前 hasCachedAccount 应为 false")
        assertNull(repository.cachedCurrentAccount(), "初始缓存账号应为 null")

        // 首次读取（未登录空库）
        assertNull(repository.currentAccount())
        assertEquals(true, repository.hasCachedAccount, "读取过后即使未登录也应标记缓存已初始化")
        assertNull(repository.cachedCurrentAccount())

        // 登录后同步更新快照
        oauth.exchangeResult = oauthEnvelope("777")
        repository.loginWithCode("auth-code")
        assertEquals("777", repository.cachedCurrentAccount()?.userId)

        // 登出后快照立即清为 null 且保持已初始化标记
        repository.logout()
        assertEquals(true, repository.hasCachedAccount)
        assertNull(repository.cachedCurrentAccount())
    }
}
