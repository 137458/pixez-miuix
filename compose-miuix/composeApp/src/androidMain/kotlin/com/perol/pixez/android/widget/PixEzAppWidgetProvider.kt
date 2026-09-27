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
import com.perol.pixez.shared.network.TrustedUrlPolicy
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
                val (settings, widgetRepository) = obtainWidgetDependencies(context.applicationContext)
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

                    val bitmap = downloadBitmapWithFallback(transformedUrl, rawPreviewUrl, effectivePictureSource)
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

    private fun downloadBitmapWithFallback(
        primaryUrl: String,
        fallbackUrl: String,
        mirrorHost: String,
    ): Bitmap? {
        val primary = downloadBitmap(primaryUrl, mirrorHost)
        if (primary != null) return primary
        return if (primaryUrl != fallbackUrl) downloadBitmap(fallbackUrl, mirrorHost) else null
    }

    /**
     * 下载小组件图片，禁用自动重定向并逐跳校验 host。
     *
     * 图源 host 可来自用户在设置里填写的镜像值，[HttpURLConnection] 的自动重定向会让
     * 该 host 通过 3xx 把请求带到任意站点，因此每一跳都先过 [TrustedUrlPolicy.widgetImageUrl]
     * （内置 pximg 白名单 ∪ 用户镜像 host）再发起下一次请求，跳数上限 [MAX_REDIRECT_HOPS]。
     */
    private fun downloadBitmap(imageUrl: String, mirrorHost: String): Bitmap? {
        var nextUrl = imageUrl
        var hop = 0
        while (true) {
            val trustedUrl = runCatching { TrustedUrlPolicy.widgetImageUrl(nextUrl, mirrorHost) }
                .getOrElse {
                    Log.w("PixEzAppWidgetProvider", "Rejected widget image URL: $nextUrl", it)
                    return null
                }
            val connection = try {
                (URL(trustedUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = WIDGET_HTTP_TIMEOUT_MS
                    readTimeout = WIDGET_HTTP_TIMEOUT_MS
                    setRequestProperty("Referer", "https://app-api.pixiv.net/")
                    setRequestProperty("User-Agent", "PixivAndroidApp/5.0.234")
                    instanceFollowRedirects = false
                }
            } catch (e: Exception) {
                Log.w("PixEzAppWidgetProvider", "Failed to open widget connection: $trustedUrl", e)
                return null
            }
            try {
                val status = runCatching { connection.responseCode }.getOrDefault(HTTP_STATUS_UNAVAILABLE)
                if (status in HTTP_STATUS_REDIRECT_RANGE) {
                    if (hop >= MAX_REDIRECT_HOPS) {
                        Log.w("PixEzAppWidgetProvider", "Widget image redirect limit reached: $trustedUrl")
                        return null
                    }
                    val location = connection.getHeaderField("Location")
                    if (location.isNullOrBlank()) return null
                    nextUrl = URL(URL(trustedUrl), location).toString()
                    hop++
                    continue
                }
                if (status != HttpURLConnection.HTTP_OK) {
                    Log.w("PixEzAppWidgetProvider", "Widget image HTTP $status for $trustedUrl")
                    return null
                }
                return connection.inputStream.use { decodeWidgetBitmap(it) }
            } catch (e: Exception) {
                Log.w("PixEzAppWidgetProvider", "Failed to download widget bitmap: $trustedUrl", e)
                return null
            } finally {
                // 释放连接与 keep-alive socket，覆盖重定向续跳与解码失败提前返回路径。
                connection.disconnect()
            }
        }
    }

    private fun decodeWidgetBitmap(input: java.io.InputStream): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val originalBitmap = BitmapFactory.decodeStream(input, null, options) ?: return null
        if (originalBitmap.width <= WIDGET_IMAGE_MAX_DIMENSION && originalBitmap.height <= WIDGET_IMAGE_MAX_DIMENSION) {
            return originalBitmap
        }
        val scale = WIDGET_IMAGE_MAX_DIMENSION.toFloat() / maxOf(originalBitmap.width, originalBitmap.height)
        val newWidth = (originalBitmap.width * scale).toInt().coerceAtLeast(1)
        val newHeight = (originalBitmap.height * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
        if (scaled != originalBitmap) {
            originalBitmap.recycle()
        }
        return scaled
    }

    companion object {
        /** 小组件图片下载超时（毫秒），广播刷新需快速返回，不宜用全局默认。 */
        private const val WIDGET_HTTP_TIMEOUT_MS = 10_000

        /** 逐跳 host 校验允许的重定向跳数上限。 */
        private const val MAX_REDIRECT_HOPS = 5

        /** 重定向状态码区间（300-399），禁用自动重定向后手动跟进。 */
        private val HTTP_STATUS_REDIRECT_RANGE = 300..399

        /** 读不到响应码时的哨兵值，避免与合法状态码混淆。 */
        private const val HTTP_STATUS_UNAVAILABLE = -1

        /** 小组件位图边长上限（px），RemoteViews Binder 事务超限会崩溃。 */
        private const val WIDGET_IMAGE_MAX_DIMENSION = 800

        const val ACTION_REFRESH_WIDGET = "com.perol.pixez.action.REFRESH_WIDGET"

        /** 进程级常驻的小部件依赖（设置仓库 + 缓存仓库）。 */
        private data class WidgetDependencies(
            val settingsRepository: SettingsRepository,
            val widgetRepository: WidgetRepository,
        )

        @Volatile
        private var widgetDependencies: WidgetDependencies? = null
        private val dependenciesMutex = Any()

        /**
         * 首次广播懒创建并常驻进程的依赖单例（P-11）：
         * AppWidgetProvider 实例每次广播都会重建，旧实现随之每次重建 DB 驱动工厂与设置仓库；
         * 单例化后数据库驱动只创建一次，随进程退出由系统回收。
         */
        private fun obtainWidgetDependencies(applicationContext: Context): WidgetDependencies {
            widgetDependencies?.let { return it }
            synchronized(dependenciesMutex) {
                widgetDependencies?.let { return it }
                val settingsRepository = SettingsRepository(SettingsFactory(applicationContext).createSettings())
                val dependencies = WidgetDependencies(
                    settingsRepository = settingsRepository,
                    widgetRepository = WidgetRepository(DriverFactory(applicationContext), settingsRepository),
                )
                widgetDependencies = dependencies
                return dependencies
            }
        }
    }
}
