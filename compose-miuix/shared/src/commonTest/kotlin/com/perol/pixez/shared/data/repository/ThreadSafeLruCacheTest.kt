package com.perol.pixez.shared.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ThreadSafeLruCacheTest {

    @Test
    fun testEvictionAtMaxCapacity() = runBlocking {
        val cache = ThreadSafeLruCache<Int, String>(maxCapacity = 3)
        cache.put(1, "one")
        cache.put(2, "two")
        cache.put(3, "three")

        assertEquals("one", cache.get(1))
        assertEquals("two", cache.get(2))
        assertEquals("three", cache.get(3))
        assertEquals(3, cache.snapshot().size)

        // 插入第 4 个元素，超出容量 3，最早插入的 1 应被淘汰
        cache.put(4, "four")

        assertNull(cache.get(1), "Key 1 should have been evicted")
        assertEquals("two", cache.get(2))
        assertEquals("three", cache.get(3))
        assertEquals("four", cache.get(4))
        assertEquals(3, cache.snapshot().size)
    }

    @Test
    fun testAccessOrderingKeepingHotItems() = runBlocking {
        val cache = ThreadSafeLruCache<Int, String>(maxCapacity = 3)
        cache.put(1, "one")
        cache.put(2, "two")
        cache.put(3, "three")

        // 重新写入 key 1（将其提升为最新访问/热数据）
        cache.put(1, "one-refreshed")

        // 插入第 4 个元素，此时最旧的元素应当是 2（因为 1 被更新过），2 应被淘汰
        cache.put(4, "four")

        assertNull(cache.get(2), "Key 2 should have been evicted as the least recently used")
        assertEquals("one-refreshed", cache.get(1))
        assertEquals("three", cache.get(3))
        assertEquals("four", cache.get(4))
    }

    @Test
    fun testGetMissingKeyReturnsNull() {
        val cache = ThreadSafeLruCache<String, String>(maxCapacity = 5)
        assertNull(cache.get("non_existent_key"))
    }

    @Test
    fun testSnapshotImmutability() = runBlocking {
        val cache = ThreadSafeLruCache<Int, String>(maxCapacity = 3)
        cache.put(1, "one")
        cache.put(2, "two")

        val snapshot1 = cache.snapshot()
        assertEquals(2, snapshot1.size)
        assertEquals("one", snapshot1[1])
        assertEquals("two", snapshot1[2])

        // 写入新数据及触发淘汰
        cache.put(3, "three")
        cache.put(4, "four")

        // 验证之前的 snapshot1 引用内容保持不变（不可变副本）
        assertEquals(2, snapshot1.size)
        assertEquals("one", snapshot1[1])
        assertEquals("two", snapshot1[2])
        assertNull(snapshot1[3])
        assertNull(snapshot1[4])

        // 验证当前新快照包含最新状态
        val snapshot2 = cache.snapshot()
        assertEquals(3, snapshot2.size)
        assertNull(snapshot2[1])
        assertEquals("two", snapshot2[2])
        assertEquals("three", snapshot2[3])
        assertEquals("four", snapshot2[4])
    }

    @Test
    fun testPutAllRespectsMaxCapacityAndOrder() = runBlocking {
        val cache = ThreadSafeLruCache<Int, String>(maxCapacity = 3)
        val entries = listOf(
            1 to "one",
            2 to "two",
            3 to "three",
            4 to "four",
        )
        cache.putAll(entries)

        assertEquals(3, cache.snapshot().size)
        assertNull(cache.get(1))
        assertEquals("two", cache.get(2))
        assertEquals("three", cache.get(3))
        assertEquals("four", cache.get(4))
    }

    @Test
    fun testThreadSafetyUnderConcurrentAccess() = runBlocking {
        val cache = ThreadSafeLruCache<Int, Int>(maxCapacity = 50)
        val jobs = (1..20).map { workerId ->
            launch(Dispatchers.Default) {
                for (i in 1..50) {
                    val key = (workerId * 100) + (i % 30)
                    cache.put(key, i)
                    cache.get(key)
                    cache.snapshot()
                }
            }
        }
        jobs.joinAll()

        val finalSnapshot = cache.snapshot()
        assertTrue(finalSnapshot.size <= 50, "Snapshot size must never exceed maxCapacity")
    }
}
