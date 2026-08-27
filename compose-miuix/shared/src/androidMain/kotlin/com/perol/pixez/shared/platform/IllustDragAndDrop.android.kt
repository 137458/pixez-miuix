package com.perol.pixez.shared.platform

import android.content.ClipData
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
import com.perol.pixez.shared.data.model.Illust

@Composable
actual fun Modifier.illustDragAndDropSource(illust: Illust): Modifier {
    val view = LocalView.current
    val hapticFeedback = LocalHapticFeedback.current

    return this.pointerInput(illust.id) {
        detectDragGesturesAfterLongPress(
            onDragStart = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                val url = "https://www.pixiv.net/artworks/${illust.id}"
                val clipData = ClipData.newPlainText(illust.title, url).apply {
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
