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
) : ProxySelector() {

    override fun select(uri: URI?): List<Proxy> {
        // 1. 检查环境变量 HTTP_PROXY / HTTPS_PROXY / ALL_PROXY
        val envProxy = getEnvProxy(uri)
        if (envProxy != null) {
            return listOf(envProxy)
        }

        // 2. Windows 平台：动态查询 Windows 注册表系统代理配置
        if (isWindows()) {
            val winProxy = getWindowsRegistryProxy(uri)
            if (winProxy != null) {
                return listOf(winProxy)
            }
        }

        // 3. 回退至 JVM 默认代理选择器或直连
        return try {
            val defaultList = delegate?.select(uri)
            if (defaultList.isNullOrEmpty()) listOf(Proxy.NO_PROXY) else defaultList
        } catch (_: Exception) {
            listOf(Proxy.NO_PROXY)
        }
    }

    override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {
        try {
            delegate?.connectFailed(uri, sa, ioe)
        } catch (_: Exception) {}
    }

    private fun getEnvProxy(uri: URI?): Proxy? {
        val http = System.getenv("HTTP_PROXY") ?: System.getenv("http_proxy")
        val https = System.getenv("HTTPS_PROXY") ?: System.getenv("https_proxy")
        val all = System.getenv("ALL_PROXY") ?: System.getenv("all_proxy")

        val target = if (uri?.scheme?.equals("https", ignoreCase = true) == true) {
            https ?: all ?: http
        } else {
            http ?: all
        }

        return if (!target.isNullOrBlank()) parseProxyAddress(target) else null
    }

    private fun getWindowsRegistryProxy(uri: URI?): Proxy? {
        return try {
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
                    map["https"] ?: map["http"] ?: map["socks"] ?: map[""]
                } else {
                    map["http"] ?: map["socks"] ?: map[""]
                }
            } else {
                proxyServer
            }

            if (!targetProxy.isNullOrBlank()) {
                parseProxyAddress(targetProxy)
            } else null
        } catch (_: Throwable) {
            null
        }
    }

    private fun parseProxyAddress(raw: String): Proxy? {
        return try {
            val isSocks = raw.startsWith("socks://", ignoreCase = true) || raw.startsWith("socks5://", ignoreCase = true)
            val cleaned = raw
                .removePrefix("http://")
                .removePrefix("https://")
                .removePrefix("socks5://")
                .removePrefix("socks://")
                .trim()
            val colonIndex = cleaned.lastIndexOf(':')
            if (colonIndex > 0 && colonIndex < cleaned.length - 1) {
                val host = cleaned.substring(0, colonIndex)
                val port = cleaned.substring(colonIndex + 1).toIntOrNull() ?: return null
                val type = if (isSocks) Proxy.Type.SOCKS else Proxy.Type.HTTP
                Proxy(type, InetSocketAddress(host, port))
            } else null
        } catch (_: Exception) {
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
