package com.perol.pixez.shared.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.highlight.BloomStroke
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.highlight.LightPosition
import top.yukonga.miuix.kmp.blur.highlight.LightSource
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import com.perol.pixez.shared.ui.i18n.AppStrings
import com.perol.pixez.shared.ui.i18n.LocalStrings
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.perol.pixez.shared.ui.libs.liquid.lens
import com.perol.pixez.shared.ui.libs.liquid.vibrancy

private val ToastHighlightLight = Highlight(
    width = 0.8.dp,
    alpha = 0.85f,
    style = BloomStroke(
        color = Color.White.copy(alpha = 0.15f),
        innerBlurRadius = 2.0.dp,
        primaryLight = LightSource(
            position = LightPosition(0.5f, -0.2f, -0.15f),
            color = Color.White,
            intensity = 0.6f,
        ),
        secondaryLight = LightSource(
            position = LightPosition(0.5f, 0.8f, -0.4f),
            color = Color.White,
            intensity = 0.25f,
        ),
        dualPeak = true,
    ),
)

private val ToastHighlightDark = Highlight(
    width = 0.8.dp,
    alpha = 0.9f,
    style = BloomStroke(
        color = Color.White.copy(alpha = 0.18f),
        innerBlurRadius = 1.8.dp,
        primaryLight = LightSource(
            position = LightPosition(0.5f, -0.15f, -0.1f),
            color = Color.White,
            intensity = 0.7f,
        ),
        secondaryLight = LightSource(
            position = LightPosition(0.5f, 0.8f, -0.4f),
            color = Color.White,
            intensity = 0.3f,
        ),
        dualPeak = true,
    ),
)

/**
 * Toast 提示消息类型。
 */
enum class ToastType {
    Normal,
    Success,
    Error,
}

/**
 * 结构化 Toast 数据载荷。
 */
data class ToastData(
    val message: String,
    val type: ToastType = ToastType.Normal,
)

/**
 * 智能推断消息状态（向下兼容未显式声明 ToastType 的传统调用）。
 * 结合当前环境语言字典动态匹配，同时兜底多语言常见关键词，杜绝非中英语言下的图标退化。
 */
fun inferToastType(message: String, strings: AppStrings? = null): ToastType {
    val lower = message.lowercase()
    if (strings != null) {
        val successKeywords = listOf(
            strings.complete,
            strings.copiedToClipboard,
            strings.share,
        )
        if (successKeywords.any { it.isNotBlank() && (lower.contains(it.lowercase()) || message.contains(it)) }) {
            return ToastType.Success
        }
        val errorKeywords = listOf(
            strings.loadFailed,
            strings.retry,
        )
        if (errorKeywords.any { it.isNotBlank() && (lower.contains(it.lowercase()) || message.contains(it)) }) {
            return ToastType.Error
        }
    }
    return when {
        lower.contains("成功") || lower.contains("success") || lower.contains("已保存") || lower.contains("已复制") ||
            lower.contains("saved") || lower.contains("copied") || lower.contains("erfolgreich") ||
            lower.contains("完了") || lower.contains("成功") || lower.contains("성공") || lower.contains("успешно") -> ToastType.Success
        lower.contains("失败") || lower.contains("failed") || lower.contains("错误") || lower.contains("error") ||
            lower.contains("fehler") || lower.contains("失敗") || lower.contains("エラー") ||
            lower.contains("실패") || lower.contains("오류") || lower.contains("ошибка") -> ToastType.Error
        else -> ToastType.Normal
    }
}

/**
 * 现代液态玻璃灵动胶囊 Toast 提示。
 *
 * 采用弹性物理动效与液态玻璃材质（SDF 折射、高光与平滑降级），提供通透灵动的瞬时状态反馈。
 *
 * @param message 提示文本，为 null 或空字符串时不显示
 * @param modifier 外部修饰符
 * @param type 提示类型（成功、错误、普通），若不传则结合 LocalStrings 智能推断
 * @param durationMillis 显示时长，默认 2 秒
 * @param backdrop 可选的背景采样 Backdrop，若提供则渲染物理透镜折射
 * @param onDismiss 提示消失后的回调，用于清空外部状态
 */
@Composable
fun ToastMessage(
    message: String?,
    modifier: Modifier = Modifier,
    type: ToastType? = null,
    durationMillis: Long = 2000L,
    backdrop: Backdrop? = null,
    onDismiss: () -> Unit = {},
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) {
            visible = true
            delay(durationMillis)
            visible = false
            onDismiss()
        } else {
            visible = false
        }
    }

    val strings = LocalStrings.current
    val isDark = MiuixTheme.colorScheme.surface.luminance() < 0.5f
    val cornerRadius = 24.dp
    val shape = remember(cornerRadius) { SquircleShape(cornerRadius) }
    val effectiveType = remember(message, type, strings) {
        type ?: if (message != null) inferToastType(message, strings) else ToastType.Normal
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(spring(dampingRatio = 0.8f)) +
                scaleIn(
                    animationSpec = spring(dampingRatio = 0.65f, stiffness = 380f),
                    initialScale = 0.85f,
                ) +
                slideInVertically(
                    animationSpec = spring(dampingRatio = 0.7f, stiffness = 380f),
                    initialOffsetY = { it / 3 },
                ),
            exit = fadeOut(spring(dampingRatio = 0.9f)) +
                scaleOut(
                    animationSpec = spring(dampingRatio = 0.9f),
                    targetScale = 0.90f,
                ) +
                slideOutVertically(
                    animationSpec = spring(dampingRatio = 0.9f),
                    targetOffsetY = { it / 4 },
                ),
        ) {
            val containerModifier = if (backdrop != null && isRuntimeShaderSupported()) {
                Modifier
                    .padding(horizontal = 32.dp, vertical = 56.dp)
                    .squircleClip(cornerRadius)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { shape },
                        effects = {
                            padding = maxOf(padding, 30.dp.toPx())
                            vibrancy()
                            blur(18.dp.toPx(), 18.dp.toPx())
                            lens(
                                refractionHeight = 16.dp.toPx(),
                                refractionAmount = 2.4.dp.toPx(),
                                depthEffect = true,
                                chromaticAberration = 0.12f,
                            )
                        },
                        highlight = { if (isDark) ToastHighlightDark else ToastHighlightLight },
                        onDrawSurface = {
                            drawRect(
                                color = if (isDark) {
                                    Color.Black.copy(alpha = 0.55f)
                                } else {
                                    Color.White.copy(alpha = 0.65f)
                                },
                            )
                        },
                    )
                    .squircleBorder(
                        width = 0.6.dp,
                        color = if (isDark) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.5f),
                        cornerRadius = cornerRadius,
                    )
            } else {
                Modifier
                    .padding(horizontal = 32.dp, vertical = 56.dp)
                    .dropShadow(
                        shape = shape,
                        shadow = Shadow(
                            radius = 16.dp,
                            color = Color.Black,
                            alpha = 0.25f,
                        ),
                    )
                    .background(
                        color = if (isDark) {
                            Color(0xFF222224).copy(alpha = 0.88f)
                        } else {
                            Color(0xFFF2F2F7).copy(alpha = 0.92f)
                        },
                        shape = shape,
                    )
                    .squircleBorder(
                        width = 0.5.dp,
                        color = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f),
                        cornerRadius = cornerRadius,
                    )
            }

            Box(
                modifier = containerModifier
                    .padding(horizontal = 18.dp, vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    when (effectiveType) {
                        ToastType.Success -> {
                            Icon(
                                imageVector = MiuixIcons.Ok,
                                contentDescription = null,
                                tint = MiuixTheme.colorScheme.primary,
                                modifier = Modifier.size(17.dp),
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                        }
                        ToastType.Error -> {
                            Icon(
                                imageVector = MiuixIcons.Report,
                                contentDescription = null,
                                tint = MiuixTheme.colorScheme.error,
                                modifier = Modifier.size(17.dp),
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                        }
                        ToastType.Normal -> {}
                    }

                    Text(
                        text = message ?: "",
                        style = MiuixTheme.textStyles.body2,
                        color = when (effectiveType) {
                            ToastType.Error -> if (isDark) Color(0xFFFF6961) else MiuixTheme.colorScheme.error
                            else -> if (isDark) Color.White else Color(0xFF1C1C1E)
                        },
                    )
                }
            }
        }
    }
}

/**
 * 结构化 [ToastData] 驱动的重载组件。
 */
@Composable
fun ToastMessage(
    toast: ToastData?,
    modifier: Modifier = Modifier,
    durationMillis: Long = 2000L,
    backdrop: Backdrop? = null,
    onDismiss: () -> Unit = {},
) {
    ToastMessage(
        message = toast?.message,
        modifier = modifier,
        type = toast?.type,
        durationMillis = durationMillis,
        backdrop = backdrop,
        onDismiss = onDismiss,
    )
}

