package com.perol.pixez.shared.network

import java.security.MessageDigest

actual fun md5(input: ByteArray): ByteArray =
    MessageDigest.getInstance("MD5").run { digest(input) }

actual fun sha256(input: ByteArray): ByteArray =
    MessageDigest.getInstance("SHA-256").run { digest(input) }
