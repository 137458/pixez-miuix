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
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.IllustStaggeredGrid
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.utils.runCatchingNonCancel
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 排行榜页：支持日/周/月等模式切换，展示真实排行榜数据。
 */
@Composable
fun RankingScreen(
    onIllustClick: (Int) -> Unit,
    repository: IllustRepository,
    banRepository: BanRepository,
) {
    // 保存用户选择的排行榜模式，进程重建后恢复。
    var selectedMode by rememberSaveable { mutableStateOf(RankingMode.DAY) }
    // 日期输入原始值，格式 YYYY-MM-DD；空字符串表示未选择。
    var dateInput by rememberSaveable { mutableStateOf("") }
    // 经防抖与正则校验后的合法日期，null 表示不应用日期筛选。
    val selectedDate = debouncedRankingDate(dateInput)
    // 重试计数，点击重试或切换模式/日期时触发重新加载。
    var retryCount by rememberSaveable { mutableIntStateOf(0) }

    // 模式、日期切换或重试时自动重新加载；加载完成后过滤掉被屏蔽作品。
    val state = produceState<Result<List<Illust>>?>(
        initialValue = null,
        selectedMode,
        selectedDate,
        retryCount,
        banRepository,
    ) {
        val illustsResult = runCatchingNonCancel {
            repository.getRanking(
                mode = selectedMode.code,
                date = selectedDate,
            )
        }
        val bannedIds = runCatchingNonCancel { banRepository.getBannedIllustIds() }
            .getOrDefault(emptySet())
        val bannedUserIds = runCatchingNonCancel { banRepository.getBannedUserIds() }
            .getOrDefault(emptySet())
        value = illustsResult.map { illusts ->
            illusts.filter { it.id !in bannedIds && it.user.id !in bannedUserIds }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = "排行榜")
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize(),
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
                    val illusts = result.getOrNull().orEmpty()
                    if (illusts.isEmpty()) {
                        EmptyPlaceholder(
                            message = "暂无排行榜数据",
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        IllustStaggeredGrid(
                            illusts = illusts,
                            onIllustClick = onIllustClick,
                            modifier = Modifier
                                .weight(1f)
                                .padding(paddingValues),
                        )
                    }
                }
                else -> ErrorPlaceholder(
                    error = result.exceptionOrNull(),
                    onRetry = { retryCount++ },
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
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(RankingMode.entries) { mode ->
            val isSelected = mode == selectedMode
            Text(
                text = mode.label,
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
    val label: String,
) {
    DAY("day", "日榜"),
    WEEK("week", "周榜"),
    MONTH("month", "月榜"),
    DAY_MALE("day_male", "男性向"),
    DAY_FEMALE("day_female", "女性向"),
    WEEK_ORIGINAL("week_original", "原创"),
    DAY_ROOKIE("day_rookie", "新人"),
    DAY_AI("day_ai", "AI 日榜"),
    DAY_R18_AI("day_r18_ai", "AI R18 日榜"),
    DAY_R18("day_r18", "R18 日榜"),
    WEEK_R18("week_r18", "R18 周榜"),
    WEEK_R18G("week_r18g", "R18G 周榜"),
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
    val isValid = date.isEmpty() || date.matches(RankingDateRegex)
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                value = date,
                onValueChange = onDateChange,
                label = "日期 (YYYY-MM-DD)",
                modifier = Modifier.weight(1f),
                enabled = true,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onClear,
                enabled = date.isNotEmpty(),
            ) {
                Text("清空")
            }
        }
        if (!isValid) {
            Text(
                text = "日期格式不正确",
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
