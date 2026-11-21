package com.perol.pixez.shared.data.cache

import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.repository.ThreadSafeLruCache

/**
 * 作品内存缓存模块：容量、淘汰与清理规则由本模块独占。
 *
 * 读写方（[com.perol.pixez.shared.data.repository.IllustRepository]、
 * [com.perol.pixez.shared.data.repository.UserRepository]、
 * [com.perol.pixez.shared.data.repository.SearchRepository]）只依赖本模块，
 * 不再把 `IllustRepository?` 当缓存句柄跨模块传递（可空即「忘传就静默不写」）。
 */
class IllustMemoryCache(capacity: Int = DEFAULT_CAPACITY) {

    private val cache = ThreadSafeLruCache<Long, Illust>(capacity)

    /** 写入单个作品（按作品 ID 去重，超出容量时淘汰最久未访问项）。 */
    suspend fun put(illust: Illust) {
        cache.put(illust.id, illust)
    }

    /** 批量写入作品。 */
    suspend fun putAll(illusts: Iterable<Illust>) {
        cache.putAll(illusts.map { it.id to it })
    }

    /** 按作品 ID 读取缓存快照，未命中返回 null。 */
    fun get(illustId: Long): Illust? = cache.get(illustId)

    /** 清空全部作品缓存（账号身份变化时经 [SessionCache] 触发）。 */
    suspend fun clear() {
        cache.clear()
    }

    companion object {
        /** 缓存容量上限（条）。 */
        const val DEFAULT_CAPACITY = 500
    }
}
