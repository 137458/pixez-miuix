package com.perol.pixez.shared.data.repository

import com.perol.pixez.shared.data.local.account.Account
import com.perol.pixez.shared.data.model.Account as OAuthAccount
import com.perol.pixez.shared.data.model.AccountPersist
import com.perol.pixez.shared.network.AuthTokenStorage
import com.perol.pixez.shared.network.OAuthClient

/**
 * 账号仓库：封装 OAuth 登录、token 刷新与本地账号持久化。
 */
class AccountRepository(
    private val oAuthClient: OAuthClient,
    private val tokenStorage: AuthTokenStorage,
) {
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
        return account
    }

    /**
     * 登出：清空本地账号数据。
     */
    suspend fun logout() {
        tokenStorage.clear()
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
