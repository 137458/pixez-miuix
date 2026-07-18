package com.perol.pixez.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.perol.pixez.PixEzApp
import com.perol.pixez.shared.AppDependencies
import com.perol.pixez.shared.data.local.DriverFactory
import com.perol.pixez.shared.data.settings.SettingsFactory

/**
 * Android 应用入口。
 * 使用 ComponentActivity + setContent 承载 Compose Multiplatform 应用。
 */
class MainActivity : ComponentActivity() {

    private lateinit var dependencies: AppDependencies

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        dependencies = AppDependencies(
            driverFactory = DriverFactory(this),
            settingsFactory = SettingsFactory(this),
        )
        setContent {
            PixEzApp(dependencies)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Activity 销毁时释放数据库与网络资源，避免 OkHttp/SQLite 句柄泄漏。
        if (::dependencies.isInitialized) {
            dependencies.close()
        }
    }
}
