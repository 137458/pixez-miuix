package com.perol.pixez.android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.arkivanov.decompose.defaultComponentContext
import com.perol.pixez.PixEzApp
import com.perol.pixez.shared.AppDependencies
import com.perol.pixez.shared.data.local.DriverFactory
import com.perol.pixez.shared.data.settings.SettingsFactory
import com.perol.pixez.shared.platform.BrowserLauncherContext
import com.perol.pixez.shared.ui.navigation.RootComponent
import kotlinx.coroutines.launch

import android.os.Build
import android.widget.Toast
import androidx.activity.OnBackPressedCallback

/**
 * Android 应用入口。
 * 使用 ComponentActivity + setContent 承载 Compose Multiplatform 应用。
 */
class MainActivity : ComponentActivity() {

    private lateinit var dependencies: AppDependencies
    private lateinit var rootComponent: RootComponent
    private var lastBackPressTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        BrowserLauncherContext.applicationContext = applicationContext
        dependencies = AppDependencies(
            driverFactory = DriverFactory(this),
            settingsFactory = SettingsFactory(this),
        )
        dependencies.warmupAsync(lifecycleScope)
        rootComponent = RootComponent(
            componentContext = defaultComponentContext(),
            settingsRepository = dependencies.settingsRepository,
        )
        setupBackPressHandler()
        applyDisplayMode()
        handleIntent(intent)
        setContent {
            PixEzApp(
                dependencies = dependencies,
                rootComponent = rootComponent,
            )
        }
    }

    private var exitCallback: OnBackPressedCallback? = null

    private fun setupBackPressHandler() {
        val callback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                if (!dependencies.settingsRepository.isReturnAgainToExit) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    return
                }
                val now = System.currentTimeMillis()
                if (now - lastBackPressTime < 2000) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                } else {
                    lastBackPressTime = now
                    Toast.makeText(this@MainActivity, "再次点击返回退出应用", Toast.LENGTH_SHORT).show()
                }
            }
        }
        exitCallback = callback
        onBackPressedDispatcher.addCallback(this, callback)

        // 监听 Decompose 页面栈变化：仅在处于一级主页面且开启「再次返回退出」时启用拦截器。
        // 在二级详情页面时 isEnabled = false，将手势完全放行给 Decompose 的 predictiveBackAnimation。
        rootComponent.stack.subscribe { updateExitCallbackState() }
    }

    private fun updateExitCallbackState() {
        val callback = exitCallback ?: return
        if (!::rootComponent.isInitialized || !::dependencies.isInitialized) return
        val childStack = rootComponent.stack.value
        val isAtRoot = childStack.backStack.isEmpty() && childStack.active.instance is RootComponent.Child.Main
        callback.isEnabled = isAtRoot && dependencies.settingsRepository.isReturnAgainToExit
    }

    override fun onResume() {
        super.onResume()
        if (::dependencies.isInitialized) {
            applyDisplayMode()
            checkClipboard()
            updateExitCallbackState()
        }
    }

    private var lastHandledClipboardText: String? = null

    private fun checkClipboard() {
        val clipboard = com.perol.pixez.shared.platform.IllustClipboard()
        val text = clipboard.getText()?.trim()
        if (!text.isNullOrBlank() && text != lastHandledClipboardText) {
            val hasIllust = text.contains("artworks/") || text.contains("illust_id=")
            val hasUser = text.contains("users/")
            if (hasIllust || hasUser) {
                lastHandledClipboardText = text
                parseAndNavigateUrlOrId(text)
            }
        }
    }

    private fun applyDisplayMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay
            }
            val modes = display?.supportedModes.orEmpty()
            val targetMode = when (dependencies.settingsRepository.displayMode) {
                1 -> modes.filter { it.refreshRate in 58f..62f }.maxByOrNull { it.physicalWidth * it.physicalHeight }
                2 -> modes.filter { it.refreshRate >= 88f }.maxByOrNull { it.refreshRate }
                else -> null
            }
            val layoutParams = window.attributes
            layoutParams.preferredDisplayModeId = targetMode?.modeId ?: 0
            window.attributes = layoutParams
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            parseAndNavigateUrlOrId(text)
            return
        }

        val uri = intent.data ?: return
        val scheme = uri.scheme?.lowercase()
        val host = uri.host?.lowercase().orEmpty()
        val path = uri.path.orEmpty()

        when {
            (scheme == "pixez" || scheme == "pixiv") -> {
                when (host) {
                    "ranking" -> rootComponent.onMainTabSelected(RootComponent.MainTab.Ranking)
                    "search" -> rootComponent.onMainTabSelected(RootComponent.MainTab.Search)
                    "downloads", "download_task" -> rootComponent.onDownloadTaskClicked()
                    "history" -> rootComponent.onHistoryClicked()
                    "illust", "artworks" -> {
                        val id = uri.lastPathSegment?.toIntOrNull()
                        if (id != null) rootComponent.onIllustClicked(id)
                    }
                    "users", "user" -> {
                        val id = uri.lastPathSegment?.toIntOrNull()
                        if (id != null) rootComponent.onUserClicked(id)
                    }
                    "account", "oauth" -> handleAuthIntent(intent)
                }
            }
            host.contains("pixiv.net") || host.contains("pixiv.me") -> {
                when {
                    path.contains("/users/auth/pixiv/callback") -> handleAuthIntent(intent)
                    path.contains("/artworks/") -> {
                        val id = uri.lastPathSegment?.toIntOrNull()
                        if (id != null) rootComponent.onIllustClicked(id)
                    }
                    path.contains("/users/") -> {
                        val id = uri.lastPathSegment?.toIntOrNull()
                        if (id != null) rootComponent.onUserClicked(id)
                    }
                    else -> parseAndNavigateUrlOrId(uri.toString())
                }
            }
        }
    }

    private fun parseAndNavigateUrlOrId(text: String?) {
        if (text.isNullOrBlank()) return
        try {
            val illustMatch = Regex("""(?:artworks/|illust_id=)(\d+)""").find(text)
            if (illustMatch != null) {
                val id = illustMatch.groupValues[1].toIntOrNull()
                if (id != null) {
                    rootComponent.onIllustClicked(id)
                    return
                }
            }
            val userMatch = Regex("""(?:users/|member\.php\?id=)(\d+)""").find(text)
            if (userMatch != null) {
                val id = userMatch.groupValues[1].toIntOrNull()
                if (id != null) {
                    rootComponent.onUserClicked(id)
                    return
                }
            }
            val pureId = text.trim().toIntOrNull()
            if (pureId != null && text.trim().length in 6..10) {
                rootComponent.onIllustClicked(pureId)
            }
        } catch (_: Throwable) {
            // 忽略重复导航或异常 URL 导致的解析错误
        }
    }

    private fun handleAuthIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        val code = uri.getQueryParameter("code")
        if (!code.isNullOrBlank()) {
            lifecycleScope.launch {
                try {
                    Log.i("MainActivity", "收到 OAuth 回调 code，开始登录")
                    dependencies.accountRepository.loginWithCode(code)
                } catch (e: Exception) {
                    Log.e("MainActivity", "OAuth 回调登录失败", e)
                }
            }
        }
    }

    override fun onDestroy() {
        // 先释放数据库与网络资源，再调用 super.onDestroy()，避免 Activity 销毁期间句柄泄漏。
        if (::dependencies.isInitialized) {
            dependencies.close()
        }
        super.onDestroy()
    }
}
