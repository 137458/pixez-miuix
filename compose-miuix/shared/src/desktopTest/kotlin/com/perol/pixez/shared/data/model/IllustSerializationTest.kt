package com.perol.pixez.shared.data.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Illust / UserDetail 模型反序列化容错测试：pixiv API 部分响应会将 caption / tools /
 * total_* / workspace 自由文本等字段置 null，模型字段必须有默认值配合 coerceInputValues
 * 才能容忍，否则单条脏数据导致搜索、关注流、排行榜、用户详情整页解码失败。
 *
 * Json 配置与 PixivHttpClient 的 ContentNegotiation 保持一致。
 */
class IllustSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    @Test
    fun `Illust 容忍 caption tools 与 total 计数类字段为 null`() {
        val payload = """
            {
              "id": 1,
              "title": "t",
              "type": "illust",
              "image_urls": {"square_medium": "s", "medium": "m", "large": "l"},
              "caption": null,
              "restrict": 0,
              "user": {
                "id": 2,
                "name": "u",
                "account": "acc",
                "profile_image_urls": {"medium": "pm"}
              },
              "tags": [{"name": "tag", "translated_name": null}],
              "tools": null,
              "create_date": "2026-01-01T00:00:00+09:00",
              "page_count": 1,
              "width": 100,
              "height": 100,
              "sanity_level": 2,
              "x_restrict": 0,
              "meta_pages": [],
              "total_view": null,
              "total_bookmarks": null,
              "is_bookmarked": false,
              "visible": true,
              "is_muted": false,
              "illust_ai_type": 0
            }
        """.trimIndent()

        val illust = json.decodeFromString<Illust>(payload)

        assertEquals("", illust.caption)
        assertEquals(emptyList(), illust.tools)
        assertEquals(0, illust.totalView)
        assertEquals(0, illust.totalBookmarks)
    }

    @Test
    fun `UserDetail 容忍 Profile 计数与 Workspace 文本字段为 null`() {
        val payload = """
            {
              "user": {
                "id": 2,
                "name": "u",
                "account": "acc",
                "profile_image_urls": {"medium": "pm"}
              },
              "profile": {
                "total_follow_users": null,
                "total_mypixiv_users": null,
                "total_illusts": null,
                "total_manga": null,
                "total_novels": null,
                "total_illust_bookmarks_public": null,
                "total_illust_series": null,
                "total_novel_series": null,
                "is_premium": false,
                "is_using_custom_profile_image": false
              },
              "profile_publicity": {
                "gender": "public",
                "region": "public",
                "birth_day": "public",
                "birth_year": "public",
                "job": "public",
                "pawoo": true
              },
              "workspace": {
                "pc": null,
                "monitor": null,
                "tool": null,
                "scanner": null,
                "tablet": null,
                "mouse": null,
                "printer": null,
                "desktop": null,
                "music": null,
                "desk": null,
                "chair": null,
                "comment": null
              }
            }
        """.trimIndent()

        val detail = json.decodeFromString<UserDetail>(payload)

        assertEquals(0, detail.profile.totalFollowUsers)
        assertEquals(0, detail.profile.totalIllusts)
        assertEquals(0, detail.profile.totalNovelSeries)
        assertEquals("", detail.workspace.pc)
        assertEquals("", detail.workspace.comment)
    }
}
