package com.perol.pixez.shared.platform

/**
 * 开机自启开关的平台写入（N-AutoStart）。
 *
 * Windows 写 HKCU\Software\Microsoft\Windows\CurrentVersion\Run；
 * 非 Windows 桌面与移动平台为 no-op。切换失败记录日志，不抛出。
 */
expect object PlatformAutoStart {
    fun setEnabled(enabled: Boolean)
}
