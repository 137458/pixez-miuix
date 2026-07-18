package com.perol.pixez.shared.data.repository

import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException

/**
 * 执行网络/IO 请求并统一记录非取消异常。
 *
 * 协程取消异常不会被捕获，避免干扰协程取消传播。
 */
internal suspend fun <T> networkCall(
    errorMessage: String,
    block: suspend () -> T,
): T {
    return try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Napier.e(errorMessage, e)
        throw e
    }
}
