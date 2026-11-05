package com.perol.pixez.shared.data.cache

import kotlinx.coroutines.CancellationException

/**
 * 会话作用域缓存失效接缝。
 *
 * 「账号身份变化必须失效全部账号相关缓存」这条不变量由本类独占：各缓存模块在构造时
 * 向此登记自己的失效动作，账号切换/登出只调用 [invalidateAll]。新增缓存模块只需登记一次，
 * 调用方（[com.perol.pixez.shared.data.repository.AccountRepository]）无需改动，
 * 也不会因漏改而跨账号读到旧数据。
 *
 * 登记发生在容器构造期（单线程），[invalidateAll] 在协程中执行；实现按登记顺序串行失效。
 */
class SessionCache {

    private val invalidators = mutableListOf<suspend () -> Unit>()

    /** 已登记的失效动作数量（诊断与测试用）。 */
    val registeredCount: Int get() = invalidators.size

    /**
     * 登记一个失效动作。重复登记同一实例会导致重复清理，故各缓存模块只在构造时登记一次。
     */
    fun register(invalidate: suspend () -> Unit) {
        invalidators += invalidate
    }

    /**
     * 失效全部已登记缓存。单个失效动作抛错不应阻断其余缓存清理，故逐个隔离并收集首个异常后重抛。
     */
    suspend fun invalidateAll() {
        var firstFailure: Exception? = null
        for (invalidate in invalidators.toList()) {
            try {
                invalidate()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (firstFailure == null) firstFailure = e
            }
        }
        firstFailure?.let { throw it }
    }
}
