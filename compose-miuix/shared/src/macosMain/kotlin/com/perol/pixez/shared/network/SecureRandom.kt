package com.perol.pixez.shared.network

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.readBytes
import platform.Security.SecRandomCopyBytes
import platform.Security.kSecRandomDefault

@OptIn(ExperimentalForeignApi::class)
actual fun secureRandomBytes(count: Int): ByteArray {
    require(count > 0) { "count 必须大于 0" }
    return memScoped {
        val bytes = allocArray<UByteVar>(count)
        val status = SecRandomCopyBytes(kSecRandomDefault, count.toULong(), bytes)
        check(status == 0) { "安全随机数生成失败: $status" }
        bytes.readBytes(count)
    }
}
