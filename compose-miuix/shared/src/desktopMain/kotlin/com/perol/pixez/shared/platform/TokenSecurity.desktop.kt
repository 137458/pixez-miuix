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
 * macOS / Linux JVM 无 DPAPI，加密回退为明文直通（仅记一次日志），不阻塞登录。
 *
 * 失败语义（中-17 fail-fast）：Windows 加解密失败与非 Windows 解密带前缀密文均抛出
 * [TokenCipherException]，由存储层按「账号凭证失效」清理，密文/明文不再被静默当作凭证返回。
 */
actual object PlatformTokenCipher {
    private const val PREFIX = AppConstants.Auth.TOKEN_ENCRYPTION_PREFIX

    @Volatile
    private var fallbackLogged = false

    private val isWindows: Boolean = System.getProperty("os.name").orEmpty().lowercase().contains("win")

    private val dpapiCipher = DpapiTokenCipher(PREFIX)

    actual fun encrypt(plainText: String): String {
        if (plainText.isBlank() || plainText.startsWith(PREFIX)) return plainText
        if (!isWindows) {
            logFallbackOnce()
            return plainText
        }
        return dpapiCipher.encrypt(plainText)
    }

    actual fun decrypt(cipherText: String): String {
        if (!cipherText.startsWith(PREFIX)) {
            // 历史未加密明文，直接返回原串
            return cipherText
        }
        if (!isWindows) {
            // 带 DPAPI 前缀的密文在本平台无法解密，按凭证失效上抛，密文不得被当作凭证返回
            throw TokenCipherException(
                TokenCipherException.STAGE_DECRYPT,
                "当前桌面平台无 DPAPI，无法解密 Windows 加密凭证",
            )
        }
        return dpapiCipher.decrypt(cipherText)
    }

    private fun logFallbackOnce() {
        if (fallbackLogged) return
        fallbackLogged = true
        Napier.w("当前桌面平台不支持 DPAPI，凭据将以明文存储（仅 Windows 加密）")
    }
}

/**
 * Windows DPAPI（CryptProtectData / CryptUnprotectData）加解密核心。
 *
 * protect/unprotect 可注入，供 desktopTest 注入平台加密故障验证 fail-fast 行为（中-17），
 * 生产路径使用系统调用默认值。加解密失败抛出 [TokenCipherException]，不再静默降级。
 */
internal class DpapiTokenCipher(
    private val prefix: String,
    private val protect: (ByteArray) -> ByteArray = {
        Crypt32Util.cryptProtectData(it, WinCrypt.CRYPTPROTECT_UI_FORBIDDEN)
    },
    private val unprotect: (ByteArray) -> ByteArray = {
        Crypt32Util.cryptUnprotectData(it, WinCrypt.CRYPTPROTECT_UI_FORBIDDEN)
    },
) {
    fun encrypt(plainText: String): String {
        if (plainText.isBlank() || plainText.startsWith(prefix)) return plainText
        return try {
            prefix + Base64.getEncoder().encodeToString(protect(plainText.toByteArray(Charsets.UTF_8)))
        } catch (e: Throwable) {
            // 中-17 fail-fast：DPAPI 加密失败上抛中断保存，明文凭证不得静默落库
            Napier.e("桌面端 DPAPI 加密失败，中断保存（fail-fast）", e)
            throw TokenCipherException(TokenCipherException.STAGE_ENCRYPT, "Windows DPAPI 凭证加密失败", e)
        }
    }

    fun decrypt(cipherText: String): String {
        if (!cipherText.startsWith(prefix)) {
            // 历史未加密明文，直接返回原串
            return cipherText
        }
        return try {
            val payload = Base64.getDecoder().decode(cipherText.removePrefix(prefix))
            String(unprotect(payload), Charsets.UTF_8)
        } catch (e: Throwable) {
            // 中-17 fail-fast：密文损坏（用户凭据变化/数据损坏）时上抛，密文不得被当作凭证原样返回；
            // 存储层捕获后按「凭证失效」清理该账号并引导重新登录。
            Napier.e("桌面端 DPAPI 解密失败，按凭证失效处理（fail-fast）", e)
            throw TokenCipherException(TokenCipherException.STAGE_DECRYPT, "Windows DPAPI 凭证解密失败", e)
        }
    }
}
