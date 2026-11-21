package com.perol.pixez.shared.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SearchQueryTargetTest {

    @Test
    fun `parseSearchQueryTarget recognizes pure numeric ID`() {
        assertEquals(
            SearchQueryTarget.NumericId(44298467),
            parseSearchQueryTarget("44298467"),
        )
        assertEquals(
            SearchQueryTarget.NumericId(101003492),
            parseSearchQueryTarget("  101003492  "),
        )
    }

    @Test
    fun `parseSearchQueryTarget recognizes Pixiv artwork URLs`() {
        assertEquals(
            SearchQueryTarget.IllustId(44298467),
            parseSearchQueryTarget("https://www.pixiv.net/artworks/44298467"),
        )
        assertEquals(
            SearchQueryTarget.IllustId(99887766),
            parseSearchQueryTarget("https://www.pixiv.net/en/artworks/99887766#big_0"),
        )
        assertEquals(
            SearchQueryTarget.IllustId(123456),
            parseSearchQueryTarget("https://www.pixiv.net/member_illust.php?mode=medium&illust_id=123456"),
        )
    }

    @Test
    fun `parseSearchQueryTarget recognizes Pixiv user URLs`() {
        assertEquals(
            SearchQueryTarget.UserId(11),
            parseSearchQueryTarget("https://www.pixiv.net/users/11"),
        )
        assertEquals(
            SearchQueryTarget.UserId(27517),
            parseSearchQueryTarget("https://www.pixiv.net/en/users/27517/artworks"),
        )
    }

    @Test
    fun `parseSearchQueryTarget returns null for regular keywords or invalid numbers`() {
        assertNull(parseSearchQueryTarget("初音ミク"))
        assertNull(parseSearchQueryTarget("10000users入り"))
        assertNull(parseSearchQueryTarget("0"))
        assertNull(parseSearchQueryTarget("-123"))
        assertNull(parseSearchQueryTarget(""))
    }
}
