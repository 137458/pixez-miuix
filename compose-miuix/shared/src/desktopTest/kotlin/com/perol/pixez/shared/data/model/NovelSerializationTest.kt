package com.perol.pixez.shared.data.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Novel 模型反序列化容错测试：pixiv API 部分响应会将 caption / total_* 等字段置 null，
 * 与 [Illust.totalComments]（Int? = null）同源。模型字段必须有默认值配合
 * coerceInputValues 才能容忍，否则单条脏数据导致整页解码失败。
 *
 * Json 配置与 PixivHttpClient 的 ContentNegotiation 保持一致。
 */
class NovelSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    @Test
    fun `Novel 容忍 caption 与 total 计数类字段为 null`() {
        val payload = """
            {
              "id": 1,
              "title": "t",
              "caption": null,
              "restrict": 0,
              "x_restrict": 0,
              "is_original": false,
              "image_urls": {"square_medium": "s", "medium": "m", "large": "l"},
              "create_date": "2026-01-01T00:00:00+09:00",
              "tags": [{"name": "tag", "translated_name": null, "added_by_uploaded_user": false}],
              "page_count": 1,
              "text_length": 100,
              "user": {
                "id": 2,
                "name": "u",
                "account": "acc",
                "profile_image_urls": {"medium": "pm"},
                "is_followed": false
              },
              "is_bookmarked": false,
              "total_bookmarks": null,
              "total_view": null,
              "visible": true,
              "total_comments": null,
              "is_muted": false,
              "is_mypixiv_only": false,
              "is_x_restricted": false,
              "novel_ai_type": 0
            }
        """.trimIndent()

        val novel = json.decodeFromString<Novel>(payload)

        assertEquals("", novel.caption)
        assertEquals(0, novel.totalBookmarks)
        assertEquals(0, novel.totalView)
        assertEquals(0, novel.totalComments)
    }

    @Test
    fun `NovelSeriesNovel 容忍 total 计数类字段为 null`() {
        val payload = """
            {
              "id": 1,
              "title": "t",
              "restrict": 0,
              "x_restrict": 0,
              "image_urls": {"square_medium": "s", "medium": "m", "large": "l"},
              "create_date": "2026-01-01T00:00:00+09:00",
              "tags": [],
              "page_count": 1,
              "text_length": 100,
              "user": {
                "id": 2,
                "name": "u",
                "account": "acc",
                "profile_image_urls": {"medium": "pm"},
                "is_followed": false,
                "is_access_blocking_user": false
              },
              "series": {"id": 3, "title": "series"},
              "is_bookmarked": false,
              "total_bookmarks": null,
              "total_view": null,
              "visible": true,
              "total_comments": null,
              "is_muted": false,
              "is_mypixiv_only": false,
              "is_x_restricted": false,
              "novel_ai_type": 0
            }
        """.trimIndent()

        val novel = json.decodeFromString<NovelSeriesNovel>(payload)

        assertEquals(0, novel.totalBookmarks)
        assertEquals(0, novel.totalView)
        assertEquals(0, novel.totalComments)
    }

    @Test
    fun `NovelSeriesFirstNovel 容忍 total 计数类字段为 null`() {
        val payload = """
            {
              "id": 1,
              "title": "t",
              "caption": null,
              "restrict": 0,
              "x_restrict": 0,
              "is_original": false,
              "image_urls": {"square_medium": "s", "medium": "m", "large": "l"},
              "create_date": "2026-01-01T00:00:00+09:00",
              "tags": [],
              "page_count": 1,
              "text_length": 100,
              "user": {
                "id": 2,
                "name": "u",
                "account": "acc",
                "profile_image_urls": {"medium": "pm"},
                "is_followed": false,
                "is_access_blocking_user": false
              },
              "series": {"id": 3, "title": "series"},
              "is_bookmarked": false,
              "total_bookmarks": null,
              "total_view": null,
              "visible": true,
              "total_comments": null,
              "novel_ai_type": 0
            }
        """.trimIndent()

        val novel = json.decodeFromString<NovelSeriesFirstNovel>(payload)

        assertEquals("", novel.caption)
        assertEquals(0, novel.totalBookmarks)
        assertEquals(0, novel.totalView)
        assertEquals(0, novel.totalComments)
    }

    @Test
    fun `NovelSeriesFirstNovel 的 is_x_restricted 使用 API 实际小写键名`() {
        val payload = """
            {
              "id": 1,
              "title": "t",
              "caption": "c",
              "restrict": 0,
              "x_restrict": 1,
              "is_original": false,
              "image_urls": {"square_medium": "s", "medium": "m", "large": "l"},
              "create_date": "2026-01-01T00:00:00+09:00",
              "tags": [],
              "page_count": 1,
              "text_length": 100,
              "user": {
                "id": 2,
                "name": "u",
                "account": "acc",
                "profile_image_urls": {"medium": "pm"},
                "is_followed": false,
                "is_access_blocking_user": false
              },
              "series": {"id": 3, "title": "series"},
              "is_bookmarked": false,
              "total_bookmarks": 0,
              "total_view": 0,
              "visible": true,
              "total_comments": 0,
              "is_x_restricted": true,
              "novel_ai_type": 0
            }
        """.trimIndent()

        val novel = json.decodeFromString<NovelSeriesFirstNovel>(payload)

        assertTrue(novel.isXRestricted == true, "is_x_restricted=true 应被解析（原 @SerialName 大小写失配导致恒为 null）")
    }
}
