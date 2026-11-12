package com.perol.pixez.desktop

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg
import io.github.aakira.napier.Napier
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI

/**
 * 桌面端全自动智能代理选择器。
 *
 * 彻底解决 JVM 默认不走 Windows 系统代理（Clash / v2rayN / Sing-box / 系统代理设置）的问题：
 * 1. 实时读取 Windows 注册表 HKCU\Software\Microsoft\Windows\CurrentVersion\Internet Settings 中的 ProxyEnable 与 ProxyServer；
 * 2. 实时读取环境变量 HTTP_PROXY / HTTPS_PROXY / ALL_PROXY；
 * 3. 动态将代理路由至目标端口，并在系统代理关闭时即时恢复直连。
 */
class DesktopProxySelector(
    private val delegate: ProxySelector? = ProxySelector.getDefault(),
    private val envProvider: (String) -> String? = System::getenv,
    private val windowsRegistryProxyProvider: ((URI?) -> String?)? = null,
) : ProxySelector() {

    override fun select(uri: URI?): List<Proxy> {
        // 1. 检查环境变量 HTTP_PROXY / HTTPS_PROXY / ALL_PROXY
        val envProxy = getEnvProxy(uri)
        if (envProxy != null) {
            return listOf(envProxy)
        }

        // 2. Windows 平台：动态查询 Windows 注册表系统代理配置
        if (isWindows() || windowsRegistryProxyProvider != null) {
            val winProxy = getWindowsRegistryProxy(uri)
            if (winProxy != null) {
                return listOf(winProxy)
            }
        }

        // 3. 回退至 JVM 默认代理选择器或直连
        return try {
            val defaultList = delegate?.select(uri)
            if (defaultList.isNullOrEmpty()) listOf(Proxy.NO_PROXY) else defaultList
        } catch (e: Exception) {
            Napier.d("委派代理选择器查询异常 uri=$uri", e, tag = "DesktopProxy")
            listOf(Proxy.NO_PROXY)
        }
    }

    override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {
        try {
            delegate?.connectFailed(uri, sa, ioe)
        } catch (e: Exception) {
            // 代理连接失败通知仅用于诊断，委派实现异常不应影响调用方
            Napier.d("委派代理选择器连接失败回调异常 uri=$uri", e, tag = "DesktopProxy")
        }
    }

    private fun getEnvProxy(uri: URI?): Proxy? {
        val http = envProvider("HTTP_PROXY") ?: envProvider("http_proxy")
        val https = envProvider("HTTPS_PROXY") ?: envProvider("https_proxy")
        val all = envProvider("ALL_PROXY") ?: envProvider("all_proxy")

        val target = if (uri?.scheme?.equals("https", ignoreCase = true) == true) {
            https ?: all ?: http
        } else {
            http ?: all
        }

        return if (!target.isNullOrBlank()) parseProxyAddress(target) else null
    }

    private fun getWindowsRegistryProxy(uri: URI?): Proxy? {
        return try {
            val injected = windowsRegistryProxyProvider?.invoke(uri)
            if (injected != null) {
                return parseProxyAddress(injected)
            }
            val isEnabled = Advapi32Util.registryGetIntValue(
                WinReg.HKEY_CURRENT_USER,
                "Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings",
                "ProxyEnable",
            ) == 1
            if (!isEnabled) return null

            val proxyServer = Advapi32Util.registryGetStringValue(
                WinReg.HKEY_CURRENT_USER,
                "Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings",
                "ProxyServer",
            )
            if (proxyServer.isNullOrBlank()) return null

            // 格式可能是 "127.0.0.1:7890" 或 "http=127.0.0.1:7890;https=127.0.0.1:7890;socks=127.0.0.1:7890"
            val targetProxy = if (proxyServer.contains("=")) {
                val isHttps = uri?.scheme?.equals("https", ignoreCase = true) == true
                val map = proxyServer.split(";").associate { entry ->
                    val pair = entry.split("=")
                    if (pair.size == 2) pair[0].trim().lowercase() to pair[1].trim() else "" to ""
                }
                if (isHttps) {
                    map["https"] ?: map["http"] ?: map["socks"]?.let { forceSocks(it) } ?: map[""]
                } else {
                    map["http"] ?: map["socks"]?.let { forceSocks(it) } ?: map[""]
                }
            } else {
                proxyServer
            }

            if (!targetProxy.isNullOrBlank()) {
                parseProxyAddress(targetProxy)
            } else null
        } catch (t: Throwable) {
            Napier.w("读取 Windows 注册表代理设置失败", t, tag = "DesktopProxy")
            null
        }
    }

    /** 注册表 socks= 条目无 scheme 前缀，必须显式标记为 SOCKS，否则会被误判为 HTTP 代理导致连接失败。 */
    private fun forceSocks(raw: String): String = if (raw.startsWith("socks://", true) || raw.startsWith("socks5://", true)) raw else "socks5://$raw"

    private fun parseProxyAddress(raw: String): Proxy? {
        return try {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return null
            val hasScheme = trimmed.contains("://")
            val normalized = if (hasScheme) trimmed else "http://$trimmed"
            val parsed = URI(normalized)
            val scheme = parsed.scheme?.lowercase().orEmpty()
            val isSocks = scheme == "socks" || scheme == "socks5" || scheme == "socks4" || scheme == "socks5h"
            val rawHost = parsed.host?.takeIf { it.isNotBlank() } ?: return null
            val host = rawHost.removePrefix("[").removeSuffix("]")
            val port = parsed.port
            if (port !in 1..65535) return null
            val type = if (isSocks) Proxy.Type.SOCKS else Proxy.Type.HTTP
            Proxy(type, InetSocketAddress(host, port))
        } catch (e: Exception) {
            Napier.d("解析代理地址失败: $raw", e, tag = "DesktopProxy")
            null
        }
    }

    private fun isWindows(): Boolean {
        val os = System.getProperty("os.name") ?: ""
        return os.contains("Windows", ignoreCase = true)
    }

    companion object {
        fun install() {
            try {
                System.setProperty("java.net.useSystemProxies", "true")
                val selector = DesktopProxySelector()
                ProxySelector.setDefault(selector)
                Napier.i("桌面端全自动系统代理选择器已激活", tag = "DesktopProxy")
            } catch (e: Throwable) {
                Napier.w("初始化系统代理选择器失败", e, tag = "DesktopProxy")
            }
        }
    }
}
