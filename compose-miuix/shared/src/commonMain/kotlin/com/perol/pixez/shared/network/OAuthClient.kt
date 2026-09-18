package com.perol.pixez.shared.network

import com.perol.pixez.shared.data.model.Account
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Parameters
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Pixiv OAuth2 认证客户端。
 *
 * 负责：
 * - 生成 PKCE 流程所需的 code_verifier / code_challenge。
 * - 构建 WebView 授权 URL。
 * - 用授权码换取 access_token / refresh_token。
 * - 用 refresh_token 刷新 access_token。
 */
class OAuthClient(
    private val httpClient: HttpClient,
) {
    // 维护最近生成的 verifier 列表（保留最近 10 个），采用 Volatile 不可变快照保障跨端原子性
    @kotlin.concurrent.Volatile
    private var verifiers: List<String> = emptyList()

    val lastCodeVerifier: String?
        get() = verifiers.lastOrNull()

    /**
     * 生成新的 PKCE code_verifier 并计算对应的 code_challenge。
     *
     * 调用后可通过 [lastCodeVerifier] 获取刚生成的 verifier，供 token 交换时使用。
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun generatePkcePair(): PkcePair {
        val verifier = generateCodeVerifier()
        verifiers = (verifiers + verifier).takeLast(10)
        val challenge = sha256(verifier.encodeToByteArray())
        val challengeBase64 = Base64.UrlSafe.encode(challenge).trimEnd { it == '=' }
        return PkcePair(verifier, challengeBase64)
    }

    /**
     * 构建用户登录授权 URL，需在 WebView / 浏览器中打开。
     *
     * @param create 是否进入创建账号流程。
     */
    fun buildLoginUrl(create: Boolean = false): String {
        val pair = generatePkcePair()
        val base = if (create) {
            "https://app-api.pixiv.net/web/v1/provisional-accounts/create"
        } else {
            "https://app-api.pixiv.net/web/v1/login"
        }
        return "$base?code_challenge=${pair.codeChallenge}&code_challenge_method=S256&client=pixiv-android"
    }

    /**
     * 用授权码换取 token。
     *
     * @param code 从授权回调中提取的 code。
     * @param codeVerifier 与登录 URL 中 challenge 对应的 verifier；
     *                     若为空则优先使用最近记录的 verifier。
     */
    suspend fun exchangeCodeForToken(
        code: String,
        codeVerifier: String? = null,
    ): Account {
        val cleanCode = code.trim()
        require(cleanCode.isNotBlank()) { "授权码 code 不能为空。" }

        val targetVerifier = if (!codeVerifier.isNullOrBlank()) {
            codeVerifier.trim()
        } else {
            verifiers.lastOrNull()
                ?: throw IllegalArgumentException("缺少 code_verifier，无法交换 token。请重新在应用内点击“使用浏览器登录”。")
        }

        // Pixiv 授权码 code 为单次消费凭据，一次性使用正确的 verifier 交换，避免重试作废凭据
        return requestTokenWithCode(cleanCode, targetVerifier)
    }

    private suspend fun requestTokenWithCode(code: String, verifier: String): Account {
        return httpClient.post("/auth/token") {
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("code", code)
                        append("redirect_uri", REDIRECT_URI)
                        append("grant_type", "authorization_code")
                        append("include_policy", "true")
                        append("client_id", CLIENT_ID)
                        append("client_secret", CLIENT_SECRET)
                        append("code_verifier", verifier)
                    },
                ),
            )
        }.body()
    }

    /**
     * 用 refresh_token 刷新 access_token。
     */
    suspend fun refreshToken(refreshToken: String): Account {
        val cleanToken = refreshToken.trim()
        require(cleanToken.isNotBlank()) { "Refresh Token 不能为空" }
        return httpClient.post("/auth/token") {
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("client_id", CLIENT_ID)
                        append("client_secret", CLIENT_SECRET)
                        append("grant_type", "refresh_token")
                        append("refresh_token", cleanToken)
                        append("include_policy", "true")
                    },
                ),
            )
        }.body()
    }

    /**
     * 生成 128 位随机 code_verifier。
     *
     * 使用密码学安全随机源，并从 256 种可能值映射到 64 字符集，分布均匀。
     */
    private fun generateCodeVerifier(): String {
        val randomBytes = secureRandomBytes(CODE_VERIFIER_LENGTH)
        return buildString(CODE_VERIFIER_LENGTH) {
            randomBytes.forEach { byte ->
                append(CODE_VERIFIER_CHARS[(byte.toInt() and 0xFF) % CODE_VERIFIER_CHARS.length])
            }
        }
    }

    data class PkcePair(
        val codeVerifier: String,
        val codeChallenge: String,
    )

    companion object {
        private const val CLIENT_ID = "MOBrBDS8blbauoSck0ZfDbtuzpyT"
        private const val CLIENT_SECRET = "lsACyCD94FhDUtGTXi3QzcFE2uU1hqtDaKeqrdwj"
        private const val REDIRECT_URI =
            "https://app-api.pixiv.net/web/v1/users/auth/pixiv/callback"

        // PKCE verifier 允许的字符集，长度 128。
        private const val CODE_VERIFIER_CHARS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
        private const val CODE_VERIFIER_LENGTH = 128
    }
}
