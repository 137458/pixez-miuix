package com.perol.pixez.desktop

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg
import io.github.aakira.napier.Napier
import java.io.File

/**
 * 在 Windows 当前用户注册表 (`HKCU\Software\Classes`) 下自动注册 `pixiv://` 与 `pixez://` 协议。
 *
 * 无需管理员权限，配合 [SingleInstanceCoordinator] 可使系统浏览器完成 OAuth PKCE 登录重定向
 * (`pixiv://account/login?code=...`) 或打开 `pixez://` 链接时直接唤醒正在运行的桌面端进程。
 */
internal object WindowsProtocolRegistrar {
    private val SCHEMES = listOf("pixiv", "pixez")

    fun registerIfNeeded() {
        val osName = System.getProperty("os.name")?.lowercase().orEmpty()
        if (!osName.contains("win")) return

        val commandString = resolveLaunchCommand() ?: return
        for (scheme in SCHEMES) {
            runCatching {
                val baseKey = "Software\\Classes\\$scheme"
                val commandKey = "$baseKey\\shell\\open\\command"
                Advapi32Util.registryCreateKey(WinReg.HKEY_CURRENT_USER, baseKey)
                Advapi32Util.registrySetStringValue(
                    WinReg.HKEY_CURRENT_USER,
                    baseKey,
                    "",
                    "URL:PixEz $scheme Protocol",
                )
                Advapi32Util.registrySetStringValue(
                    WinReg.HKEY_CURRENT_USER,
                    baseKey,
                    "URL Protocol",
                    "",
                )
                Advapi32Util.registryCreateKey(WinReg.HKEY_CURRENT_USER, commandKey)
                Advapi32Util.registrySetStringValue(
                    WinReg.HKEY_CURRENT_USER,
                    commandKey,
                    "",
                    commandString,
                )
            }.onFailure { e ->
                Napier.w("注册 Windows 自定义协议 $scheme:// 失败", e)
            }
        }
    }

    private fun resolveLaunchCommand(): String? {
        // 1. jpackage 打包后的原生 EXE 路径
        val jpackageExe = System.getProperty("jpackage.app-path")?.trim()?.takeIf { it.isNotBlank() }
        if (jpackageExe != null && File(jpackageExe).exists()) {
            return "\"$jpackageExe\" \"%1\""
        }

        // 2. ProcessHandle 获取当前运行进程可执行文件（若是打包后的 PixEz.exe）
        val currentCmd = runCatching {
            ProcessHandle.current().info().command().orElse(null)
        }.getOrNull()?.trim()
        if (!currentCmd.isNullOrBlank() &&
            !currentCmd.endsWith("java.exe", ignoreCase = true) &&
            !currentCmd.endsWith("javaw.exe", ignoreCase = true) &&
            File(currentCmd).exists()
        ) {
            return "\"$currentCmd\" \"%1\""
        }

        return null
    }
}
