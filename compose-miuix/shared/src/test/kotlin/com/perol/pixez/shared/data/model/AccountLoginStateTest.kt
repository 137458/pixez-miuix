package com.perol.pixez.shared.data.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AccountLoginStateTest {

    @Test
    fun `账号行为空时不算登录`() {
        val account: AccountPersist? = null
        assertFalse(account.isLoggedIn, "无账号行时必须判定为未登录")
    }

    @Test
    fun `同时持有 access 与 refresh token 时算登录`() {
        assertTrue(account(accessToken = "access", refreshToken = "refresh").isLoggedIn)
    }

    @Test
    fun `access token 被清空时不算登录`() {
        assertFalse(account(accessToken = "", refreshToken = "refresh").isLoggedIn, "凭证被清理后不得仍判定为已登录")
    }

    @Test
    fun `refresh token 被清空时不算登录`() {
        assertFalse(account(accessToken = "access", refreshToken = " ").isLoggedIn, "刷新令牌缺失时无法维持登录态")
    }

    private fun account(accessToken: String, refreshToken: String) = AccountPersist(
        userId = "100",
        userImage = "",
        accessToken = accessToken,
        refreshToken = refreshToken,
        deviceToken = "",
        name = "tester",
        account = "tester",
        mailAddress = "tester@example.com",
        passWord = "",
        isPremium = 0,
        xRestrict = 0,
        isMailAuthorized = 0,
    )
}
