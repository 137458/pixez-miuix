package com.perol.pixez.shared.data.cache

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * 会话作用域缓存失效接缝（架构审查 DN4）：
 * 各缓存模块自登记，账号身份变化只调一次 invalidateAll，新增缓存自动被清。
 */
class SessionCacheTest {

    @Test
    fun `invalidateAll 失效全部已登记缓存`() = runTest {
        val sessionCache = SessionCache()
        var clearedA = 0
        var clearedB = 0
        sessionCache.register { clearedA++ }
        sessionCache.register { clearedB++ }

        sessionCache.invalidateAll()

        assertEquals(1, clearedA)
        assertEquals(1, clearedB)
        assertEquals(2, sessionCache.registeredCount)
    }

    @Test
    fun `后登记的缓存无需改调用方也会被失效`() = runTest {
        val sessionCache = SessionCache()
        val first = mutableListOf<String>()
        sessionCache.register { first.clear() }
        first.add("old")
        sessionCache.invalidateAll()
        assertTrue(first.isEmpty())

        // 新增缓存模块只登记自己，调用方（AccountRepository）代码不变
        val second = mutableListOf<String>()
        sessionCache.register { second.clear() }
        second.add("new")
        first.add("refilled")

        sessionCache.invalidateAll()

        assertTrue(first.isEmpty(), "既有缓存仍被清")
        assertTrue(second.isEmpty(), "新登记缓存自动被清，无需改调用方")
        assertEquals(2, sessionCache.registeredCount)
    }

    @Test
    fun `单个失效动作失败不阻断其余清理且异常上抛`() = runTest {
        val sessionCache = SessionCache()
        val cleared = mutableListOf<String>()
        sessionCache.register { throw IllegalStateException("缓存 A 失效失败") }
        sessionCache.register { cleared.add("B") }

        val failure = assertFailsWith<IllegalStateException> { sessionCache.invalidateAll() }

        assertEquals("缓存 A 失效失败", failure.message)
        assertEquals(listOf("B"), cleared, "前一个缓存失效失败不得跳过后续缓存清理")
    }

    @Test
    fun `未登记任何缓存时 invalidateAll 为无操作`() = runTest {
        val sessionCache = SessionCache()

        sessionCache.invalidateAll()

        assertEquals(0, sessionCache.registeredCount)
    }
}
