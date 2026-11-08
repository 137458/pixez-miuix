package com.perol.pixez.shared.ui.screens

import com.perol.pixez.shared.ui.components.BlurTopBarScaffold
import com.perol.pixez.shared.ui.components.rememberBlurBackdrop

import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.perol.pixez.shared.ui.components.blurBackdropSource
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.BoardInfo
import com.perol.pixez.shared.ui.LocalBoardRepository
import com.perol.pixez.shared.ui.utils.openSafeUrl
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.HtmlCaptionText
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.components.PixivApiErrorFormatter
import com.perol.pixez.shared.ui.components.LocalToastSink
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.components.ToastType
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.blur.layerBackdrop

/**
 * 公告板页：展示官方公告列表，支持标题与 HTML 内容渲染、链接点击与刷新。
 *
 * @param onBack 返回上一级页面。
 */
@Composable
fun BoardScreen(
    onBack: () -> Unit,
) {
    // 中-9：数据仓储统一经 CompositionLocal 注入（RootContent 根部 provide）。
    val boardRepository = LocalBoardRepository.current
    val coroutineScope = rememberCoroutineScope()
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current

    // 公告列表、加载态、错误态与 Toast 提示。
    var boardList by remember { mutableStateOf<List<BoardInfo>>(emptyList()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<Throwable?>(null) }
    val toastSink = LocalToastSink.current

    /**
     * 加载公告列表。
     *
     * 首次进入与点击刷新时调用；保留已有列表，避免刷新过程中出现空白闪屏。
     */
    suspend fun loadBoardList() {
        isRefreshing = true
        loadError = null
        // 当前处于 suspend 函数挂起上下文，需要调用挂起函数，使用 suspendRunCatchingNonCancel 捕获异常并保留取消语义。
        suspendRunCatchingNonCancel { boardRepository.loadBoardList() }
            .onSuccess { boardList = it }
            .onFailure { error ->
                loadError = error
                if (boardList.isEmpty()) {
                    toastSink(ToastData(PixivApiErrorFormatter.format(error, strings), ToastType.Error))
                }
            }
        isRefreshing = false
    }

    // 页面进入时自动加载一次公告。
    LaunchedEffect(Unit) {
        loadBoardList()
    }

    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val colorScheme = MiuixTheme.colorScheme

    BlurTopBarScaffold(
        modifier = Modifier.fillMaxSize(),
        title = strings.boardTitle,
        backdrop = backdrop,
        scrollBehavior = scrollBehavior,
        onBack = onBack,
        actions = {
            // 顶部刷新按钮：Compose Multiplatform 通用的刷新入口，
            // 与 PRD 中允许的手动刷新按钮方案一致。
            IconButton(
                onClick = {
                    if (isRefreshing) return@IconButton
                    coroutineScope.launch { loadBoardList() }
                },
                enabled = !isRefreshing,
            ) {
                Icon(
                    imageVector = MiuixIcons.Refresh,
                    contentDescription = strings.refresh,
                )
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.surface)
                .blurBackdropSource(backdrop),
        ) {
            when {
                isRefreshing && boardList.isEmpty() -> {
                    LoadingPlaceholder(modifier = Modifier.fillMaxSize())
                }

                loadError != null && boardList.isEmpty() -> {
                    ErrorPlaceholder(
                        error = loadError,
                        onRetry = {
                            if (isRefreshing) return@ErrorPlaceholder
                            coroutineScope.launch { loadBoardList() }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                boardList.isEmpty() -> {
                    EmptyPlaceholder(
                        message = strings.boardEmpty,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
                        contentPadding = paddingValues,
                    ) {
                        items(
                            items = boardList,
                            // 公告模型无唯一 id，用标题与起始日期组合作为稳定 key，
                            // 避免列表项在刷新或重组时发生不必要的复用错位。
                            key = { it.title + it.startDate },
                            contentType = { "board_item" },
                        ) { board ->
                            BoardItem(
                                board = board,
                                onLinkClick = { url ->
                                    openSafeUrl(url, strings, setOf("http", "https")) {
                                        toastSink(ToastData(it, ToastType.Error))
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 单条公告卡片：标题 + HTML 内容，链接可点击。
 */
@Composable
private fun BoardItem(
    board: BoardInfo,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = board.title,
            style = MiuixTheme.textStyles.title3,
        )
        HtmlCaptionText(
            html = board.content,
            onLinkClick = onLinkClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            style = MiuixTheme.textStyles.body2,
            // 公告需要完整展示，不与作品简介共用折叠逻辑
            collapsible = false,
        )
    }
}
