package com.perol.pixez.shared.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.perol.pixez.shared.data.settings.LocalSettingsRepository

private val StandardHeaders = NetworkHeaders.Builder()
    .set("Referer", "https://app-api.pixiv.net/")
    .set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
    .build()

private val PixivisionHeaders = NetworkHeaders.Builder()
    .set("Referer", "https://www.pixivision.net/")
    .set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
    .build()

/**
 * 自动附加 Pixiv 图片必需 Referer 的 AsyncImage 包装组件。
 *
 * i.pximg.net 要求请求头 `Referer: https://app-api.pixiv.net/`，否则返回 403。
 * 同时根据用户设置的图片源（如 i.pixiv.re）自动进行 Host 替换。
 *
 * 支持通过 [thumbnailUrl] 提供渐进式缩略图占位：在高清/原图尚未下载完成时，优先展示已缓存的缩略图，避免白屏/黑屏等待。
 */
@Composable
fun PixivAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    thumbnailUrl: Any? = null,
) {
    if (thumbnailUrl != null && thumbnailUrl != model && (thumbnailUrl as? String)?.isNotBlank() == true) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            // 底层缩略图：未加载高清图前垫底展示，始终与容器尺寸匹配，不抢占主测量锚点
            PixivAsyncImageInternal(
                model = thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = contentScale,
            )
            // 顶层高清图：撑开容器尺寸，并接入缩略图内存缓存占位，加载成功后平滑覆盖
            PixivAsyncImageInternal(
                model = model,
                thumbnailCacheKey = thumbnailUrl,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxWidth(),
                contentScale = contentScale,
            )
        }
    } else {
        PixivAsyncImageInternal(
            model = model,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
        )
    }
}

@Composable
private fun PixivAsyncImageInternal(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    thumbnailCacheKey: Any? = null,
    onSuccess: (() -> Unit)? = null,
) {
    val context = LocalPlatformContext.current
    val settings = LocalSettingsRepository.current

    val transformedModel = remember(model, settings?.pictureSource, settings?.changeVersion) {
        val pictureSource = settings?.pictureSource
        if (model is String && !pictureSource.isNullOrBlank() && pictureSource != "i.pximg.net") {
            model.replace("://i.pximg.net", "://$pictureSource")
        } else {
            model
        }
    }

    val transformedThumbnailCacheKey = remember(thumbnailCacheKey, settings?.pictureSource, settings?.changeVersion) {
        val pictureSource = settings?.pictureSource
        if (thumbnailCacheKey is String && !pictureSource.isNullOrBlank() && pictureSource != "i.pximg.net") {
            thumbnailCacheKey.replace("://i.pximg.net", "://$pictureSource")
        } else {
            thumbnailCacheKey
        }
    }

    val request = remember(transformedModel, transformedThumbnailCacheKey, context) {
        val isPixivision = transformedModel is String && (transformedModel.contains("pixivision") || transformedModel.contains("embed.pixiv.net"))
        val headers = if (isPixivision) PixivisionHeaders else StandardHeaders
        ImageRequest.Builder(context)
            .data(transformedModel)
            .httpHeaders(headers)
            .memoryCacheKey(transformedModel?.toString())
            .apply {
                val thumbKey = transformedThumbnailCacheKey?.toString()
                if (!thumbKey.isNullOrBlank() && thumbKey != transformedModel?.toString()) {
                    placeholderMemoryCacheKey(thumbKey)
                }
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
        onSuccess = { onSuccess?.invoke() },
        onError = { state ->
            io.github.aakira.napier.Napier.e("PixivAsyncImage error for $transformedModel: ${state.result.throwable}", tag = "CoilImage")
        },
    )
}
