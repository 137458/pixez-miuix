package com.perol.pixez.android.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
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
import com.perol.pixez.shared.data.local.glanceillustpersist.GlanceIllustPersistDatabase
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
 * 根据用户的推荐类型偏好展示 Pixiv 插画推荐或日榜作品，点击可直接拉起主界面并定位至插画详情。
 */
class PixEzAppWidgetProvider : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        scope.launch {
            try {
                val settingsFactory = SettingsFactory(context)
                val settings = SettingsRepository(settingsFactory.createSettings())
                val driverFactory = DriverFactory(context)
                val driver = driverFactory.createDriver(GlanceIllustPersistDatabase.Schema, "glance_illust_persist.db")
                val db = GlanceIllustPersistDatabase(driver)

                val targetType = settings.widgetIllustType.ifBlank { "recom" }
                val cachedList = try {
                    db.glanceIllustPersistQueries.selectByType(targetType).executeAsList()
                } catch (e: Exception) {
                    emptyList()
                }

                val cached = cachedList.firstOrNull()
                val illustId = cached?.illust_id?.toInt() ?: 0
                val title = cached?.title ?: "PixEz Daily"
                val author = cached?.user_name ?: "Pixiv"
                val previewUrl = cached?.picture_url ?: cached?.large_url.orEmpty()

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

                // 异步下载并绑定缩略图
                if (previewUrl.isNotBlank()) {
                    val bitmap = downloadBitmap(previewUrl)
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

    private fun downloadBitmap(imageUrl: String): Bitmap? {
        return try {
            val url = URL(imageUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Referer", "https://app-api.pixiv.net/")
                setRequestProperty("User-Agent", "PixivAndroidApp/5.0.234")
            }
            connection.inputStream.use { input ->
                BitmapFactory.decodeStream(input)
            }
        } catch (e: Exception) {
            Log.w("PixEzAppWidgetProvider", "Failed to download widget bitmap: $imageUrl", e)
            null
        }
    }
}
