package com.perol.pixez.shared.data.repository

import com.perol.pixez.shared.data.model.AmWork
import com.perol.pixez.shared.data.model.SpotlightArticle
import com.perol.pixez.shared.data.model.SpotlightDetail
import com.perol.pixez.shared.network.PixivHosts
import com.perol.pixez.shared.utils.HtmlUtils

// 标题清洗与 meta 提取
private val FULLWIDTH_BRACKET_PATTERN = Regex("""【.*?】""")
private val SQUARE_BRACKET_PATTERN = Regex("""\[.*?\]""")
private val OG_TITLE_PATTERN = Regex("""<meta\s+property=["']og:title["']\s+content=["'](.*?)["']""", RegexOption.IGNORE_CASE)
private val TWITTER_TITLE_PATTERN = Regex("""<meta\s+name=["']twitter:title["']\s+content=["'](.*?)["']""", RegexOption.IGNORE_CASE)
private val H1_AM_TITLE_PATTERN = Regex("""<h1[^>]*class=["'][^"']*am__title[^"']*["'][^>]*>(.*?)</h1>""", RegexOption.IGNORE_CASE)
private val HTML_TITLE_PATTERN = Regex("""<title>(.*?)</title>""", RegexOption.IGNORE_CASE)

// 描述提取
private val HEADER_PATTERN = Regex("""<header[^>]*>([\s\S]*?)</header>""", RegexOption.IGNORE_CASE)
private val P_TAG_PATTERN = Regex("""<p[^>]*>([\s\S]*?)</p>""", RegexOption.IGNORE_CASE)
private val FEATURE_INTRO_PATTERN = Regex(
    """<div\b[^>]*class=["'][^"']*(?:_feature-article-body__paragraph|fab__paragraph)[^"']*["'][^>]*>([\s\S]*?)</div>""",
    RegexOption.IGNORE_CASE,
)
private val P_OR_DIV_TAG_PATTERN = Regex("""<(?:p|div)[^>]*>([\s\S]*?)</(?:p|div)>""", RegexOption.IGNORE_CASE)
private val OG_DESCRIPTION_PATTERN = Regex("""<meta\s+property=["']og:description["']\s+content=["'](.*?)["']""", RegexOption.IGNORE_CASE)
private val META_DESCRIPTION_PATTERN = Regex("""<meta\s+name=["']description["']\s+content=["'](.*?)["']""", RegexOption.IGNORE_CASE)

// 封面图提取
private val OG_IMAGE_PATTERN = Regex("""<meta\s+property=["']og:image["']\s+content=["'](.*?)["']""", RegexOption.IGNORE_CASE)
private val TWITTER_IMAGE_PATTERN = Regex("""<meta\s+name=["']twitter:image(?::src)?["']\s+content=["'](.*?)["']""", RegexOption.IGNORE_CASE)

// 作品区块与作品字段提取
private val WORK_ARTICLE_PATTERN = Regex("""<article\b[^>]*class=["'][^"']*\bam__work\b[^"']*["'][^>]*>([\s\S]*?)</article>""", RegexOption.IGNORE_CASE)
private val WORK_DIV_PATTERN = Regex("""<div\b[^>]*class=["'][^"']*\bam__work\b[^"']*["'][^>]*>([\s\S]*?)(?=<div\b[^>]*class=["'][^"']*\bam__work\b|</main>|</body>|$)""", RegexOption.IGNORE_CASE)
private val ARTWORK_URL_PATTERN = Regex("""https?://(?:www\.)?pixiv\.net/(?:[a-z]{2}(?:-[a-z]{2})?/)?artworks/(\d+)""", RegexOption.IGNORE_CASE)
private val MEMBER_ILLUST_URL_PATTERN = Regex("""https?://(?:www\.)?pixiv\.net/member_illust\.php\?(?:[^"'\s]*&)?illust_id=(\d+)""", RegexOption.IGNORE_CASE)
private val USER_URL_PATTERN = Regex("""https?://(?:www\.)?pixiv\.net/(?:[a-z]{2}(?:-[a-z]{2})?/)?users/(\d+)""", RegexOption.IGNORE_CASE)
private val MEMBER_URL_PATTERN = Regex("""https?://(?:www\.)?pixiv\.net/member\.php\?(?:[^"'\s]*&)?id=(\d+)""", RegexOption.IGNORE_CASE)
private val WORK_TITLE_TAG_PATTERN = Regex("""<h[34][^>]*>(.*?)</h[34]>""", RegexOption.IGNORE_CASE)
private val WORK_TITLE_CLASS_PATTERN = Regex("""class=["'][^"']*(?:am__title|work__title|illust__title)[^"']*["'][^>]*>(.*?)<""", RegexOption.IGNORE_CASE)
private val IMG_ALT_PATTERN = Regex("""alt=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
private val IMG_TAG_PATTERN = Regex("""<img\b[^>]*>""", RegexOption.IGNORE_CASE)
private val PXIMG_URL_PATTERN = Regex("""https?://(?:i|s)\.pximg\.net/c/[^\s"'<>]+\.(?:jpg|png|jpeg|webp)""", RegexOption.IGNORE_CASE)
private val IMG_URL_ATTRIBUTE_PATTERN = Regex("""(?:data-src|data-original|data-url|data-lazy-src|src)=["']([^"']+)["']""", RegexOption.IGNORE_CASE)

// 画师名提取
private val USER_LINK_PATTERN = Regex("""<a\b[^>]*users/\d+[^>]*>([\s\S]*?)</a>""", RegexOption.IGNORE_CASE)
private val USER_NAME_TAG_PATTERN = Regex(
    """<(?:p|span)[^>]*class=["'][^"']*(?:userName|user-name|am__user|am__work__userName|am__work-user|author|illustrator)[^"']*["'][^>]*>([\s\S]*?)</(?:p|span)>""",
    RegexOption.IGNORE_CASE,
)

// 特辑文章卡片提取
private val CURRENT_ARTICLE_ID_PATTERN = Regex("""/a/(\d+)""")
private val SUB_ARTICLE_CARD_PATTERN = Regex(
    """<div\b[^>]*class=["'][^"']*_feature-article-body__article_card[^"']*["'][^>]*>([\s\S]*?)(?=<div\b[^>]*class=["'][^"']*article-item|<footer|</body>|$)""",
    RegexOption.IGNORE_CASE,
)
private val ARTICLE_CARD_PATTERN = Regex("""<article\b[^>]*class=["'][^"']*_article-card[^"']*["'][^>]*>([\s\S]*?)</article>""", RegexOption.IGNORE_CASE)
private val ARTICLE_LINK_PATTERN = Regex("""href=["']([^"']*(?:/zh|/en|/ja|/ko|/zh-tw)?/a/(\d+)[^"']*)["']""", RegexOption.IGNORE_CASE)
private val ARTICLE_TITLE_PATTERN = Regex("""<h[1-4][^>]*class=["'][^"']*arc__title[^"']*["'][^>]*>[\s\S]*?<a[^>]*>([\s\S]*?)</a></h[1-4]>""", RegexOption.IGNORE_CASE)
private val ARTICLE_TITLE_FALLBACK_PATTERN = Regex("""<h[1-4][^>]*>[\s\S]*?<a[^>]*>([\s\S]*?)</a></h[1-4]>""", RegexOption.IGNORE_CASE)
private val ANCHOR_TITLE_PATTERN = Regex("""<a[^>]*title=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
private val BACKGROUND_IMAGE_PATTERN = Regex("""url\(\s*["']?([^"')]+)["']?\s*\)""", RegexOption.IGNORE_CASE)
private val IMG_SOURCE_PATTERN = Regex("""<img\b[^>]*(?:data-src|src)=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
private val TIME_DATETIME_PATTERN = Regex("""<time[^>]*datetime=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
private val TIME_TAG_PATTERN = Regex("""<time[^>]*>([\s\S]*?)</time>""", RegexOption.IGNORE_CASE)

// 通用清洗
private val HTML_TAG_PATTERN = Regex("""<[^>]*>""")
private val WHITESPACE_PATTERN = Regex("""\s+""")

/**
 * 健壮的 Pixivision 特辑 HTML 解析器。
 * 复刻原版 Flutter PixEz 「SoupStore」的解析逻辑，并针对 Pixivision 最新网页排版及合集特辑进行深度优化。
 */
object PixivisionParser {

    fun parse(html: String, rawUrl: String): SpotlightDetail {
        val title = extractTitle(html)
        val description = extractDescription(html)
        val coverUrl = extractCoverUrl(html)
        val works = extractWorks(html)
        // 仅当文章明确包含合集文章卡片 (_feature-article-body__article_card) 或在无作品时才提取子特辑，
        // 杜绝普通特辑把底部的相关/推荐文章误当成子特辑
        val subArticles = if (html.contains("_feature-article-body__article_card") || (works.isEmpty() && html.contains("_article-card"))) {
            extractSubArticles(html, rawUrl)
        } else {
            emptyList()
        }

        return SpotlightDetail(
            title = title,
            pureTitle = cleanTitle(title),
            description = description,
            coverUrl = coverUrl,
            works = works,
            subArticles = subArticles,
            rawUrl = rawUrl,
        )
    }

    private fun cleanTitle(title: String): String {
        return title
            .replace(FULLWIDTH_BRACKET_PATTERN, "")
            .replace(SQUARE_BRACKET_PATTERN, "")
            .trim()
    }

    private fun extractTitle(html: String): String {
        val ogTitleMatch = OG_TITLE_PATTERN.find(html)
            ?: TWITTER_TITLE_PATTERN.find(html)
            ?: H1_AM_TITLE_PATTERN.find(html)
            ?: HTML_TITLE_PATTERN.find(html)

        val rawTitle = ogTitleMatch?.groupValues?.get(1)?.trim().orEmpty()
        return decodeHtml(rawTitle.replace(" - pixivision", "").replace("[pixivision]", "").trim())
    }

    private fun extractDescription(html: String): String {
        // 1. 优先从 header / am__lead 提取段落文本（与原版 SoupStore 逻辑一致）
        val headerMatch = HEADER_PATTERN.find(html)
        if (headerMatch != null) {
            val pTags = P_TAG_PATTERN
                .findAll(headerMatch.groupValues[1])
                .map { decodeHtml(stripHtml(it.groupValues[1])).trim() }
                .filter { it.isNotBlank() }
                .toList()
            if (pTags.isNotEmpty()) {
                return pTags.joinToString("\n\n")
            }
        }

        // 2. 特辑合集（_feature-article-body）导语提取
        val featureIntroMatch = FEATURE_INTRO_PATTERN.find(html)
        if (featureIntroMatch != null) {
            val pTags = P_OR_DIV_TAG_PATTERN
                .findAll(featureIntroMatch.groupValues[1])
                .map { decodeHtml(stripHtml(it.groupValues[1])).trim() }
                .filter { it.isNotBlank() && it.length > 5 }
                .distinct()
                .toList()
            if (pTags.isNotEmpty()) {
                return pTags.joinToString("\n\n")
            }
        }

        // 3. 备选：从 og:description 或 meta description 提取
        val ogDescMatch = OG_DESCRIPTION_PATTERN.find(html)
            ?: META_DESCRIPTION_PATTERN.find(html)

        var desc = ogDescMatch?.groupValues?.get(1)?.trim().orEmpty()
        if (desc.startsWith("[pixivision]")) {
            desc = desc.removePrefix("[pixivision]").trim()
        }
        return decodeHtml(desc)
    }

    private fun extractCoverUrl(html: String): String? {
        val ogImageMatch = OG_IMAGE_PATTERN.find(html)
            ?: TWITTER_IMAGE_PATTERN.find(html)

        return ogImageMatch?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun extractWorks(html: String): List<AmWork> {
        val works = mutableListOf<AmWork>()
        val seenArtworkLinks = mutableSetOf<String>()

        // 策略 1：匹配所有 <article ...> ... </article>（Pixivision 官方标准结构）
        for (match in WORK_ARTICLE_PATTERN.findAll(html)) {
            val work = parseWorkFromBlock(match.groupValues[1]) ?: continue
            val artworkLink = work.arworkLink ?: continue
            if (seenArtworkLinks.add(artworkLink)) {
                works.add(work)
            }
        }

        // 策略 2：若无 article 标签，则匹配 <div class="am__work"> 块
        if (works.isEmpty()) {
            for (match in WORK_DIV_PATTERN.findAll(html)) {
                val work = parseWorkFromBlock(match.groupValues[1]) ?: continue
                val artworkLink = work.arworkLink ?: continue
                if (seenArtworkLinks.add(artworkLink)) {
                    works.add(work)
                }
            }
        }

        // 策略 3：备选全局作品扫描
        if (works.isEmpty()) {
            val allArtworks = ARTWORK_URL_PATTERN.findAll(html).map { it.groupValues[1] }.distinct().toList()

            for (artId in allArtworks) {
                val artLink = PixivHosts.PIXIV_ARTWORK_PREFIX + artId
                if (seenArtworkLinks.add(artLink)) {
                    val index = html.indexOf("artworks/$artId")
                    val subStart = (index - 600).coerceAtLeast(0)
                    val subEnd = (index + 1200).coerceAtMost(html.length)
                    val subBlock = html.substring(subStart, subEnd)

                    val work = parseWorkFromBlock(subBlock) ?: AmWork(
                        // 兜底标题用纯符号，避免中文透传到非中文语言的特辑列表。
                        title = "#$artId",
                        arworkLink = artLink,
                        showImage = "https://embed.pixiv.net/spotlight.php?id=$artId",
                    )
                    works.add(work.copy(arworkLink = artLink))
                }
            }
        }

        return works
    }

    private fun parseWorkFromBlock(block: String): AmWork? {
        val artworkMatch = ARTWORK_URL_PATTERN.find(block)
            ?: MEMBER_ILLUST_URL_PATTERN.find(block)

        val artworkId = artworkMatch?.groupValues?.get(1) ?: return null
        val artworkLink = PixivHosts.PIXIV_ARTWORK_PREFIX + artworkId

        val userMatch = USER_URL_PATTERN.find(block)
            ?: MEMBER_URL_PATTERN.find(block)
        val userId = userMatch?.groupValues?.get(1)
        val userLink = userId?.let { PixivHosts.WEB_ORIGIN + "/users/$it" }

        // 提取作品标题：h3、h4、am__title、title="..." 或 img alt
        val titleMatch = WORK_TITLE_TAG_PATTERN.find(block)
            ?: WORK_TITLE_CLASS_PATTERN.find(block)
            ?: IMG_ALT_PATTERN.find(block)
        val title = titleMatch?.let { decodeHtml(stripHtml(it.groupValues[1])).trim() }?.takeIf { it.isNotBlank() } ?: "#$artworkId"

        // 提取画师名字
        val user = extractUserName(block)

        // 从 <img> 标签中精确提取缩略图（优先获取插画预览中图/缩略图，避免直接下载原图大文件）
        val imgTags = IMG_TAG_PATTERN.findAll(block).map { it.value }.toList()

        // 识别插画预览图片标签（排除画师头像和图标）
        val illustImgTag = imgTags.firstOrNull { tag ->
            (tag.contains("illust", ignoreCase = true) || tag.contains("main", ignoreCase = true) || tag.contains("work", ignoreCase = true))
                && !tag.contains("avatar", ignoreCase = true) && !tag.contains("user", ignoreCase = true) && !tag.contains("profile", ignoreCase = true)
        } ?: imgTags.firstOrNull { tag ->
            !tag.contains("avatar", ignoreCase = true) && !tag.contains("user", ignoreCase = true) && !tag.contains("profile", ignoreCase = true)
        }

        // 识别画师头像图片标签
        val userImgTag = imgTags.firstOrNull { tag ->
            tag.contains("avatar", ignoreCase = true) || tag.contains("user", ignoreCase = true) || tag.contains("profile", ignoreCase = true)
        }

        val illustImgUrls = illustImgTag?.let { extractImageUrlsFromTag(it) }.orEmpty()
        val userImgUrls = userImgTag?.let { extractImageUrlsFromTag(it) }.orEmpty()

        // 获取缩略图 URL
        val showImage = illustImgUrls.firstOrNull {
            it.contains(PixivHosts.HOST_PXIMG) ||
                it.contains(PixivHosts.PIXIVISION_BARE_HOST) ||
                it.contains(PixivHosts.PIXIVISION_EMBED_HOST)
        } ?: illustImgUrls.firstOrNull()
          ?: PXIMG_URL_PATTERN.find(block)?.value
          ?: "https://${PixivHosts.PIXIVISION_EMBED_HOST}/spotlight.php?id=$artworkId"

        val userImage = userImgUrls.firstOrNull {
            it.contains("user-profile") || it.contains("custom-profile") || it.contains("profile") || it.contains("avatar")
        } ?: userImgUrls.firstOrNull()

        return AmWork(
            title = title,
            user = user,
            arworkLink = artworkLink,
            userLink = userLink,
            userImage = userImage,
            showImage = showImage,
        )
    }

    private fun extractUserName(block: String): String? {
        // 1. 从 <a href="...users/...">...</a> 链接内部提取纯文本
        val userLinkMatch = USER_LINK_PATTERN.find(block)
        if (userLinkMatch != null) {
            val text = decodeHtml(stripHtml(userLinkMatch.groupValues[1])).trim()
            if (text.isNotBlank() && !text.equals("画师", ignoreCase = true) && !text.equals("artist", ignoreCase = true)) {
                return text
            }
        }

        // 2. 从显式的 <p> 或 <span> 类名提取纯文本
        val pUserMatch = USER_NAME_TAG_PATTERN.find(block)
        if (pUserMatch != null) {
            val text = decodeHtml(stripHtml(pUserMatch.groupValues[1])).trim()
            if (text.isNotBlank() && !text.equals("画师", ignoreCase = true) && !text.equals("artist", ignoreCase = true)) {
                return text
            }
        }

        // 3. 从 block 内的所有 <p> 标签提取
        for (pMatch in P_TAG_PATTERN.findAll(block)) {
            val text = decodeHtml(stripHtml(pMatch.groupValues[1])).trim()
            if (text.isNotBlank() && !text.equals("画师", ignoreCase = true) && !text.equals("artist", ignoreCase = true) && text.length < 50) {
                return text
            }
        }

        return null
    }

    /**
     * 从详情页 HTML 中提取子特辑列表。
     *
     * 优先只在明确的合集卡片容器 (_feature-article-body__article_card) 中取块；
     * 没有该容器时退化为在排除推荐/相关/排行榜区域后的页面里查找 _article-card 卡片。
     * [rawUrl] 用于识别当前文章自身，避免把自己列为子特辑。
     */
    private fun extractSubArticles(html: String, rawUrl: String): List<SpotlightArticle> {
        val currentArticleId = CURRENT_ARTICLE_ID_PATTERN.find(rawUrl)?.groupValues?.get(1)?.toLongOrNull()

        val cardItemMatches = SUB_ARTICLE_CARD_PATTERN.findAll(html).toList()

        val blocks = if (cardItemMatches.isNotEmpty()) {
            cardItemMatches.map { it.groupValues[1] }
        } else {
            // 备用：若无 _feature-article-body__article_card，则在排除所有推荐、相关、排行榜区域后查找
            ARTICLE_CARD_PATTERN.findAll(trimToArticleListSection(html)).map { it.groupValues[1] }.toList()
        }

        return parseArticleBlocks(blocks, excludedId = currentArticleId)
    }

    /**
     * 从 Pixivision 分类列表页（例如 /zh/c/novels 等）HTML 中提取特辑文章列表。
     */
    fun parseArticleList(html: String): List<SpotlightArticle> {
        val cardMatches = ARTICLE_CARD_PATTERN
            .findAll(trimToArticleListSection(html))
            .map { it.groupValues[1] }
            .toList()

        return parseArticleBlocks(cardMatches, excludedId = null)
    }

    /**
     * 把文章卡片区块解析成 [SpotlightArticle] 列表，保持文档顺序。
     *
     * [excludedId] 为需要排除的文章 ID（详情页传当前文章自身，列表页传 null），
     * 同一 ID 只保留首次出现的卡片。
     */
    private fun parseArticleBlocks(blocks: List<String>, excludedId: Long?): List<SpotlightArticle> {
        val articles = mutableListOf<SpotlightArticle>()
        val seenIds = mutableSetOf<Long>()

        for (block in blocks) {
            val linkMatch = ARTICLE_LINK_PATTERN.find(block) ?: continue
            val relOrAbsUrl = linkMatch.groupValues[1]
            val id = linkMatch.groupValues[2].toLongOrNull() ?: continue

            // 短路顺序与逐条实现一致：命中排除项时不做去重登记。
            if (id == excludedId || !seenIds.add(id)) {
                continue
            }

            val articleUrl = if (relOrAbsUrl.startsWith("http")) relOrAbsUrl else "${PixivHosts.PIXIVISION_ORIGIN}${if (relOrAbsUrl.startsWith("/")) "" else "/"}$relOrAbsUrl"

            // 提取标题
            val titleMatch = ARTICLE_TITLE_PATTERN.find(block)
                ?: ARTICLE_TITLE_FALLBACK_PATTERN.find(block)
                ?: ANCHOR_TITLE_PATTERN.find(block)

            val rawTitle = titleMatch?.let { decodeHtml(stripHtml(it.groupValues[1])).trim() }?.takeIf { it.isNotBlank() } ?: "#$id"

            // 封面图：从 style="background-image: url(...)" 或 <img> 提取
            val bgMatch = BACKGROUND_IMAGE_PATTERN.find(block)
            val imgMatch = IMG_SOURCE_PATTERN.find(block)
            val thumbnail = bgMatch?.groupValues?.get(1)?.trim()
                ?: imgMatch?.groupValues?.get(1)?.trim()
                ?: ""

            // 发布日期
            val dateMatch = TIME_DATETIME_PATTERN.find(block)
                ?: TIME_TAG_PATTERN.find(block)
            val publishDate = dateMatch?.let { decodeHtml(stripHtml(it.groupValues[1])).trim() }.orEmpty()

            articles.add(
                SpotlightArticle(
                    id = id,
                    title = rawTitle,
                    pureTitle = cleanTitle(rawTitle),
                    thumbnail = thumbnail,
                    articleUrl = articleUrl,
                    publishDate = publishDate,
                ),
            )
        }

        return articles
    }

    /**
     * 裁掉页面尾部的相关文章、排行榜、推荐与页脚区域，避免把底部的其他特辑误当成文章列表。
     */
    private fun trimToArticleListSection(html: String): String {
        return html
            .substringBefore("<div class=\"_related-articles")
            .substringBefore("<div class=\"_ranking-articles")
            .substringBefore("<div class=\"_recommend")
            .substringBefore("<footer")
    }

    private fun extractImageUrlsFromTag(imgTag: String): List<String> {
        val urls = mutableListOf<String>()
        for (attr in IMG_URL_ATTRIBUTE_PATTERN.findAll(imgTag)) {
            val url = attr.groupValues[1].trim()
            if (url.isNotBlank() && !url.startsWith("data:") && !url.endsWith(".gif") && !url.contains("blank.gif")) {
                urls.add(url)
            }
        }
        return urls
    }

    private fun stripHtml(html: String): String {
        return html.replace(HTML_TAG_PATTERN, " ").replace(WHITESPACE_PATTERN, " ").trim()
    }

    /**
     * 解码 HTML 实体：委托 [HtmlUtils.decodeHtmlEntities]，统一覆盖命名实体与十进制/十六进制数值实体。
     * 解码结果仍做 trim，因为标题与描述文本会直接进入列表展示。
     */
    private fun decodeHtml(text: String): String {
        return HtmlUtils.decodeHtmlEntities(text).trim()
    }
}
