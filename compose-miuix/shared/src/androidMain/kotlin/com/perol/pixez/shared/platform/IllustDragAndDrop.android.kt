package com.perol.pixez.shared.platform

import android.content.ClipData
import android.content.ClipDescription
import android.net.Uri
import android.os.Build
import android.view.View
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.core.content.FileProvider
import coil3.SingletonImageLoader
import com.perol.pixez.shared.data.model.Illust
import java.io.File

@Composable
actual fun Modifier.illustDragAndDropSource(illust: Illust): Modifier {
    val view = LocalView.current
    val context = view.context.applicationContext
    val hapticFeedback = LocalHapticFeedback.current

    return this.pointerInput(illust.id) {
        detectDragGesturesAfterLongPress(
            onDragStart = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                val url = "https://www.pixiv.net/artworks/${illust.id}"

                // 准备跨应用拖拽图片文件：优先从 Coil 缓存中提取已加载的图片文件
                val dragDir = File(context.cacheDir, "drag_shares").apply { mkdirs() }
                val imageFile = File(dragDir, "${illust.id}.jpg")

                try {
                    val imageLoader = SingletonImageLoader.get(context)
                    val diskCache = imageLoader.diskCache
                    val imageUrls = listOfNotNull(
                        illust.imageUrls.large,
                        illust.imageUrls.medium,
                        illust.imageUrls.squareMedium,
                    )
                    var foundCache = false
                    for (candidateUrl in imageUrls) {
                        diskCache?.openSnapshot(candidateUrl)?.use { snapshot ->
                            val sourcePath = snapshot.data.toFile()
                            if (sourcePath.exists() && sourcePath.length() > 0) {
                                sourcePath.copyTo(imageFile, overwrite = true)
                                foundCache = true
                            }
                        }
                        if (foundCache) break
                    }
                    if (!imageFile.exists()) {
                        imageFile.createNewFile()
                    }
                } catch (_: Exception) {
                    if (!imageFile.exists()) {
                        imageFile.createNewFile()
                    }
                }

                val contentUri: Uri = try {
                    FileProvider.getUriForFile(
                        context,
                        "com.perol.pixez.miuix.fileprovider",
                        imageFile,
                    )
                } catch (_: Exception) {
                    Uri.parse(url)
                }

                val clipDescription = ClipDescription(
                    illust.title,
                    arrayOf("image/jpeg", "image/*", ClipDescription.MIMETYPE_TEXT_PLAIN, ClipDescription.MIMETYPE_TEXT_URILIST),
                )
                val clipData = ClipData(clipDescription, ClipData.Item(contentUri)).apply {
                    addItem(ClipData.Item(illust.title))
                    addItem(ClipData.Item(Uri.parse(url)))
                }

                val shadow = View.DragShadowBuilder(view)
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    View.DRAG_FLAG_GLOBAL or View.DRAG_FLAG_GLOBAL_URI_READ
                } else {
                    0
                }
                view.startDragAndDrop(clipData, shadow, null, flags)
            },
            onDrag = { _, _ -> },
        )
    }
}
