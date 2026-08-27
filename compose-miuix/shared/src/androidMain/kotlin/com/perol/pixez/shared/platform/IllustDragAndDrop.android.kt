package com.perol.pixez.shared.platform

import android.content.ClipData
import android.net.Uri
import android.os.Build
import android.view.View
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import coil3.SingletonImageLoader
import com.perol.pixez.shared.data.model.Illust
import java.io.File

@Composable
actual fun Modifier.illustDragAndDropSource(illust: Illust): Modifier {
    val context = LocalContext.current.applicationContext

    return this.dragAndDropSource(
        transferData = {
            val url = "https://www.pixiv.net/artworks/${illust.id}"
            val dragDir = File(context.cacheDir, "drag_shares").apply { mkdirs() }
            val imageFile = File(dragDir, "${illust.id}.jpg")

            var hasValidImage = false
            try {
                if (imageFile.exists() && imageFile.length() > 0) {
                    hasValidImage = true
                } else {
                    val imageLoader = SingletonImageLoader.get(context)
                    val diskCache = imageLoader.diskCache
                    val imageUrls = listOfNotNull(
                        illust.imageUrls.large,
                        illust.imageUrls.medium,
                        illust.imageUrls.squareMedium,
                    )
                    for (candidateUrl in imageUrls) {
                        diskCache?.openSnapshot(candidateUrl)?.use { snapshot ->
                            val sourceFile = snapshot.data.toFile()
                            if (sourceFile.exists() && sourceFile.length() > 0) {
                                sourceFile.copyTo(imageFile, overwrite = true)
                                hasValidImage = true
                            }
                        }
                        if (hasValidImage) break
                    }
                }
            } catch (_: Throwable) {
                hasValidImage = false
            }

            val clipData: ClipData = if (hasValidImage && imageFile.exists() && imageFile.length() > 0) {
                try {
                    val contentUri: Uri = FileProvider.getUriForFile(
                        context,
                        "com.perol.pixez.miuix.fileprovider",
                        imageFile,
                    )
                    ClipData.newUri(context.contentResolver, illust.title, contentUri)
                } catch (_: Throwable) {
                    ClipData.newPlainText(illust.title, url)
                }
            } else {
                ClipData.newPlainText(illust.title, url)
            }

            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                View.DRAG_FLAG_GLOBAL or View.DRAG_FLAG_GLOBAL_URI_READ
            } else {
                0
            }

            DragAndDropTransferData(
                clipData = clipData,
                flags = flags,
            )
        }
    )
}

