package com.perol.pixez.android

import android.app.Application
import com.perol.pixez.shared.AppDependencies
import com.perol.pixez.shared.data.local.DriverFactory
import com.perol.pixez.shared.data.settings.SettingsFactory
import com.perol.pixez.shared.platform.BrowserLauncherContext

/**
 * 全局 Application 入口。
 *
 * 负责全局应用上下文的持有与早期初始化，规避将 Activity 实例作为长生命周期依赖传递造成的内存泄漏，
 * 并确保桌面小部件 (AppWidget) 与快捷磁贴 (TileService) 在后台被系统唤醒时全局上下文已准备就绪。
 *
 * 应用级依赖容器 [AppDependencies] 亦在此构建、随进程常驻：不再绑定 Activity 生命周期，
 * 字体缩放等未覆盖 configChanges 的配置变化触发 Activity 重建时，容器与其网络/数据库资源保持存活。
 */
class PixEzApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        BrowserLauncherContext.applicationContext = this
        container = AppDependencies(
            driverFactory = DriverFactory(this),
            settingsFactory = SettingsFactory(this),
        )
    }

    companion object {

        @kotlin.concurrent.Volatile
        private var container: AppDependencies? = null

        /**
         * 进程级依赖容器的静态获取入口。
         *
         * 容器在 [PixEzApplication.onCreate] 构建，先于任何 Activity/Service 启动，
         * 因此正常装配下必然非空；未初始化即获取属装配错误，快速失败暴露问题。
         * Widget 与快捷磁贴复用该容器属待确认项，当前仍各自独立装配，暂不接入。
         */
        val dependencies: AppDependencies
            get() = requireNotNull(container) { "PixEzApplication 尚未完成 onCreate，依赖容器不可用" }
    }
}
