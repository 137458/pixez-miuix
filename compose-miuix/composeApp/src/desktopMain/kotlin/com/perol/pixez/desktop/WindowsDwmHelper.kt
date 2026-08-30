package com.perol.pixez.desktop

import com.sun.jna.Native
import com.sun.jna.Platform
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.win32.StdCallLibrary
import io.github.aakira.napier.Napier
import java.awt.Window

/**
 * Windows 11 DWM (Desktop Window Manager) 本地交互接口。
 * 用于开启 Mica (云母) / Acrylic (亚克力) 硬件加速沉浸式窗口材质与深浅色模式同步。
 */
interface DwmApi : StdCallLibrary {
    companion object {
        val INSTANCE: DwmApi by lazy {
            Native.load("dwmapi", DwmApi::class.java)
        }
    }

    class MARGINS : Structure {
        @JvmField var cxLeftWidth: Int = 0
        @JvmField var cxRightWidth: Int = 0
        @JvmField var cyTopHeight: Int = 0
        @JvmField var cyBottomHeight: Int = 0

        constructor() : super()
        constructor(all: Int) : super() {
            cxLeftWidth = all
            cxRightWidth = all
            cyTopHeight = all
            cyBottomHeight = all
        }

        override fun getFieldOrder(): List<String> =
            listOf("cxLeftWidth", "cxRightWidth", "cyTopHeight", "cyBottomHeight")
    }

    fun DwmSetWindowAttribute(
        hwnd: WinDef.HWND,
        dwAttribute: Int,
        pvAttribute: Pointer,
        cbAttribute: Int,
    ): Int

    fun DwmExtendFrameIntoClientArea(
        hwnd: WinDef.HWND,
        pMarInset: MARGINS,
    ): Int
}

object WindowsDwmHelper {
    private const val DWMWA_USE_IMMERSIVE_DARK_MODE = 20
    private const val DWMWA_SYSTEMBACKDROP_TYPE = 38
    private const val DWMWA_MICA_EFFECT = 1029

    /**
     * 系统背景类型：
     * 1: None (实色)
     * 2: Mica (云母材质，Windows 11 推荐)
     * 3: Acrylic (亚克力材质)
     * 4: Mica Alt / Tabbed (云母 Alt)
     */
    fun applyMica(window: Window, isDarkMode: Boolean, backdropType: Int = 2) {
        if (!Platform.isWindows()) return
        try {
            val hwnd = WinDef.HWND(Native.getWindowPointer(window))

            // 1. 同步沉浸式深浅色模式
            val darkModeMem = com.sun.jna.Memory(4).apply { setInt(0, if (isDarkMode) 1 else 0) }
            DwmApi.INSTANCE.DwmSetWindowAttribute(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE, darkModeMem, 4)

            // 2. 启用 Windows 11 Build 22621+ DWMWA_SYSTEMBACKDROP_TYPE
            val backdropMem = com.sun.jna.Memory(4).apply { setInt(0, backdropType) }
            val res = DwmApi.INSTANCE.DwmSetWindowAttribute(hwnd, DWMWA_SYSTEMBACKDROP_TYPE, backdropMem, 4)

            // 3. 若为旧版 Win11 (Build 22000)，尝试旧版 Mica 属性 1029
            if (res != 0) {
                val micaMem = com.sun.jna.Memory(4).apply { setInt(0, 1) }
                DwmApi.INSTANCE.DwmSetWindowAttribute(hwnd, DWMWA_MICA_EFFECT, micaMem, 4)
            }

            // 4. 将系统模糊效果延伸至全客户区
            val margins = DwmApi.MARGINS(-1)
            DwmApi.INSTANCE.DwmExtendFrameIntoClientArea(hwnd, margins)
        } catch (e: Throwable) {
            Napier.w("Windows DWM Mica 设置异常 (可能处于旧版系统)", e)
        }
    }
}
