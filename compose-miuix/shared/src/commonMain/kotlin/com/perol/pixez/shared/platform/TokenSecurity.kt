package com.perol.pixez.shared.platform

/**
 * 敏感凭据（OAuth Token、Refresh Token、密码等）的硬件与平台级加密接口。
 *
 * 在持久化到 SQLite 数据库前进行硬件级 AES-GCM 加密，读取时自动解密；
 * 采用版本前缀保证历史明文凭证无感向前兼容并逐步平滑升级。
 *
 * 失败语义（中-17 fail-fast）：
 * - [encrypt] 平台加密服务异常（密钥不可用等）时抛出 [TokenCipherException]，调用方必须中断保存，
 *   不得静默回退为明文落库；
 * - [decrypt] 对无前缀的历史明文原样返回；对带前缀密文解密失败（密钥丢失、密文损坏）时抛出
 *   [TokenCipherException]，调用方不得把密文原样当作凭证使用；
 *   存储层捕获该异常后按「账号凭证失效」清理对应账号的 token 并引导重新登录。
 */
expect object PlatformTokenCipher {
    /**
     * 加密明文字符串。若已是密文则原样返回。
     *
     * @throws TokenCipherException 平台加密服务异常时（此时明文不得落库）。
     */
    fun encrypt(plainText: String): String

    /**
     * 解密密文字符串。若为未加密的历史明文则原样返回。
     *
     * @throws TokenCipherException 带前缀密文解密失败时（密钥丢失或密文损坏）。
     */
    fun decrypt(cipherText: String): String
}
