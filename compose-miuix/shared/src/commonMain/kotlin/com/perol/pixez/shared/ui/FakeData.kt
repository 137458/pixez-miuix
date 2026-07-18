package com.perol.pixez.shared.ui

import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.IllustProfileImageUrls
import com.perol.pixez.shared.data.model.IllustTag
import com.perol.pixez.shared.data.model.IllustUser
import com.perol.pixez.shared.data.model.ImageUrls
import com.perol.pixez.shared.data.model.MetaPage
import com.perol.pixez.shared.data.model.MetaSinglePage
import com.perol.pixez.shared.data.model.Profile
import com.perol.pixez.shared.data.model.ProfilePublicity
import com.perol.pixez.shared.data.model.UserDetail
import com.perol.pixez.shared.data.model.Workspace

/**
 * M3 阶段用于验证 UI 布局的 mock 数据。
 * M4 接入真实网络仓库后，这些样本数据仅保留在单元测试或预览中。
 */
object FakeData {

    /**
     * 生成若干张示例插画，用于首页/推荐/排行榜等网格展示。
     */
    fun illusts(count: Int = 20): List<Illust> = List(count) { index ->
        val id = 100000 + index
        val width = 1200
        val height = if (index % 3 == 0) 1800 else 1600
        val authorId = 200000 + (index % 5)
        Illust(
            id = id,
            title = "示例作品 ${index + 1}",
            type = "illust",
            imageUrls = ImageUrls(
                squareMedium = sampleImageUrl(id, width, height, "square_medium"),
                medium = sampleImageUrl(id, width, height, "medium"),
                large = sampleImageUrl(id, width, height, "large"),
            ),
            caption = "这是一段用于 M3 UI 验证的示例说明文字，第 ${index + 1} 张。",
            restrict = 0,
            user = IllustUser(
                id = authorId,
                name = "画师 ${(authorId % 5) + 1}",
                account = "painter_${(authorId % 5) + 1}",
                profileImageUrls = IllustProfileImageUrls(
                    medium = sampleAvatarUrl(authorId),
                ),
                comment = null,
                isFollowed = index % 2 == 0,
            ),
            tags = listOf(
                IllustTag(name = "オリジナル", translatedName = "原创"),
                IllustTag(name = "女の子", translatedName = "女孩子"),
                IllustTag(name = "風景", translatedName = "风景"),
            ),
            tools = emptyList(),
            createDate = "2026-07-18T12:00:00+09:00",
            pageCount = if (index % 7 == 0) 3 else 1,
            width = width,
            height = height,
            sanityLevel = 2,
            xRestrict = 0,
            metaSinglePage = MetaSinglePage(
                originalImageUrl = sampleImageUrl(id, width, height, "original"),
            ),
            metaPages = emptyList(),
            totalView = 1000 + index * 100,
            totalBookmarks = 200 + index * 20,
            isBookmarked = index % 4 == 0,
            visible = true,
            isMuted = false,
            illustAIType = 0,
            series = null,
            illustBookStyle = 0,
            totalComments = 10 + index,
        )
    }

    /**
     * 示例用户详情，用于用户详情页 UI 验证。
     */
    fun userDetail(): UserDetail {
        val user = IllustUser(
            id = 200001,
            name = "示例画师",
            account = "sample_painter",
            profileImageUrls = IllustProfileImageUrls(
                medium = sampleAvatarUrl(200001),
            ),
            comment = "这是示例画师的个人简介，用于 M3 UI 验证。",
            isFollowed = false,
        )
        return UserDetail(
            user = user,
            profile = Profile(
                webpage = "https://example.com",
                gender = "male",
                birth = "1990-01-01",
                birthDay = "01-01",
                birthYear = 1990,
                region = "Tokyo",
                addressId = 13,
                countryCode = "JP",
                job = "Illustrator",
                jobId = 1,
                totalFollowUsers = 150,
                totalMypixivUsers = 10,
                totalIllusts = 42,
                totalManga = 3,
                totalNovels = 0,
                totalIllustBookmarksPublic = 500,
                totalIllustSeries = 2,
                totalNovelSeries = 0,
                backgroundImageUrl = sampleImageUrl(999999, 1920, 600, "large"),
                twitterAccount = null,
                twitterUrl = null,
                pawooUrl = null,
                isPremium = false,
                isUsingCustomProfileImage = false,
            ),
            profilePublicity = ProfilePublicity(
                gender = "public",
                region = "public",
                birthDay = "public",
                birthYear = "public",
                job = "public",
                pawoo = false,
            ),
            workspace = Workspace(
                pc = "",
                monitor = "",
                tool = "",
                scanner = "",
                tablet = "",
                mouse = "",
                printer = "",
                desktop = "",
                music = "",
                desk = "",
                chair = "",
                comment = "",
                workspaceImageUrl = null,
            ),
        )
    }

    /**
     * 热门搜索标签样例。
     */
    fun trendTags(): List<String> = listOf(
        "オリジナル",
        "女の子",
        "風景",
        "FGO",
        "初音ミク",
        "ホロライブ",
        "鬼滅の刃",
        "葬送のフリーレン",
    )

    /**
     * 搜索历史样例。
     */
    fun searchHistory(): List<String> = listOf(
        "原神",
        "崩壊：スターレイル",
        "ブルーアーカイブ",
    )

    private fun sampleImageUrl(id: Int, width: Int, height: Int, type: String): String {
        // 使用 picsum 作为跨平台占位图服务，避免依赖真实 Pixiv CDN。
        return "https://picsum.photos/seed/pixez${id}_${type}/${width}/${height}"
    }

    private fun sampleAvatarUrl(id: Int): String {
        return "https://picsum.photos/seed/pixez_avatar_${id}/200/200"
    }
}
