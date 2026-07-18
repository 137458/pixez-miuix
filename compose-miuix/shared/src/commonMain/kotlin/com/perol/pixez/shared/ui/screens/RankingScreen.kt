package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.repository.IllustRepository
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.utils.runCatchingNonCancel
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.IllustStaggeredGrid
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 排行榜页：支持日/周/月等模式切换，展示真实排行榜数据。
 */
@Composable
fun RankingScreen(
    onIllustClick: (Int) -> Unit,
    repository: IllustRepository,
) {
    // 保存用户选择的排行榜模式，进程重建后恢复。
    var selectedMode by rememberSaveable { mutableStateOf(RankingMode.DAY) }
    // 重试计数，点击重试或切换模式时触发重新加载。
    var retryCount by rememberSaveable { mutableIntStateOf(0) }

    // 模式切换或重试时自动重新加载。
    val state = produceState<Result<List<Illust>>?>(
        initialValue = null,
        selectedMode,
        retryCount,
    ) {
        value = runCatchingNonCancel { repository.getRanking(selectedMode.code) }
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
}
