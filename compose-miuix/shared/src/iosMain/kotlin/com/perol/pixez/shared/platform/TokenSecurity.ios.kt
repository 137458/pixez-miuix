package com.perol.pixez.shared.platform

actual object PlatformTokenCipher {
    actual fun encrypt(plainText: String): String = plainText
    actual fun decrypt(cipherText: String): String = cipherText
}
