package com.perol.pixez.shared.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.perol.pixez.shared.data.model.DownloadStatus
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.repository.BanRepository
import com.perol.pixez.shared.data.repository.BookmarkRepository
import com.perol.pixez.shared.data.repository.DownloadRepository
import com.perol.pixez.shared.data.repository.IllustRepository
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.platform.HapticType
import com.perol.pixez.shared.platform.performHapticFeedback
import com.perol.pixez.shared.ui.components.IllustDetailTopBar
import com.perol.pixez.shared.ui.i18n.AppStrings
import com.perol.pixez.shared.ui.utils.suspendRunCatchingNonCancel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.LayerBackdrop

/**
 * 作品详情页统一顶栏区块：封装「收藏 / 取消收藏」与「下载」两项业务操作逻辑，
 * 并将其接入 [IllustDetailTopBar]。
 *
 * 收藏态、下载态等页面级状态仍由 [IllustDetailSingleContent] 持有，
 * 此组件通过参数读取当前值、通过回调提交更新，自身不持有任何状态。
 */
@Composable
internal fun IllustDetailTopBarSection(
    illust: Illust?,
    isBookmarked: Boolean,
    isBookmarkLoading: Boolean,
    isDownloading: Boolean,
    isBanned: Boolean,
    settings: SettingsRepository?,
    strings: AppStrings,
    detailBackdrop: LayerBackdrop?,
    collapseProgressProvider: () -> Float,
    bookmarkHeartScale: Animatable<Float, AnimationVector1D>,
    coroutineScope: CoroutineScope,
    repository: IllustRepository,
    bookmarkRepository: BookmarkRepository,
    downloadRepository: DownloadRepository,
    banRepository: BanRepository,
    onBookmarkedChange: (Boolean) -> Unit,
    onBookmarkLoadingChange: (Boolean) -> Unit,
    onBookmarkErrorChange: (String?) -> Unit,
    onDownloadingChange: (Boolean) -> Unit,
    onToast: (String?) -> Unit,
    onBanSuccess: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 提取通用业务操作逻辑
    val performBookmark: () -> Unit = {
        illust?.let { targetIllust ->
            coroutineScope.launch {
                try {
                    onBookmarkLoadingChange(true)
                    onBookmarkErrorChange(null)
                    val wasBookmarked = isBookmarked
                    suspendRunCatchingNonCancel {
                        if (wasBookmarked) {
                            bookmarkRepository.deleteBookmark(targetIllust.id)
                        } else {
                            val autoTags = if (settings?.autoTagWhenStar == true) {
                                targetIllust.tags.map { tag -> tag.name }.take(10).joinToString(" ").ifBlank { null }
                            } else null
                            bookmarkRepository.addBookmark(
                                illustId = targetIllust.id,
                                isPrivate = settings?.defaultPrivateLike ?: false,
                                tags = autoTags,
                            )
                        }
                    }.onSuccess {
                        performHapticFeedback(HapticType.Confirm)
                        onBookmarkedChange(!wasBookmarked)
                        if (!wasBookmarked) {
                            if (settings?.saveAfterStar == true) {
                                coroutineScope.launch {
                                    onToast("${strings.downloadStatusDownloading}…")
                                    val task = downloadRepository.download(targetIllust, pageIndex = 0)
                                    onToast(
                                        when (task.status) {
                                            DownloadStatus.Success -> strings.downloadStatusSuccess
                                            DownloadStatus.Failed -> "${strings.downloadStatusFailed}: ${task.error ?: strings.loadFailed}"
                                            else -> null
                                        },
                                    )
                                }
                            }
                            if (settings?.followAfterStar == true) {
                                coroutineScope.launch {
                                    suspendRunCatchingNonCancel {
                                        bookmarkRepository.followUser(targetIllust.user.id)
                                    }
                                }
                            }
                        }
                    }.onFailure { e ->
                        performHapticFeedback(HapticType.Reject)
                        onBookmarkErrorChange(e.message ?: strings.loadFailed)
                    }
                } finally {
                    onBookmarkLoadingChange(false)
                }
            }
        }
    }

    val performDownload: () -> Unit = {
        if (!isDownloading && illust != null) {
            val targetIllust = illust
            coroutineScope.launch {
                try {
                    onDownloadingChange(true)
                    performHapticFeedback(HapticType.GestureStart)
                    onToast("${strings.downloadStatusDownloading}…")
                    if (targetIllust.type == "ugoira") {
                        val meta = repository.getUgoiraMetadata(targetIllust.id)
                        val zipBytes = repository.downloadUgoiraZip(meta.ugoiraMetadata.zipUrls.medium)
                        val savedPath = downloadRepository.saveUgoiraZip(
                            illust = targetIllust,
                            bytes = zipBytes,
                            zipUrl = meta.ugoiraMetadata.zipUrls.medium,
                        )
                        if (settings?.starAfterSave == true && !isBookmarked) {
                            coroutineScope.launch {
                                suspendRunCatchingNonCancel {
                                    bookmarkRepository.addBookmark(
                                        illustId = targetIllust.id,
                                        isPrivate = settings.defaultPrivateLike,
                                    )
                                }.onSuccess {
                                    onBookmarkedChange(true)
                                }
                            }
                        }
                        performHapticFeedback(HapticType.Confirm)
                        onToast(strings.downloadStatusSuccess)
                    } else {
                        val task = downloadRepository.download(targetIllust, pageIndex = 0)
                        onToast(
                            when (task.status) {
                                DownloadStatus.Success -> {
                                    performHapticFeedback(HapticType.Confirm)
                                    if (settings?.starAfterSave == true && !isBookmarked) {
                                        coroutineScope.launch {
                                            suspendRunCatchingNonCancel {
                                                bookmarkRepository.addBookmark(
                                                    illustId = targetIllust.id,
                                                    isPrivate = settings.defaultPrivateLike,
                                                )
                                            }.onSuccess {
                                                onBookmarkedChange(true)
                                            }
                                        }
                                    }
                                    strings.downloadStatusSuccess
                                }
                                DownloadStatus.Failed -> {
                                    performHapticFeedback(HapticType.Reject)
                                    "${strings.downloadStatusFailed}: ${task.error ?: strings.loadFailed}"
                                }
                                else -> null
                            },
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    performHapticFeedback(HapticType.Reject)
                    onToast("${strings.downloadStatusFailed}: ${e.message ?: strings.loadFailed}")
                } finally {
                    onDownloadingChange(false)
                }
            }
        }
    }

    // ── 统一锚点单层顶栏与液态玻璃操作菜单 ──
    IllustDetailTopBar(
        illust = illust,
        collapseProgressProvider = collapseProgressProvider,
        detailBackdrop = detailBackdrop,
        isBookmarked = isBookmarked,
        isBookmarkLoading = isBookmarkLoading,
        bookmarkHeartScale = bookmarkHeartScale,
        onBookmarkClick = performBookmark,
        isDownloading = isDownloading,
        onDownloadClick = performDownload,
        onBack = onBack,
        isBanned = isBanned,
        banRepository = banRepository,
        onBanSuccess = onBanSuccess,
        onToast = { onToast(it) },
        modifier = modifier,
    )
}