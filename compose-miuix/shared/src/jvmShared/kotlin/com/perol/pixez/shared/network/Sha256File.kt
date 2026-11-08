package com.perol.pixez.shared.network

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

private const val STREAM_BUFFER_SIZE = 64 * 1024

/**
 * 流式计算文件 SHA-256（64KB 分块，支持大安装包），返回小写 hex 或 null（IO 失败）。
 */
fun sha256File(file: File): String? = runCatching {
    MessageDigest.getInstance("SHA-256").run {
        FileInputStream(file).use { input ->
            val buffer = ByteArray(STREAM_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                update(buffer, 0, read)
            }
        }
        digest().joinToString("") { byte -> (byte.toInt() and 0xFF).toString(16).padStart(2, '0') }
    }
}.getOrNull()