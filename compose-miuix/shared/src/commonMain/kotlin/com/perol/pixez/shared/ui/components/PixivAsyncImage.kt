package com.perol.pixez.shared.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
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
 * 支持通过 [thumbnailUrl] 提供渐进式无缝占位：
 * 首帧立即可见已缓存缩略图，后台静默预加载高清图，加载成功后平滑切换，网络异常时持续保留缩略图，杜绝任何白屏、灰色占位与闪烁。
 */
@Composable
fun PixivAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    thumbnailUrl: Any? = null,
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

    val transformedThumbnailUrl = remember(thumbnailUrl, settings?.pictureSource, settings?.changeVersion) {
        val pictureSource = settings?.pictureSource
        if (thumbnailUrl is String && !pictureSource.isNullOrBlank() && pictureSource != "i.pximg.net") {
            thumbnailUrl.replace("://i.pximg.net", "://$pictureSource")
        } else {
            thumbnailUrl
        }
    }

    val hasThumbnail = transformedThumbnailUrl != null &&
        transformedThumbnailUrl != transformedModel &&
        (transformedThumbnailUrl as? String)?.isNotBlank() == true

    // 如果提供了缩略图，初始直接以缩略图作为展示模型，实现 0ms 瞬显
    var currentModel by remember(transformedModel, transformedThumbnailUrl) {
        mutableStateOf(if (hasThumbnail) transformedThumbnailUrl else transformedModel)
    }

    // 后台静默预加载目标高清图；若成功则无缝升级，若失败则始终保底保留缩略图展示
    LaunchedEffect(transformedModel, transformedThumbnailUrl) {
        if (hasThumbnail) {
            val isPixivision = transformedModel is String && (transformedModel.contains("pixivision") || transformedModel.contains("embed.pixiv.net"))
            val headers = if (isPixivision) PixivisionHeaders else StandardHeaders
            val highResRequest = ImageRequest.Builder(context)
                .data(transformedModel)
                .httpHeaders(headers)
                .memoryCacheKey(transformedModel?.toString())
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build()

            val imageLoader = SingletonImageLoader.get(context)
            val result = imageLoader.execute(highResRequest)
            if (result is SuccessResult) {
                currentModel = transformedModel
                onSuccess?.invoke()
            }
        } else {
            currentModel = transformedModel
        }
    }

    val request = remember(currentModel, context) {
        val modelObj = currentModel
        val isPixivision = modelObj is String && (modelObj.contains("pixivision") || modelObj.contains("embed.pixiv.net"))
        val headers = if (isPixivision) PixivisionHeaders else StandardHeaders
        ImageRequest.Builder(context)
            .data(modelObj)
            .httpHeaders(headers)
            .memoryCacheKey(modelObj?.toString())
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
        onSuccess = {
            if (!hasThumbnail || currentModel == transformedModel) {
                onSuccess?.invoke()
            }
        },
        onError = { state ->
            io.github.aakira.napier.Napier.e("PixivAsyncImage load error for $currentModel: ${state.result.throwable}", tag = "CoilImage")
        },
    )
}
