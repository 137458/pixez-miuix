package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.repository.BanRepository
import com.perol.pixez.shared.data.repository.IllustRepository
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.IllustStaggeredGrid
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.utils.suspendRunCatchingNonCancel
import kotlinx.coroutines.delay
import androidx.compose.ui.input.nestedscroll.nestedScroll
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Button
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

/**
 * 排行榜页：支持日/周/月等模式切换，展示真实排行榜数据。
 */
@Composable
fun RankingScreen(
    onIllustClick: (Int) -> Unit,
    repository: IllustRepository,
    banRepository: BanRepository,
    settingsRepository: SettingsRepository,
) {
    // 保存用户选择的排行榜模式，进程重建后恢复。
    var selectedMode by rememberSaveable { mutableStateOf(RankingMode.DAY) }
    // 日期输入原始值，格式 YYYY-MM-DD；空字符串表示未选择。
    var dateInput by rememberSaveable { mutableStateOf("") }
    // 经防抖与正则校验后的合法日期，null 表示不应用日期筛选。
    val selectedDate = debouncedRankingDate(dateInput)
    // 重试计数，点击重试或切换模式/日期时触发重新加载。
    var retryCount by rememberSaveable { mutableIntStateOf(0) }
    var isManualRefreshing by rememberSaveable { mutableStateOf(false) }

    suspend fun filterBanned(rawIllusts: List<Illust>): List<Illust> {
        val bannedIds = suspendRunCatchingNonCancel { banRepository.getBannedIllustIds() }
            .getOrDefault(emptySet())
        val bannedUserIds = suspendRunCatchingNonCancel { banRepository.getBannedUserIds() }
            .getOrDefault(emptySet())
        val banTags = suspendRunCatchingNonCancel { banRepository.getAllBanTags() }
            .getOrDefault(emptyList())
        val banAIIllust = settingsRepository.banAIIllust
        val hIsNotAllow = settingsRepository.hIsNotAllow
        return rawIllusts.filter {
            it.id !in bannedIds &&
                it.user.id !in bannedUserIds &&
                (!banAIIllust || it.illustAIType != 2) &&
                (!hIsNotAllow || (it.xRestrict == 0 && it.tags.none { tag -> tag.name.equals("R-18", ignoreCase = true) || tag.name.equals("R-18G", ignoreCase = true) })) &&
                !banRepository.isBannedByTags(
                    banTags,
                    it.tags.flatMap { tag -> listOfNotNull(tag.name, tag.translatedName) }
                )
        }
    }

    // 模式、日期切换或重试时自动重新加载；加载完成后过滤掉被屏蔽作品。
    val state = produceState<Result<Pair<List<Illust>, String?>>?>(
        initialValue = null,
        selectedMode,
        selectedDate,
        retryCount,
        banRepository,
        settingsRepository,
    ) {
        val rankingResult = suspendRunCatchingNonCancel {
            repository.getRankingResponse(
                mode = selectedMode.code,
                date = selectedDate,
            )
        }
        isManualRefreshing = false
        value = rankingResult.map { filterBanned(it.illusts) to it.nextUrl }
    }

    var illusts by remember(selectedMode, selectedDate) { mutableStateOf(listOf<Illust>()) }
    var nextUrl by remember(selectedMode, selectedDate) { mutableStateOf<String?>(null) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var loadMoreError by remember { mutableStateOf<Throwable?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(state.value) {
        state.value?.onSuccess { (initialIllusts, initialNextUrl) ->
            illusts = initialIllusts
            nextUrl = initialNextUrl
            isLoadingMore = false
            loadMoreError = null
        }
    }

    fun loadMore() {
        val currentNextUrl = nextUrl ?: return
        if (isLoadingMore) return
        coroutineScope.launch {
            isLoadingMore = true
            loadMoreError = null
            suspendRunCatchingNonCancel {
                repository.getRankingResponse(
                    mode = selectedMode.code,
                    date = selectedDate,
                    nextUrl = currentNextUrl,
                )
            }.onSuccess { response ->
                val filtered = filterBanned(response.illusts)
                illusts = illusts + filtered
                nextUrl = response.nextUrl
            }.onFailure { error ->
                loadMoreError = error
            }
            isLoadingMore = false
        }
    }

    val triggerManualRefresh: () -> Unit = {
        isManualRefreshing = true
        retryCount++
    }

    val scrollBehavior = MiuixScrollBehavior()

    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = strings.tabRanking,
                scrollBehavior = scrollBehavior,
                actions = {
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
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
        ) {
            RankingModeSelector(
                selectedMode = selectedMode,
                onModeSelected = {
                    selectedMode = it
                    retryCount = 0
                },
            )

            RankingDateInput(
                date = dateInput,
                onDateChange = { dateInput = it },
                onClear = { dateInput = "" },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            val result = state.value
            when {
                result == null -> LoadingPlaceholder(
                    modifier = Modifier.weight(1f),
                )
                result.isSuccess -> {
                    if (illusts.isEmpty()) {
                        EmptyPlaceholder(
                            message = strings.rankingEmpty,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        PullToRefresh(
                            isRefreshing = isManualRefreshing,
                            onRefresh = triggerManualRefresh,
                            modifier = Modifier.weight(1f),
                        ) {
                            IllustStaggeredGrid(
                                illusts = illusts,
                                onIllustClick = onIllustClick,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                                hasMore = nextUrl != null,
                                isLoadingMore = isLoadingMore,
                                loadMoreError = loadMoreError,
                                onLoadMore = ::loadMore,
                            )
                        }
                    }
                }
                else -> ErrorPlaceholder(
                    error = result.exceptionOrNull(),
                    onRetry = { triggerManualRefresh() },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}


@Composable
private fun RankingModeSelector(
    selectedMode: RankingMode,
    onModeSelected: (RankingMode) -> Unit,
) {
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(RankingMode.entries) { mode ->
            val isSelected = mode == selectedMode
            Text(
                text = mode.label(strings),
                modifier = Modifier
                    .clickable { onModeSelected(mode) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                style = if (isSelected) {
                    MiuixTheme.textStyles.body1
                } else {
                    MiuixTheme.textStyles.body2
                },
                color = if (isSelected) {
                    MiuixTheme.colorScheme.primary
                } else {
                    MiuixTheme.colorScheme.onSurface
                },
            )
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
        delay(500)
        debounced = date
    }
    return debounced.takeIf { it.matches(RankingDateRegex) }
}

private val RankingDateRegex = Regex("""^\d{4}-\d{2}-\d{2}$""")
