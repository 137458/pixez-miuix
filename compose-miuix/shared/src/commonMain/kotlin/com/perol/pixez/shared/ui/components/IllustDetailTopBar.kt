package com.perol.pixez.shared.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.repository.BanRepository
import com.perol.pixez.shared.platform.IllustClipboard
import com.perol.pixez.shared.platform.IllustShare
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.utils.accessibleTouchTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.FileSystem
import top.yukonga.miuix.kmp.basic.DropdownDefaults
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.popup.OverlayDropdownPopup

/**
 * 作品详情页沉浸式顶栏：
 * 物理恒定锚点、微气泡背景平滑折叠、动态标题与右侧液态玻璃操作组（收藏、下载、更多操作菜单）。
 *
 * 本函数仅负责状态持有与整体编排；顶栏主体行与操作按钮组见 [IllustDetailTopBarHeader] /
 * [IllustDetailTopBarActions]，更多操作菜单条目构建见 [buildIllustDetailMoreMenuEntries]。
 */
@Composable
fun IllustDetailTopBar(
    illust: Illust?,
    collapseProgressProvider: () -> Float = { 0f },
    detailBackdrop: Backdrop? = null,
    isBookmarked: Boolean,
    isBookmarkLoading: Boolean,
    bookmarkHeartScale: Animatable<Float, AnimationVector1D>,
    onBookmarkClick: () -> Unit,
    isDownloading: Boolean,
    onDownloadClick: () -> Unit,
    onBack: () -> Unit,
    isBanned: Boolean,
    banRepository: BanRepository,
    onBanSuccess: () -> Unit,
    onToast: (ToastData) -> Unit,
    modifier: Modifier = Modifier,
) {
    val collapseProgress = collapseProgressProvider()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalPlatformContext.current
    val clipboard = remember { IllustClipboard() }
    val share = remember { IllustShare() }

    var showMoreMenu by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        // 1. 底层全宽毛玻璃背景（随滚动进度平滑淡入淡出）
        BlurredBar(
            backdrop = detailBackdrop,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = collapseProgress },
        ) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp),
            )
        }

        // 2. 顶栏主体内容行
        IllustDetailTopBarHeader(
            collapseProgress = collapseProgress,
            illust = illust,
            detailBackdrop = detailBackdrop,
            isBookmarked = isBookmarked,
            isBookmarkLoading = isBookmarkLoading,
            bookmarkHeartScale = bookmarkHeartScale,
            onBookmarkClick = onBookmarkClick,
            isDownloading = isDownloading,
            onDownloadClick = onDownloadClick,
            onBack = onBack,
            onMoreClick = { showMoreMenu = !showMoreMenu },
        )

        // 3. 更多操作下拉菜单：锚定顶栏容器、展开于其下方右缘；
        // 遮罩、进出场动画、点外关闭与返回键处理均由 Miuix 弹层接管。
        if (illust != null) {
            OverlayDropdownPopup(
                entries = buildIllustDetailMoreMenuEntries(
                    strings = LocalStrings.current,
                    illust = illust,
                    isBanned = isBanned,
                    banRepository = banRepository,
                    onBanSuccess = onBanSuccess,
                    onToast = onToast,
                    clipboard = clipboard,
                    share = share,
                    context = context,
                    coroutineScope = coroutineScope,
                ),
                show = showMoreMenu,
                onDismiss = { showMoreMenu = false },
                onDismissFinished = {},
                maxHeight = null,
                dropdownColors = DropdownDefaults.dropdownColors(),
                renderInRootScaffold = true,
                collapseOnSelection = true,
            )
        }
    }
}

/**
 * 从 Coil 磁盘缓存中尝试提取已有图片数据。
 */
internal suspend fun extractCachedImageBytes(context: PlatformContext, urls: List<String>): ByteArray? = withContext(Dispatchers.IO) {
    val imageLoader = SingletonImageLoader.get(context)
    val diskCache = imageLoader.diskCache ?: return@withContext null
    for (candidateUrl in urls) {
        diskCache.openSnapshot(candidateUrl)?.use { snapshot ->
            val fileSystem = FileSystem.SYSTEM
            if (fileSystem.exists(snapshot.data) && (fileSystem.metadata(snapshot.data).size ?: 0L) > 0L) {
                return@withContext fileSystem.read(snapshot.data) { readByteArray() }
            }
        }
    }
    null
}

@Composable
internal fun LiquidCircleActionButton(
    tooltip: String,
    onClick: () -> Unit,
    bubbleAlpha: Float = 1f,
    detailBackdrop: Backdrop? = LocalBackdrop.current,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale = remember { Animatable(1f) }
    LaunchedEffect(isPressed) {
        pressScale.animateTo(
            targetValue = if (isPressed) 0.90f else 1f,
            animationSpec = spring(
                dampingRatio = 0.7f,
                stiffness = 500f,
            ),
        )
    }

    TooltipBox(
        text = tooltip,
        modifier = modifier.accessibleTouchTarget(48.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .graphicsLayer {
                    scaleX = pressScale.value
                    scaleY = pressScale.value
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = bubbleAlpha }
                    .liquidGlass(
                        backdrop = detailBackdrop,
                        shape = CircleShape,
                        blurRadius = 16.dp,
                        tintColor = Color.Black,
                        tintAlpha = 0.38f,
                    )
                    .then(
                        if (detailBackdrop == null) {
                            Modifier.border(0.5.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                        } else {
                            Modifier
                        }
                    ),
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = enabled,
                        onClick = onClick,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                content()
            }
        }
    }
}