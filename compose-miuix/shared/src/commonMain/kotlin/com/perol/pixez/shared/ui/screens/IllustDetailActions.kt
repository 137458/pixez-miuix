package com.perol.pixez.shared.ui.screens

import androidx.compose.runtime.Composable
import com.perol.pixez.shared.data.model.DownloadStatus
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.repository.BookmarkRepository
import com.perol.pixez.shared.data.repository.DownloadRepository
import com.perol.pixez.shared.data.repository.IllustRepository
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.platform.HapticType
import com.perol.pixez.shared.platform.performHapticFeedback
import com.perol.pixez.shared.ui.AppConstants.IllustType
import com.perol.pixez.shared.ui.components.PixivApiErrorFormatter
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.components.ToastType
import com.perol.pixez.shared.ui.components.saveUgoiraIllust
import com.perol.pixez.shared.ui.components.toLocalizedDownloadError
import com.perol.pixez.shared.ui.i18n.AppStrings
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 作品详情页的三项业务动作（收藏 / 下载当前页 / 下载全部）。
 *
 * MIUIX 与 COUI 两套皮肤共用同一份实现：视觉层只决定按钮形态，
 * 收藏后自动下载/关注、保存后自动收藏等设置链路保持一致。
 */
internal class IllustDetailActions(
    private val onBookmark: () -> Unit,
    private val onDownload: () -> Unit,
    private val onDownloadAll: () -> Unit,
) {
    fun bookmark() = onBookmark()

    fun download() = onDownload()

    fun downloadAll() = onDownloadAll()
}

/**
 * 构建详情页业务动作。入参均为页面级状态与仓储，返回的动作对象每次组合刷新，
 * 调用时读取到的收藏/下载状态始终是最新值。
 */
@Composable
internal fun rememberIllustDetailActions(
    illust: Illust?,
    isBookmarked: Boolean,
    isDownloading: Boolean,
    settings: SettingsRepository?,
    strings: AppStrings,
    coroutineScope: CoroutineScope,
    repository: IllustRepository,
    bookmarkRepository: BookmarkRepository,
    downloadRepository: DownloadRepository,
    onBookmarkedChange: (Boolean) -> Unit,
    onBookmarkLoadingChange: (Boolean) -> Unit,
    onBookmarkErrorChange: (String?) -> Unit,
    onDownloadingChange: (Boolean) -> Unit,
    onToast: (ToastData?) -> Unit,
): IllustDetailActions {
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
                                    onToast(ToastData("${strings.downloadStatusDownloading}…", ToastType.Normal))
                                    val task = downloadRepository.download(targetIllust, pageIndex = 0)
                                    onToast(
                                        when (task.status) {
                                            DownloadStatus.Success ->
                                                ToastData(strings.downloadStatusSuccess, ToastType.Success)
                                            DownloadStatus.Failed ->
                                                ToastData(
                                                    "${strings.downloadStatusFailed}: ${task.error.toLocalizedDownloadError(strings)}",
                                                    ToastType.Error,
                                                )
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
                        onBookmarkErrorChange(PixivApiErrorFormatter.format(e, strings))
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
                    onToast(ToastData("${strings.downloadStatusDownloading}…", ToastType.Normal))
                    if (IllustType.isUgoira(targetIllust.type)) {
                        val savedPath = saveUgoiraIllust(
                            illust = targetIllust,
                            illustRepository = repository,
                            downloadRepository = downloadRepository,
                        ).getOrThrow()
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
                        onToast(ToastData(strings.downloadStatusSuccess, ToastType.Success))
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
                                    ToastData(strings.downloadStatusSuccess, ToastType.Success)
                                }
                                DownloadStatus.Failed -> {
                                    performHapticFeedback(HapticType.Reject)
                                    ToastData(
                                        "${strings.downloadStatusFailed}: ${task.error.toLocalizedDownloadError(strings)}",
                                        ToastType.Error,
                                    )
                                }
                                else -> null
                            },
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    performHapticFeedback(HapticType.Reject)
                    onToast(
                        ToastData(
                            PixivApiErrorFormatter.format(e, strings),
                            ToastType.Error,
                        ),
                    )
                } finally {
                    onDownloadingChange(false)
                }
            }
        }
    }

    val performDownloadAll: () -> Unit = {
        if (!isDownloading && illust != null) {
            val targetIllust = illust
            coroutineScope.launch {
                try {
                    onDownloadingChange(true)
                    val tasks = downloadRepository.downloadAllPages(
                        illust = targetIllust,
                        onProgress = { completed, total ->
                            onToast(ToastData("${strings.downloadStatusDownloading} $completed/$total", ToastType.Normal))
                        },
                        maxConcurrency = settings?.maxRunningTask ?: 2,
                    )
                    val successCount = tasks.count { it.status == DownloadStatus.Success }
                    val failedCount = tasks.count { it.status == DownloadStatus.Failed }
                    if (successCount > 0 && settings?.starAfterSave == true && !isBookmarked) {
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
                    onToast(
                        when {
                            failedCount == 0 -> ToastData("${strings.downloadStatusSuccess}: $successCount/${tasks.size}", ToastType.Success)
                            successCount == 0 -> ToastData(strings.downloadStatusFailed, ToastType.Error)
                            else -> ToastData("${strings.downloadStatusSuccess}: $successCount, ${strings.downloadStatusFailed} $failedCount", ToastType.Normal)
                        },
                    )
                } finally {
                    onDownloadingChange(false)
                }
            }
        }
    }

    return IllustDetailActions(
        onBookmark = performBookmark,
        onDownload = performDownload,
        onDownloadAll = performDownloadAll,
    )
}
