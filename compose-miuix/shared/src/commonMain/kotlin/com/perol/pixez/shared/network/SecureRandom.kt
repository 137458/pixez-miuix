package com.perol.pixez.shared.network

/**
 * 跨平台密码学安全随机字节生成。
 *
 * Android/Desktop 使用 SecureRandom。
 */
expect fun secureRandomBytes(count: Int): ByteArray
