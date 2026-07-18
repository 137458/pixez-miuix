package com.perol.pixez.shared.data.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * M2 模型序列化单元测试。
 * 使用内联 JSON 验证核心网络模型能否被正确反序列化，并保持往返一致。
 */
class ModelSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun `Account OAuth response round trip`() {
        val raw = """
            {
              "response": {
                "access_token": "access_123",
                "expires_in": 3600,
                "token_type": "Bearer",
                "scope": "",
                "refresh_token": "refresh_456",
                "user": {
                  "profile_image_urls": {
                    "px_16x16": "https://i.pximg.net/u/16.jpg",
                    "px_50x50": "https://i.pximg.net/u/50.jpg",
                    "px_170x170": "https://i.pximg.net/u/170.jpg"
                  },
                  "id": "12345",
                  "name": "test_user",
                  "account": "test_account",
                  "mail_address": "test@example.com",
                  "is_premium": false,
                  "x_restrict": 0,
                  "is_mail_authorized": true
                }
              }
            }
        """.trimIndent()

        val account = json.decodeFromString<Account>(raw)
        assertEquals("access_123", account.response.accessToken)
        assertEquals("test_user", account.response.user.name)
        assertEquals("https://i.pximg.net/u/50.jpg", account.response.user.profileImageUrls.px50x50)

        val encoded = json.encodeToString(Account.serializer(), account)
        assertTrue(encoded.contains("access_token"))
    }

    @Test
    fun `Illust decode with nested pages`() {
        val raw = """
            {
              "id": 98765432,
              "title": "test illust",
              "type": "illust",
              "image_urls": {
                "square_medium": "https://i.pximg.net/c/360x360_70/img-master/img/1.jpg",
                "medium": "https://i.pximg.net/c/540x540_70/img-master/img/1.jpg",
                "large": "https://i.pximg.net/c/600x600/img-master/img/1.jpg"
              },
              "caption": "",
              "restrict": 0,
              "user": {
                "id": 12345,
                "name": "artist",
                "account": "artist_account",
                "profile_image_urls": {
                  "medium": "https://i.pximg.net/u/170.jpg"
                },
                "is_followed": false
              },
              "tags": [
                { "name": "tag_a", "translated_name": "タグA" },
                { "name": "tag_b" }
              ],
              "tools": [],
              "create_date": "2024-01-01T00:00:00+09:00",
              "page_count": 2,
              "width": 1200,
              "height": 900,
              "sanity_level": 2,
              "x_restrict": 0,
              "meta_single_page": {},
              "meta_pages": [
                {
                  "image_urls": {
                    "square_medium": "https://i.pximg.net/c/360x360_70/img-master/img/1_p0.jpg",
                    "medium": "https://i.pximg.net/c/540x540_70/img-master/img/1_p0.jpg",
                    "large": "https://i.pximg.net/c/600x600/img-master/img/1_p0.jpg",
                    "original": "https://i.pximg.net/img-original/img/1_p0.jpg"
                  }
                }
              ],
              "total_view": 100,
              "total_bookmarks": 10,
              "is_bookmarked": false,
              "visible": true,
              "is_muted": false,
              "illust_ai_type": 0,
              "total_comments": 0
            }
        """.trimIndent()

        val illust = json.decodeFromString<Illust>(raw)
        assertEquals(98765432, illust.id)
        assertEquals(2, illust.pageCount)
        assertEquals(1, illust.metaPages.size)
        assertEquals("tag_a", illust.tags.first().name)
        assertEquals("タグA", illust.tags.first().translatedName)
    }

    @Test
    fun `Comment response decode`() {
        val raw = """
            {
              "total_comments": 3,
              "comments": [
                {
                  "id": 111,
                  "comment": "nice",
                  "date": "2024-01-01T00:00:00+09:00",
                  "user": {
                    "id": 12345,
                    "name": "commenter",
                    "account": "commenter_account",
                    "profile_image_urls": { "medium": "https://i.pximg.net/u/170.jpg" }
                  },
                  "has_replies": false
                }
              ],
              "next_url": "https://app-api.pixiv.net/v1/illust/comments?illust_id=1"
            }
        """.trimIndent()

        val response = json.decodeFromString<CommentResponse>(raw)
        assertEquals(3, response.totalComments)
        assertNotNull(response.comments)
        assertEquals("commenter", response.comments.first().user?.name)
    }

    @Test
    fun `Bookmark detail decode`() {
        val raw = """
            {
              "bookmark_detail": {
                "is_bookmarked": true,
                "tags": [
                  { "name": "tag_a", "is_registered": true }
                ],
                "restrict": "public"
              }
            }
        """.trimIndent()

        val detail = json.decodeFromString<BookmarkDetailResponse>(raw)
        assertTrue(detail.bookmarkDetail.isBookmarked)
        assertEquals("public", detail.bookmarkDetail.restrict)
    }
}
