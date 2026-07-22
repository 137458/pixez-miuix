package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.BoardInfo
import com.perol.pixez.shared.data.repository.BoardRepository
import com.perol.pixez.shared.platform.openBrowser
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.components.ToastMessage
import com.perol.pixez.shared.ui.utils.runCatchingNonCancel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 公告板页：展示官方公告列表，支持标题与 HTML 内容渲染、链接点击与刷新。
 *
 * @param onBack 返回上一级页面。
 * @param boardRepository 公告板仓库，由调用方注入，避免设置页与公告页重复请求。
 */
@Composable
fun BoardScreen(
    onBack: () -> Unit,
    boardRepository: BoardRepository,
) {
    val coroutineScope = rememberCoroutineScope()

    // 公告列表、加载态、错误态与 Toast 提示。
    var boardList by remember { mutableStateOf<List<BoardInfo>>(emptyList()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<Throwable?>(null) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    /**
     * 加载公告列表。
     *
     * 首次进入与点击刷新时调用；保留已有列表，避免刷新过程中出现空白闪屏。
     */
    suspend fun loadBoardList() {
        isRefreshing = true
        loadError = null
        runCatchingNonCancel { boardRepository.loadBoardList() }
            .onSuccess { boardList = it }
            .onFailure { error ->
                loadError = error
                if (boardList.isEmpty()) {
                    toastMessage = "加载失败: ${error.message ?: "未知错误"}"
                }
            }
        isRefreshing = false
    }

    // 页面进入时自动加载一次公告。
    LaunchedEffect(Unit) {
        loadBoardList()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "公告板",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
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
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "刷新",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
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
                        message = "暂无公告",
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        items(boardList) { board ->
                            BoardItem(
                                board = board,
                                onLinkClick = { url -> openUrlOrToast(url) { toastMessage = it } },
                            )
                        }
                    }
                }
            }
        }

        ToastMessage(
            message = toastMessage,
            onDismiss = { toastMessage = null },
        )
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
        HtmlText(
            html = board.content,
            onLinkClick = onLinkClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            style = MiuixTheme.textStyles.body2,
        )
    }
}

/**
 * 简单 HTML 文本组件：解析常见标签并渲染为 AnnotatedString，链接支持点击。
 *
 * 当前支持：
 * - `<a href="...">...</a>`：可点击链接，使用主题主色加下划线标识。
 * - `<br>`、`<p>`：换行与段落分隔。
 * - 常见 HTML 实体转义。
 *
 * 该实现为轻量级解析器，不依赖第三方 HTML 库，优先保证链接可点击。
 */
@Composable
private fun HtmlText(
    html: String,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    style: TextStyle = MiuixTheme.textStyles.body1,
) {
    val linkColor = MiuixTheme.colorScheme.primary
    val annotatedString = remember(html, linkColor) { parseHtmlToAnnotatedString(html, linkColor) }
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = annotatedString,
        style = style,
        modifier = modifier.pointerInput(Unit) {
            // 通过 pointerInput 手动检测点击位置，再查询该位置是否存在 URL 注解；
            // 这样可以在不依赖 ClickableText 的情况下实现链接点击。
            detectTapGestures { offset ->
                layoutResult.value?.let { layout ->
                    val position = layout.getOffsetForPosition(offset)
                    annotatedString
                        .getStringAnnotations(URL_ANNOTATION_TAG, position, position)
                        .firstOrNull()
                        ?.let { annotation -> onLinkClick(annotation.item) }
                }
            }
        },
        onTextLayout = { layoutResult.value = it },
    )
}

/**
 * 将简单 HTML 解析为 AnnotatedString。
 *
 * 解析流程：
 * 1. 先转义常见 HTML 实体，避免标签识别被实体干扰。
 * 2. 顺序扫描字符，遇到 '<' 时提取完整标签。
 * 3. 开标签记录链接起始位置；闭标签写入 URL 注解与链接样式。
 * 4. 普通字符直接追加到输出文本，保证注解范围与实际文本对齐。
 */
private fun parseHtmlToAnnotatedString(
    html: String,
    linkColor: Color,
): AnnotatedString {
    return buildAnnotatedString {
        val normalized = html.unescapeHtmlEntities()
        var index = 0
        var pendingLinkUrl: String? = null
        var pendingLinkStart = -1

        while (index < normalized.length) {
            val char = normalized[index]
            if (char != '<') {
                append(char)
                index++
                continue
            }

            val tagEnd = normalized.indexOf('>', startIndex = index)
            if (tagEnd == -1) {
                // 标签未闭合，按普通文本输出 '<'，继续后续解析。
                append(char)
                index++
                continue
            }

            val rawTag = normalized.substring(index + 1, tagEnd).trim()
            index = tagEnd + 1

            if (rawTag.startsWith('/')) {
                // 处理闭标签：当前只处理 </a> 来结束链接注解。
                val tagName = rawTag.substring(1).lowercase().substringBefore(' ')
                if (tagName == "a") {
                    pendingLinkUrl?.let { url ->
                        addStringAnnotation(URL_ANNOTATION_TAG, url, pendingLinkStart, length)
                        addStyle(
                            SpanStyle(
                                color = linkColor,
                                textDecoration = TextDecoration.Underline,
                            ),
                            pendingLinkStart,
                            length,
                        )
                    }
                    pendingLinkUrl = null
                    pendingLinkStart = -1
                }
            } else {
                // 处理开标签与空标签。
                val firstSpace = rawTag.indexOf(' ')
                val tagName = if (firstSpace == -1) {
                    rawTag.lowercase()
                } else {
                    rawTag.substring(0, firstSpace).lowercase()
                }
                val attributes = if (firstSpace == -1) "" else rawTag.substring(firstSpace + 1)

                when (tagName) {
                    "br" -> append('\n')
                    "p" -> {
                        // 段落之间插入空行，模拟 HTML 段落间距。
                        if (length > 0) {
                            append("\n\n")
                        }
                    }

                    "a" -> {
                        val url = extractHref(attributes)
                        if (url != null) {
                            pendingLinkUrl = url
                            pendingLinkStart = length
                        }
                    }
                }
            }
        }
    }
}

/**
 * 从标签属性字符串中提取 href 值，支持双引号与单引号包裹。
 */
private fun extractHref(attributes: String): String? {
    val regex = Regex("""href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    return regex.find(attributes)?.groupValues?.get(1)
}

/**
 * 反转义常见 HTML 实体，避免标签与文本被错误解析。
 */
private fun String.unescapeHtmlEntities(): String {
    return this
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&#x27;", "'")
        .replace("&#x2F;", "/")
        .replace("&nbsp;", " ")
}

/**
 * 打开 URL，失败时通过回调返回提示信息。
 *
 * 仅允许 `http` 与 `https` scheme，防止公告 HTML 中的恶意链接触发
 * `javascript:`、`file://`、`intent://` 等危险协议。
 */
private fun openUrlOrToast(url: String, onError: (String) -> Unit) {
    try {
        val scheme = url.substringBefore(":", "").lowercase()
        require(scheme in ALLOWED_URL_SCHEMES) { "不支持的链接协议: $scheme" }
        openBrowser(url)
    } catch (e: Exception) {
        Napier.e("打开链接失败 url=$url", e)
        onError("打开失败: ${e.message ?: "未知错误"}")
    }
}

private val ALLOWED_URL_SCHEMES = setOf("http", "https")

private const val URL_ANNOTATION_TAG = "URL"
