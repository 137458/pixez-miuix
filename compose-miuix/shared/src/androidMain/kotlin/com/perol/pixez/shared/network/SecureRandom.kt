package com.perol.pixez.shared.network

import java.security.SecureRandom

/**
 * 密码学安全随机字节生成（基于 [SecureRandom]）。
 */
fun secureRandomBytes(count: Int): ByteArray {
    require(count > 0) { "count 必须大于 0" }
    return ByteArray(count).apply {
        SecureRandom().nextBytes(this)
    }
}
