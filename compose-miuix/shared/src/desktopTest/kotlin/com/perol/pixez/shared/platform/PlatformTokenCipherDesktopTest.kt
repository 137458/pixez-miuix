package com.perol.pixez.shared.platform

import com.perol.pixez.shared.ui.AppConstants
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * 桌面端凭据加解密测试（S-2 / 中-17）：Windows 走系统 DPAPI（CryptProtectData），
 * 密文以 [AppConstants.Auth.TOKEN_ENCRYPTION_PREFIX] 前缀标识，与 Android Keystore 实现共用同一套
 * 前缀协议——无前缀视为历史明文原样返回，下次写回时自动升级为密文。
 *
 * 失败语义（中-17 fail-fast）：带前缀密文解密失败（损坏/平台无 DPAPI）抛出 [TokenCipherException]，
 * 密文不再被原样当凭证返回；非 Windows 加密回退明文直通不抛异常（平台无加密服务，非加密失败）。
 */
class PlatformTokenCipherDesktopTest {

    private val isWindows = System.getProperty("os.name").orEmpty().lowercase().contains("win")

    @Test
    fun `windows 平台加密结果带版本前缀且不泄露明文`() {
        if (!isWindows) return
        val encrypted = PlatformTokenCipher.encrypt(PLAIN)

        assertTrue(encrypted.startsWith(AppConstants.Auth.TOKEN_ENCRYPTION_PREFIX), "密文应带 enc_v1: 前缀")
        assertFalse(encrypted.contains(PLAIN), "密文不应包含明文")
    }

    @Test
    fun `windows 平台 DPAPI 解密可还原明文`() {
        if (!isWindows) return
        val plain = "token-中文-€-with-symbols-1234567890"

        assertEquals(plain, PlatformTokenCipher.decrypt(PlatformTokenCipher.encrypt(plain)))
    }

    @Test
    fun `对已加密的值重复加密幂等`() {
        if (!isWindows) return
        val once = PlatformTokenCipher.encrypt(PLAIN)

        assertEquals(once, PlatformTokenCipher.encrypt(once))
    }

    @Test
    fun `无前缀的历史明文解密时原样返回`() {
        assertEquals(PLAIN, PlatformTokenCipher.decrypt(PLAIN))
    }

    @Test
    fun `空白串不参与加密`() {
        assertEquals("", PlatformTokenCipher.encrypt(""))
        assertEquals("   ", PlatformTokenCipher.encrypt("   "))
        assertEquals("", PlatformTokenCipher.decrypt(""))
    }

    @Test
    fun `损坏密文解密 fail-fast 抛出 TokenCipherException 而非原样返回`() {
        listOf(
            "${AppConstants.Auth.TOKEN_ENCRYPTION_PREFIX}@@not-valid-base64@@",
            AppConstants.Auth.TOKEN_ENCRYPTION_PREFIX,
            "${AppConstants.Auth.TOKEN_ENCRYPTION_PREFIX}AAAA",
        ).forEach { broken ->
            val thrown = assertFailsWith<TokenCipherException> { PlatformTokenCipher.decrypt(broken) }
            assertEquals(TokenCipherException.STAGE_DECRYPT, thrown.stage, "失败环节应标识为解密: $broken")
        }
    }

    @Test
    fun `非 windows 平台回退明文且不抛异常`() {
        if (isWindows) return
        assertEquals(PLAIN, PlatformTokenCipher.encrypt(PLAIN))
        assertEquals(PLAIN, PlatformTokenCipher.decrypt(PLAIN))
    }

    @Test
    fun `DPAPI 加密失败应抛 TokenCipherException 而非返回明文`() {
        if (!isWindows) return
        val cipher = DpapiTokenCipher(
            prefix = AppConstants.Auth.TOKEN_ENCRYPTION_PREFIX,
            protect = { error("模拟 DPAPI 加密故障") },
        )

        assertFailsWith<TokenCipherException> { cipher.encrypt(PLAIN) }
    }

    @Test
    fun `DPAPI 解密失败应抛 TokenCipherException 而非返回密文`() {
        if (!isWindows) return
        val cipher = DpapiTokenCipher(
            prefix = AppConstants.Auth.TOKEN_ENCRYPTION_PREFIX,
            unprotect = { error("模拟 DPAPI 解密故障") },
        )

        val thrown = assertFailsWith<TokenCipherException> {
            cipher.decrypt("${AppConstants.Auth.TOKEN_ENCRYPTION_PREFIX}AAAA")
        }
        assertEquals(TokenCipherException.STAGE_DECRYPT, thrown.stage)
    }

    private companion object {
        const val PLAIN = "1234567890abcdef-access-token"
    }
}
