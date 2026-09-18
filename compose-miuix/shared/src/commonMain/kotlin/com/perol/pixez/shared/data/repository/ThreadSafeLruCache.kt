package com.perol.pixez.shared.data.repository

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 协程安全的泛型内存 LRU 缓存。
 *
 * - 写入操作通过 [Mutex] 串行化，保持最近最少使用淘汰策略与高并发原子性；
 * - 读取操作直接读取无锁不可变 [snapshot]，具备极高吞吐量与无锁开销；
 * - 每次写操作更新不可变快照，保障外部并发读取的强一致性与隔离性。
 */
class ThreadSafeLruCache<K, V>(val maxCapacity: Int) {
    init {
        require(maxCapacity > 0) { "maxCapacity must be greater than 0, but was $maxCapacity" }
    }

    private val mutex = Mutex()
    private val map = LinkedHashMap<K, V>()

    @kotlin.concurrent.Volatile
    private var snapshot: Map<K, V> = emptyMap()

    fun get(key: K): V? = snapshot[key]

    fun snapshot(): Map<K, V> = snapshot

    suspend fun put(key: K, value: V) {
        mutex.withLock {
            map.remove(key)
            map[key] = value
            evictIfNeeded()
            snapshot = map.toMap()
        }
    }

    suspend fun putAll(entries: List<Pair<K, V>>) {
        putAll(entries.asIterable())
    }

    suspend fun putAll(entries: Iterable<Pair<K, V>>) {
        mutex.withLock {
            for ((key, value) in entries) {
                map.remove(key)
                map[key] = value
            }
            evictIfNeeded()
            snapshot = map.toMap()
        }
    }

    suspend fun touch(key: K): Boolean {
        return mutex.withLock {
            val value = map.remove(key) ?: return@withLock false
            map[key] = value
            snapshot = map.toMap()
            true
        }
    }

    suspend fun remove(key: K): V? {
        return mutex.withLock {
            val removed = map.remove(key)
            if (removed != null) {
                snapshot = map.toMap()
            }
            removed
        }
    }

    suspend fun clear() {
        mutex.withLock {
            map.clear()
            snapshot = emptyMap()
        }
    }

    private fun evictIfNeeded() {
        while (map.size > maxCapacity) {
            val oldest = map.keys.firstOrNull() ?: break
            map.remove(oldest)
        }
    }
}
