package com.perol.pixez.android.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.RemoteViews
import com.perol.pixez.R
import com.perol.pixez.android.MainActivity
import com.perol.pixez.shared.data.local.DriverFactory
import com.perol.pixez.shared.data.repository.WidgetRepository
import com.perol.pixez.shared.data.settings.SettingsFactory
import com.perol.pixez.shared.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

/**
 * PixEz 桌面小部件 Provider。
 *
 * 核心功能：
 * 1. 支持自动从本地数据库缓存或网络按需获取最新插画，彻底解决小部件初始黑屏无内容的问题。
 * 2. 支持独立图源 CDN（如 i.pixiv.re 镜像与官方原站）以及独立的内容推荐类型（日榜/周榜/月榜/推荐/最新/关注等）。
 * 3. 自动缩放位图至安全尺寸，避免 RemoteViews Binder 事务超限崩溃。
 */
class PixEzAppWidgetProvider : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE ||
            intent.action == ACTION_REFRESH_WIDGET
        ) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, PixEzAppWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(component)
            if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                for (id in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, id)
                }
            }
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        scope.launch {
            try {
                val settingsFactory = SettingsFactory(context)
                val settings = SettingsRepository(settingsFactory.createSettings())
                val driverFactory = DriverFactory(context)
                val widgetRepository = WidgetRepository(driverFactory, settings)

                val targetType = settings.widgetIllustType.ifBlank { "recom" }
                val cached = widgetRepository.getOrFetchWidgetIllust(targetType)

                val illustId = cached?.illust_id?.toInt() ?: 0
                val title = cached?.title ?: "PixEz"
                val author = cached?.user_name ?: "Pixiv"
                val rawPreviewUrl = cached?.large_url?.ifBlank { cached.picture_url } ?: cached?.picture_url.orEmpty()

                val views = RemoteViews(context.packageName, R.layout.widget_illust)
                views.setTextViewText(R.id.widget_title, title)
                views.setTextViewText(R.id.widget_author, author)

                // 点击跳转 Intent
                val intent = Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    if (illustId > 0) {
                        data = Uri.parse("pixez://illust/$illustId")
                    }
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    appWidgetId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

                // 计算实际图源 CDN 并下载 Bitmap
                if (rawPreviewUrl.isNotBlank()) {
                    val effectivePictureSource = settings.widgetPictureSource.ifBlank { settings.pictureSource }
                    val transformedUrl = if (effectivePictureSource.isNotBlank() && effectivePictureSource != "i.pximg.net") {
                        rawPreviewUrl.replace("://i.pximg.net", "://$effectivePictureSource")
                    } else {
                        rawPreviewUrl
                    }

                    val bitmap = downloadBitmapWithFallback(transformedUrl, rawPreviewUrl)
                    if (bitmap != null) {
                        views.setImageViewBitmap(R.id.widget_image, bitmap)
                    }
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                Log.e("PixEzAppWidgetProvider", "Failed to update app widget $appWidgetId", e)
            }
        }
    }

    private fun downloadBitmapWithFallback(primaryUrl: String, fallbackUrl: String): Bitmap? {
        val primary = downloadBitmap(primaryUrl)
        if (primary != null) return primary
        return if (primaryUrl != fallbackUrl) downloadBitmap(fallbackUrl) else null
    }

    private fun downloadBitmap(imageUrl: String): Bitmap? {
        return try {
            val url = URL(imageUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Referer", "https://app-api.pixiv.net/")
                setRequestProperty("User-Agent", "PixivAndroidApp/5.0.234")
                instanceFollowRedirects = true
            }
            connection.inputStream.use { input ->
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                val originalBitmap = BitmapFactory.decodeStream(input, null, options) ?: return null
                val maxDimension = 800
                if (originalBitmap.width > maxDimension || originalBitmap.height > maxDimension) {
                    val scale = maxDimension.toFloat() / maxOf(originalBitmap.width, originalBitmap.height)
                    val newWidth = (originalBitmap.width * scale).toInt().coerceAtLeast(1)
                    val newHeight = (originalBitmap.height * scale).toInt().coerceAtLeast(1)
                    val scaled = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
                    if (scaled != originalBitmap) {
                        originalBitmap.recycle()
                    }
                    scaled
                } else {
                    originalBitmap
                }
            }
        } catch (e: Exception) {
            Log.w("PixEzAppWidgetProvider", "Failed to download widget bitmap: $imageUrl", e)
            null
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.perol.pixez.action.REFRESH_WIDGET"
    }
}
