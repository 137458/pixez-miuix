package com.perol.pixez.shared.ui.utils

import kotlinx.coroutines.CancellationException

/**
 * 执行代码块并包装为 [Result]，但显式重新抛出 [CancellationException]。
 *
 * 在 [androidx.compose.runtime.produceState] 等协程环境中必须使用此函数替代 [runCatching]，
 * 否则协程取消时产生的 [CancellationException] 会被捕获并转为错误状态，导致取消语义被破坏。
 */
inline fun <T> runCatchingNonCancel(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    Result.failure(e)
}
