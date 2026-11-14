package com.perol.pixez.shared.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.perol.pixez.shared.data.model.AccountPersist
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.isLoggedIn
import com.perol.pixez.shared.data.repository.BanRepository
import com.perol.pixez.shared.data.repository.SearchRepository
import com.perol.pixez.shared.data.repository.filterIllustsBySettings
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.AppConstants.IllustType
import com.perol.pixez.shared.ui.LocalAccountRepository
import com.perol.pixez.shared.ui.state.IllustPagedListState
import com.perol.pixez.shared.ui.state.IllustPagedPage
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import kotlinx.coroutines.delay

/**
 * 作品搜索结果的分页状态：查询词 + 排序 + 筛选条件构成的搜索上下文、
 * 账号身份跟踪、防抖后的日期范围与分页状态机。
 *
 * MIUIX 瀑布流与 COUI 瀑布流共用本状态，避免两套皮肤各自维护请求与竞态逻辑。
 */
internal class SearchIllustResultsState(
    val listState: IllustPagedListState,
    val retry: () -> Unit,
)

@Composable
internal fun rememberSearchIllustResultsState(
    query: String,
    sort: String,
    filterState: SearchFilterState,
    repository: SearchRepository,
    banRepository: BanRepository,
    settingsRepository: SettingsRepository,
): SearchIllustResultsState {
    // 登录状态与账号身份跟踪：登录/登出/切号（loginEventFlow）到达时作废旧账号结果并重拉首屏。
    val accountRepository = LocalAccountRepository.current
    var user by remember { mutableStateOf<AccountPersist?>(null) }
    var isLoggedIn by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) {
        // 挂起上下文读取当前账号：suspendRunCatchingNonCancel 捕获异常并保留取消语义（同 NewScreen）。
        user = suspendRunCatchingNonCancel { accountRepository.currentAccount() }.getOrNull()
        isLoggedIn = user.isLoggedIn
        accountRepository.loginEventFlow.collect {
            user = suspendRunCatchingNonCancel { accountRepository.currentAccount() }.getOrNull()
            isLoggedIn = user.isLoggedIn
        }
    }

    val searchTarget = filterState.searchTarget
    val searchAiType = filterState.searchAiType
    val bookmarkThreshold = filterState.bookmarkThreshold
    val startDate = filterState.startDate.takeIf { it.isNotBlank() }
    val endDate = filterState.endDate.takeIf { it.isNotBlank() }

    // 根据收藏数阈值构建实际搜索词：先清除原有 \d+users入り，再按需追加 " ${bookmarkThreshold}users入り"。
    val searchWord = remember(query, bookmarkThreshold) {
        val cleanQuery = query.replace(Regex("""\s*\d+users入り"""), "").trim()
        if (bookmarkThreshold > 0) "$cleanQuery ${bookmarkThreshold}users入り" else cleanQuery
    }

    // 搜索结果重试计数，作为 LaunchedEffect 的 key 触发重新加载。
    var retryCount by rememberSaveable(
        searchWord,
        sort,
        searchTarget,
        searchAiType,
        startDate,
        endDate,
    ) { mutableIntStateOf(0) }

    // 对日期输入做防抖，避免用户逐字输入时频繁请求。
    val effectiveStartDate = debouncedSearchDate(startDate)
    val effectiveEndDate = debouncedSearchDate(endDate)

    // 高-4b：分页/竞态/加载态收敛到 IllustPagedListState（RankingScreen 版 generation 守卫），
    // 过滤链为高-3「原始/过滤」双列表本地重过滤。状态机以查询条件为 remember key：
    // 分类/关键词/排序/筛选条件变化时整体重建（等价原实现的清空重载），retryCount 重试与
    // filterChangeVersion 本地重过滤复用同一实例。
    val listState = remember(
        searchWord,
        sort,
        searchTarget,
        searchAiType,
        effectiveStartDate,
        effectiveEndDate,
    ) {
        IllustPagedListState(
            fetch = { nextUrl ->
                val response = repository.searchIllustResponse(
                    word = searchWord,
                    sort = sort,
                    searchTarget = searchTarget,
                    searchAiType = searchAiType,
                    startDate = effectiveStartDate,
                    endDate = effectiveEndDate,
                    nextUrl = nextUrl,
                )
                IllustPagedPage(response.illusts, response.nextUrl)
            },
            filter = { raw -> banRepository.filterIllustsBySettings(raw, settingsRepository) },
            filterVersionProvider = { settingsRepository.filterChangeVersion.value },
        )
    }

    val filterChangeVersion = settingsRepository.filterChangeVersion.collectAsState().value
    // 账号身份跟踪：登录/登出/切号（loginEventFlow 更新 user/isLoggedIn）时作废旧账号结果并重拉首屏。
    val accountUnloaded = remember { Any() }
    var loadedForAccount by remember { mutableStateOf<Any?>(accountUnloaded) }
    LaunchedEffect(
        searchWord,
        sort,
        searchTarget,
        searchAiType,
        effectiveStartDate,
        effectiveEndDate,
        retryCount,
        filterChangeVersion,
        isLoggedIn,
        user,
    ) {
        // 登录态未检测完成（null）时不发起加载，避免账号解析前后误判为账号切换。
        if (isLoggedIn == null) return@LaunchedEffect
        val accountSwitched = loadedForAccount !== accountUnloaded && loadedForAccount != user
        if (accountSwitched) listState.reset()
        loadedForAccount = user
        // 过滤条件变化且已持有原始列表时仅本地重过滤（高-3 语义，零网络）；
        // 查询条件变化时状态机已随 remember key 重建，此处走首屏加载；retryCount 变化时走重试。
        if (listState.refilterIfChanged()) return@LaunchedEffect
        listState.loadFirst()
    }

    return SearchIllustResultsState(
        listState = listState,
        retry = { retryCount++ },
    )
}

/**
 * 作品结果的客户端二次过滤：Ugoira 类型与长宽比筛选在服务端结果上本地执行。
 */
internal fun applySearchClientFilters(list: List<Illust>, ugoira: Int, ratio: Int): List<Illust> {
    var res = when (ugoira) {
        1 -> list.filter { IllustType.isUgoira(it.type) }
        2 -> list.filterNot { IllustType.isUgoira(it.type) }
        else -> list
    }
    res = when (ratio) {
        1 -> res.filter { it.width > it.height }
        2 -> res.filter { it.height > it.width }
        3 -> res.filter { it.width == it.height }
        else -> res
    }
    return res
}

/** 搜索日期输入防抖时长（毫秒）。 */
private const val SEARCH_DATE_DEBOUNCE_MILLIS = 500L

/**
 * 将日期字符串延迟 500ms 后返回，避免用户逐字输入时频繁触发搜索。
 * 空字符串或格式不符合 YYYY-MM-DD 时返回 null，表示不应用该日期筛选。
 */
@Composable
internal fun debouncedSearchDate(date: String?): String? {
    if (date == null) return null
    var debounced by remember { mutableStateOf(date) }
    LaunchedEffect(date) {
        delay(SEARCH_DATE_DEBOUNCE_MILLIS)
        debounced = date
    }
    return debounced.takeIf { it.matches(SearchDateRegex) }
}

private val SearchDateRegex = Regex("""^\d{4}-\d{2}-\d{2}$""")
