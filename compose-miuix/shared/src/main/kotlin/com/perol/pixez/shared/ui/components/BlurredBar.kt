package com.perol.pixez.shared.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 带有底色保护与平台着色器能力检测的 Backdrop 采样源。
 *
 * 1. 运行时熔断：若硬件/平台不支持 RuntimeShader (Android < API 33) 或主动禁用模糊，返回 null；
 * 2. 底色注入：在 onDraw 最底层绘制 surfaceColor，确保采样源在透明区域有不透明背景支撑，杜绝 Skia 高斯模糊边缘采样透明导致的暗黑伪影与噪点。
 *
 * @param enableBlur 是否启用模糊采样
 */
@Composable
fun rememberBlurBackdrop(enableBlur: Boolean = true): LayerBackdrop? {
    if (!enableBlur || !isRuntimeShaderSupported()) return null
    val surfaceColor = MiuixTheme.colorScheme.surface
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}

/**
 * Xiaomi HyperOS / MIUIX 官方规范沉浸式顶部毛玻璃包装器。
 *
 * 采用组合包装机制包裹官方原生 TopAppBar 或 SmallTopAppBar：
 * 1. 动态尺寸自适应：使用 matchParentSize() 自动适应顶栏展开（大标题）与折叠（小标题）时的实时高度；
 * 2. 动态滚动涌现：初始停靠在页面顶部时透明度为 0，随着页面滚动在 48dp 阈值内平滑淡入毛玻璃；
 * 3. 渐进式纹理过渡：顶部状态栏区域 100% 满额模糊，沿 Y 轴向下柔和衰减至 0（底部边缘 100% 像素级清晰），无任何硬切断；
 * 4. 优雅降级：在不支持模糊的环境或 backdrop 为 null 时，自动降级为普通实色顶栏。
 *
 * @param backdrop 跨图层采样的 Backdrop，若为 null 则自动降级不显示毛玻璃
 * @param modifier 外部修饰符
 * @param blurEnabled 是否启用毛玻璃效果
 * @param scrollBehavior 顶栏滚动行为控制器，用于根据 contentOffset 动态驱动毛玻璃平滑淡入
 * @param content 顶栏主体内容（通常为设置了透明背景色的 TopAppBar 或 SmallTopAppBar）
 */
@Composable
fun BlurredBar(
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
    blurEnabled: Boolean = true,
    scrollBehavior: ScrollBehavior? = null,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        if (backdrop != null && blurEnabled) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        // 停靠顶部时透明度为 0；在 48dp 滑动区间内平滑淡入
                        alpha = scrollBehavior?.state
                            ?.let { (-it.contentOffset / 48.dp.toPx()).coerceIn(0f, 1f) }
                            ?: 1f
                    }
                    .progressiveTextureBlur(
                        backdrop = backdrop,
                        shape = RectangleShape,
                        // 顶部状态栏满额模糊向底部平滑渐隐至 0，curve = 2.2f 柔和过渡，绝无硬切割边缘
                        gradient = ProgressiveBlur.Top.copy(curve = 2.2f),
                        blurRadius = 10f,
                        colors = BlurDefaults.blurColors(
                            blendColors = listOf(
                                BlendColorEntry(color = MiuixTheme.colorScheme.surface.copy(alpha = 0.3f)),
                            ),
                        ),
                    ),
            )
        }
        content()
    }
}
