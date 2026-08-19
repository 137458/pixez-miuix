package com.perol.pixez.shared.data.repository

import com.perol.pixez.shared.data.local.account.Account
import com.perol.pixez.shared.data.model.Account as OAuthAccount
import com.perol.pixez.shared.data.model.AccountEditResponse
import com.perol.pixez.shared.data.model.AccountPersist
import com.perol.pixez.shared.network.AuthTokenStorage
import com.perol.pixez.shared.network.OAuthClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Parameters

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 账号仓库：封装 OAuth 登录、token 刷新、账号信息编辑与本地账号持久化。
 */
class AccountRepository(
    private val oAuthClient: OAuthClient,
    private val tokenStorage: AuthTokenStorage,
    private val accountClient: HttpClient,
) {
    private val _loginEventFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val loginEventFlow: SharedFlow<Unit> = _loginEventFlow.asSharedFlow()

    /**
     * 当前已登录账号，未登录返回 null。
     */
    suspend fun currentAccount(): AccountPersist? = tokenStorage.getCurrentAccount()?.toPersist()

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
        _loginEventFlow.tryEmit(Unit)
        return account
    }

    /**
     * 用 Pixiv Refresh Token 直接完成登录并持久化账号信息。
     */
    suspend fun loginWithToken(token: String): OAuthAccount {
        val account = oAuthClient.refreshToken(token.trim())
        tokenStorage.saveAccount(account.response)
        _loginEventFlow.tryEmit(Unit)
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
        } catch (tokenEx: Exception) {
            try {
                loginWithCode(clean)
            } catch (codeEx: Exception) {
                // 如果两项均失败，抛出最具诊断价值的异常信息
                throw tokenEx
            }
        }
    }


    /**
     * 登出：清空本地账号数据。
     */
    suspend fun logout() {
        tokenStorage.clear()
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

        // 使用原子更新接口：读取、网络请求、本地保存全程受 Mutex 保护，消除并发窗口。
        tokenStorage.updateCurrentAccount { account ->
            // 读取阶段：当前未登录时直接抛异常，中断后续网络请求与写入。
            val current = account
                ?: throw IllegalStateException("没有登录账号，无法编辑账号信息")

            // 提交阶段：调用 Pixiv 账号编辑接口修改邮箱或密码。
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

            // 响应校验阶段：解析响应体并校验业务成功标志；失败时抛异常，避免错误更新本地缓存。
            if (response.error || !response.body.isSucceed) {
                throw IllegalStateException(
                    response.message.takeIf { it.isNotBlank() } ?: "账号信息修改失败",
                )
            }

            // 保存阶段：接口调用成功后，返回更新后的账号对象，由存储层原子写入数据库。
            current.copy(
                password = if (!newPassword.isNullOrBlank()) newPassword else current.password,
                mail_address = if (!newMailAddress.isNullOrBlank()) newMailAddress else current.mail_address,
            )
        }
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
