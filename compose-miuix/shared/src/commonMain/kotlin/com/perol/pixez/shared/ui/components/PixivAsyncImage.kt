package com.perol.pixez.shared.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Dimension
import coil3.size.Precision
import com.perol.pixez.shared.data.settings.LocalSettingsRepository
import com.perol.pixez.shared.platform.configurePlatformOptimizations
import com.perol.pixez.shared.ui.AppConstants
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

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

private const val STANDARD_MAX_DECODE_DIMENSION = 2048

/**
 * 请求停滞看门狗超时：请求发出后超过该时长仍未收到任何引擎状态事件
 * （onStart 或加载结果），判定为底层管线挂起并重建请求重启加载。
 */
private const val REQUEST_STALL_TIMEOUT_MS = 5_000L

/** 看门狗对同一图片模型的最大重启次数。 */
private const val REQUEST_STALL_MAX_RESTARTS = 2

/** 调用方未接管错误时的静默重试次数（瞬时网络/代理抖动自愈）。 */
private const val SILENT_ERROR_MAX_RETRIES = 1

/**
 * 把缺失内容画家的加载状态替换为 [placeholder] 占位画家。
 *
 * Coil 3 的 [AsyncImagePainter.State.Loading] 由引擎 target 的 onStart 派发且请求未配置
 * 占位画家时 painter 为 null，[AsyncImagePainter.State.Error] 在请求未配置 error 画家时
 * painter 也为 null，两种状态下整个节点什么都不绘制，外部只看到容器底色的灰底。
 * 这里统一补上占位画家，保证缩略图位图（通常已命中列表页内存缓存）在加载全程可见。
 * [AsyncImagePainter.State.Empty] 是请求启动前的初始态、不会流经 transform，此处映射为
 * 防御性兜底。
 */
internal fun substitutePixivImagePlaceholder(
    state: AsyncImagePainter.State,
    placeholder: Painter?,
): AsyncImagePainter.State {
    if (placeholder == null || state.painter != null) return state
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
 * 内置停滞看门狗：请求在引擎层挂起（无任何状态事件）时通过 key 重建图片节点重启加载，
 * 修复「详情页图片加载成功前永远灰底、只有大图查看页正常」的问题。
 * 支持通过 [loadOriginalSize] 强制按图片真实原始分辨率解码，防止大图查看器在缩放时因下采样模糊失真。
 */
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

    val transformedModel = remember(model, settings?.pictureSource, settings?.changeVersion) {
        val pictureSource = settings?.pictureSource
        if (model is String && !model.startsWith("file:") && !pictureSource.isNullOrBlank() && pictureSource != "i.pximg.net") {
            model.replace("://i.pximg.net", "://$pictureSource")
        } else {
            model
        }
    }

    val transformedThumbnailCacheKey = remember(thumbnailUrl, settings?.pictureSource, settings?.changeVersion) {
        val pictureSource = settings?.pictureSource
        if (thumbnailUrl is String && !thumbnailUrl.startsWith("file:") && !pictureSource.isNullOrBlank() && pictureSource != "i.pximg.net") {
            thumbnailUrl.replace("://i.pximg.net", "://$pictureSource")
        } else {
            thumbnailUrl
        }
    }

    val hasThumbnail = transformedThumbnailCacheKey != null &&
        transformedThumbnailCacheKey != transformedModel &&
        transformedThumbnailCacheKey.toString().isNotBlank()

    val decodeDimension = if (loadOriginalSize) {
        AppConstants.Network.IMAGE_MAX_DECODE_DIMENSION
    } else {
        STANDARD_MAX_DECODE_DIMENSION
    }

    // 看门狗与静默重试计数：按图片模型标识记忆，模型变化时归零。
    var requestRestartCount by remember(transformedModel, loadOriginalSize) { mutableIntStateOf(0) }
    var silentRetryCount by remember(transformedModel, loadOriginalSize) { mutableIntStateOf(0) }
    var observedLoadEvent by remember(transformedModel, loadOriginalSize) { mutableStateOf(false) }

    val mainRequest = remember<ImageRequest>(
        transformedModel,
        transformedThumbnailCacheKey,
        context,
        loadOriginalSize,
        requestRestartCount,
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
                .size(Dimension(STANDARD_MAX_DECODE_DIMENSION), Dimension(STANDARD_MAX_DECODE_DIMENSION))
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

    // 看门狗：重启计数变化时用 key() 强制销毁重建 AsyncImage 节点，新 painter 实例必然
    // 重新走 onRemembered → launchJob 执行加载。仅重建 ImageRequest 是无效的——Coil 的
    // AsyncImageModelEqualityDelegate 按结构比较请求，等价请求会被 Input 相等去重跳过
    // restart()。用于从「底层请求被取消/挂起导致状态永远停在 Empty」中自愈。
    LaunchedEffect(mainRequest) {
        observedLoadEvent = false
        while (requestRestartCount < REQUEST_STALL_MAX_RESTARTS) {
            val progressed = withTimeoutOrNull(REQUEST_STALL_TIMEOUT_MS) {
                snapshotFlow { observedLoadEvent }.filter { it }.first()
            }
            if (progressed != null) return@LaunchedEffect
            Napier.w(
                "PixivAsyncImage request stalled without any engine event, restarting (attempt ${requestRestartCount + 1}): $transformedModel",
                tag = "CoilImage",
            )
            requestRestartCount++
        }
    }

    key(requestRestartCount) {
        AsyncImage(
            model = mainRequest,
            contentDescription = contentDescription,
            modifier = modifier,
            transform = { state ->
                substitutePixivImagePlaceholder(state, thumbnailPainter)
            },
            onState = { state ->
                observedLoadEvent = true
                when (state) {
                    is AsyncImagePainter.State.Loading -> onLoading?.invoke()
                    is AsyncImagePainter.State.Success -> onSuccess?.invoke()
                    is AsyncImagePainter.State.Error -> {
                        val throwable = state.result.throwable
                        if (throwable !is CancellationException) {
                            if (transformedModel != null) {
                                Napier.e("PixivAsyncImage error for $transformedModel: $throwable", tag = "CoilImage")
                            }
                            if (onError == null && silentRetryCount < SILENT_ERROR_MAX_RETRIES) {
                                // 调用方未接管错误回调时静默重试一次，瞬时抖动无需用户感知。
                                silentRetryCount++
                                requestRestartCount++
                            } else {
                                onError?.invoke(throwable)
                            }
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
