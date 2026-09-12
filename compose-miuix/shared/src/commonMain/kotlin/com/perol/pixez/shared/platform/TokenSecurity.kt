package com.perol.pixez.shared.platform

/**
 * 敏感凭据（OAuth Token、Refresh Token、密码等）的硬件与平台级加密接口。
 *
 * 在持久化到 SQLite 数据库前进行硬件级 AES-GCM 加密，读取时自动解密；
 * 采用版本前缀保证历史明文凭证无感向前兼容并逐步平滑升级。
 */
expect object PlatformTokenCipher {
    /**
     * 加密明文字符串。若已是密文则原样返回。
     */
    fun encrypt(plainText: String): String

    /**
     * 解密密文字符串。若为未加密的历史明文则原样返回。
     */
    fun decrypt(cipherText: String): String
}
