package com.perol.pixez.shared.platform

import com.perol.pixez.shared.ui.AppConstants
import com.sun.jna.platform.win32.Crypt32Util
import com.sun.jna.platform.win32.WinCrypt
import io.github.aakira.napier.Napier
import java.util.Base64
import kotlin.concurrent.Volatile

/**
 * 桌面端凭据加解密：Windows 使用系统 DPAPI（CryptProtectData，密钥由当前登录用户凭据派生），
 * 密文以 [AppConstants.Auth.TOKEN_ENCRYPTION_PREFIX] 前缀标识，与 Android 的 Keystore AES-GCM
 * 实现共用同一套前缀协议。
 *
 * 无前缀的历史明文原样返回，账号下一次写库时即自动升级为密文；
 * macOS / Linux JVM 无 DPAPI，加解密回退为明文直通（仅记一次日志），不阻塞登录。
 */
actual object PlatformTokenCipher {
    private const val PREFIX = AppConstants.Auth.TOKEN_ENCRYPTION_PREFIX

    @Volatile
    private var fallbackLogged = false

    private val isWindows: Boolean = System.getProperty("os.name").orEmpty().lowercase().contains("win")

    actual fun encrypt(plainText: String): String {
        if (plainText.isBlank() || plainText.startsWith(PREFIX)) return plainText
        if (!isWindows) {
            logFallbackOnce()
            return plainText
        }
        return runCatching {
            val protectedBytes = Crypt32Util.cryptProtectData(
                plainText.toByteArray(Charsets.UTF_8),
                WinCrypt.CRYPTPROTECT_UI_FORBIDDEN,
            )
            PREFIX + Base64.getEncoder().encodeToString(protectedBytes)
        }.getOrElse {
            Napier.w("桌面端 DPAPI 加密失败，安全降级保持原样", it)
            plainText
        }
    }

    actual fun decrypt(cipherText: String): String {
        if (!cipherText.startsWith(PREFIX)) {
            // 历史未加密明文，直接返回原串
            return cipherText
        }
        if (!isWindows) {
            logFallbackOnce()
            return cipherText
        }
        return runCatching {
            val payload = Base64.getDecoder().decode(cipherText.removePrefix(PREFIX))
            val plainBytes = Crypt32Util.cryptUnprotectData(payload, WinCrypt.CRYPTPROTECT_UI_FORBIDDEN)
            String(plainBytes, Charsets.UTF_8)
        }.getOrElse {
            Napier.w("桌面端 DPAPI 解密失败，回退返回原始数据", it)
            cipherText
        }
    }

    private fun logFallbackOnce() {
        if (fallbackLogged) return
        fallbackLogged = true
        Napier.w("当前桌面平台不支持 DPAPI，凭据将以明文存储（仅 Windows 加密）")
    }
}
