package com.perol.pixez.shared.ui.state

import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.IllustProfileImageUrls
import com.perol.pixez.shared.data.model.IllustTag
import com.perol.pixez.shared.data.model.IllustUser
import com.perol.pixez.shared.data.model.ImageUrls
import com.perol.pixez.shared.data.model.MangaSeries
import com.perol.pixez.shared.data.model.MetaSinglePage
import com.perol.pixez.shared.data.model.SpotlightArticle

/**
 * 页面状态算子测试共用的数据构造器。
 *
 * 被测状态机只消费领域模型，构造函数字段多且大多与断言无关，
 * 集中一处构造可让各测试类只表达「差异化数据」（ID、分级、方向、类型），而非字段噪音。
 */
internal object StateTestFixtures {

    fun illust(
        id: Long,
        xRestrict: Int = 0,
        type: String = "illust",
        width: Int = 1000,
        height: Int = 1000,
        isBookmarked: Boolean = false,
    ): Illust = Illust(
        id = id,
        title = "Illust $id",
        type = type,
        imageUrls = ImageUrls(
            squareMedium = "https://example.com/$id/sq.jpg",
            medium = "https://example.com/$id/med.jpg",
            large = "https://example.com/$id/large.jpg",
        ),
        caption = "",
        restrict = 0,
        user = IllustUser(
            id = 1L,
            name = "Artist",
            account = "artist",
            profileImageUrls = IllustProfileImageUrls(medium = "https://example.com/avatar.jpg"),
        ),
        tags = listOf(IllustTag(name = "original")),
        tools = emptyList(),
        createDate = "2026-11-15T00:00:00+09:00",
        pageCount = 1,
        width = width,
        height = height,
        sanityLevel = 2,
        xRestrict = xRestrict,
        metaSinglePage = MetaSinglePage(originalImageUrl = "https://example.com/$id/orig.jpg"),
        metaPages = emptyList(),
        totalView = 0,
        totalBookmarks = 0,
        isBookmarked = isBookmarked,
        visible = true,
        isMuted = false,
        illustAIType = 0,
    )

    fun series(id: Long): MangaSeries = MangaSeries(
        latestContentId = id * 10,
        id = id,
        title = "Series $id",
        publishedContentCount = 1,
    )

    fun spotlightArticle(id: Long): SpotlightArticle = SpotlightArticle(
        id = id,
        title = "Article $id",
        pureTitle = "Pure $id",
        thumbnail = "https://example.com/$id/thumb.jpg",
        articleUrl = "https://www.pixivision.net/a/$id",
        publishDate = "2026-11-15",
    )
}
