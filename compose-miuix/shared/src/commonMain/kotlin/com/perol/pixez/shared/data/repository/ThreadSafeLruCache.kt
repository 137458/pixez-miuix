package com.perol.pixez.shared.data.repository

/**
 * 协程安全的泛型内存 LRU 缓存。
 *
 * - 内部基于 [LinkedHashMap] 保证最近最少使用（LRU）淘汰顺序；
 * - 线程安全通过内部同步互斥锁保障，[get] 读取时更新访问顺序（真正的 LRU 契约）；
 * - 消除写操作过程中的冗余深拷贝分配，仅在 [snapshot] 调用时按需生成只读副本。
 */
class ThreadSafeLruCache<K, V>(val maxCapacity: Int) {
    init {
        require(maxCapacity > 0) { "maxCapacity must be greater than 0, but was $maxCapacity" }
    }

    private val lock = Any()
    private val map = LinkedHashMap<K, V>()

    /**
     * 读取指定 [key] 对应的值。若存在则将其标记为最近访问并返回；不存在返回 null。
     */
    fun get(key: K): V? = synchronized(lock) {
        val value = map.remove(key) ?: return null
        map[key] = value
        value
    }

    /**
     * 获取当前缓存内容的只读快照副本。
     */
    fun snapshot(): Map<K, V> = synchronized(lock) {
        map.toMap()
    }

    /**
     * 写入键值对。已存在时更新值并移至最新，超过容量时淘汰最久未访问的项。
     */
    suspend fun put(key: K, value: V) {
        putSync(key, value)
    }

    /** 同步写入重载，便于非协程上下文调用。 */
    fun putSync(key: K, value: V) = synchronized(lock) {
        map.remove(key)
        map[key] = value
        evictIfNeeded()
    }

    suspend fun putAll(entries: List<Pair<K, V>>) {
        putAll(entries.asIterable())
    }

    suspend fun putAll(entries: Iterable<Pair<K, V>>) {
        putAllSync(entries)
    }

    /** 同步批量写入重载。 */
    fun putAllSync(entries: Iterable<Pair<K, V>>) = synchronized(lock) {
        for ((key, value) in entries) {
            map.remove(key)
            map[key] = value
        }
        evictIfNeeded()
    }

    /**
     * 手动将 [key] 标记为最近使用。
     */
    suspend fun touch(key: K): Boolean = synchronized(lock) {
        val value = map.remove(key) ?: return false
        map[key] = value
        true
    }

    /**
     * 移除指定 [key] 的缓存项。
     */
    suspend fun remove(key: K): V? = synchronized(lock) {
        map.remove(key)
    }

    /**
     * 清空缓存。
     */
    suspend fun clear() = synchronized(lock) {
        map.clear()
    }

    private fun evictIfNeeded() {
        while (map.size > maxCapacity) {
            val oldest = map.keys.firstOrNull() ?: break
            map.remove(oldest)
        }
    }
}
