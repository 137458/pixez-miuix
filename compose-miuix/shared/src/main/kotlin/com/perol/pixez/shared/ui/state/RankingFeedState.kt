package com.perol.pixez.shared.ui.state

import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.perol.pixez.shared.data.repository.filterIllustsBySettings
import com.perol.pixez.shared.ui.LocalBanRepository
import com.perol.pixez.shared.ui.LocalIllustRepository
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.i18n.AppStrings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 排行榜页的皮肤无关状态集合：榜单模式/日期筛选、分页状态机、恢复快照与刷新/翻页动作。
 *
 * MIUIX 与 COUI 两套页面共用本实现，皮肤只替换 UI 壳，不复制业务与竞态逻辑。
 */
internal class RankingFeedState(
    val selectedMode: RankingMode,
    val dateInput: String,
    val selectedDate: String?,
    val listState: IllustPagedListState,
    val gridState: LazyStaggeredGridState,
    val isManualRefreshing: Boolean,
    val onModeSelected: (RankingMode) -> Unit,
    val onDateInputChange: (String) -> Unit,
    val refresh: () -> Unit,
    val loadMore: () -> Unit,
)

@Composable
internal fun rememberRankingFeedState(
    feedRestore: RankingFeedRestoreCache,
): RankingFeedState {
    // 中-9：数据仓储统一经 CompositionLocal 注入（RootContent 根部 provide）。
    val repository = LocalIllustRepository.current
    val banRepository = LocalBanRepository.current
    val settingsRepository = LocalSettingsRepository.current
    // 保存用户选择的排行榜模式，进程重建后恢复。
    var selectedMode by rememberSaveable { mutableStateOf(RankingMode.DAY) }
    // 日期输入原始值，格式 YYYY-MM-DD；空字符串表示未选择。
    var dateInput by rememberSaveable { mutableStateOf("") }
    // 经防抖与正则校验后的合法日期，null 表示不应用日期筛选。
    val selectedDate = debouncedRankingDate(dateInput)
    // 重试计数，点击重试或切换模式/日期时触发重新加载。
    var retryCount by rememberSaveable { mutableIntStateOf(0) }
    var isManualRefreshing by rememberSaveable { mutableStateOf(false) }

    val rankingCacheKey = "${selectedMode.code}_${selectedDate.orEmpty()}"
    // 高-5：跨页恢复快照来自 UI 层恢复缓存，只存原始 ID 列表，经仓库 illustsCache 回查原始数据。
    val cachedRanking = feedRestore.get(rankingCacheKey)
    // 高-4c：分页/竞态/加载态收敛到 IllustPagedListState（generation 守卫语义），
    // 过滤链仍为高-3「原始/过滤」双列表本地重过滤。状态机以榜单模式与日期为 remember key：
    // 切换时整体重建，并从恢复缓存快照回查恢复首屏原始列表与下一页 URL。
    val listState = remember(selectedMode, selectedDate) {
        IllustPagedListState(
            fetch = { nextUrl ->
                val response = repository.getRankingResponse(
                    mode = selectedMode.code,
                    date = selectedDate,
                    nextUrl = nextUrl,
                )
                IllustPagedPage(response.illusts, response.nextUrl)
            },
            filter = { rawIllusts -> banRepository.filterIllustsBySettings(rawIllusts, settingsRepository) },
            filterVersionProvider = { settingsRepository.filterChangeVersion.value },
        ).also { state ->
            cachedRanking?.let { snapshot ->
                val raw = snapshot.illustIds.mapNotNull(repository::getCachedIllust)
                if (snapshot.illustIds.isEmpty() || raw.isNotEmpty()) {
                    state.restore(raw, snapshot.nextUrl)
                }
            }
        }
    }
    val coroutineScope = rememberCoroutineScope()
    val gridState = rememberLazyStaggeredGridState()

    // 模式、日期切换或重试时自动重新加载；过滤条件变化时仅对原始列表本地重过滤（高-3）。
    val filterChangeVersion = settingsRepository.filterChangeVersion.collectAsState().value
    LaunchedEffect(selectedMode, selectedDate, retryCount, filterChangeVersion) {
        val force = isManualRefreshing
        if (!force && listState.refilterIfChanged()) {
            // 本地重过滤不改变原始列表与下一页 URL，恢复快照无需更新。
            return@LaunchedEffect
        }
        if (!force && listState.filteredIllusts != null && listState.initialError == null) {
            return@LaunchedEffect
        }
        val outcome = listState.loadFirst(force = force)
        isManualRefreshing = false
        // UI-8：写入成功由状态机回报；失败与竞态失配都不写恢复快照、不回顶。
        if (outcome == PagedLoadOutcome.Written) {
            listState.rawIllusts?.let { feedRestore.update(rankingCacheKey, it, listState.nextUrl) }
            if (force && gridState.firstVisibleItemIndex > 0) {
                gridState.scrollToItem(0)
            }
        }
    }

    val refresh: () -> Unit = {
        isManualRefreshing = true
        retryCount++
    }

    return RankingFeedState(
        selectedMode = selectedMode,
        dateInput = dateInput,
        selectedDate = selectedDate,
        listState = listState,
        gridState = gridState,
        isManualRefreshing = isManualRefreshing,
        onModeSelected = {
            selectedMode = it
            retryCount = 0
        },
        onDateInputChange = { dateInput = it },
        refresh = refresh,
        loadMore = {
            coroutineScope.launch {
                // UI-8：翻页写入成功才写恢复快照；失败/被拒绝/竞态失配都不写。
                if (listState.loadMore() == PagedLoadOutcome.Written) {
                    listState.rawIllusts?.let { feedRestore.update(rankingCacheKey, it, listState.nextUrl) }
                }
            }
        },
    )
}

/**
 * 排行榜模式枚举，code 与 Pixiv API 参数保持一致。
 */
internal enum class RankingMode(
    val code: String,
) {
    DAY("day"),
    WEEK("week"),
    MONTH("month"),
    DAY_MALE("day_male"),
    DAY_FEMALE("day_female"),
    WEEK_ORIGINAL("week_original"),
    DAY_ROOKIE("day_rookie"),
    DAY_AI("day_ai"),
    DAY_R18_AI("day_r18_ai"),
    DAY_R18("day_r18"),
    WEEK_R18("week_r18"),
    WEEK_R18G("week_r18g");

    fun label(strings: AppStrings): String = when (this) {
        DAY -> strings.rankingDay
        WEEK -> strings.rankingWeek
        MONTH -> strings.rankingMonth
        DAY_MALE -> strings.rankingDayMale
        DAY_FEMALE -> strings.rankingDayFemale
        WEEK_ORIGINAL -> strings.rankingWeekOriginal
        DAY_ROOKIE -> strings.rankingDayRookie
        DAY_AI -> strings.rankingDayAi
        DAY_R18_AI -> strings.rankingDayR18Ai
        DAY_R18 -> strings.rankingDayR18
        WEEK_R18 -> strings.rankingWeekR18
        WEEK_R18G -> strings.rankingWeekR18G
    }
}

/**
 * 将日期字符串延迟 500ms 后返回，避免用户逐字输入时频繁触发请求。
 * 空字符串或格式不符合 YYYY-MM-DD 时返回 null，表示不应用日期筛选。
 */
@Composable
private fun debouncedRankingDate(date: String): String? {
    var debounced by remember { mutableStateOf(date) }
    LaunchedEffect(date) {
        delay(RANKING_DATE_DEBOUNCE_MILLIS)
        debounced = date
    }
    return debounced.takeIf { it.matches(RankingDateRegex) }
}

private val RankingDateRegex = Regex("""^\d{4}-\d{2}-\d{2}$""")

/** 榜单日期输入防抖时长（毫秒），避免逐字输入频繁触发请求。 */
private const val RANKING_DATE_DEBOUNCE_MILLIS = 500L
