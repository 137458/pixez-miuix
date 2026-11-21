package com.perol.pixez.shared.network

import java.security.MessageDigest

/**
 * 哈希工具：生成 Pixiv 请求头所需的 MD5 摘要，以及 OAuth PKCE 的 SHA256 摘要。
 */
fun md5(input: ByteArray): ByteArray =
    MessageDigest.getInstance("MD5").run { digest(input) }

fun sha256(input: ByteArray): ByteArray =
    MessageDigest.getInstance("SHA-256").run { digest(input) }
