package com.perol.pixez.shared.network

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.toCValues
import platform.CoreCrypto.CC_MD5
import platform.CoreCrypto.CC_MD5_DIGEST_LENGTH
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH

@OptIn(ExperimentalForeignApi::class)
actual fun md5(input: ByteArray): ByteArray = memScoped {
    val digest = allocArray<UByteVar>(CC_MD5_DIGEST_LENGTH)
    CC_MD5(input.toUByteArray().toCValues(), input.size.toUInt(), digest)
    digest.readBytes(CC_MD5_DIGEST_LENGTH)
}

@OptIn(ExperimentalForeignApi::class)
actual fun sha256(input: ByteArray): ByteArray = memScoped {
    val digest = allocArray<UByteVar>(CC_SHA256_DIGEST_LENGTH)
    CC_SHA256(input.toUByteArray().toCValues(), input.size.toUInt(), digest)
    digest.readBytes(CC_SHA256_DIGEST_LENGTH)
}
