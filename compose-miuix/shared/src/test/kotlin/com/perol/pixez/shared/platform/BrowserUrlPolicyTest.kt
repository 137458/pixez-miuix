package com.perol.pixez.shared.platform

import kotlin.test.Test
import kotlin.test.assertEquals

class BrowserUrlPolicyTest {

    @Test
    fun `https url is allowed`() {
        assertEquals(true, isBrowserUrlAllowed("https://www.pixiv.net/artworks/123"))
    }

    @Test
    fun `http url is allowed`() {
        assertEquals(true, isBrowserUrlAllowed("http://example.com/page"))
    }

    @Test
    fun `mailto url is allowed`() {
        assertEquals(true, isBrowserUrlAllowed("mailto:support@pixiv.net"))
    }

    @Test
    fun `scheme case is ignored`() {
        assertEquals(true, isBrowserUrlAllowed("HTTPS://WWW.PIXIV.NET"))
        assertEquals(true, isBrowserUrlAllowed("MailTo:a@b.c"))
    }

    @Test
    fun `intent scheme is rejected`() {
        // Android intent redirection：远程内容里的 intent:// 可拉起任意导出组件
        assertEquals(
            false,
            isBrowserUrlAllowed("intent://x#Intent;package=com.example;component=com.example/.MainActivity;end"),
        )
    }

    @Test
    fun `android app scheme is rejected`() {
        assertEquals(false, isBrowserUrlAllowed("android-app://com.example/main"))
    }

    @Test
    fun `javascript scheme is rejected`() {
        assertEquals(false, isBrowserUrlAllowed("javascript:alert(1)"))
    }

    @Test
    fun `file scheme is rejected`() {
        assertEquals(false, isBrowserUrlAllowed("file:///data/data/app/files/secret.db"))
    }

    @Test
    fun `content scheme is rejected`() {
        assertEquals(false, isBrowserUrlAllowed("content://com.android.providers/secret"))
    }

    @Test
    fun `unknown custom scheme is rejected`() {
        assertEquals(false, isBrowserUrlAllowed("pixez://illust/123"))
    }

    @Test
    fun `url without scheme is rejected`() {
        assertEquals(false, isBrowserUrlAllowed("www.pixiv.net/artworks/123"))
        assertEquals(false, isBrowserUrlAllowed(""))
    }
}
