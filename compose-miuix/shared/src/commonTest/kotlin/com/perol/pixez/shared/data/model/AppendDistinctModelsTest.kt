package com.perol.pixez.shared.data.model

import kotlin.test.Test
import kotlin.test.assertEquals

class AppendDistinctModelsTest {

    private fun comment(id: Long?) = Comment(id = id, comment = "c$id")
    private fun userPreview(id: Long) = UserPreview(
        user = IllustUser(
            id = id,
            name = "u$id",
            account = "account$id",
            profileImageUrls = IllustProfileImageUrls(medium = "m$id"),
        ),
        illusts = emptyList(),
        novels = emptyList(),
        isMuted = false,
    )

    // ---------- Comment ----------

    @Test
    fun `comment append filters duplicates by id and keeps order`() {
        val existing = listOf(comment(1), comment(2))
        val newItems = listOf(comment(2), comment(3))
        val merged = existing.appendDistinct(newItems)
        assertEquals(listOf(1L, 2L, 3L), merged.map { it.id })
    }

    @Test
    fun `comment append keeps null-id items without dedup`() {
        val existing = listOf(comment(1), comment(null))
        val newItems = listOf(comment(null), comment(2))
        val merged = existing.appendDistinct(newItems)
        // null id 视为彼此不同，不做过滤
        assertEquals(listOf(1L, null, null, 2L), merged.map { it.id })
    }

    @Test
    fun `comment append with empty inputs returns original references`() {
        val existing = listOf(comment(1))
        assertEquals(existing, existing.appendDistinct(emptyList()))
        // 追加到空列表等价于新列表本身
        assertEquals(listOf(comment(1)), emptyList<Comment>().appendDistinct(listOf(comment(1))))
    }

    // ---------- UserPreview ----------

    @Test
    fun `user preview append filters duplicates by user id and keeps order`() {
        val existing = listOf(userPreview(10), userPreview(20))
        val newItems = listOf(userPreview(20), userPreview(30))
        val merged = existing.appendDistinct(newItems)
        assertEquals(listOf(10L, 20L, 30L), merged.map { it.user.id })
    }

    @Test
    fun `user preview append with empty inputs returns original references`() {
        val existing = listOf(userPreview(10))
        assertEquals(existing, existing.appendDistinct(emptyList()))
        // 追加到空列表等价于新列表本身
        assertEquals(listOf(userPreview(10)), emptyList<UserPreview>().appendDistinct(listOf(userPreview(10))))
    }

    // ---------- Novel ----------

    private fun novel(id: Long) = Novel(
        id = id,
        title = "title$id",
        restrict = 0,
        xRestrict = 0,
        isOriginal = false,
        imageUrls = NovelImageUrls("", "", ""),
        createDate = "",
        tags = emptyList(),
        pageCount = 1,
        textLength = 100,
        user = NovelUser(id = 1L, name = "u", account = "a", profileImageUrls = NovelProfileImageUrls(""), isFollowed = false),
        isBookmarked = false,
        visible = true,
        isMuted = false,
        isMypixivOnly = false,
        isXRestricted = false,
        novelAIType = 0,
    )

    @Test
    fun `novel append filters duplicates by id and keeps order`() {
        val existing = listOf(novel(100), novel(200))
        val newItems = listOf(novel(200), novel(300))
        val merged = existing.appendDistinct(newItems)
        assertEquals(listOf(100L, 200L, 300L), merged.map { it.id })
    }

    @Test
    fun `novel append with empty inputs returns original references`() {
        val existing = listOf(novel(100))
        assertEquals(existing, existing.appendDistinct(emptyList()))
        assertEquals(listOf(novel(100)), emptyList<Novel>().appendDistinct(listOf(novel(100))))
    }
}
