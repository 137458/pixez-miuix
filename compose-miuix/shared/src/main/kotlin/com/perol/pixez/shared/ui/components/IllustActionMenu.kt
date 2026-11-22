package com.perol.pixez.shared.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import coil3.compose.LocalPlatformContext
import com.perol.pixez.shared.data.model.DownloadStatus
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.network.SauceNaoEndpoints
import com.perol.pixez.shared.platform.IllustClipboard
import com.perol.pixez.shared.platform.IllustShare
import com.perol.pixez.shared.platform.mapToPictureSource
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.LocalBanRepository
import com.perol.pixez.shared.ui.LocalBookmarkRepository
import com.perol.pixez.shared.ui.LocalDownloadRepository
import com.perol.pixez.shared.ui.LocalIllustRepository
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.screens.starAfterSaveIfNeeded
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.window.WindowBottomSheet

/**
 * 作品详情页底部操作菜单。
 *
 * 基于 [WindowBottomSheet] 实现，支持独立 Window 渲染，无需依赖页面级 Scaffold。
 * 当前提供「复制信息」「复制链接」「分享链接」与「屏蔽作品」四项操作，
 * 与原 Flutter PixEz 作品详情页「更多」菜单保持一致。
 *
 * @param show 是否显示菜单
 * @param showBan 是否显示「屏蔽作品」入口；作品已屏蔽时可设为 false 避免重复屏蔽
 * @param onDismissRequest 用户请求关闭菜单时的回调
 * @param onCopyInfo 点击「复制信息」时的回调
 * @param onCopyLink 点击「复制链接」时的回调
 * @param onShareLink 点击「分享链接」时的回调
 * @param onBan 点击「屏蔽作品」时的回调
 */
@Composable
fun IllustActionMenu(
    show: Boolean,
    showBan: Boolean,
    onDismissRequest: () -> Unit,
    onCopyInfo: () -> Unit,
    onCopyImage: (() -> Unit)? = null,
    onDownload: (() -> Unit)? = null,
    onCopyLink: () -> Unit,
    onShareLink: () -> Unit,
    onSauceNao: (() -> Unit)? = null,
    onBan: () -> Unit,
) {
    val strings = LocalStrings.current

    WindowBottomSheet(
        show = show,
        title = strings.menuMoreActions,
        onDismissRequest = onDismissRequest,
    ) {
        Column {
            if (onDownload != null) {
                BasicComponent(
                    title = strings.download,
                    onClick = onDownload,
                )
            }
            BasicComponent(
                title = strings.menuCopyInfo,
                onClick = onCopyInfo,
            )
            if (onCopyImage != null) {
                BasicComponent(
                    title = strings.menuCopyImage,
                    onClick = onCopyImage,
                )
            }
            BasicComponent(
                title = strings.menuCopyLink,
                onClick = onCopyLink,
            )
            BasicComponent(
                title = strings.menuShareLink,
                onClick = onShareLink,
            )
            if (onSauceNao != null) {
                BasicComponent(
                    title = strings.menuSauceNao,
                    onClick = onSauceNao,
                )
            }
            if (showBan) {
                BasicComponent(
                    title = strings.menuBanWork,
                    onClick = onBan,
                )
            }
        }
    }
}

/**
 * 插画卡片快捷操作集合：下载、复制信息/图片/链接、分享链接、屏蔽作品。
 *
 * 只描述动作，不承担菜单视觉；MIUIX 用 [IllustActionMenu] + `WindowBottomSheet`，
 * COUI 用官方 Dialog 承载同一批动作，避免两套皮肤各写一份编排逻辑。
 */
internal class IllustQuickActions(
    val onDownload: () -> Unit,
    val onCopyInfo: () -> Unit,
    val onCopyImage: () -> Unit,
    val onCopyLink: () -> Unit,
    val onShareLink: () -> Unit,
    val onBan: () -> Unit,
)

/**
 * 构造插画快捷操作。动作执行后统一调用 [onDismiss] 关闭所在菜单。
 */
@Composable
internal fun rememberIllustQuickActions(
    illust: Illust,
    onDismiss: () -> Unit,
): IllustQuickActions {
    val strings = LocalStrings.current
    val settings = LocalSettingsRepository.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalPlatformContext.current
    val downloadRepository = LocalDownloadRepository.current
    val bookmarkRepository = LocalBookmarkRepository.current
    val illustRepository = LocalIllustRepository.current
    val banRepository = LocalBanRepository.current
    // 长按菜单经全局 Toast 通道反馈，列表页无需逐层透传承载容器。
    val toastSink = LocalToastSink.current

    return IllustQuickActions(
        onDownload = {
            onDismiss()
            coroutineScope.launch {
                val succeeded = suspendRunCatchingNonCancel {
                    if (AppConstants.IllustType.isUgoira(illust.type)) {
                        saveUgoiraIllust(
                            illust = illust,
                            illustRepository = illustRepository,
                            downloadRepository = downloadRepository,
                        ).isSuccess
                    } else {
                        downloadRepository.download(illust, pageIndex = 0).status == DownloadStatus.Success
                    }
                }.getOrDefault(false)
                if (succeeded) {
                    starAfterSaveIfNeeded(
                        illustId = illust.id,
                        isBookmarked = illust.isBookmarked,
                        settings = settings,
                        bookmarkRepository = bookmarkRepository,
                    )
                }
            }
        },
        onCopyInfo = {
            onDismiss()
            runClipboardShare(
                success = ToastData(strings.copiedToClipboard, ToastType.Success),
                failurePrefix = "${strings.copy}${strings.failedSuffix}",
                onToast = toastSink,
            ) { IllustClipboard().copy(buildIllustCopyInfo(illust, settings.copyInfoText)) }
        },
        onCopyImage = {
            onDismiss()
            coroutineScope.launch {
                // 缩略图降级沿用卡片自身的 pictureSource 改写，缓存缺失时静默不复制。
                suspendRunClipboardShare(
                    success = ToastData(strings.imageCopySuccess, ToastType.Success),
                    failurePrefix = strings.menuCopyImage,
                    onToast = toastSink,
                ) {
                    val candidateUrls = listOf(illust.imageUrls.large, illust.imageUrls.medium, illust.imageUrls.squareMedium)
                    val transformedUrls = candidateUrls.map { it.mapToPictureSource(settings.pictureSource) }
                    withContext(Dispatchers.IO) {
                        val bytes = extractCachedImageBytes(context, transformedUrls)
                        bytes?.let { IllustClipboard().copyImage(it) }
                    }
                }
            }
        },
        onCopyLink = {
            onDismiss()
            runClipboardShare(
                success = ToastData(strings.copiedToClipboard, ToastType.Success),
                failurePrefix = "${strings.copy}${strings.failedSuffix}",
                onToast = toastSink,
            ) { IllustClipboard().copy(buildIllustShareLink(illust)) }
        },
        onShareLink = {
            onDismiss()
            runClipboardShare(
                success = ToastData(strings.share, ToastType.Success),
                failurePrefix = "${strings.share}${strings.failedSuffix}",
                onToast = toastSink,
            ) { IllustShare().share(buildIllustShareLink(illust), illust.title) }
        },
        onBan = {
            onDismiss()
            coroutineScope.launch {
                suspendRunCatchingNonCancel {
                    banRepository.insertBanIllust(
                        illustId = illust.id,
                        name = illust.title,
                    )
                    settings.notifyFilterChanged()
                }
            }
        },
    )
}

/**
 * 按模板构造「复制信息」文本；模板为空时回落至默认模板。
 *
 * 支持占位符：`{title}`、`{illust_id}`、`{user_id}`、`{user_name}`、`{author}`、`{tags}`。
 */
internal fun buildIllustCopyInfo(
    illust: Illust,
    template: String = SettingsRepository.DEFAULT_COPY_TEXT_FORMAT,
): String {
    val effectiveTemplate = template.takeIf { it.isNotBlank() } ?: SettingsRepository.DEFAULT_COPY_TEXT_FORMAT
    val tagsText = illust.tags.joinToString(", ") { it.name }
    return effectiveTemplate
        .replace("{title}", illust.title)
        .replace("{illust_id}", illust.id.toString())
        .replace("{user_id}", illust.user.id.toString())
        .replace("{user_name}", illust.user.name)
        .replace("{author}", illust.user.name)
        .replace("{tags}", tagsText)
}

/**
 * 构造作品详情页链接。
 */
internal fun buildIllustShareLink(illust: Illust): String {
    return AppConstants.Urls.pixivArtworkUrl(illust.id)
}

/**
 * 构造 SauceNAO 以图搜图链接。
 */
internal fun buildSauceNaoUrl(imageUrl: String): String {
    return io.ktor.http.URLBuilder(SauceNaoEndpoints.SEARCH).apply {
        parameters.append("db", "999")
        parameters.append("url", imageUrl)
    }.buildString()
}