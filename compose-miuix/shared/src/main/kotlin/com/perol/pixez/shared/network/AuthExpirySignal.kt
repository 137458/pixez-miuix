package com.perol.pixez.shared.network

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** 凭证失效事件：携带失效账号的 userId，供 UI 提示重新登录。 */
data class AuthExpiredEvent(val userId: String)

/**
 * 凭证失效信号：网络层（[TokenRefreshPlugin]）与账号仓库（AccountRepository）共享同一实例。
 *
 * 使用无重放的 [SharedFlow]：订阅者由 UI 根层常驻，事件产生时必然已挂载；
 * 启动自检事件在收集器就绪后触发，不依赖 replay 缓冲。
 */
class AuthExpirySignal {
    private val _events = MutableSharedFlow<AuthExpiredEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<AuthExpiredEvent> = _events.asSharedFlow()

    fun emit(userId: String) {
        _events.tryEmit(AuthExpiredEvent(userId))
    }
}

/** 凭证失效异常：刷新失败收敛后的统一出口。 */
class AuthExpiredException(
    val userId: String,
) : RuntimeException("登录凭证已失效，需要重新登录（userId=$userId）")
