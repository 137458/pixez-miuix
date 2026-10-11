package com.perol.pixez.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.Darwin

// modeProvider 由桌面/Android actual 的 DoH DNS 消费；Darwin 引擎自管 DNS，此参数仅对齐 expect 签名。
actual fun createPlatformHttpClient(
    modeProvider: (isOauthHost: Boolean) -> String?,
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
