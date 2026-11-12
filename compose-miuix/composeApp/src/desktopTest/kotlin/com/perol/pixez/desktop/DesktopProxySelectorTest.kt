package com.perol.pixez.desktop

import org.junit.Test
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import kotlin.test.assertEquals

class DesktopProxySelectorTest {

    @Test
    fun `parses IPv6 trailing slash userinfo and socks5 proxy URIs from env`() {
        val selector = DesktopProxySelector(
            delegate = null,
            envProvider = { key ->
                when (key) {
                    "HTTPS_PROXY" -> "http://user:pass@[::1]:7890/"
                    "HTTP_PROXY" -> "socks5://127.0.0.1:1080/"
                    else -> null
                }
            },
        )

        val httpsProxy = selector.select(URI("https://app-api.pixiv.net")).single()
        assertEquals(Proxy.Type.HTTP, httpsProxy.type())
        val httpsAddr = httpsProxy.address() as InetSocketAddress
        assertEquals(7890, httpsAddr.port)
        assertEquals("0:0:0:0:0:0:0:1", httpsAddr.address.hostAddress)

        val httpProxy = selector.select(URI("http://example.com")).single()
        assertEquals(Proxy.Type.SOCKS, httpProxy.type())
        val httpAddr = httpProxy.address() as InetSocketAddress
        assertEquals(1080, httpAddr.port)
    }

    @Test
    fun `returns NO_PROXY safely when delegate throws`() {
        val throwingDelegate = object : ProxySelector() {
            override fun select(uri: URI?): List<Proxy> = error("boom")
            override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: java.io.IOException?) {}
        }
        val selector = DesktopProxySelector(
            delegate = throwingDelegate,
            envProvider = { null },
            windowsRegistryProxyProvider = { null },
        )

        val result = selector.select(URI("https://app-api.pixiv.net"))
        assertEquals(listOf(Proxy.NO_PROXY), result)
    }
}
