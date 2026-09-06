package com.perol.pixez.desktop.platform

import androidx.compose.ui.awt.ComposeWindow
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.platform.win32.W32Errors
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.ptr.IntByReference
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.win32.W32APIOptions

/**
 * Windows 11 系统级硬件加速 Mica 材质与暗色标题栏整合管理器。
 *
 * 通过 Win32 DWM (Desktop Window Manager) API 与 JNA 实现：
 * - 针对 Windows 11 22H2+ (Build >= 22621) 注入 [DWMWA_SYSTEMBACKDROP_TYPE] (DWMSBT_MAINWINDOW)；
 * - 针对 Windows 11 21H2 (Build >= 22000) 注入 [DWMWA_MICA_EFFECT]；
 * - 动态联动 [DWMWA_USE_IMMERSIVE_DARK_MODE] 实现原生标题栏明暗主题无缝同步；
 * - 非 Windows / 低版本 Windows 环境安全降级，避免崩溃。
 */
object WindowsMica {

    @Structure.FieldOrder("cxLeftWidth", "cxRightWidth", "cyTopHeight", "cyBottomHeight")
    class Margins(
        @JvmField var cxLeftWidth: Int = -1,
        @JvmField var cxRightWidth: Int = -1,
        @JvmField var cyTopHeight: Int = -1,
        @JvmField var cyBottomHeight: Int = -1,
    ) : Structure()

    internal interface DwmApi : StdCallLibrary {
        fun DwmSetWindowAttribute(
            hwnd: WinDef.HWND,
            dwAttribute: Int,
            pvAttribute: IntByReference,
            cbAttribute: Int,
        ): WinNT.HRESULT

        fun DwmExtendFrameIntoClientArea(
            hwnd: WinDef.HWND,
            pMarInset: Margins,
        ): WinNT.HRESULT

        companion object {
            val INSTANCE: DwmApi? by lazy {
                runCatching {
                    Native.load("dwmapi", DwmApi::class.java, W32APIOptions.DEFAULT_OPTIONS)
                }.getOrNull()
            }
        }
    }

    internal interface NtDll : StdCallLibrary {
        fun RtlGetVersion(versionInformation: WinNT.OSVERSIONINFOEX): Int

        companion object {
            val INSTANCE: NtDll? by lazy {
                runCatching {
                    Native.load("ntdll", NtDll::class.java, W32APIOptions.DEFAULT_OPTIONS)
                }.getOrNull()
            }
        }
    }

    // Win32 DWM 常量
    const val DWMWA_USE_IMMERSIVE_DARK_MODE = 20
    const val DWMWA_SYSTEMBACKDROP_TYPE = 38
    const val DWMWA_MICA_EFFECT = 1029

    // DWM_SYSTEMBACKDROP_TYPE 枚举值
    const val DWMSBT_AUTO = 0
    const val DWMSBT_NONE = 1
    const val DWMSBT_MAINWINDOW = 2 // Mica
    const val DWMSBT_TRANSIENTWINDOW = 3 // Acrylic
    const val DWMSBT_TABBEDWINDOW = 4 // Mica Alt

    val windowsBuildNumber: Int by lazy {
        val os = System.getProperty("os.name").orEmpty()
        if (!os.contains("Windows", ignoreCase = true)) return@lazy -1
        runCatching {
            val info = WinNT.OSVERSIONINFOEX()
            info.dwOSVersionInfoSize = WinDef.DWORD(info.size().toLong())
            val status = NtDll.INSTANCE?.RtlGetVersion(info) ?: -1
            if (status == 0) {
                info.dwBuildNumber.toInt()
            } else {
                -1
            }
        }.getOrDefault(-1)
    }

    val isWindows: Boolean get() = windowsBuildNumber > 0 || System.getProperty("os.name").orEmpty().contains("Windows", ignoreCase = true)
    val isWindows11: Boolean get() = windowsBuildNumber >= 22000
    val isWindows11_22H2OrGreater: Boolean get() = windowsBuildNumber >= 22621

    /**
     * 将 Mica 材质与暗色/亮色主题属性应用到指定的 [ComposeWindow]。
     *
     * @param window 当前 Compose 桌面窗口
     * @param isDark 是否处于暗色主题模式
     * @return 若成功应用 Mica 材质则返回 true，若因系统版本不支持或 API 缺失安全降级则返回 false
     */
    fun apply(window: ComposeWindow, isDark: Boolean): Boolean {
        val dwm = DwmApi.INSTANCE ?: return false

        val hwnd = runCatching {
            val handleLong = window.windowHandle
            if (handleLong != 0L) {
                WinDef.HWND(Pointer.createConstant(handleLong))
            } else {
                WinDef.HWND(Native.getWindowPointer(window))
            }
        }.getOrNull() ?: return false

        var success = false

        // 1. 同步暗色/亮色沉浸式标题栏 (Windows 10 20H1+ 及 Windows 11 均支持)
        if (windowsBuildNumber >= 19041 || isWindows11) {
            val darkVal = IntByReference(if (isDark) 1 else 0)
            dwm.DwmSetWindowAttribute(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE, darkVal, 4)
        }

        // 2. 注入 Mica 材质
        if (isWindows11_22H2OrGreater) {
            val backdropVal = IntByReference(DWMSBT_MAINWINDOW)
            val hr = dwm.DwmSetWindowAttribute(hwnd, DWMWA_SYSTEMBACKDROP_TYPE, backdropVal, 4)
            success = hr == W32Errors.S_OK || hr.toInt() == 0
        } else if (isWindows11) {
            val micaVal = IntByReference(1)
            val hr = dwm.DwmSetWindowAttribute(hwnd, DWMWA_MICA_EFFECT, micaVal, 4)
            success = hr == W32Errors.S_OK || hr.toInt() == 0
        }

        return success
    }
}
