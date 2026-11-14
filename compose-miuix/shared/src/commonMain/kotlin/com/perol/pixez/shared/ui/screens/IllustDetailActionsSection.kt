package com.perol.pixez.shared.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.repository.BanRepository
import com.perol.pixez.shared.data.repository.BookmarkRepository
import com.perol.pixez.shared.data.repository.DownloadRepository
import com.perol.pixez.shared.data.repository.IllustRepository
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.components.IllustDetailTopBar
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.i18n.AppStrings
import kotlinx.coroutines.CoroutineScope
import top.yukonga.miuix.kmp.blur.LayerBackdrop

/**
 * 作品详情页统一顶栏区块：把收藏 / 下载动作接入 [IllustDetailTopBar]。
 *
 * 两项动作的实现收敛在 [rememberIllustDetailActions]，与 COUI 详情页共用同一份业务逻辑；
 * 收藏态、下载态等页面级状态仍由页面持有，此组件仅读取并按结果回调提交。
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
    onToast: (ToastData?) -> Unit,
    onBanSuccess: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actions = rememberIllustDetailActions(
        illust = illust,
        isBookmarked = isBookmarked,
        isDownloading = isDownloading,
        settings = settings,
        strings = strings,
        coroutineScope = coroutineScope,
        repository = repository,
        bookmarkRepository = bookmarkRepository,
        downloadRepository = downloadRepository,
        onBookmarkedChange = onBookmarkedChange,
        onBookmarkLoadingChange = onBookmarkLoadingChange,
        onBookmarkErrorChange = onBookmarkErrorChange,
        onDownloadingChange = onDownloadingChange,
        onToast = onToast,
    )

    // ── 统一锚点单层顶栏与液态玻璃操作菜单 ──
    IllustDetailTopBar(
        illust = illust,
        collapseProgressProvider = collapseProgressProvider,
        detailBackdrop = detailBackdrop,
        isBookmarked = isBookmarked,
        isBookmarkLoading = isBookmarkLoading,
        bookmarkHeartScale = bookmarkHeartScale,
        onBookmarkClick = actions::bookmark,
        isDownloading = isDownloading,
        onDownloadClick = actions::download,
        onBack = onBack,
        isBanned = isBanned,
        banRepository = banRepository,
        onBanSuccess = onBanSuccess,
        onToast = { onToast(it) },
        modifier = modifier,
    )
}
