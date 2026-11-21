package com.perol.pixez.shared.data.repository

import com.perol.pixez.shared.data.settings.SettingsRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pixivision 特辑列表缓存的语言维度（架构审查 DN17）。
 *
 * 抓取路径按语言拼接（/zh/c/novels、/ja/c/novels ...），而缓存键曾只有分类：
 * 切换语言后同一分类会命中旧语言快照。本用例断言「切语言必须重新抓取」。
 */
class PixivisionCacheLanguageTest {

    private val webPaths = mutableListOf<String>()
    private lateinit var settings: SettingsRepository

    private val novelsHtml = """
        <html><body><div>
            <article class="_article-card spotlight">
                <div class="arc__thumbnail-container">
                    <a href="/zh/a/9001"><div class="_thumbnail" style="background-image: url(https://i.pximg.net/c/1200x630/9001.jpg)"></div></a>
                </div>
                <div class="arc__title-container"><h2 class="arc__title"><a href="/zh/a/9001">测试特辑甲</a></h2></div>
                <div class="arc__footer-date-pr"><time datetime="2026-02-01">2026.02.01</time></div>
            </article>
        </div></body></html>
    """.trimIndent()

    /** 语言码：1=zh、3=ja（见 SettingsRepository.languageNum 与 Pixivision 语言路径映射）。 */
    private fun repository(languageNum: Int): PixivisionRepository {
        webPaths.clear()
        settings = SettingsRepository(InMemorySettings()).apply { this.languageNum = languageNum }
        val webClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    webPaths.add(request.url.encodedPath)
                    respond(
                        novelsHtml,
                        HttpStatusCode.OK,
                        headersOf("Content-Type", ContentType.Text.Html.toString()),
                    )
                }
            }
        }
        return PixivisionRepository(
            apiClient = HttpClient(),
            webClient = webClient,
            settingsRepository = settings,
        )
    }

    @Test
    fun `同一语言重复读取命中缓存不重复抓取`() = runBlocking {
        val repository = repository(languageNum = 1)

        repository.getSpotlightArticles(category = "novel")
        repository.getSpotlightArticles(category = "novel")

        assertEquals(listOf("/zh/c/novels"), webPaths, "同语言已有快照时不得重复抓取")
        // 末句保持 Unit 返回，JUnit4 要求 @Test 方法返回 void。
        assertTrue(repository.getCachedSpotlightArticles("novel") != null, "首页快照应已缓存")
    }

    @Test
    fun `切换语言后同分类不命中旧语言快照`() = runBlocking {
        val repository = repository(languageNum = 1)
        repository.getSpotlightArticles(category = "novel")
        assertEquals(listOf("/zh/c/novels"), webPaths)

        // 用户把语言切到日语后，同一分类必须重新抓取 /ja/ 路径，而不是返回中文旧快照。
        settings.languageNum = 3
        repository.getSpotlightArticles(category = "novel")

        assertEquals(
            listOf("/zh/c/novels", "/ja/c/novels"),
            webPaths,
            "语言变更后必须重新抓取，不得命中旧语言快照",
        )
        assertTrue(repository.getCachedSpotlightArticles("novel") != null, "新语言快照应已缓存")
    }
}
