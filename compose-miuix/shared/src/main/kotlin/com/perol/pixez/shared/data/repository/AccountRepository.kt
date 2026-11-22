package com.perol.pixez.shared.data.repository

import com.perol.pixez.shared.data.cache.SessionCache
import com.perol.pixez.shared.data.local.account.Account
import com.perol.pixez.shared.data.model.Account as OAuthAccount
import com.perol.pixez.shared.data.model.AccountEditResponse
import com.perol.pixez.shared.data.model.AccountPersist
import com.perol.pixez.shared.network.AuthExpiredEvent
import com.perol.pixez.shared.network.AuthExpirySignal
import com.perol.pixez.shared.network.AuthTokenStorage
import com.perol.pixez.shared.network.OAuthClient
import com.perol.pixez.shared.network.PixivApiException
import com.perol.pixez.shared.network.TokenRefreshPlugin
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Parameters

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.Volatile

/**
 * 账号仓库：封装 OAuth 登录、token 刷新、账号信息编辑与本地账号持久化。
 */
class AccountRepository(
    private val oAuthClient: OAuthClient,
    private val tokenStorage: AuthTokenStorage,
    private val accountClient: HttpClient,
    private val sessionCache: SessionCache,
    private val authExpirySignal: AuthExpirySignal = AuthExpirySignal(),
) {
    private val _loginEventFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val loginEventFlow: SharedFlow<Unit> = _loginEventFlow.asSharedFlow()

    private val refreshSessionCounter = AtomicInteger(0)
    private val _isRefreshingSession = MutableStateFlow(false)

    /** 启动静默恢复是否正在进行中；UI 首帧据此前置等待，避免把软失效账号误判为未登录。 */
    val isRefreshingSession: StateFlow<Boolean> = _isRefreshingSession.asStateFlow()

    @Volatile
    private var cachedAccount: AccountPersist? = null

    /** 是否已同步过至少一次当前账号内存快照（区分「未读取」与「已读取为未登录」）。 */
    @Volatile
    var hasCachedAccount: Boolean = false
        private set

    /**
     * 凭证失效事件：刷新失败或启动自检发现账号凭证为空时发出，UI 根层据此提示重新登录。
     */
    val authExpiredFlow: SharedFlow<AuthExpiredEvent> = authExpirySignal.events

    /**
     * 账号身份发生变化后的统一收尾：失效全部已登记会话缓存并通知 UI 重新加载。
     *
     * 登录、切换、删除、登出都会改变当前账号身份，任何按账号维度缓存的数据
     * （收藏标记、用户资料、关注列表等）都必须整体作废，否则会读到上一个账号的残留。
     * 缓存清单由各模块向 [SessionCache] 自登记，新增缓存自动被覆盖清理。
     */
    private suspend fun onSessionIdentityChanged() {
        cachedAccount = tokenStorage.getCurrentAccount()?.toPersist()
        hasCachedAccount = true
        sessionCache.invalidateAll()
        _loginEventFlow.tryEmit(Unit)
    }

    /** 同步读取最近一次已缓存的当前账号，供设置页等二级页退回重建首帧免延迟渲染。 */
    fun cachedCurrentAccount(): AccountPersist? = cachedAccount

    /**
     * 当前已登录账号，未登录返回 null。
     */
    suspend fun currentAccount(): AccountPersist? =
        tokenStorage.getCurrentAccount()?.toPersist().also {
            cachedAccount = it
            hasCachedAccount = true
        }

    /**
     * 启动自检：凭证缺失时先尝试静默恢复，恢复不了才发失效事件。
     *
     * 刷新被服务端拒绝只会软失效（只清 access_token、保留 refresh_token，见
     * [AuthTokenStorage.expireAccessToken]）；启动时用保留的 refresh_token 换一次新凭证，
     * 成功则用户完全无感，失败再提示重新登录。
     * 由 UI 根层在订阅 [authExpiredFlow] 之后调用，保证提示不会因时序问题丢失。
     */
    suspend fun refreshSessionState() {
        refreshSessionCounter.incrementAndGet()
        _isRefreshingSession.value = true
        try {
            val restored = TokenRefreshPlugin.withRefreshLock {
                val account = tokenStorage.getCurrentAccount() ?: return@withRefreshLock false
                if (account.hasCredentials()) return@withRefreshLock false
                if (account.refresh_token.isBlank()) {
                    authExpirySignal.emit(account.user_id)
                    return@withRefreshLock false
                }
                if (TokenRefreshPlugin.isRefreshBreakerActive(account.user_id)) {
                    return@withRefreshLock false
                }
                val refreshed = try {
                    oAuthClient.refreshToken(account.refresh_token)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: PixivApiException) {
                    // 只有 OAuth 端点明确拒绝才判定需要重新登录；服务端 5xx 等保持凭证待下次重试。
                    if (e.statusCode == 400 || e.statusCode == 401) {
                        Napier.w("账号(${account.user_id})静默恢复被拒（HTTP ${e.statusCode}），需要重新登录")
                        TokenRefreshPlugin.markRefreshBreaker(account.user_id)
                        authExpirySignal.emit(account.user_id)
                    } else {
                        Napier.w("账号(${account.user_id})静默恢复失败（HTTP ${e.statusCode}），保留凭证待下次启动重试")
                    }
                    return@withRefreshLock false
                } catch (e: IllegalArgumentException) {
                    Napier.w("账号(${account.user_id})静默恢复被拒（refresh_token 格式非法），需要重新登录", e)
                    TokenRefreshPlugin.markRefreshBreaker(account.user_id)
                    authExpirySignal.emit(account.user_id)
                    return@withRefreshLock false
                } catch (e: Exception) {
                    // 离线或超时等网络异常不得把用户踢下线：保留凭证，下次启动再试。
                    Napier.w("账号(${account.user_id})静默恢复网络失败，保留凭证待下次启动重试", e)
                    return@withRefreshLock false
                }

                // 写库异常与网络异常隔离：网络续期已消耗旧 refresh_token，但本地落库故障既不是服务端拒绝，
                // 也未完成本地持久化，不得误发失效信号或通知 UI 假恢复。
                try {
                    tokenStorage.updateTokensForUser(
                        userId = account.user_id,
                        accessToken = refreshed.response.accessToken,
                        refreshToken = refreshed.response.refreshToken,
                    )
                    TokenRefreshPlugin.clearRefreshBreaker(account.user_id)
                    Napier.i("账号(${account.user_id})凭证已静默恢复")
                    true
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Napier.e("账号(${account.user_id})静默恢复成功但凭证写回本地失败，保留原凭证待重试", e)
                    false
                }
            }
            if (restored) {
                onSessionIdentityChanged()
            }
        } finally {
            if (refreshSessionCounter.decrementAndGet() == 0) {
                _isRefreshingSession.value = false
            }
        }
    }

    /**
     * 生成 WebView 登录 URL。
     *
     * @param create 是否进入创建账号流程。
     */
    fun loginUrl(create: Boolean = false): String = oAuthClient.buildLoginUrl(create)

    /**
     * 用授权码完成登录并持久化账号信息。
     */
    suspend fun loginWithCode(code: String): OAuthAccount {
        val account = oAuthClient.exchangeCodeForToken(code)
        tokenStorage.saveAccount(account.response)
        TokenRefreshPlugin.clearRefreshBreaker(account.response.user.id)
        onSessionIdentityChanged()
        return account
    }

    /**
     * 用 Pixiv Refresh Token 直接完成登录并持久化账号信息。
     */
    suspend fun loginWithToken(token: String): OAuthAccount {
        val account = oAuthClient.refreshToken(token.trim())
        tokenStorage.saveAccount(account.response)
        TokenRefreshPlugin.clearRefreshBreaker(account.response.user.id)
        onSessionIdentityChanged()
        return account
    }

    /**
     * 智能统一登录：自动识别 Refresh Token、授权码或回调 URL 完成登录。
     *
     * 1. 若输入为 pixiv:// 回调 URL 或包含 code= 参数，自动提取 code 走 PKCE 授权码登录。
     * 2. 若输入包含 JSON 结构（如导出或复制的 refresh_token JSON），自动提取 token 登录。
     * 3. 自动剥离 Bearer、引号、换行符等常见噪音字符。
     * 4. 纯文本凭证自动结合格式特征走 refresh_token 刷新或授权码交换。
     */
    suspend fun login(input: String): OAuthAccount {
        var raw = input.trim()
        // 去除外层可能包裹的引号或换行
        raw = raw.removeSurrounding("\"", "\"")
            .removeSurrounding("'", "'")
            .trim()
        require(raw.isNotBlank()) { "登录凭证不能为空" }

        // 1. 如果是回调 URL 或带有 code 参数（例如 pixiv://account/login?code=xxx）
        val codeMatch = Regex("""(?:[?&]|^)code=([^&\s"'#]+)""").find(raw)
        if (codeMatch != null) {
            val code = codeMatch.groupValues[1]
            return loginWithCode(code)
        }

        // 2. 如果是 JSON 格式凭证（例如 {"refresh_token": "xxx"}）
        val jsonTokenMatch = Regex(""""refresh_token"\s*:\s*"([^"]+)"""").find(raw)
        if (jsonTokenMatch != null) {
            val token = jsonTokenMatch.groupValues[1].trim()
            return loginWithToken(token)
        }

        // 3. 剥离可能存在的 Bearer / token: / refresh_token: 前缀
        var clean = raw
        for (prefix in listOf("bearer ", "token:", "token：", "refresh_token:", "refresh_token：")) {
            if (clean.startsWith(prefix, ignoreCase = true)) {
                clean = clean.substring(prefix.length).trim()
                clean = clean.removeSurrounding("\"", "\"").removeSurrounding("'", "'").trim()
            }
        }

        // 4. 优先尝试作为 Refresh Token 登录；若失败则降级尝试作为授权码登录
        return try {
            loginWithToken(clean)
        } catch (ce: CancellationException) {
            throw ce
        } catch (tokenEx: Exception) {
            try {
                loginWithCode(clean)
            } catch (ce: CancellationException) {
                throw ce
            } catch (codeEx: Exception) {
                // 如果两项均失败，抛出最具诊断价值的异常信息
                throw tokenEx
            }
        }
    }

    /**
     * 获取本地所有已登录账号列表。
     */
    suspend fun getAllAccounts(): List<AccountPersist> =
        tokenStorage.getAllAccounts().map { it.toPersist() }

    /**
     * 切换当前活跃账号。
     */
    suspend fun switchAccount(userId: String) {
        tokenStorage.switchAccount(userId)
        onSessionIdentityChanged()
    }

    /**
     * 移除指定账号。
     */
    suspend fun deleteAccount(userId: String) {
        tokenStorage.deleteAccount(userId)
        onSessionIdentityChanged()
    }

    /**
     * 登出：清空本地所有账号数据。
     */
    suspend fun logout() {
        tokenStorage.clear()
        TokenRefreshPlugin.resetRefreshBreaker()
        onSessionIdentityChanged()
    }

    /**
     * 编辑当前账号信息：修改邮箱或密码。
     *
     * 调用 accounts.pixiv.net/api/account/edit，成功后更新本地账号缓存中的邮箱与密码。
     *
     * @param currentPassword 当前密码，用于接口鉴权。
     * @param newPassword 新密码，为空表示不修改。
     * @param newMailAddress 新邮箱地址，为空表示不修改。
     * @throws IllegalStateException 当前未登录时抛出。
     */
    suspend fun editAccount(
        currentPassword: String,
        newPassword: String?,
        newMailAddress: String?,
    ) = networkCall("编辑账号信息失败") {
        // 边界校验：当前密码不能为空，避免将空密码发送到服务端。
        require(currentPassword.isNotBlank()) { "当前密码不能为空" }

        // 只在读取和提交时短暂持有 storage 锁；网络请求必须在锁外执行，
        // 否则 Authorization 注入/Token 刷新会再次访问 tokenStorage 形成自锁死锁。
        val current = tokenStorage.getCurrentAccount()
            ?: throw IllegalStateException("没有登录账号，无法编辑账号信息")
        val response = accountClient.post("/api/account/edit") {
            header("Content-Type", "application/x-www-form-urlencoded")
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("current_password", currentPassword)
                        if (!newPassword.isNullOrBlank()) {
                            append("new_password", newPassword)
                        }
                        if (!newMailAddress.isNullOrBlank()) {
                            append("new_mail_address", newMailAddress)
                        }
                    },
                ),
            )
        }.body<AccountEditResponse>()

        if (response.error || !response.body.isSucceed) {
            throw IllegalStateException(
                response.message.takeIf { it.isNotBlank() } ?: "账号信息修改失败",
            )
        }

        tokenStorage.updateCurrentAccount { latest ->
            val account = latest ?: throw IllegalStateException("账号已退出登录")
            require(account.id == current.id) { "账号在编辑期间已切换，请重试" }
            account.copy(
                password = if (!newPassword.isNullOrBlank()) newPassword else account.password,
                mail_address = if (!newMailAddress.isNullOrBlank()) newMailAddress else account.mail_address,
            )
        }
        cachedAccount = tokenStorage.getCurrentAccount()?.toPersist()
        hasCachedAccount = true
    }
}

/**
 * 将 SQLDelight 生成的 [Account] 转换为对外暴露的 [AccountPersist]。
 */
private fun Account.toPersist(): AccountPersist = AccountPersist(
    id = id.toInt(),
    userId = user_id,
    userImage = user_image,
    accessToken = access_token,
    refreshToken = refresh_token,
    deviceToken = device_token,
    name = name,
    account = account,
    mailAddress = mail_address,
    passWord = password,
    isPremium = is_premium.toInt(),
    xRestrict = x_restrict.toInt(),
    isMailAuthorized = is_mail_authorized.toInt(),
)

/**
 * 是否仍持有可用凭证：解密失败或被判定吊销后 token 会被清空，但账号行保留。
 * 登录态判定必须用它，不能用「账号行是否存在」。
 */
private fun Account.hasCredentials(): Boolean =
    access_token.isNotBlank() && refresh_token.isNotBlank()
