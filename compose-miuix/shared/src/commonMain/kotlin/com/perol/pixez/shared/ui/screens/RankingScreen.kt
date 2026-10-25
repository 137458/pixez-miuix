package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.repository.filterIllustsBySettings
import com.perol.pixez.shared.ui.LocalBanRepository
import com.perol.pixez.shared.ui.LocalIllustRepository
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.state.IllustPagedListState
import com.perol.pixez.shared.ui.state.IllustPagedPage
import com.perol.pixez.shared.ui.state.RankingFeedRestoreCache
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.BlurredBar
import com.perol.pixez.shared.ui.components.rememberBlurBackdrop
import com.perol.pixez.shared.ui.components.LiquidFilterBar
import top.yukonga.miuix.kmp.blur.Backdrop
import androidx.compose.ui.text.font.FontWeight
import com.perol.pixez.shared.ui.components.IllustStaggeredGrid
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.i18n.LocalStrings
import kotlinx.coroutines.delay
import androidx.compose.ui.input.nestedscroll.nestedScroll
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.icon.extended.Recent
import com.perol.pixez.shared.ui.components.blurBackdropSource
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding

/**
 * 排行榜页：支持日/周/月等模式切换，展示真实排行榜数据。
 */
@Composable
fun RankingScreen(
    onIllustClick: (Long) -> Unit,
    feedRestore: RankingFeedRestoreCache,
) {
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
    // 高-5：跨页恢复快照来自 UI 层恢复缓存（随 Main child 的 InstanceKeeper 存活整个会话，
    // 登录成功栈重置或进程死亡时释放；过期过滤结果由 restoreRaw 强制重过滤化解），只存原始
    // ID 列表，经仓库 illustsCache 回查原始数据（LRU 淘汰项跳过；回源策略见架构审查待确认项）。
    val cachedRanking = feedRestore.get(rankingCacheKey)
    // 高-4c：分页/竞态/加载态收敛到 IllustPagedListState（generation 守卫语义），
    // 过滤链仍为高-3「原始/过滤」双列表本地重过滤。状态机以榜单模式与日期为 remember key：
    // 切换时整体重建（等价原实现的清空重载），并从恢复缓存快照回查恢复首屏原始列表与下一页 URL。
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
    val gridState = androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState()

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
        val previous = listState.filteredIllusts
        listState.loadFirst(force = force)
        isManualRefreshing = false
        // 引用变化 = 本轮成功替换了列表（失败与竞态失配时状态机不写入）；
        // 成功才同步恢复快照（原始 ID 列表）并复位滚动位置。
        val current = listState.rawIllusts
        if (current != null && listState.filteredIllusts !== previous) {
            feedRestore.update(rankingCacheKey, current, listState.nextUrl)
            if (force && gridState.firstVisibleItemIndex > 0) {
                gridState.scrollToItem(0)
            }
        }
    }

    fun loadMore() {
        coroutineScope.launch {
            val previous = listState.filteredIllusts
            listState.loadMore()
            // 引用变化 = 翻页成功追加；失败/被拒绝/竞态失配时列表未变，不重复写恢复快照。
            val current = listState.rawIllusts
            if (current != null && listState.filteredIllusts !== previous) {
                feedRestore.update(rankingCacheKey, current, listState.nextUrl)
            }
        }
    }

    val triggerManualRefresh: () -> Unit = {
        isManualRefreshing = true
        retryCount++
    }

    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val colorScheme = MiuixTheme.colorScheme

    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    var showDateDialog by rememberSaveable { mutableStateOf(false) }

    // 日期筛选弹窗
    OverlayDialog(
        title = strings.rankingDateLabel,
        show = showDateDialog,
        onDismissRequest = { showDateDialog = false },
    ) {
        RankingDateInput(
            date = dateInput,
            onDateChange = { dateInput = it },
            onClear = {
                dateInput = ""
                showDateDialog = false
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        )
        TextButton(
            text = strings.cancel,
            onClick = { showDateDialog = false },
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(
            text = strings.confirm,
            onClick = { showDateDialog = false },
            colors = ButtonDefaults.textButtonColorsPrimary(),
            modifier = Modifier.fillMaxWidth(),
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            BlurredBar(
                backdrop = backdrop,
                scrollBehavior = scrollBehavior,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TopAppBar(
                        title = strings.tabRanking,
                        scrollBehavior = scrollBehavior,
                        color = if (backdrop != null) androidx.compose.ui.graphics.Color.Transparent else colorScheme.surface,
                        actions = {
                            if (dateInput.isNotBlank()) {
                                TextButton(
                                    text = dateInput,
                                    onClick = { showDateDialog = true },
                                )
                            } else {
                                IconButton(
                                    onClick = { showDateDialog = true },
                                ) {
                                    Icon(
                                        imageVector = MiuixIcons.Recent,
                                        contentDescription = strings.rankingDateLabel,
                                    )
                                }
                            }
                            IconButton(
                                onClick = triggerManualRefresh,
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.Refresh,
                                    contentDescription = strings.refresh,
                                )
                            }
                        },
                    )
                    LiquidFilterBar(
                        items = RankingMode.entries,
                        selectedItem = selectedMode,
                        onItemSelected = {
                            selectedMode = it
                            retryCount = 0
                        },
                        labelProvider = { it.label(strings) },
                        backdrop = backdrop,
                        isScrollable = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.surface)
                .blurBackdropSource(backdrop),
        ) {
            val currentIllusts = listState.filteredIllusts
            when {
                currentIllusts == null && listState.initialError == null -> LoadingPlaceholder(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )
                currentIllusts == null && listState.initialError != null -> ErrorPlaceholder(
                    error = listState.initialError,
                    onRetry = { triggerManualRefresh() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )
                currentIllusts != null -> {
                    if (currentIllusts.isEmpty()) {
                        EmptyPlaceholder(
                            message = strings.rankingEmpty,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                        )
                    } else {
                        PullToRefresh(
                            isRefreshing = isManualRefreshing,
                            onRefresh = triggerManualRefresh,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = paddingValues.calculateTopPadding()),
                            topAppBarScrollBehavior = scrollBehavior,
                        ) {
                            IllustStaggeredGrid(
                                illusts = currentIllusts,
                                onIllustClick = onIllustClick,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                                contentPadding = PaddingValues(
                                    start = 8.dp,
                                    top = paddingValues.calculateTopPadding() + 8.dp,
                                    end = 8.dp,
                                    bottom = LocalBottomBarContentPadding.current,
                                ),
                                hasMore = listState.nextUrl != null,
                                isLoadingMore = listState.isLoadingMore,
                                loadMoreError = listState.loadMoreError,
                                onLoadMore = ::loadMore,
                            )
                        }
                    }
                }
            }
        }
    }
}


/**
 * 排行榜模式枚举，code 与 Pixiv API 参数保持一致。
 */
private enum class RankingMode(
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

    fun label(strings: com.perol.pixez.shared.ui.i18n.AppStrings): String = when (this) {
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
 * 排行榜日期输入区：TextField + 清空按钮。
 *
 * 接收 YYYY-MM-DD 格式；清空后恢复最新榜单。
 */
@Composable
private fun RankingDateInput(
    date: String,
    onDateChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val isValid = date.isEmpty() || date.matches(RankingDateRegex)
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                value = date,
                onValueChange = onDateChange,
                label = strings.rankingDateLabel,
                modifier = Modifier.weight(1f),
                enabled = true,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onClear,
                enabled = date.isNotEmpty(),
            ) {
                Text(strings.actionClear)
            }
        }
        if (!isValid) {
            Text(
                text = strings.rankingDateFormatError,
                color = MiuixTheme.colorScheme.error,
                style = MiuixTheme.textStyles.body2,
                modifier = Modifier.fillMaxWidth(),
            )
        }
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
