package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.appendDistinct
import com.perol.pixez.shared.ui.LocalNovelRepository
import com.perol.pixez.shared.ui.components.NovelCard
import com.perol.pixez.shared.ui.components.UserPagedListScreen
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.state.PagedListPage

/**
 * 用户小说列表页：账号守卫、分页、四态占位与下拉刷新由 [UserPagedListScreen] 统一提供，
 * 本页只描述数据源、条目渲染、标题拼装与列表内边距/间距。
 */
@Composable
fun UserNovelListScreen(
    userId: Long,
    authorName: String = "",
    onBack: () -> Unit,
    onNovelClick: (Long) -> Unit,
    onUserClick: ((Long) -> Unit)? = null,
) {
    val repository = LocalNovelRepository.current
    val strings = LocalStrings.current

    val pageTitle = remember(authorName, strings.userNovels) {
        if (authorName.isNotBlank()) {
            "$authorName - ${strings.userNovels}"
        } else {
            strings.userNovels
        }
    }

    UserPagedListScreen(
        title = pageTitle,
        emptyText = strings.userNovelsEmpty,
        pageKey = userId,
        onBack = onBack,
        fetch = { nextUrl ->
            val response = repository.getUserNovels(userId, nextUrl = nextUrl)
            PagedListPage(response.novels, response.nextUrl)
        },
        mergeItems = { old, new -> old.appendDistinct(new) },
        itemKey = { it.id },
        itemContentType = "novel_card_item",
        footerKey = "novel_pagination_footer",
        itemContent = { novel ->
            NovelCard(
                novel = novel,
                onClick = { onNovelClick(novel.id) },
                onUserClick = onUserClick,
            )
        },
        // 小说卡片按卡片留白排版：列表内边距在顶栏内边距之上再加卡片间距。
        contentPadding = { paddingValues ->
            PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = paddingValues.calculateTopPadding() + 8.dp,
                bottom = paddingValues.calculateBottomPadding() + 16.dp,
            )
        },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    )
}
