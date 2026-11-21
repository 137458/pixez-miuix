package com.perol.pixez.shared.data.cache

import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * 会话作用域缓存失效接缝。
 *
 * 「账号身份变化必须失效全部账号相关缓存」这条不变量由本类独占：各缓存模块在构造时
 * 向此登记自己的失效动作，账号切换/登出只调用 [invalidateAll]。新增缓存模块只需登记一次，
 * 调用方（[com.perol.pixez.shared.data.repository.AccountRepository]）无需改动，
 * 也不会因漏改而跨账号读到旧数据。
 *
 * 登记与失效可能来自不同线程（账号切换与网络回调），因此登记列表使用 [MutableStateFlow]
 * 的原子 [update] 维护；[invalidateAll] 基于快照执行，单个动作失败只记录日志，
 * 不阻断其余缓存清理，也不向上重抛——否则调用方后续的登录事件会被吞掉。
 */
class SessionCache {

    private val invalidators = MutableStateFlow<List<suspend () -> Unit>>(emptyList())

    /** 已登记的失效动作数量（诊断与测试用）。 */
    val registeredCount: Int get() = invalidators.value.size

    /**
     * 登记一个失效动作。重复登记同一实例会导致重复清理，故各缓存模块只在构造时登记一次。
     */
    fun register(invalidate: suspend () -> Unit) {
        invalidators.update { it + invalidate }
    }

    /**
     * 失效全部已登记缓存。逐个隔离异常：取消继续传播，其余失败仅记录日志，
     * 保证调用方一定能在其后发出登录事件。
     */
    suspend fun invalidateAll() {
        for (invalidate in invalidators.value) {
            try {
                invalidate()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Napier.w("会话缓存失效动作执行失败，已跳过", e)
            }
        }
    }
}
