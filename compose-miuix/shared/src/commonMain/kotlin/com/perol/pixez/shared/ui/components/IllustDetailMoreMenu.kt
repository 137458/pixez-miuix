/**
 * [IllustDetailTopBar] 的「更多操作」液态玻璃浮动菜单（自 IllustDetailTopBar.kt 拆分而来，纯代码搬运）。
 *
 * [IllustDetailMoreMenu] 负责菜单的进出场动画、全屏透明遮罩与液态玻璃卡片容器；
 * [IllustDetailMoreMenuCopyActions] 负责复制信息 / 复制图片 / 复制链接；
 * [IllustDetailMoreMenuShareActions] 负责分享 / SauceNao 搜图 / 屏蔽作品。
 * 菜单可见性与剪贴板、分享、协程作用域等状态仍由入口函数持有并作为参数传入。
 */

package com.perol.pixez.shared.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import coil3.PlatformContext
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.repository.BanRepository
import com.perol.pixez.shared.platform.IllustClipboard
import com.perol.pixez.shared.platform.IllustShare
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.utils.openSafeUrl
import com.perol.pixez.shared.ui.utils.suspendRunCatchingNonCancel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Blocklist
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.icon.extended.Share
import top.yukonga.miuix.kmp.icon.extended.Show
import top.yukonga.miuix.kmp.squircle.squircleBorder

/**
 * 更多操作浮动菜单容器。
 *
 * @param visible 菜单是否可见（由入口函数的 rememberSaveable 状态驱动）
 * @param onDismiss 关闭菜单回调（点击遮罩或任一菜单项后触发）
 */
@Composable
internal fun IllustDetailMoreMenu(
    visible: Boolean,
    onDismiss: () -> Unit,
    detailBackdrop: Backdrop?,
    illust: Illust,
    isBanned: Boolean,
    banRepository: BanRepository,
    onBanSuccess: () -> Unit,
    onToast: (String) -> Unit,
    clipboard: IllustClipboard,
    share: IllustShare,
    context: PlatformContext,
    coroutineScope: CoroutineScope,
) {
    AnimatedVisibility(
        visible = visible,
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
                        onClick = onDismiss,
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
                IllustDetailMoreMenuCopyActions(
                    illust = illust,
                    onDismiss = onDismiss,
                    clipboard = clipboard,
                    context = context,
                    coroutineScope = coroutineScope,
                    onToast = onToast,
                )
                IllustDetailMoreMenuShareActions(
                    illust = illust,
                    onDismiss = onDismiss,
                    share = share,
                    isBanned = isBanned,
                    banRepository = banRepository,
                    onBanSuccess = onBanSuccess,
                    coroutineScope = coroutineScope,
                    onToast = onToast,
                )
            }
        }
    }
}

/**
 * 菜单「复制」分组：复制信息、复制图片、复制链接。
 */
@Composable
internal fun IllustDetailMoreMenuCopyActions(
    illust: Illust,
    onDismiss: () -> Unit,
    clipboard: IllustClipboard,
    context: PlatformContext,
    coroutineScope: CoroutineScope,
    onToast: (String) -> Unit,
) {
    val strings = LocalStrings.current
    LiquidMenuItem(
        icon = MiuixIcons.Copy,
        text = strings.menuCopyInfo,
        onClick = {
            onDismiss()
            val text = buildIllustCopyInfo(illust)
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
            onDismiss()
            coroutineScope.launch {
                suspendRunCatchingNonCancel {
                    withContext(Dispatchers.IO) {
                        val candidateUrls = listOf(
                            illust.imageUrls.large,
                            illust.imageUrls.medium,
                            illust.imageUrls.squareMedium,
                        )
                        val bytes = extractCachedImageBytes(context, candidateUrls)
                        bytes?.let { clipboard.copyImage(it) } ?: throw IllegalStateException(strings.imageNoCacheFound)
                    }
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
            onDismiss()
            val link = buildIllustShareLink(illust)
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
}

/**
 * 菜单「分享 / 其他」分组：分享作品、SauceNao 搜图、屏蔽作品（未屏蔽时）。
 */
@Composable
internal fun IllustDetailMoreMenuShareActions(
    illust: Illust,
    onDismiss: () -> Unit,
    share: IllustShare,
    isBanned: Boolean,
    banRepository: BanRepository,
    onBanSuccess: () -> Unit,
    coroutineScope: CoroutineScope,
    onToast: (String) -> Unit,
) {
    val strings = LocalStrings.current
    LiquidMenuItem(
        icon = MiuixIcons.Share,
        text = strings.share,
        onClick = {
            onDismiss()
            val link = buildIllustShareLink(illust)
            runCatching { share.share(link, illust.title) }.fold(
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
            onDismiss()
            val imgUrl = illust.imageUrls.medium.ifEmpty { illust.imageUrls.large }
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
                onDismiss()
                coroutineScope.launch {
                    suspendRunCatchingNonCancel {
                        banRepository.insertBanIllust(illust.id, illust.title)
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