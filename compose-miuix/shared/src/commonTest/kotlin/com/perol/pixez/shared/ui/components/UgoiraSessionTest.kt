package com.perol.pixez.shared.ui.components

import com.perol.pixez.shared.data.model.UgoiraFrame
import com.perol.pixez.shared.ui.AppConstants.IllustType
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * [UgoiraReadySession] 与 [UgoiraSessionCache] 的行为验证。
 *
 * 这两个对象是详情页内嵌播放器与全屏查看器之间唯一的共享状态：
 * 一旦进度交接或缓存淘汰出错，就会出现「全屏后从第一帧重播」或
 * 「切换两个作品后已淘汰会话的临时文件残留」等缺陷。
 */
class UgoiraSessionTest {

    private fun session(zipUrl: String = "https://example.test/a.zip"): UgoiraReadySession =
        UgoiraReadySession(
            provider = UgoiraFrameProvider(frames = sampleFrames(), framesDir = "/nonexistent".toPath()),
            tempZipPath = null,
            framesDir = null,
            zipUrl = zipUrl,
        )

    private fun sampleFrames(): List<UgoiraFrame> = listOf(
        UgoiraFrame(file = "000.jpg", delay = 100),
        UgoiraFrame(file = "001.jpg", delay = 100),
    )

    @Test
    fun `首次领取播放从第 0 帧开始`() {
        val session = session()

        assertEquals(0, session.beginPlayback())
    }

    @Test
    fun `播报进度后另一个视图领取时从该进度续播`() {
        val session = session()

        session.reportProgress(7)

        // 模拟从内嵌播放器切换到全屏查看器：新视图应拿到上一视图的进度。
        assertEquals(7, session.beginPlayback())
    }

    @Test
    fun `两个视图各自领取播放互不改写对方起点`() {
        val session = session()
        session.reportProgress(3)

        // 内嵌播放器与全屏查看器同时存在时，领取动作本身不得推进进度。
        val first = session.beginPlayback()
        val second = session.beginPlayback()

        assertEquals(3, first)
        assertEquals(3, second)
    }

    @Test
    fun `负数进度被收敛为非负起点`() {
        val session = session()

        session.reportProgress(-5)

        assertTrue(session.beginPlayback() >= 0)
    }

    @Test
    fun `缓存按作品 ID 命中同一会话实例`() {
        UgoiraSessionCache.clear()
        val cached = session()
        UgoiraSessionCache.put(illustId = 101, session = cached)

        assertSame(cached, UgoiraSessionCache.get(101))
        assertEquals(cached, UgoiraSessionCache.get(101))
    }

    @Test
    fun `未登记的会话返回空`() {
        UgoiraSessionCache.clear()

        assertNull(UgoiraSessionCache.get(999))
    }

    @Test
    fun `超出容量时淘汰最久未使用的会话`() {
        UgoiraSessionCache.clear()
        val oldest = session("https://example.test/old.zip")
        UgoiraSessionCache.put(1, oldest)
        UgoiraSessionCache.put(2, session("https://example.test/b.zip"))
        UgoiraSessionCache.put(3, session("https://example.test/c.zip"))

        // 容量为 2：最早登记的 1 号应被淘汰，2 与 3 保留。
        assertNull(UgoiraSessionCache.get(1))
        assertNotEquals(null, UgoiraSessionCache.get(2))
        assertNotEquals(null, UgoiraSessionCache.get(3))
    }

    @Test
    fun `清空后所有会话均不可取回`() {
        UgoiraSessionCache.clear()
        UgoiraSessionCache.put(1, session())
        UgoiraSessionCache.put(2, session())

        UgoiraSessionCache.clear()

        assertNull(UgoiraSessionCache.get(1))
        assertNull(UgoiraSessionCache.get(2))
    }

    @Test
    fun `同一作品重复登记以最后一次为准`() {
        UgoiraSessionCache.clear()
        val first = session("https://example.test/first.zip")
        val second = session("https://example.test/second.zip")

        UgoiraSessionCache.put(5, first)
        UgoiraSessionCache.put(5, second)

        assertSame(second, UgoiraSessionCache.get(5))
    }

    @Test
    fun `ugoira 类型判定只认动图`() {
        assertTrue(IllustType.isUgoira("ugoira"))
        assertTrue(!IllustType.isUgoira("illust"))
        assertTrue(!IllustType.isUgoira("manga"))
        assertTrue(!IllustType.isUgoira(""))
    }
}
