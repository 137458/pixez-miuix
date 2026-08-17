package com.perol.pixez.shared.data.repository

import com.perol.pixez.shared.data.model.AmWork
import com.perol.pixez.shared.data.model.SpotlightArticle
import com.perol.pixez.shared.data.model.SpotlightDetail

/**
 * 健壮的 Pixivision 特辑 HTML 解析器。
 * 复刻原版 Flutter PixEz `SoupStore` 的解析逻辑，并针对 Pixivision 最新网页排版及合集特辑进行深度优化。
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
            .replace(Regex("""【.*?】"""), "")
            .replace(Regex("""\[.*?\]"""), "")
            .trim()
    }

    private fun extractTitle(html: String): String {
        val ogTitleMatch = Regex("""<meta\s+property=["']og:title["']\s+content=["'](.*?)["']""", RegexOption.IGNORE_CASE).find(html)
            ?: Regex("""<meta\s+name=["']twitter:title["']\s+content=["'](.*?)["']""", RegexOption.IGNORE_CASE).find(html)
            ?: Regex("""<h1[^>]*class=["'][^"']*am__title[^"']*["'][^>]*>(.*?)</h1>""", RegexOption.IGNORE_CASE).find(html)
            ?: Regex("""<title>(.*?)</title>""", RegexOption.IGNORE_CASE).find(html)

        val rawTitle = ogTitleMatch?.groupValues?.get(1)?.trim().orEmpty()
        return decodeHtml(rawTitle.replace(" - pixivision", "").replace("[pixivision]", "").trim())
    }

    private fun extractDescription(html: String): String {
        // 1. 优先从 header / am__lead 提取段落文本（与原版 SoupStore 逻辑一致）
        val headerMatch = Regex("""<header[^>]*>([\s\S]*?)</header>""", RegexOption.IGNORE_CASE).find(html)
        if (headerMatch != null) {
            val pTags = Regex("""<p[^>]*>([\s\S]*?)</p>""", RegexOption.IGNORE_CASE)
                .findAll(headerMatch.groupValues[1])
                .map { decodeHtml(stripHtml(it.groupValues[1])).trim() }
                .filter { it.isNotBlank() }
                .toList()
            if (pTags.isNotEmpty()) {
                return pTags.joinToString("\n\n")
            }
        }

        // 2. 特辑合集（_feature-article-body）导语提取
        val featureIntroMatch = Regex(
            """<div\b[^>]*class=["'][^"']*(?:_feature-article-body__paragraph|fab__paragraph)[^"']*["'][^>]*>([\s\S]*?)</div>""",
            RegexOption.IGNORE_CASE,
        ).find(html)
        if (featureIntroMatch != null) {
            val pTags = Regex("""<(?:p|div)[^>]*>([\s\S]*?)</(?:p|div)>""", RegexOption.IGNORE_CASE)
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
        val ogDescMatch = Regex("""<meta\s+property=["']og:description["']\s+content=["'](.*?)["']""", RegexOption.IGNORE_CASE).find(html)
            ?: Regex("""<meta\s+name=["']description["']\s+content=["'](.*?)["']""", RegexOption.IGNORE_CASE).find(html)

        var desc = ogDescMatch?.groupValues?.get(1)?.trim().orEmpty()
        if (desc.startsWith("[pixivision]")) {
            desc = desc.removePrefix("[pixivision]").trim()
        }
        return decodeHtml(desc)
    }

    private fun extractCoverUrl(html: String): String? {
        val ogImageMatch = Regex("""<meta\s+property=["']og:image["']\s+content=["'](.*?)["']""", RegexOption.IGNORE_CASE).find(html)
            ?: Regex("""<meta\s+name=["']twitter:image(?::src)?["']\s+content=["'](.*?)["']""", RegexOption.IGNORE_CASE).find(html)

        return ogImageMatch?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun extractWorks(html: String): List<AmWork> {
        val works = mutableListOf<AmWork>()
        val seenArtworkLinks = mutableSetOf<String>()

        // 策略 1：匹配所有 <article ...> ... </article>（Pixivision 官方标准结构）
        val articleRegex = Regex("""<article\b[^>]*class=["'][^"']*\bam__work\b[^"']*["'][^>]*>([\s\S]*?)</article>""", RegexOption.IGNORE_CASE)
        for (match in articleRegex.findAll(html)) {
            val work = parseWorkFromBlock(match.groupValues[1])
            if (work?.arworkLink != null && seenArtworkLinks.add(work.arworkLink!!)) {
                works.add(work)
            }
        }

        // 策略 2：若无 article 标签，则匹配 <div class="am__work"> 块
        if (works.isEmpty()) {
            val divRegex = Regex("""<div\b[^>]*class=["'][^"']*\bam__work\b[^"']*["'][^>]*>([\s\S]*?)(?=<div\b[^>]*class=["'][^"']*\bam__work\b|</main>|</body>|$)""", RegexOption.IGNORE_CASE)
            for (match in divRegex.findAll(html)) {
                val work = parseWorkFromBlock(match.groupValues[1])
                if (work?.arworkLink != null && seenArtworkLinks.add(work.arworkLink!!)) {
                    works.add(work)
                }
            }
        }

        // 策略 3：备选全局作品扫描
        if (works.isEmpty()) {
            val globalArtworkRegex = Regex(
                """https?://(?:www\.)?pixiv\.net/(?:[a-z]{2}(?:-[a-z]{2})?/)?artworks/(\d+)""",
                RegexOption.IGNORE_CASE,
            )
            val allArtworks = globalArtworkRegex.findAll(html).map { it.groupValues[1] }.distinct().toList()

            for (artId in allArtworks) {
                val artLink = "https://www.pixiv.net/artworks/$artId"
                if (seenArtworkLinks.add(artLink)) {
                    val index = html.indexOf("artworks/$artId")
                    val subStart = (index - 600).coerceAtLeast(0)
                    val subEnd = (index + 1200).coerceAtMost(html.length)
                    val subBlock = html.substring(subStart, subEnd)

                    val work = parseWorkFromBlock(subBlock) ?: AmWork(
                        title = "作品 $artId",
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
        val artworkMatch = Regex("""https?://(?:www\.)?pixiv\.net/(?:[a-z]{2}(?:-[a-z]{2})?/)?artworks/(\d+)""", RegexOption.IGNORE_CASE).find(block)
            ?: Regex("""https?://(?:www\.)?pixiv\.net/member_illust\.php\?(?:[^"'\s]*&)?illust_id=(\d+)""", RegexOption.IGNORE_CASE).find(block)

        val artworkId = artworkMatch?.groupValues?.get(1) ?: return null
        val artworkLink = "https://www.pixiv.net/artworks/$artworkId"

        val userMatch = Regex("""https?://(?:www\.)?pixiv\.net/(?:[a-z]{2}(?:-[a-z]{2})?/)?users/(\d+)""", RegexOption.IGNORE_CASE).find(block)
            ?: Regex("""https?://(?:www\.)?pixiv\.net/member\.php\?(?:[^"'\s]*&)?id=(\d+)""", RegexOption.IGNORE_CASE).find(block)
        val userId = userMatch?.groupValues?.get(1)
        val userLink = userId?.let { "https://www.pixiv.net/users/$it" }

        // 提取作品标题：h3、h4、am__title、title="..." 或 img alt
        val titleMatch = Regex("""<h[34][^>]*>(.*?)</h[34]>""", RegexOption.IGNORE_CASE).find(block)
            ?: Regex("""class=["'][^"']*(?:am__title|work__title|illust__title)[^"']*["'][^>]*>(.*?)<""", RegexOption.IGNORE_CASE).find(block)
            ?: Regex("""alt=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(block)
        val title = titleMatch?.let { decodeHtml(stripHtml(it.groupValues[1])).trim() }?.takeIf { it.isNotBlank() } ?: "作品 $artworkId"

        // 提取画师名字
        val user = extractUserName(block)

        // 从 <img> 标签中精确提取缩略图（优先获取插画预览中图/缩略图，避免直接下载原图大文件）
        val imgTags = Regex("""<img\b[^>]*>""", RegexOption.IGNORE_CASE).findAll(block).map { it.value }.toList()

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
            it.contains("i.pximg.net") || it.contains("pixivision") || it.contains("embed.pixiv.net")
        } ?: illustImgUrls.firstOrNull()
          ?: Regex("""https?://(?:i|s)\.pximg\.net/c/[^\s"'<>]+\.(?:jpg|png|jpeg|webp)""", RegexOption.IGNORE_CASE).find(block)?.value
          ?: "https://embed.pixiv.net/spotlight.php?id=$artworkId"

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
        val userLinkMatch = Regex("""<a\b[^>]*users/\d+[^>]*>([\s\S]*?)</a>""", RegexOption.IGNORE_CASE).find(block)
        if (userLinkMatch != null) {
            val text = decodeHtml(stripHtml(userLinkMatch.groupValues[1])).trim()
            if (text.isNotBlank() && !text.equals("画师", ignoreCase = true) && !text.equals("artist", ignoreCase = true)) {
                return text
            }
        }

        // 2. 从显式的 <p> 或 <span> 类名提取纯文本
        val pUserMatch = Regex(
            """<(?:p|span)[^>]*class=["'][^"']*(?:userName|user-name|am__user|am__work__userName|am__work-user|author|illustrator)[^"']*["'][^>]*>([\s\S]*?)</(?:p|span)>""",
            RegexOption.IGNORE_CASE,
        ).find(block)
        if (pUserMatch != null) {
            val text = decodeHtml(stripHtml(pUserMatch.groupValues[1])).trim()
            if (text.isNotBlank() && !text.equals("画师", ignoreCase = true) && !text.equals("artist", ignoreCase = true)) {
                return text
            }
        }

        // 3. 从 block 内的所有 <p> 标签提取
        val allPTags = Regex("""<p[^>]*>([\s\S]*?)</p>""", RegexOption.IGNORE_CASE).findAll(block)
        for (pMatch in allPTags) {
            val text = decodeHtml(stripHtml(pMatch.groupValues[1])).trim()
            if (text.isNotBlank() && !text.equals("画师", ignoreCase = true) && !text.equals("artist", ignoreCase = true) && text.length < 50) {
                return text
            }
        }

        return null
    }

    private fun extractSubArticles(html: String, rawUrl: String): List<SpotlightArticle> {
        val currentArticleId = Regex("""/a/(\d+)""").find(rawUrl)?.groupValues?.get(1)?.toIntOrNull()

        // 仅在明确的合集卡片容器 (_feature-article-body__article_card) 中提取子特辑
        val cardItemRegex = Regex(
            """<div\b[^>]*class=["'][^"']*_feature-article-body__article_card[^"']*["'][^>]*>([\s\S]*?)(?=<div\b[^>]*class=["'][^"']*article-item|<footer|</body>|$)""",
            RegexOption.IGNORE_CASE,
        )
        val cardItemMatches = cardItemRegex.findAll(html).toList()

        val subArticles = mutableListOf<SpotlightArticle>()
        val seenIds = mutableSetOf<Int>()

        val blocks = if (cardItemMatches.isNotEmpty()) {
            cardItemMatches.map { it.groupValues[1] }
        } else {
            // 备用：若无 _feature-article-body__article_card，则在排除所有推荐、相关、排行榜区域后查找
            val cleanHtml = html
                .substringBefore("<div class=\"_related-articles")
                .substringBefore("<div class=\"_ranking-articles")
                .substringBefore("<div class=\"_recommend")
                .substringBefore("<footer")
            Regex("""<article\b[^>]*class=["'][^"']*_article-card[^"']*["'][^>]*>([\s\S]*?)</article>""", RegexOption.IGNORE_CASE)
                .findAll(cleanHtml).map { it.groupValues[1] }.toList()
        }

        for (block in blocks) {
            val linkMatch = Regex("""href=["']([^"']*(?:/zh|/en|/ja|/ko|/zh-tw)?/a/(\d+)[^"']*)["']""", RegexOption.IGNORE_CASE).find(block) ?: continue
            val relOrAbsUrl = linkMatch.groupValues[1]
            val id = linkMatch.groupValues[2].toIntOrNull() ?: continue

            if (id == currentArticleId || !seenIds.add(id)) {
                continue
            }

            val articleUrl = if (relOrAbsUrl.startsWith("http")) relOrAbsUrl else "https://www.pixivision.net${if (relOrAbsUrl.startsWith("/")) "" else "/"}$relOrAbsUrl"

            // 提取标题
            val titleMatch = Regex("""<h[1-4][^>]*class=["'][^"']*arc__title[^"']*["'][^>]*>[\s\S]*?<a[^>]*>([\s\S]*?)</a></h[1-4]>""", RegexOption.IGNORE_CASE).find(block)
                ?: Regex("""<h[1-4][^>]*>[\s\S]*?<a[^>]*>([\s\S]*?)</a></h[1-4]>""", RegexOption.IGNORE_CASE).find(block)
                ?: Regex("""<a[^>]*title=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(block)

            val rawTitle = titleMatch?.let { decodeHtml(stripHtml(it.groupValues[1])).trim() }?.takeIf { it.isNotBlank() } ?: "特辑 $id"

            // 封面图：从 style="background-image: url(...)" 或 <img> 提取
            val bgMatch = Regex("""url\(\s*["']?([^"')]+)["']?\s*\)""", RegexOption.IGNORE_CASE).find(block)
            val imgMatch = Regex("""<img\b[^>]*(?:data-src|src)=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(block)
            val thumbnail = bgMatch?.groupValues?.get(1)?.trim()
                ?: imgMatch?.groupValues?.get(1)?.trim()
                ?: ""

            // 发布日期
            val dateMatch = Regex("""<time[^>]*datetime=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(block)
                ?: Regex("""<time[^>]*>([\s\S]*?)</time>""", RegexOption.IGNORE_CASE).find(block)
            val publishDate = dateMatch?.let { decodeHtml(stripHtml(it.groupValues[1])).trim() }.orEmpty()

            subArticles.add(
                SpotlightArticle(
                    id = id,
                    title = rawTitle,
                    pureTitle = cleanTitle(rawTitle),
                    thumbnail = thumbnail,
                    articleUrl = articleUrl,
                    publishDate = publishDate,
                )
            )
        }

        return subArticles
    }

    private fun extractImageUrlsFromTag(imgTag: String): List<String> {
        val attrRegex = Regex("""(?:data-src|data-original|data-url|data-lazy-src|src)=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val urls = mutableListOf<String>()
        for (attr in attrRegex.findAll(imgTag)) {
            val url = attr.groupValues[1].trim()
            if (url.isNotBlank() && !url.startsWith("data:") && !url.endsWith(".gif") && !url.contains("blank.gif")) {
                urls.add(url)
            }
        }
        return urls
    }

    private fun stripHtml(html: String): String {
        return html.replace(Regex("""<[^>]*>"""), " ").replace(Regex("""\s+"""), " ").trim()
    }

    private fun decodeHtml(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&apos;", "'")
            .replace("&nbsp;", " ")
            .replace("&#39;", "'")
            .trim()
    }
}
