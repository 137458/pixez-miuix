package com.perol.pixez.android

import android.app.Application
import com.perol.pixez.shared.platform.BrowserLauncherContext

/**
 * 全局 Application 入口。
 *
 * 负责全局应用上下文的持有与早期初始化，规避将 Activity 实例作为长生命周期依赖传递造成的内存泄漏，
 * 并确保桌面小部件 (AppWidget) 与快捷磁贴 (TileService) 在后台被系统唤醒时全局上下文已准备就绪。
 */
class PixEzApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        BrowserLauncherContext.applicationContext = this
    }
}
