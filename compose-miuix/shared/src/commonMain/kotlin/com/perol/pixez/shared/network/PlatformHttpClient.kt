package com.perol.pixez.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

/**
 * 创建经平台网络引擎深度优化（如 OkHttp 线程池、连接复用、并发上限与 HTTP/2 支持）的 HttpClient。
 *
 * @param modeProvider DoH 网络模式提供者：按请求主机是否为 OAuth 域返回用户设置的网络模式
 *   （standard 时 DNS 走系统解析，其余走 DoH）；返回 null 表示模式未知，按 DoH 处理。
 *   由装配层（AppDependencies 等）构造注入，network 层不反向依赖容器。
 * @param block Ktor HttpClient 的通用插件配置块。
 */
expect fun createPlatformHttpClient(
    modeProvider: (isOauthHost: Boolean) -> String? = { null },
    block: HttpClientConfig<*>.() -> Unit = {},
): HttpClient
