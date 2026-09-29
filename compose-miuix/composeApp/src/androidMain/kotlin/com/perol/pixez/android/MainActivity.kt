package com.perol.pixez.android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.arkivanov.decompose.Cancellation
import com.arkivanov.decompose.defaultComponentContext
import com.perol.pixez.PixEzApp
import com.perol.pixez.shared.AppDependencies
import com.perol.pixez.shared.data.local.DriverFactory
import com.perol.pixez.shared.data.settings.SettingsFactory
import com.perol.pixez.shared.platform.BrowserLauncherContext
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.navigation.RootComponent
import kotlinx.coroutines.launch

import android.os.Build
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import com.perol.pixez.shared.ui.i18n.AppStrings

/**
 * Android 应用入口。
 * 使用 ComponentActivity + setContent 承载 Compose Multiplatform 应用。
 */
class MainActivity : ComponentActivity() {

    private companion object {
        // SettingsRepository.displayMode 的档位取值
        const val DISPLAY_MODE_LIMIT_60HZ = 1
        const val DISPLAY_MODE_HIGH_REFRESH = 2

        // 各档位对应的刷新率过滤区间（Hz）：60Hz 档容忍 58-62，高刷档取 >= 88（90/120Hz）
        val REFRESH_RATE_60HZ_RANGE = 58f..62f
        const val REFRESH_RATE_HIGH_MIN = 88f
    }

    private lateinit var dependencies: AppDependencies
    private lateinit var rootComponent: RootComponent
    private var lastBackPressTime = 0L
    private var stackSubscription: Cancellation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        BrowserLauncherContext.applicationContext = applicationContext
        dependencies = AppDependencies(
            driverFactory = DriverFactory(applicationContext),
            settingsFactory = SettingsFactory(applicationContext),
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
                    val strings = AppStrings.fromLanguageNum(dependencies.settingsRepository.languageNum)
                    Toast.makeText(this@MainActivity, strings.doubleBackToExit, Toast.LENGTH_SHORT).show()
                }
            }
        }
        exitCallback = callback
        onBackPressedDispatcher.addCallback(this, callback)

        // 监听 Decompose 页面栈变化：仅在处于一级主页面且开启「再次返回退出」时启用拦截器。
        // 在二级详情页面时 isEnabled = false，将手势完全放行给 Decompose 的 predictiveBackAnimation。
        // Decompose 的 router state 经 InstanceKeeper 跨 Activity 重建保留，旧订阅不清会累积泄漏已销毁的 Activity。
        stackSubscription = rootComponent.stack.subscribe { updateExitCallbackState() }
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
            val hasScheme = text.startsWith("pixiv://", ignoreCase = true) || text.startsWith("pixez://", ignoreCase = true)
            if (hasIllust || hasUser || hasScheme) {
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
                // 与 SettingsRepository.displayMode 的取值映射：1 = 限制 60Hz，2 = 高刷新率
                DISPLAY_MODE_LIMIT_60HZ ->
                    modes.filter { it.refreshRate in REFRESH_RATE_60HZ_RANGE }.maxByOrNull { it.physicalWidth * it.physicalHeight }
                DISPLAY_MODE_HIGH_REFRESH ->
                    modes.filter { it.refreshRate >= REFRESH_RATE_HIGH_MIN }.maxByOrNull { it.refreshRate }
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
        parseAndNavigateUrlOrId(uri.toString())
    }

    private fun parseAndNavigateUrlOrId(text: String?) {
        if (text.isNullOrBlank()) return
        try {
            val parsed = com.perol.pixez.shared.navigation.DeepLinkParser.parse(text) ?: return
            com.perol.pixez.shared.navigation.DeepLinkParser.dispatch(
                parsed = parsed,
                rootComponent = rootComponent,
                onOAuthCode = { code ->
                    lifecycleScope.launch {
                        try {
                            Log.i("MainActivity", "收到 OAuth 回调 code，开始登录")
                            dependencies.accountRepository.loginWithCode(code)
                        } catch (e: kotlinx.coroutines.CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Log.e("MainActivity", "OAuth 回调登录失败", e)
                        }
                    }
                },
            )
        } catch (e: Throwable) {
            Log.w("MainActivity", "解析跳转文本失败: $text", e)
        }
    }

    override fun onDestroy() {
        // 栈订阅先于依赖关闭取消，避免销毁期间栈状态回调引用已关闭的依赖。
        stackSubscription?.cancel()
        // 先释放数据库与网络资源，再调用 super.onDestroy()，避免 Activity 销毁期间句柄泄漏。
        if (::dependencies.isInitialized) {
            dependencies.close()
        }
        super.onDestroy()
    }
}
