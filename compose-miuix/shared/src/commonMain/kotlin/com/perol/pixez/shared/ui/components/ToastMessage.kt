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
 * 现代液态玻璃灵动胶囊 Toast 提示。
 *
 * 采用弹性物理动效与液态玻璃材质（SDF 折射、高光与平滑降级），提供通透灵动的瞬时状态反馈。
 *
 * @param message 提示文本，为 null 或空字符串时不显示
 * @param modifier 外部修饰符
 * @param durationMillis 显示时长，默认 2 秒
 * @param backdrop 可选的背景采样 Backdrop，若提供则渲染物理透镜折射
 * @param onDismiss 提示消失后的回调，用于清空外部状态
 */
@Composable
fun ToastMessage(
    message: String?,
    modifier: Modifier = Modifier,
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

    val isDark = MiuixTheme.colorScheme.surface.luminance() < 0.5f
    val cornerRadius = 24.dp
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    val isSuccess = remember(message) {
        message?.let {
            it.contains("成功") || it.contains("Success") || it.contains("已保存") || it.contains("已复制")
        } == true
    }
    val isFailed = remember(message) {
        message?.let {
            it.contains("失败") || it.contains("Failed") || it.contains("错误") || it.contains("Error")
        } == true
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
                    if (isSuccess) {
                        Icon(
                            imageVector = MiuixIcons.Ok,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF4CD964) else Color(0xFF34C759),
                            modifier = Modifier.size(17.dp),
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                    } else if (isFailed) {
                        Icon(
                            imageVector = MiuixIcons.Report,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFFFF453A) else Color(0xFFFF3B30),
                            modifier = Modifier.size(17.dp),
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                    }

                    Text(
                        text = message ?: "",
                        style = MiuixTheme.textStyles.body2,
                        color = if (isDark) Color.White else Color(0xFF1C1C1E),
                    )
                }
            }
        }
    }
}
