package com.perol.pixez.shared.platform

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.chooser.ChooserAction

/**
 * Android 平台实现：使用系统 `Intent.ACTION_SEND` 启动分享选择器。
 *
 * 在 Android 14+ (API 34) 上接入系统 Sharesheet 自定义快捷操作 (ChooserAction)。
 */
actual class IllustShare {
    actual fun share(text: String, subject: String?) {
        val context = BrowserLauncherContext.applicationContext
            ?: throw IllegalStateException("BrowserLauncherContext 未初始化，无法获取 Context")

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val chooser = Intent.createChooser(sendIntent, subject).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // Android 14+ (API 34) 系统级分享面板自定义快捷操作 (Custom Chooser Actions)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val copyIntent = Intent(context, ShareActionReceiver::class.java).apply {
                action = ShareActionReceiver.ACTION_COPY_TEXT
                putExtra(ShareActionReceiver.EXTRA_TEXT, text)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                text.hashCode(),
                copyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val copyAction = ChooserAction.Builder(
                Icon.createWithResource(context, android.R.drawable.ic_menu_save),
                "复制",
                pendingIntent,
            ).build()

            chooser.putExtra(Intent.EXTRA_CHOOSER_CUSTOM_ACTIONS, arrayOf(copyAction))
        }

        context.startActivity(chooser)
    }
}
