package com.perol.pixez.shared.data.model

/**
 * 基于 [selector] 的幂等合并：已有元素的选择值先入集合，新元素仅在选择值未出现时保留。
 *
 * 6 个模型曾各写一份逐行同构的 appendDistinct（架构审查 R1），语义只在两点上不同：
 * 选择器（作品 ID / 用户 ID / 系列 ID）与「选择值为 null 时是否参与去重」。
 * 后者收敛为 [keepNullKey] 策略参数，各模型的公开扩展函数保留原签名以兼容调用点。
 *
 * @param keepNullKey 选择值为 null 的元素是否直接保留（不参与去重）。
 *   Comment 的 null-id 属异常数据，语义为 true；其余模型选择值非空，参数无副作用。
 */
fun <T, K : Any> List<T>.appendDistinctBy(
    newItems: List<T>,
    keepNullKey: Boolean = false,
    selector: (T) -> K?,
): List<T> {
    if (newItems.isEmpty()) return this
    if (this.isEmpty()) return newItems
    val existingKeys = HashSet<K>(size + newItems.size)
    for (item in this) {
        val key = selector(item) ?: continue
        existingKeys.add(key)
    }
    val uniqueNew = newItems.filter { item ->
        val key = selector(item)
        if (key == null) keepNullKey else existingKeys.add(key)
    }
    return this + uniqueNew
}
