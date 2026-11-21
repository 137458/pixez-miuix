package com.perol.pixez.shared.platform

import kotlin.test.Test
import kotlin.test.assertEquals

class PictureSourceMappingTest {

    @Test
    fun `null source returns url unchanged`() {
        val url = "https://i.pximg.net/img-master/img/2024/01/01/00/00/00/123_p0_master1200.jpg"
        assertEquals(url, url.mapToPictureSource(null))
    }

    @Test
    fun `blank source returns url unchanged`() {
        val url = "https://i.pximg.net/img-master/img/123_p0_master1200.jpg"
        assertEquals(url, url.mapToPictureSource(""))
        assertEquals(url, url.mapToPictureSource("   "))
    }

    @Test
    fun `default pximg source returns url unchanged`() {
        val url = "https://i.pximg.net/img-master/img/123_p0_master1200.jpg"
        assertEquals(url, url.mapToPictureSource("i.pximg.net"))
    }

    @Test
    fun `mirror source replaces pximg host keeping scheme and path`() {
        assertEquals(
            "https://i.pixiv.re/img-master/img/123_p0_master1200.jpg",
            "https://i.pximg.net/img-master/img/123_p0_master1200.jpg".mapToPictureSource("i.pixiv.re"),
        )
    }

    @Test
    fun `mirror source replaces all pximg occurrences`() {
        assertEquals(
            "https://i.pixiv.re/a?fallback=https://i.pixiv.re/b",
            "https://i.pximg.net/a?fallback=https://i.pximg.net/b".mapToPictureSource("i.pixiv.re"),
        )
    }

    @Test
    fun `non pximg url is unchanged even with mirror source`() {
        val url = "https://www.pixivision.net/123"
        assertEquals(url, url.mapToPictureSource("i.pixiv.re"))
    }

    @Test
    fun `local file url is unchanged`() {
        val url = "file:///cache/123.jpg"
        assertEquals(url, url.mapToPictureSource("i.pixiv.re"))
    }
}
