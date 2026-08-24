package com.perol.pixez.shared.platform

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * 接收 Android 14+ Sharesheet 自定义动作点击的广播接收器。
 */
class ShareActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_COPY_TEXT) {
            val text = intent.getStringExtra(EXTRA_TEXT) ?: return
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("PixEz", text))
            Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val ACTION_COPY_TEXT = "com.perol.pixez.action.COPY_TEXT"
        const val EXTRA_TEXT = "extra_copy_text"
    }
}
