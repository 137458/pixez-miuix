package com.perol.pixez.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json

/**
 * 创建用于检查 GitHub Release 的 HttpClient，复用平台网络引擎（含 DoH / 代理支持）。
 *
 * @param modeProvider DoH 网络模式提供者，语义见 [createPlatformHttpClient]；
 *   由 AppDependencies 注入用户策略，产出客户端交由容器 [com.perol.pixez.shared.AppDependencies.close] 统一关闭。
 */
internal fun createUpdateCheckClient(
    modeProvider: (isOauthHost: Boolean) -> String? = { null },
): HttpClient = createPlatformHttpClient(modeProvider) {
    install(ContentNegotiation) {
        json(HttpClientDefaults.commonJson)
    }
    webTimeouts()
}
