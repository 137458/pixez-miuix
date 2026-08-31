package com.perol.pixez.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

/**
 * 创建经平台网络引擎深度优化（如 OkHttp 线程池、连接复用、并发上限与 HTTP/2 支持）的 HttpClient。
 *
 * @param block Ktor HttpClient 的通用插件配置块。
 */
expect fun createPlatformHttpClient(block: HttpClientConfig<*>.() -> Unit = {}): HttpClient
