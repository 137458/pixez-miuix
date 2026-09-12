package com.perol.pixez.shared.data.repository

import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * 执行网络/IO 请求并统一记录非取消异常。
 *
 * 默认调度到 [Dispatchers.IO] 上执行，确保响应流读取与大型 JSON 反序列化不会阻塞主线程。
 * 协程取消异常不会被捕获，避免干扰协程取消传播。
 */
internal suspend fun <T> networkCall(
    errorMessage: String,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    block: suspend () -> T,
): T = withContext(dispatcher) {
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Napier.e(errorMessage, e)
        throw e
    }
}

