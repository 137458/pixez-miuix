package com.perol.pixez.shared.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
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
 * 自动附加 Pixiv 图片必需 Referer 的 AsyncImage 包装组件。
 *
 * i.pximg.net 要求请求头 `Referer: https://app-api.pixiv.net/`，否则返回 403。
 * 同时根据用户设置的图片源（如 i.pixiv.re）自动进行 Host 替换。
 *
 * 支持通过 [thumbnailUrl] 提供渐进式缩略图占位：
 * 在高清/原图尚未下载完成时，优先命中内存缓存或并发加载缩略图 Painter 占位，避免白屏等待；
 * 同时使用单一 [AsyncImage] 节点直接承载外部 [modifier]，避免在 LazyColumn 中因双层 Box 测量导致高度坍缩或 ConstraintsSizeResolver 死锁。
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

    val mainRequest = remember<ImageRequest>(transformedModel, transformedThumbnailCacheKey, context, loadOriginalSize) {
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

    AsyncImage(
        model = mainRequest,
        contentDescription = contentDescription,
        placeholder = thumbnailPainter,
        error = thumbnailPainter,
        contentScale = contentScale,
        filterQuality = filterQuality,
        modifier = modifier,
        onLoading = { onLoading?.invoke() },
        onSuccess = { onSuccess?.invoke() },
        onError = { state ->
            val throwable = state.result.throwable
            if (throwable !is CancellationException) {
                if (transformedModel != null) {
                    Napier.e("PixivAsyncImage error for $transformedModel: $throwable", tag = "CoilImage")
                }
                onError?.invoke(throwable)
            }
        },
    )
}
