package com.perol.pixez.shared.ui.components

import androidx.compose.ui.graphics.vector.ImageVector
import coil3.PlatformContext
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.platform.IllustClipboard
import com.perol.pixez.shared.platform.IllustShare
import com.perol.pixez.shared.ui.i18n.AppStrings
import com.perol.pixez.shared.ui.utils.openSafeUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.icon.extended.HorizontalSplit
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.icon.extended.Share

/**
 * 全屏查看器的纯逻辑与菜单装配：不持有 Compose 状态，可单独阅读与复用。
 * 页面渲染与手势留在 [IllustFullScreenViewer] 与 [ZoomableImage]。
 */

/**
 * 解析当前全屏查看页的目标大图 URL，用于复制位图及以图搜图等次级操作。
 */
internal fun resolveViewerTargetImageUrl(illust: Illust, pageIndex: Int): String {
    return if (illust.metaPages.isNotEmpty() && pageIndex in illust.metaPages.indices) {
        illust.metaPages[pageIndex].imageUrls?.large
            ?: illust.metaPages[pageIndex].imageUrls?.medium
            ?: illust.imageUrls.large
    } else {
        illust.imageUrls.large.ifEmpty { illust.imageUrls.medium }
    }
}

/**
 * 构造全屏查看器单页分享信息（链接与标题）。多图时携带 `#page=X` 锚点与 P 数标题。
 */
internal fun buildViewerShareInfo(
    illust: Illust,
    pageCount: Int,
    currentPage: Int,
): Pair<String, String> {
    val pageAnchor = if (pageCount > 1) "#page=${currentPage + 1}" else ""
    val link = "${buildIllustShareLink(illust)}$pageAnchor"
    val shareTitle = if (pageCount > 1) "${illust.title} (P${currentPage + 1})" else illust.title
    return link to shareTitle
}

/**
 * 计算阅读模式切换按钮的目标 Tooltip 与对应展示图标。
 */
internal fun resolveViewerModeSwitchState(
    isVerticalScrollMode: Boolean,
    strings: AppStrings,
): Pair<String, ImageVector> {
    return if (isVerticalScrollMode) {
        strings.viewerModeHorizontal to MiuixIcons.HorizontalSplit
    } else {
        strings.viewerModeVertical to MiuixIcons.ListView
    }
}

/**
 * 构建全屏查看器「更多操作」下拉菜单条目：
 * 第一组：复制图片、复制作品链接；
 * 第二组：分享作品、SauceNAO 搜图。
 */
internal fun buildViewerMoreMenuEntries(
    strings: AppStrings,
    illust: Illust,
    pageCount: Int,
    currentDisplayPage: Int,
    context: PlatformContext,
    coroutineScope: CoroutineScope,
    onToast: (ToastData) -> Unit,
): List<DropdownEntry> = listOf(
    DropdownEntry(
        items = listOf(
            DropdownItem(
                text = strings.menuCopyImage,
                icon = { modifier ->
                    Icon(imageVector = MiuixIcons.Copy, contentDescription = null, modifier = modifier)
                },
                onClick = {
                    val targetUrl = resolveViewerTargetImageUrl(illust, currentDisplayPage)
                    coroutineScope.launch {
                        suspendRunClipboardShare(
                            success = ToastData(strings.imageCopySuccess, ToastType.Success),
                            failurePrefix = strings.menuCopyImage,
                            onToast = onToast,
                        ) {
                            withContext(Dispatchers.IO) {
                                val candidateUrls = listOfNotNull(
                                    targetUrl,
                                    illust.imageUrls.large,
                                    illust.imageUrls.medium,
                                )
                                val bytes = extractCachedImageBytes(context, candidateUrls)
                                bytes?.let { IllustClipboard().copyImage(it) }
                                    ?: throw IllegalStateException(strings.imageNoCacheFound)
                            }
                        }
                    }
                },
            ),
            DropdownItem(
                text = strings.menuCopyLink,
                icon = { modifier ->
                    Icon(imageVector = MiuixIcons.Link, contentDescription = null, modifier = modifier)
                },
                onClick = {
                    val (link, _) = buildViewerShareInfo(illust, pageCount, currentDisplayPage)
                    runClipboardShare(
                        success = ToastData(strings.copiedToClipboard, ToastType.Success),
                        failurePrefix = "${strings.copy}${strings.failedSuffix}",
                        onToast = onToast,
                    ) { IllustClipboard().copy(link) }
                },
            ),
        ),
    ),
    DropdownEntry(
        items = listOf(
            DropdownItem(
                text = strings.share,
                icon = { modifier ->
                    Icon(imageVector = MiuixIcons.Share, contentDescription = null, modifier = modifier)
                },
                onClick = {
                    val (link, shareTitle) = buildViewerShareInfo(illust, pageCount, currentDisplayPage)
                    runClipboardShare(
                        success = ToastData(strings.share, ToastType.Success),
                        failurePrefix = strings.share,
                        onToast = onToast,
                    ) { IllustShare().share(link, shareTitle) }
                },
            ),
            DropdownItem(
                text = strings.menuSauceNao,
                icon = { modifier ->
                    Icon(imageVector = MiuixIcons.Search, contentDescription = null, modifier = modifier)
                },
                onClick = {
                    val currentPage = currentDisplayPage
                    val imgUrl = if (illust.metaPages.isNotEmpty() && currentPage in illust.metaPages.indices) {
                        illust.metaPages[currentPage].imageUrls?.medium
                            ?: illust.metaPages[currentPage].imageUrls?.squareMedium
                            ?: illust.imageUrls.medium
                    } else {
                        illust.imageUrls.medium.ifEmpty { illust.imageUrls.large }
                    }
                    val sauceUrl = buildSauceNaoUrl(imgUrl)
                    openSafeUrl(sauceUrl, strings, onError = { onToast(ToastData(it, ToastType.Error)) })
                },
            ),
        ),
    ),
)
