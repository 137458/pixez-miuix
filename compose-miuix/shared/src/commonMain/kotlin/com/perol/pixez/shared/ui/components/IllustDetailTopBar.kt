package com.perol.pixez.shared.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.repository.BanRepository
import com.perol.pixez.shared.platform.IllustClipboard
import com.perol.pixez.shared.platform.IllustShare
import com.perol.pixez.shared.platform.PlatformBackHandler
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.utils.openSafeUrl
import com.perol.pixez.shared.ui.utils.suspendRunCatchingNonCancel
import io.ktor.http.URLBuilder
import kotlinx.coroutines.launch
import okio.FileSystem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Blocklist
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.FavoritesFill
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.icon.extended.Share
import top.yukonga.miuix.kmp.icon.extended.Show
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 作品详情页沉浸式顶栏：
 * 物理恒定锚点、微气泡背景平滑折叠、动态标题与右侧液态玻璃操作组（收藏、下载、更多操作菜单）。
 */
@Composable
fun IllustDetailTopBar(
    illust: Illust?,
    collapseProgress: Float,
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
    onToast: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val bubbleAlpha = (1f - collapseProgress * 2f).coerceIn(0f, 1f)
            val dynamicIconTint = lerp(
                Color.White,
                MiuixTheme.colorScheme.onSurface,
                collapseProgress,
            )

            // 返回按钮
            LiquidCircleActionButton(
                tooltip = strings.back,
                onClick = onBack,
                bubbleAlpha = bubbleAlpha,
                detailBackdrop = detailBackdrop,
            ) {
                Icon(
                    imageVector = MiuixIcons.Back,
                    contentDescription = strings.back,
                    tint = dynamicIconTint,
                    modifier = Modifier.size(22.dp),
                )
            }

            // 中间标题区（随上滑折叠平滑淡入）
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
                    .graphicsLayer {
                        alpha = ((collapseProgress - 0.35f) / 0.65f).coerceIn(0f, 1f)
                    },
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = illust?.title.orEmpty(),
                    style = MiuixTheme.textStyles.title3,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MiuixTheme.colorScheme.onSurface,
                )
            }

            // 右侧操作按钮组（收藏、下载、更多）
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 1. 收藏按钮
                LiquidCircleActionButton(
                    tooltip = if (isBookmarked) strings.bookmarked else strings.bookmark,
                    onClick = onBookmarkClick,
                    bubbleAlpha = bubbleAlpha,
                    detailBackdrop = detailBackdrop,
                    enabled = !isBookmarkLoading && illust != null,
                ) {
                    Icon(
                        imageVector = if (isBookmarked) MiuixIcons.FavoritesFill else MiuixIcons.Favorites,
                        contentDescription = if (isBookmarked) strings.bookmarked else strings.bookmark,
                        tint = if (isBookmarked) Color(0xFFFF4D6A) else dynamicIconTint,
                        modifier = Modifier
                            .size(22.dp)
                            .graphicsLayer {
                                scaleX = bookmarkHeartScale.value
                                scaleY = bookmarkHeartScale.value
                            },
                    )
                }

                // 2. 下载按钮
                LiquidCircleActionButton(
                    tooltip = strings.download,
                    onClick = onDownloadClick,
                    bubbleAlpha = bubbleAlpha,
                    detailBackdrop = detailBackdrop,
                    enabled = !isDownloading && illust != null,
                ) {
                    if (isDownloading) {
                        InfiniteProgressIndicator(
                            color = if (collapseProgress > 0.5f) MiuixTheme.colorScheme.primary else Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    } else {
                        Icon(
                            imageVector = MiuixIcons.Download,
                            contentDescription = strings.download,
                            tint = dynamicIconTint,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                // 3. 更多菜单按钮
                if (illust != null) {
                    LiquidCircleActionButton(
                        tooltip = strings.menuMoreActions,
                        onClick = { showMoreMenu = !showMoreMenu },
                        bubbleAlpha = bubbleAlpha,
                        detailBackdrop = detailBackdrop,
                    ) {
                        Icon(
                            imageVector = MiuixIcons.More,
                            contentDescription = strings.menuMoreActions,
                            tint = dynamicIconTint,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }
    }

    // 更多操作液态玻璃浮动菜单
    if (illust != null) {
        val currentIllust = illust
        PlatformBackHandler(enabled = showMoreMenu) {
            showMoreMenu = false
        }

        AnimatedVisibility(
            visible = showMoreMenu,
            enter = fadeIn(spring(dampingRatio = 0.8f)) +
                scaleIn(
                    animationSpec = spring(dampingRatio = 0.65f, stiffness = 420f),
                    initialScale = 0.80f,
                    transformOrigin = TransformOrigin(0.95f, 0f),
                ),
            exit = fadeOut(spring(dampingRatio = 0.9f)) +
                scaleOut(
                    animationSpec = spring(dampingRatio = 0.85f),
                    targetScale = 0.85f,
                    transformOrigin = TransformOrigin(0.95f, 0f),
                ),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
            ) {
                // 全屏透明遮罩（点击外部关闭菜单）
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { showMoreMenu = false },
                        ),
                )

                // 悬浮液态玻璃菜单卡片
                val menuCornerRadius = 18.dp
                val menuShape = remember { RoundedCornerShape(menuCornerRadius) }
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 58.dp, end = 16.dp)
                        .widthIn(min = 160.dp, max = 200.dp)
                        .liquidGlass(
                            backdrop = detailBackdrop,
                            shape = menuShape,
                            blurRadius = 18.dp,
                            tintColor = Color.Black,
                            tintAlpha = 0.45f,
                        )
                        .squircleBorder(
                            width = 0.6.dp,
                            color = Color.White.copy(alpha = 0.18f),
                            cornerRadius = menuCornerRadius,
                        )
                        .clip(menuShape)
                        .padding(vertical = 4.dp),
                ) {
                    LiquidMenuItem(
                        icon = MiuixIcons.Copy,
                        text = strings.menuCopyInfo,
                        onClick = {
                            showMoreMenu = false
                            val text = buildIllustCopyInfo(currentIllust)
                            runCatching { clipboard.copy(text) }.fold(
                                onSuccess = { onToast(strings.copiedToClipboard) },
                                onFailure = { e -> onToast("${strings.copy}${strings.loadFailed}: ${e.message}") },
                            )
                        },
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .height(0.5.dp)
                            .background(Color.White.copy(alpha = 0.10f)),
                    )
                    LiquidMenuItem(
                        icon = MiuixIcons.Show,
                        text = strings.menuCopyImage,
                        onClick = {
                            showMoreMenu = false
                            coroutineScope.launch {
                                suspendRunCatchingNonCancel {
                                    val candidateUrls = listOf(
                                        currentIllust.imageUrls.large,
                                        currentIllust.imageUrls.medium,
                                        currentIllust.imageUrls.squareMedium,
                                    )
                                    val bytes = extractCachedImageBytes(context, candidateUrls)
                                    bytes?.let { clipboard.copyImage(it) } ?: throw IllegalStateException(strings.imageNoCacheFound)
                                }.fold(
                                    onSuccess = { onToast(strings.imageCopySuccess) },
                                    onFailure = { e -> onToast("${strings.menuCopyImage}: ${e.message}") },
                                )
                            }
                        },
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .height(0.5.dp)
                            .background(Color.White.copy(alpha = 0.10f)),
                    )
                    LiquidMenuItem(
                        icon = MiuixIcons.Link,
                        text = strings.menuCopyLink,
                        onClick = {
                            showMoreMenu = false
                            val link = buildIllustShareLink(currentIllust)
                            runCatching { clipboard.copy(link) }.fold(
                                onSuccess = { onToast(strings.copiedToClipboard) },
                                onFailure = { e -> onToast("${strings.copy}${strings.loadFailed}: ${e.message}") },
                            )
                        },
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .height(0.5.dp)
                            .background(Color.White.copy(alpha = 0.10f)),
                    )
                    LiquidMenuItem(
                        icon = MiuixIcons.Share,
                        text = strings.share,
                        onClick = {
                            showMoreMenu = false
                            val link = buildIllustShareLink(currentIllust)
                            runCatching { share.share(link, currentIllust.title) }.fold(
                                onSuccess = { onToast(strings.share) },
                                onFailure = { e -> onToast("${strings.share}: ${e.message}") },
                            )
                        },
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .height(0.5.dp)
                            .background(Color.White.copy(alpha = 0.10f)),
                    )
                    LiquidMenuItem(
                        icon = MiuixIcons.Search,
                        text = strings.menuSauceNao,
                        onClick = {
                            showMoreMenu = false
                            val imgUrl = currentIllust.imageUrls.medium.ifEmpty { currentIllust.imageUrls.large }
                            val sauceUrl = buildSauceNaoUrl(imgUrl)
                            openSafeUrl(sauceUrl, strings, onError = { onToast(it) })
                        },
                    )
                    if (!isBanned) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .height(0.5.dp)
                                .background(Color.White.copy(alpha = 0.10f)),
                        )
                        LiquidMenuItem(
                            icon = MiuixIcons.Blocklist,
                            text = strings.menuBanWork,
                            onClick = {
                                showMoreMenu = false
                                coroutineScope.launch {
                                    suspendRunCatchingNonCancel {
                                        banRepository.insertBanIllust(currentIllust.id, currentIllust.title)
                                    }.fold(
                                        onSuccess = {
                                            onBanSuccess()
                                            onToast(strings.menuBanWork)
                                        },
                                        onFailure = { e -> onToast("${strings.menuBanWork}: ${e.message}") },
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * 液态玻璃菜单单项组件。
 */
@Composable
fun LiquidMenuItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isPressed) Color.White.copy(alpha = 0.12f) else Color.Transparent,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = text,
            style = MiuixTheme.textStyles.body2,
            color = Color.White,
        )
    }
}

/**
 * 从 Coil 磁盘缓存中尝试提取已有图片数据。
 */
fun extractCachedImageBytes(context: PlatformContext, urls: List<String>): ByteArray? {
    val imageLoader = SingletonImageLoader.get(context)
    val diskCache = imageLoader.diskCache ?: return null
    for (candidateUrl in urls) {
        diskCache.openSnapshot(candidateUrl)?.use { snapshot ->
            val fileSystem = FileSystem.SYSTEM
            if (fileSystem.exists(snapshot.data) && (fileSystem.metadata(snapshot.data).size ?: 0L) > 0L) {
                return fileSystem.read(snapshot.data) { readByteArray() }
            }
        }
    }
    return null
}

@Composable
fun LiquidCircleActionButton(
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

    TooltipBox(text = tooltip, modifier = modifier) {
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
