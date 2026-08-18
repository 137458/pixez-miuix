package com.perol.pixez.android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.arkivanov.decompose.defaultComponentContext
import com.perol.pixez.PixEzApp
import com.perol.pixez.shared.AppDependencies
import com.perol.pixez.shared.data.local.DriverFactory
import com.perol.pixez.shared.data.settings.SettingsFactory
import com.perol.pixez.shared.platform.BrowserLauncherContext
import com.perol.pixez.shared.ui.navigation.RootComponent
import kotlinx.coroutines.launch

/**
 * Android 应用入口。
 * 使用 ComponentActivity + setContent 承载 Compose Multiplatform 应用。
 */
class MainActivity : ComponentActivity() {

    private lateinit var dependencies: AppDependencies
    private lateinit var rootComponent: RootComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        BrowserLauncherContext.applicationContext = applicationContext
        dependencies = AppDependencies(
            driverFactory = DriverFactory(this),
            settingsFactory = SettingsFactory(this),
        )
        rootComponent = RootComponent(
            componentContext = defaultComponentContext(),
            settingsRepository = dependencies.settingsRepository,
        )
        handleAuthIntent(intent)
        setContent {
            PixEzApp(
                dependencies = dependencies,
                rootComponent = rootComponent,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthIntent(intent)
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
