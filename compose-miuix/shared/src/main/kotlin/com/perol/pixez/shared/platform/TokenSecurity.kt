package com.perol.pixez.shared.platform

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import io.github.aakira.napier.Napier
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

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
object PlatformTokenCipher {
    private const val PREFIX = TokenCipherDefaults.TOKEN_ENCRYPTION_PREFIX
    private const val KEY_ALIAS = TokenCipherDefaults.KEYSTORE_ALIAS
    private const val ANDROID_KEY_STORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val IV_LENGTH = 12

    private val keyLock = Any()
    @Volatile
    private var cachedSecretKey: SecretKey? = null

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val currentKey = cachedSecretKey
        if (currentKey != null) return currentKey
        return synchronized(keyLock) {
            val doubleCheckKey = cachedSecretKey
            if (doubleCheckKey != null) return@synchronized doubleCheckKey

            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
                val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(keyGenParameterSpec)
                val key = keyGenerator.generateKey()
                cachedSecretKey = key
                key
            } else {
                val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
                val key = entry?.secretKey ?: throw IllegalStateException("未能从 AndroidKeyStore 获取到凭证密钥")
                cachedSecretKey = key
                key
            }
        }
    }

    /**
     * 加密明文字符串。若已是密文则原样返回。
     *
     * @throws TokenCipherException 平台加密服务异常时（此时明文不得落库）。
     */
    fun encrypt(plainText: String): String {
        if (plainText.isBlank() || plainText.startsWith(PREFIX)) return plainText
        return try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv ?: throw TokenCipherException(
                TokenCipherException.STAGE_ENCRYPT,
                "Android Cipher 未返回 IV",
            )
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
            PREFIX + Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: TokenCipherException) {
            throw e
        } catch (e: Throwable) {
            // 中-17 fail-fast：Keystore 异常（系统升级/恢复出厂后密钥丢失）时上抛中断保存，
            // 明文凭证不得静默落库；存储层捕获 TokenCipherException 后按凭证失效清理。
            Napier.e("凭证硬件加密失败，中断保存（fail-fast）", e)
            throw TokenCipherException(TokenCipherException.STAGE_ENCRYPT, "Android Keystore 凭证加密失败", e)
        }
    }

    /**
     * 解密密文字符串。若为未加密的历史明文则原样返回。
     *
     * @throws TokenCipherException 带前缀密文解密失败时（密钥丢失或密文损坏）。
     */
    fun decrypt(cipherText: String): String {
        if (!cipherText.startsWith(PREFIX)) {
            // 历史未加密明文，直接返回原串
            return cipherText
        }
        return try {
            val base64Payload = cipherText.removePrefix(PREFIX)
            val combined = Base64.decode(base64Payload, Base64.NO_WRAP)
            if (combined.size <= IV_LENGTH) {
                throw TokenCipherException(
                    TokenCipherException.STAGE_DECRYPT,
                    "密文长度不足（${combined.size} <= $IV_LENGTH），密文已损坏",
                )
            }

            val iv = ByteArray(IV_LENGTH)
            val encryptedBytes = ByteArray(combined.size - IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH)
            System.arraycopy(combined, IV_LENGTH, encryptedBytes, 0, encryptedBytes.size)

            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: TokenCipherException) {
            throw e
        } catch (e: Throwable) {
            // 中-17 fail-fast：密文损坏或密钥丢失时上抛，密文不得被当作凭证原样返回；
            // 存储层捕获后按「凭证失效」清理该账号并引导重新登录。
            Napier.e("凭据解密失败，按凭证失效处理（fail-fast）", e)
            throw TokenCipherException(TokenCipherException.STAGE_DECRYPT, "Android Keystore 凭证解密失败", e)
        }
    }
}
