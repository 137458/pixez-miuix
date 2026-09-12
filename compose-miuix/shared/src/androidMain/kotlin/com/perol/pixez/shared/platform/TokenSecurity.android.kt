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

import com.perol.pixez.shared.ui.AppConstants

actual object PlatformTokenCipher {
    private const val PREFIX = AppConstants.Auth.TOKEN_ENCRYPTION_PREFIX
    private const val KEY_ALIAS = AppConstants.Auth.KEYSTORE_ALIAS
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

    actual fun encrypt(plainText: String): String {
        if (plainText.isBlank() || plainText.startsWith(PREFIX)) return plainText
        return try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv ?: return plainText
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
            PREFIX + Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Throwable) {
            Napier.w("凭证硬件加密失败，安全降级保持原样", e)
            plainText
        }
    }

    actual fun decrypt(cipherText: String): String {
        if (!cipherText.startsWith(PREFIX)) {
            // 历史未加密明文，直接返回原串
            return cipherText
        }
        return try {
            val base64Payload = cipherText.removePrefix(PREFIX)
            val combined = Base64.decode(base64Payload, Base64.NO_WRAP)
            if (combined.size <= IV_LENGTH) return cipherText

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
        } catch (e: Throwable) {
            Napier.w("凭据解密失败，回退返回原始数据", e)
            cipherText
        }
    }
}
