package com.perol.pixez.shared.network

/**
 * 跨平台哈希工具：commonMain 声明 expect，各平台提供 actual 实现。
 *
 * 用于生成 Pixiv 请求头所需的 MD5 摘要，以及 OAuth PKCE 的 SHA256 摘要。
 */
expect fun md5(input: ByteArray): ByteArray

expect fun sha256(input: ByteArray): ByteArray
