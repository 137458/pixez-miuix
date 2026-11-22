package com.perol.pixez.shared.data.repository

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 发布源兜底解析测试：用真实 releases.atom / expanded_assets 的片段结构固定解析契约。
 */
class UpdateFeedParserTest {

    /** 与线上一致的两条 entry 结构，第一条为最新发布。 */
    private val atomXml: String = """
        <?xml version="1.0" encoding="UTF-8"?>
        <feed xmlns="http://www.w3.org/2005/Atom" xmlns:media="http://search.yahoo.com/mrss/" xml:lang="zh-CN">
          <id>tag:github.com,2008:/137458/pixez-miuix/releases</id>
          <title>Release notes from pixez-miuix</title>
          <entry>
            <id>tag:github.com,2008:Repository/1304646975/v99.0.0</id>
            <updated>2026-10-01T00:00:00Z</updated>
            <link rel="alternate" type="text/html" href="https://github.com/137458/pixez-miuix/releases/tag/v99.0.0"/>
            <title>PixEz MIUIX v99.0.0</title>
            <content type="html">&lt;h2&gt;新增&lt;/h2&gt;
        &lt;ul&gt;
        &lt;li&gt;支持 Atom 兜底&lt;/li&gt;
        &lt;li&gt;下载与安装说明见 &lt;a href=&quot;https://github.com/137458/pixez-miuix/releases&quot;&gt;发布页&lt;/a&gt;&lt;/li&gt;
        &lt;/ul&gt;
        &lt;p&gt;GPL-3.0 &amp;amp; 保留所有权利&lt;/p&gt;</content>
          </entry>
          <entry>
            <id>tag:github.com,2008:Repository/1304646975/v98.0.0</id>
            <updated>2026-09-01T00:00:00Z</updated>
            <link rel="alternate" type="text/html" href="https://github.com/137458/pixez-miuix/releases/tag/v98.0.0"/>
            <title>PixEz MIUIX v98.0.0</title>
            <content type="html">&lt;h2&gt;旧版&lt;/h2&gt;</content>
          </entry>
        </feed>
    """.trimIndent()

    private val expandedAssetsHtml: String = """
        <div data-view-component="true" class="Box Box--condensed tmp-mt-3"> <ul data-view-component="true"> <li data-view-component="true" class="Box-row d-flex flex-column flex-md-row">
        <span class="d-flex flex-items-start">
        <a href="/137458/pixez-miuix/releases/download/v99.0.0/PixEz-MIUIX-v99.0.0.apk" rel="nofollow" data-turbo="false" class="wb-break-all"> <span class="text-bold">PixEz-MIUIX-v99.0.0.apk</span> </a> </span>
        <span data-view-component="true" class="Truncate text-mono text-small color-fg-muted"> <span data-view-component="true" class="Truncate-text">sha256:dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd</span> </span>
        <span>8.04 MB</span>
        </li> <li data-view-component="true" class="Box-row">
        <a href="/137458/pixez-miuix/releases/download/v99.0.0/pixez-miuix-99.0.0-source.zip" rel="nofollow">pixez-miuix-99.0.0-source.zip</a>
        </li> </ul> </div>
    """.trimIndent()

    @Test
    fun `parseLatestFeedEntry 只取第一条 entry 并解出标签标题链接与时间`() {
        val entry = parseLatestFeedEntry(atomXml)

        assertTrue(entry != null, "含 entry 的 feed 应解析成功")
        assertEquals("v99.0.0", entry.tagName, "应取最新一条 entry 的 tag")
        assertEquals("PixEz MIUIX v99.0.0", entry.title)
        assertEquals(
            "https://github.com/137458/pixez-miuix/releases/tag/v99.0.0",
            entry.releaseUrl,
        )
        assertEquals("2026-10-01T00:00:00Z", entry.publishedAt)
        assertEquals(
            "## 新增\n- 支持 Atom 兜底\n- 下载与安装说明见 [发布页](https://github.com/137458/pixez-miuix/releases)\nGPL-3.0 & 保留所有权利",
            entry.changelogMarkdown,
        )
    }

    @Test
    fun `parseLatestFeedEntry 缺少 entry 时返回 null`() {
        assertNull(parseLatestFeedEntry("<?xml version=\"1.0\"?><feed></feed>"))
        assertNull(parseLatestFeedEntry(""))
    }

    @Test
    fun `htmlFragmentToMarkdown 转换标题列表链接代码与实体`() {
        val html = """
            <h1>PixEz MIUIX v1.0.0</h1>
            <p>本次更新说明</p>
            <ul>
            <li>下载 <code>PixEz-MIUIX-v1.0.0.apk</code></li>
            </ul>
            <p>详见 <a href="https://github.com/137458/pixez-miuix">仓库</a> &amp; 文档。</p>
        """.trimIndent()

        assertEquals(
            "# PixEz MIUIX v1.0.0\n本次更新说明\n- 下载 `PixEz-MIUIX-v1.0.0.apk`\n详见 [仓库](https://github.com/137458/pixez-miuix) & 文档。",
            htmlFragmentToMarkdown(html),
        )
        assertEquals("", htmlFragmentToMarkdown("   "), "空白片段应归一为空串")
    }

    @Test
    fun `parseExpandedAssetApk 取首个 apk 直链并补齐绝对地址与摘要`() {
        val asset = parseExpandedAssetApk(expandedAssetsHtml)

        assertTrue(asset != null, "含 apk 直链的片段应解析成功")
        assertEquals(
            "https://github.com/137458/pixez-miuix/releases/download/v99.0.0/PixEz-MIUIX-v99.0.0.apk",
            asset.browser_download_url,
        )
        assertEquals("PixEz-MIUIX-v99.0.0.apk", asset.name)
        assertEquals("sha256:dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd", asset.digest)
        assertEquals(8_430_551L, asset.size, "8.04 MB 应换算为 8.04 * 1024 * 1024 字节")
    }

    @Test
    fun `parseExpandedAssetApk 无 apk 时返回 null`() {
        assertNull(parseExpandedAssetApk("<div><a href=\"/137458/pixez-miuix/releases/download/v1.0.0/src.zip\">src.zip</a></div>"))
    }
}
