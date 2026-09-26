package com.perol.pixez.shared.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.perol.pixez.shared.data.settings.LocalSettingsRepository
import com.perol.pixez.shared.platform.configurePlatformOptimizations
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException

import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import coil3.size.Dimension
import coil3.size.Precision
import coil3.size.Size
import com.perol.pixez.shared.ui.AppConstants

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

/**
 * 自动附加 Pixiv 图片必需 Referer 的 AsyncImage 包装组件。
 *
 * i.pximg.net 要求请求头 `Referer: https://app-api.pixiv.net/`，否则返回 403。
 * 同时根据用户设置的图片源（如 i.pixiv.re）自动进行 Host 替换。
 *
 * 支持通过 [thumbnailUrl] 提供真正的两阶段渐进式缩略图占位：
 * 在高清/原图尚未下载完成时，优先并发拉取/显示缩略图，避免白屏/黑屏等待；
 * 当高清原图加载成功后，平滑覆盖缩略图；即便高清图加载失败，缩略图仍稳定可见。
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
                        // 原图尺寸请求（全屏查看器）使用独立内存缓存键：Coil 对 Precision.INEXACT
                        // 请求不校验缓存位图尺寸，共用普通键会命中列表/详情页的小尺寸解码结果，
                        // 导致放大后画面模糊；独立键同时避免高清位图被列表项复用。
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
            .apply {
                if (loadOriginalSize) {
                    // 约束最大解码边长不超过 4096px，既保证 4K 高清画质体验，又防止极端长条/巨幅画作瞬间撑爆堆内存导致 OOM
                    size(Dimension(AppConstants.Network.IMAGE_MAX_DECODE_DIMENSION), Dimension(AppConstants.Network.IMAGE_MAX_DECODE_DIMENSION))
                    precision(Precision.INEXACT)
                }
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
        if (!hasThumbnail) null
        else {
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
                .configurePlatformOptimizations()
                .crossfade(150)
                .build()
        }
    }

    if (!hasThumbnail || thumbnailRequest == null) {
        AsyncImage(
            model = mainRequest,
            contentDescription = contentDescription,
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
    } else {
        Box(modifier = modifier) {
            // 底层：缩略图渐进式占位图层（快速异步加载显示，目标图成功后保持静止或由上层覆盖）
            AsyncImage(
                model = thumbnailRequest,
                contentDescription = null,
                contentScale = contentScale,
                filterQuality = filterQuality,
                modifier = Modifier.matchParentSize(),
            )

            // 顶层：目标高清/原图图层
            AsyncImage(
                model = mainRequest,
                contentDescription = contentDescription,
                contentScale = contentScale,
                filterQuality = filterQuality,
                modifier = Modifier.fillMaxSize(),
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
    }
}
