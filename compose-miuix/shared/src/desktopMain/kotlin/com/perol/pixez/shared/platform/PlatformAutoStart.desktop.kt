package com.perol.pixez.shared.platform

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg
import io.github.aakira.napier.Napier

/**
 * Windows 开机自启：写 HKCU CurrentVersion\Run（无需管理员权限）。
 *
 * 可执行文件路径取 jpackage 启动器注入的 jpackage.app-path；开发运行（无该属性）
 * 无法指向稳定 exe，记录日志并跳过写入，避免注册表指向临时 JVM。
 */
actual object PlatformAutoStart {
    private const val RUN_KEY = "Software\\Microsoft\\Windows\\CurrentVersion\\Run"
    private const val VALUE_NAME = "PixEz"

    actual fun setEnabled(enabled: Boolean) {
        try {
            if (!System.getProperty("os.name").orEmpty().contains("win", ignoreCase = true)) {
                Napier.w("开机自启当前仅在 Windows 支持", tag = "AutoStart")
                return
            }
            val exePath = System.getProperty("jpackage.app-path")
            if (enabled && exePath.isNullOrBlank()) {
                Napier.w("非打包环境（jpackage.app-path 缺失），跳过开机自启注册", tag = "AutoStart")
                return
            }
            if (enabled) {
                val command = "\\"$exePath\\""
                Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER, RUN_KEY, VALUE_NAME, command)
                Napier.i("已启用开机自启: $command", tag = "AutoStart")
            } else {
                if (Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, RUN_KEY, VALUE_NAME)) {
                    Advapi32Util.registryDeleteValue(WinReg.HKEY_CURRENT_USER, RUN_KEY, VALUE_NAME)
                }
                Napier.i("已禁用开机自启", tag = "AutoStart")
            }
        } catch (e: Exception) {
            Napier.e("设置开机自启失败 enabled=$enabled", e, tag = "AutoStart")
        }
    }
}
