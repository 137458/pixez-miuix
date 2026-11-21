package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
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
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.itos.coui.theme.LocalCouiTextStyle
import com.perol.pixez.shared.data.model.BoardInfo
import com.perol.pixez.shared.ui.components.HtmlCaptionText
import com.perol.pixez.shared.ui.components.LocalToastSink
import com.perol.pixez.shared.ui.components.PixivApiErrorFormatter
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.components.ToastType
import com.perol.pixez.shared.ui.coui.component.CouiCard
import com.perol.pixez.shared.ui.coui.core.CouiErrorPlaceholder
import com.perol.pixez.shared.ui.coui.core.CouiPlaceholder
import com.perol.pixez.shared.ui.coui.core.CouiSecondaryScaffold
import com.perol.pixez.shared.ui.coui.core.CouiTopBarIconButton
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.LocalBoardRepository
import com.perol.pixez.shared.ui.utils.openSafeUrl
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import kotlinx.coroutines.launch
import com.perol.pixez.shared.ui.coui.icon.CouiIcons

/**
 * COUI 公告板页：二级页骨架 + 公告卡片列表。
 *
 * 数据加载、刷新、Toast 与错误提示与 MIUIX 版一致；差异只有皮肤层：
 * 容器由 [CouiSecondaryScaffold] 唯一提供（含返回与折叠大标题），
 * 公告卡片用 COUI 卡片承载，三态占位用 core 占位组件。
 */
@Composable
internal fun CouiBoardScreen(
    onBack: () -> Unit,
) {
    val boardRepository = LocalBoardRepository.current
    val strings = LocalStrings.current
    val coroutineScope = rememberCoroutineScope()
    val toastSink = LocalToastSink.current

    var boardList by remember { mutableStateOf<List<BoardInfo>>(emptyList()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<Throwable?>(null) }

    /**
     * 加载公告列表：保留已有列表避免刷新闪屏；失败且列表为空时补一次 Toast。
     */
    suspend fun loadBoardList() {
        isRefreshing = true
        loadError = null
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

    LaunchedEffect(Unit) {
        loadBoardList()
    }

    CouiSecondaryScaffold(
        title = strings.boardTitle,
        onBack = onBack,
        actions = {
            CouiTopBarIconButton(
                onClick = { coroutineScope.launch { loadBoardList() } },
                icon = CouiIcons.Refresh,
                contentDescription = strings.refresh,
                enabled = !isRefreshing,
            )
        },
    ) { padding ->
        when {
            isRefreshing && boardList.isEmpty() -> CouiPlaceholder(
                message = strings.loading,
                modifier = Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding),
            )

            loadError != null && boardList.isEmpty() -> CouiErrorPlaceholder(
                error = loadError,
                onRetry = { coroutineScope.launch { loadBoardList() } },
                modifier = Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding),
            )

            boardList.isEmpty() -> CouiPlaceholder(
                message = strings.boardEmpty,
                modifier = Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding),
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .consumeWindowInsets(padding),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    end = 12.dp,
                    bottom = padding.calculateBottomPadding(),
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                overscrollEffect = rememberCouiOverscrollEffect(),
            ) {
                items(
                    items = boardList,
                    // 公告模型无唯一 id，用标题与起始日期组合保持列表项稳定。
                    key = { it.title + it.startDate },
                    contentType = { "coui_board_item" },
                ) { board ->
                    CouiBoardItem(
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

/** 单条公告卡：标题 + 完整 HTML 内容，链接可点击。 */
@Composable
private fun CouiBoardItem(
    board: BoardInfo,
    onLinkClick: (String) -> Unit,
) {
    val scheme = CouiTheme.colorScheme
    CouiCard {
        Text(
            text = board.title,
            kind = CouiTextKind.ItemTitle,
            color = scheme.onSurface,
            modifier = Modifier.padding(start = 14.dp, top = 12.dp, end = 14.dp),
        )
        HtmlCaptionText(
            html = board.content,
            onLinkClick = onLinkClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, bottom = 12.dp, top = 8.dp),
            style = LocalCouiTextStyle.current,
            color = scheme.onSurface,
            linkColor = scheme.accent,
            toggleStyle = LocalCouiTextStyle.current,
            // 公告需要完整展示，不与作品简介共用折叠逻辑。
            collapsible = false,
        )
    }
}
