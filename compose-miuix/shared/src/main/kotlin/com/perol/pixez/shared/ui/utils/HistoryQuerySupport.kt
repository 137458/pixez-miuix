package com.perol.pixez.shared.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel

/**
 * 通用历史记录加载：以仓储实例与 [refreshToken] 为 key 的 [produceState] 包装，
 * 插画与小说两页签共用；value 为 null 表示首次加载中。
 *
 * 仓库内部已切换 IO 调度器，调用方无需再包 withContext。
 *
 * 落位说明：本函数被 MIUIX 与 COUI 两套历史页共同调用，属皮肤无关的共享工具，
 * 因此放在 ui/utils 中立位置，避免页面文件反向充当共享工具宿主（COUI 页面原本需要
 * import 一个具体页面文件才能复用它）。签名与可见性保持迁移前不变。
 */
@Composable
internal fun <T> rememberHistoryResult(
    repository: Any?,
    refreshToken: Int,
    load: suspend () -> List<T>,
): State<Result<List<T>>?> =
    produceState<Result<List<T>>?>(initialValue = null, repository, refreshToken) {
        value = suspendRunCatchingNonCancel { load() }
    }

/**
 * 历史搜索过滤的公共脚手架：关键词去首尾空白后为空时返回全量，否则按谓词过滤。
 * 仅供 derivedStateOf 闭包调用，query 与结果 State 的读取保持在派生作用域内。
 *
 * 落位说明同 [rememberHistoryResult]：两套皮肤共用的纯函数，不放页面文件。
 */
internal fun <T> filterHistoryByQuery(
    items: List<T>,
    query: String,
    matches: (item: T, keyword: String) -> Boolean,
): List<T> {
    val trimmed = query.trim()
    return if (trimmed.isEmpty()) items else items.filter { matches(it, trimmed) }
}
