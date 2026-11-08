package com.perol.pixez.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.Darwin

// modeProvider 由桌面/Android actual 的 DoH DNS 消费；Darwin 引擎自管 DNS，此参数仅对齐 expect 签名。
// followRedirects 仅由更新包下载使用，iOS 不提供应用内更新下载，故忽略。
actual fun createPlatformHttpClient(
    modeProvider: (isOauthHost: Boolean) -> String?,
    followRedirects: Boolean,
    block: HttpClientConfig<*>.() -> Unit,
): HttpClient =
    HttpClient(Darwin) {
        engine {
            configureRequest {
                setAllowsCellularAccess(true)
            }
        }
        block()
    }
