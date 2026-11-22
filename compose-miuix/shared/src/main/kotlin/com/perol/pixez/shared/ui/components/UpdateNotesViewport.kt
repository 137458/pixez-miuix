package com.perol.pixez.shared.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.ui.i18n.AppStrings
import com.perol.pixez.shared.ui.i18n.format

/** 视口上界缺失时的兜底高度：宁可矮一截，也不能回落到无界测量。 */
internal val NotesViewportMinHeight = 48.dp

/**
 * 更新说明滚动视口的规格：限定最大高度与边缘羽化带长度。
 */
internal data class NotesViewportSpec(val maxHeight: Dp, val fade: Dp)

/**
 * 收敛视口规格：
 * - 上界必须是有限正数，避免弹窗正文外层滚动区传递无限高度约束导致内层滚动崩溃；
 * - 羽化不能超过半个视口，避免上下渐变重叠成整块薄雾。
 */
internal fun notesViewportSpec(maxHeight: Dp, fade: Dp): NotesViewportSpec {
    val height = if (maxHeight.value.isFinite() && maxHeight.value >= NotesViewportMinHeight.value) {
        maxHeight
    } else {
        NotesViewportMinHeight
    }
    val fadeValue = if (fade.value.isFinite()) fade.value else 0f
    return NotesViewportSpec(
        maxHeight = height,
        fade = fadeValue.coerceIn(0f, height.value / 2f).dp,
    )
}

/** 弹窗内的说明视口。 */
internal val NotesDialogViewport = notesViewportSpec(
    maxHeight = UPDATE_NOTES_MAX_HEIGHT_DP.dp,
    fade = UPDATE_NOTES_FADE_DP.dp,
)

/**
 * 说明折叠行描述符：null 表示当前无需折叠入口。
 */
internal class NotesFold(
    val expanded: Boolean,
    val label: String,
    val onToggle: () -> Unit,
)

/** 折叠行的显隐口径：展开态必然可收起；折叠态只有确实藏了内容才给出入口。 */
internal fun updateNotesFoldVisible(hiddenLineCount: Int, expanded: Boolean): Boolean =
    expanded || hiddenLineCount > 0

/** 弹窗折叠行文案：收起态把剩余行数写进按钮，用户不必展开才知道还有多少。 */
internal fun updateNotesFoldLabel(
    expanded: Boolean,
    hiddenLineCount: Int,
    strings: AppStrings,
): String = if (expanded) {
    strings.collapse
} else {
    strings.updateNotesExpandWithCount.format(hiddenLineCount)
}

/** 弹窗折叠行：不可折叠时返回 null，调用方无需自己判断显隐。 */
internal fun updateNotesFold(
    expanded: Boolean,
    hiddenLineCount: Int,
    strings: AppStrings,
    onToggle: () -> Unit,
): NotesFold? = if (!updateNotesFoldVisible(hiddenLineCount, expanded)) {
    null
} else {
    NotesFold(
        expanded = expanded,
        label = updateNotesFoldLabel(expanded, hiddenLineCount, strings),
        onToggle = onToggle,
    )
}

/**
 * 说明正文的滚动视口：恒定有限上界 + 边缘羽化 + 内部滚动。
 */
@Composable
internal fun NotesScrollViewport(
    spec: NotesViewportSpec,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollState = rememberScrollState()
    val hasContentAbove by remember(scrollState) { derivedStateOf { scrollState.value > 0 } }
    val hasContentBelow by remember(scrollState) { derivedStateOf { scrollState.maxValue > scrollState.value } }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = spec.maxHeight)
            .verticalEdgeFade(
                top = hasContentAbove,
                bottom = hasContentBelow,
                fade = spec.fade,
            )
            .verticalScroll(scrollState),
        content = content,
    )
}

/**
 * 可折叠的说明区块：正文进视口，折叠行留在视口之外。
 */
@Composable
internal fun FoldableNotes(
    fold: NotesFold?,
    spec: NotesViewportSpec,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
    footer: (@Composable () -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        NotesScrollViewport(spec = spec, content = content)
        if (fold != null && footer != null) {
            footer()
        }
    }
}

/**
 * 边缘羽化底座：离屏合成后用 DstIn 遮罩把内容在指定边缘淡出。
 */
internal fun Modifier.edgeFadeMask(brushFactory: DrawScope.() -> Brush): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }.drawWithContent {
        drawContent()
        if (size.width <= 0f || size.height <= 0f) return@drawWithContent
        drawRect(brush = brushFactory(), blendMode = BlendMode.DstIn)
    }

/**
 * 竖向边缘羽化：只在 [top] / [bottom] 为 true 的一侧铺渐变，滚到端点时对应侧完全实心。
 */
internal fun Modifier.verticalEdgeFade(
    top: Boolean,
    bottom: Boolean,
    fade: Dp = UPDATE_NOTES_FADE_DP.dp,
): Modifier {
    if (!top && !bottom) return this
    return edgeFadeMask {
        val fadePx = fade.toPx().coerceAtMost(size.height / 2f)
        val stop = (fadePx / size.height).coerceIn(0f, 0.5f)
        when {
            top && bottom -> Brush.verticalGradient(
                0f to Color.Transparent,
                stop to Color.Black,
                1f - stop to Color.Black,
                1f to Color.Transparent,
            )
            top -> Brush.verticalGradient(0f to Color.Transparent, stop to Color.Black)
            else -> Brush.verticalGradient(1f - stop to Color.Black, 1f to Color.Transparent)
        }
    }
}
