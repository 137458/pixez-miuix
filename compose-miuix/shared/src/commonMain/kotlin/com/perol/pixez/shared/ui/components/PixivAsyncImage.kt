package com.perol.pixez.shared.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.preferEndFirstIntrinsicSize
import coil3.compose.rememberAsyncImagePainter
import coil3.decode.DataSource
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Dimension
import coil3.size.Precision
import com.perol.pixez.shared.data.settings.LocalSettingsRepository
import com.perol.pixez.shared.platform.configurePlatformOptimizations
import com.perol.pixez.shared.platform.mapToPictureSource
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.navigation.animation.LocalSharedBoundsRegistry
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException

private val StandardHeaders = NetworkHeaders.Builder()
    .set("Referer", AppConstants.Urls.PIXIV_APP_API)
    .set("User-Agent", AppConstants.Network.IMAGE_REQUEST_USER_AGENT)
    .build()

private val PixivisionHeaders = NetworkHeaders.Builder()
    .set("Referer", AppConstants.Network.REFERER_PIXIVISION)
    .set("User-Agent", AppConstants.Network.IMAGE_REQUEST_USER_AGENT)
    .build()

/**
 * 原图尺寸请求的内存缓存键后缀。
 *
 * 与列表/详情页使用的普通 URL 键隔离，避免小尺寸解码结果被全屏查看器命中。
 */
private const val ORIGINAL_SIZE_CACHE_KEY_SUFFIX = "#original_size"

/** 调用方未接管错误时的静默重试次数（瞬时网络/代理抖动自愈）。 */
internal const val SILENT_ERROR_MAX_RETRIES = 1

/**
 * 裁决一次引擎错误回调该怎么处理。
 *
 * 取消类 [CancellationException] 返回 [SilentErrorDecision.Ignore]：节点被回收、去重让位等
 * 场景下的取消不是加载失败，重建重试只会形成无限重启循环。
 */
internal fun resolveSilentErrorDecision(
    throwable: Throwable?,
    onError: ((Throwable?) -> Unit)?,
    silentRetryCount: Int,
): SilentErrorDecision = when {
    throwable is CancellationException -> SilentErrorDecision.Ignore
    onError == null && silentRetryCount < SILENT_ERROR_MAX_RETRIES -> SilentErrorDecision.Retry
    else -> SilentErrorDecision.Report(throwable)
}

/** 错误回调的裁决结果。 */
internal sealed interface SilentErrorDecision {
    /** 重建图片节点后重试一次。 */
    data object Retry : SilentErrorDecision

    /** 取消类错误：不重试也不上报。 */
    data object Ignore : SilentErrorDecision

    /** 把错误上报给调用方（无回调时仅记录日志）。 */
    data class Report(val throwable: Throwable?) : SilentErrorDecision
}

/**
 * 判定一次 [AsyncImagePainter.State.Success] 是否需要触发一次 `MEMORY_CACHE` 同步重绑。
 *
 * 当图片首次通过 [DataSource.DISK] 或 [DataSource.NETWORK] 异步加载完成时，若组件处于固定宽高约束
 * （如 `fillMaxWidth().aspectRatio(...)`），`AbstractContentPainterNode.modifyConstraints` 在测量阶段直接早退，
 * 不订阅 `painter$delegate`，入场窗口期的绘制失效可能丢失。
 * 此时位图已写入 Coil `MemoryCache`，触发单次节点重绑即可在 `onAttach` 阶段通过 `CoroutineStart.UNDISPATCHED`
 * 同步直取 `MEMORY_CACHE` 位图并绑定到新节点，同帧完成绘制且无二次交叉淡入闪烁。
 */
internal fun shouldCommitMemoryCacheRebind(
    dataSource: DataSource,
    memoryCacheCommitCount: Int,
): Boolean = memoryCacheCommitCount == 0 && dataSource != DataSource.MEMORY_CACHE

/**
 * 把缺失内容画家的加载状态替换为 [placeholder] 占位画家。
 *
 * 仅当 [placeholder] 已具备明确的内在尺寸（`intrinsicSize != Size.Unspecified`）时才允许注入：
 * 若将尚未解码完成的 `AsyncImagePainter`（其 `intrinsicSize` 为 `Size.Unspecified`）注入 `Loading`，
 * `CrossfadePainter.computeIntrinsicSize(start, end)` 在 `start != null && start.intrinsicSize == Unspecified`
 * 时会永久返回 `Size.Unspecified`，导致多图详情页在移除预设宽高比后高度坍缩为 0。
 */
internal fun substitutePixivImagePlaceholder(
    state: AsyncImagePainter.State,
    placeholder: Painter?,
): AsyncImagePainter.State {
    if (placeholder == null || placeholder.intrinsicSize == Size.Unspecified || state.painter != null) {
        return state
    }
    return when (state) {
        is AsyncImagePainter.State.Error -> state.copy(painter = placeholder)
        is AsyncImagePainter.State.Loading -> state.copy(painter = placeholder)
        is AsyncImagePainter.State.Success -> state
        is AsyncImagePainter.State.Empty -> AsyncImagePainter.State.Loading(placeholder)
    }
}

/**
 * 自动附加 Pixiv 图片必需 Referer 的 AsyncImage 包装组件。
 *
 * i.pximg.net 要求请求头 `Referer: https://app-api.pixiv.net/`，否则返回 403。
 * 同时根据用户设置的图片源（如 i.pixiv.re）自动进行 Host 替换。
 *
 * 支持通过 [thumbnailUrl] 提供渐进式缩略图占位：
 * 缩略图占位画家通过 transform 注入 Loading/Error 中间状态，
 * 在高清/原图尚未下载完成时优先命中列表页内存缓存，避免灰底等待；
 * 同时使用单一 [AsyncImage] 节点直接承载外部 [modifier]，避免在 LazyColumn 中因双层 Box 测量导致高度坍缩或 ConstraintsSizeResolver 死锁。
 * 调用方未接管 onError 时对真实错误静默重试一次（取消类错误不重试）。
 *
 * 不要在这里加「请求停滞看门狗」：网络层已用 30s 请求超时 + 2 次重试保证终态，任何早于该契约
 * 的强拆都会把下载中的健康请求连已下载字节一起丢弃，反而造成详情页永久灰底（见 CHANGELOG 批次 4）。
 *
 * 支持通过 [loadOriginalSize] 强制按图片真实原始分辨率解码，防止大图查看器在缩放时因下采样模糊失真。
 */
@OptIn(coil3.annotation.ExperimentalCoilApi::class)
@Composable
fun PixivAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    thumbnailUrl: Any? = null,
    loadOriginalSize: Boolean = false,
    filterQuality: FilterQuality = DrawScope.DefaultFilterQuality,
    onLoading: (() -> Unit)? = null,
    onSuccess: (() -> Unit)? = null,
    onError: ((Throwable?) -> Unit)? = null,
) {
    val context = LocalPlatformContext.current
    val settings = LocalSettingsRepository.current
    val sharedBoundsRegistry = LocalSharedBoundsRegistry.current

    val transformedModel = remember(model, settings?.pictureSource, settings?.changeVersion) {
        (model as? String)?.mapToPictureSource(settings?.pictureSource) ?: model
    }

    val transformedThumbnailCacheKey = remember(thumbnailUrl, settings?.pictureSource, settings?.changeVersion) {
        (thumbnailUrl as? String)?.mapToPictureSource(settings?.pictureSource) ?: thumbnailUrl
    }

    val hasThumbnail = transformedThumbnailCacheKey != null &&
        transformedThumbnailCacheKey != transformedModel &&
        transformedThumbnailCacheKey.toString().isNotBlank()

    val decodeDimension = if (loadOriginalSize) {
        AppConstants.Network.IMAGE_MAX_DECODE_DIMENSION
    } else {
        AppConstants.Network.IMAGE_STANDARD_DECODE_DIMENSION
    }

    // 静默重试预算：真实错误时递增，计数变化用 key() 强制销毁重建 AsyncImage 节点重启加载。
    var silentRetryCount by remember(transformedModel, loadOriginalSize) { mutableIntStateOf(0) }
    // 异步加载完成后的 MEMORY_CACHE 同步重绑计数：仅对首次 DISK/NETWORK 异步结果在转场稳定后递增一次。
    var memoryCacheCommitCount by remember(transformedModel, loadOriginalSize) { mutableIntStateOf(0) }
    var pendingMemoryCacheRebind by remember(transformedModel, loadOriginalSize) { mutableStateOf(false) }
    val isTransitionActive = sharedBoundsRegistry.activeTransitionIllustId != null

    LaunchedEffect(pendingMemoryCacheRebind, isTransitionActive) {
        if (pendingMemoryCacheRebind && !isTransitionActive) {
            pendingMemoryCacheRebind = false
            memoryCacheCommitCount++
        }
    }

    val mainRequest = remember<ImageRequest>(
        transformedModel,
        transformedThumbnailCacheKey,
        context,
        loadOriginalSize,
        silentRetryCount,
    ) {
        val isLocalFile = transformedModel is String && transformedModel.startsWith("file:")
        val isPixivision = transformedModel is String && (transformedModel.contains("pixivision") || transformedModel.contains("embed.pixiv.net"))
        val headers = if (isPixivision) PixivisionHeaders else StandardHeaders
        ImageRequest.Builder(context)
            .data(transformedModel)
            .apply {
                if (!isLocalFile) {
                    httpHeaders(headers)
                    val modelStr = transformedModel?.toString()
                    if (!modelStr.isNullOrBlank()) {
                        memoryCacheKey(
                            if (loadOriginalSize) "$modelStr$ORIGINAL_SIZE_CACHE_KEY_SUFFIX" else modelStr,
                        )
                        diskCacheKey(modelStr)
                    }
                }
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .size(Dimension(decodeDimension), Dimension(decodeDimension))
            .precision(Precision.INEXACT)
            .apply {
                val thumbKey = transformedThumbnailCacheKey?.toString()
                if (!thumbKey.isNullOrBlank() && thumbKey != transformedModel?.toString()) {
                    placeholderMemoryCacheKey(thumbKey)
                }
            }
            .configurePlatformOptimizations()
            .preferEndFirstIntrinsicSize(true)
            .crossfade(200)
            .build()
    }

    val thumbnailRequest = remember(transformedThumbnailCacheKey, context, hasThumbnail) {
        if (!hasThumbnail) {
            null
        } else {
            val isLocalFile = transformedThumbnailCacheKey is String && transformedThumbnailCacheKey.startsWith("file:")
            val isPixivision = transformedThumbnailCacheKey is String && (transformedThumbnailCacheKey.contains("pixivision") || transformedThumbnailCacheKey.contains("embed.pixiv.net"))
            val headers = if (isPixivision) PixivisionHeaders else StandardHeaders
            ImageRequest.Builder(context)
                .data(transformedThumbnailCacheKey)
                .apply {
                    if (!isLocalFile) {
                        httpHeaders(headers)
                        val thumbStr = transformedThumbnailCacheKey.toString()
                        if (thumbStr.isNotBlank()) {
                            memoryCacheKey(thumbStr)
                            diskCacheKey(thumbStr)
                        }
                    }
                }
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .networkCachePolicy(CachePolicy.ENABLED)
                .size(Dimension(AppConstants.Network.IMAGE_STANDARD_DECODE_DIMENSION), Dimension(AppConstants.Network.IMAGE_STANDARD_DECODE_DIMENSION))
                .precision(Precision.INEXACT)
                .configurePlatformOptimizations()
                .build()
        }
    }

    val thumbnailPainter = if (thumbnailRequest != null) {
        rememberAsyncImagePainter(
            model = thumbnailRequest,
            contentScale = contentScale,
            filterQuality = filterQuality,
        )
    } else {
        null
    }
    val thumbnailState = thumbnailPainter?.state?.collectAsState()
    val resolvedThumbnailPainter = thumbnailState?.value?.painter

    // 静默重试与异步加载后的 MEMORY_CACHE 同步重绑均通过 key() 驱动：
    // 重绑时位图已存于 MemoryCache，新节点在 onAttach 阶段通过 CoroutineStart.UNDISPATCHED 同步完成绑定，
    // 在同帧首次 draw() 前即就绪，无闪烁、无重复网络请求。
    key(silentRetryCount, memoryCacheCommitCount) {
        AsyncImage(
            model = mainRequest,
            contentDescription = contentDescription,
            modifier = modifier,
            transform = { state ->
                substitutePixivImagePlaceholder(state, resolvedThumbnailPainter)
            },
            onState = { state ->
                when (state) {
                    is AsyncImagePainter.State.Loading -> onLoading?.invoke()
                    is AsyncImagePainter.State.Success -> {
                        if (shouldCommitMemoryCacheRebind(state.result.dataSource, memoryCacheCommitCount)) {
                            if (isTransitionActive) {
                                pendingMemoryCacheRebind = true
                            } else {
                                memoryCacheCommitCount++
                            }
                        }
                        onSuccess?.invoke()
                    }
                    is AsyncImagePainter.State.Error -> {
                        val throwable = state.result.throwable
                        val decision = resolveSilentErrorDecision(throwable, onError, silentRetryCount)
                        // 取消不是失败：节点回收与去重让位属常态，记错误日志只会淹没真实故障。
                        if (decision != SilentErrorDecision.Ignore && transformedModel != null) {
                            Napier.e("PixivAsyncImage error for $transformedModel: $throwable", tag = "CoilImage")
                        }
                        when (decision) {
                            // 调用方未接管错误回调时静默重试一次，瞬时抖动无需用户感知。
                            SilentErrorDecision.Retry -> silentRetryCount++
                            is SilentErrorDecision.Report -> onError?.invoke(decision.throwable)
                            SilentErrorDecision.Ignore -> Unit
                        }
                    }
                    is AsyncImagePainter.State.Empty -> Unit
                }
            },
            contentScale = contentScale,
            filterQuality = filterQuality,
        )
    }
}
