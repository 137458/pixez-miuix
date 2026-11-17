package com.perol.pixez.shared.ui.screens

import androidx.compose.runtime.Composable
import com.perol.pixez.shared.data.model.appendDistinct
import com.perol.pixez.shared.ui.LocalUserRepository
import com.perol.pixez.shared.ui.components.UserPagedListScreen
import com.perol.pixez.shared.ui.components.UserPreviewItem
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.state.PagedListPage

/**
 * 相关用户列表页：账号守卫、分页、四态占位与下拉刷新由 [UserPagedListScreen] 统一提供，
 * 本页只描述数据源、条目渲染与文案。
 */
@Composable
fun UserRelatedListScreen(
    userId: Long,
    onBack: () -> Unit,
    onUserClick: (Long) -> Unit,
) {
    val repository = LocalUserRepository.current
    val strings = LocalStrings.current

    UserPagedListScreen(
        title = strings.relatedUsers,
        emptyText = strings.relatedUsersEmpty,
        pageKey = userId,
        onBack = onBack,
        fetch = { nextUrl ->
            val response = repository.getUserRelated(userId, nextUrl = nextUrl)
            PagedListPage(response.userPreviews, response.nextUrl)
        },
        mergeItems = { old, new -> old.appendDistinct(new) },
        itemKey = { it.user.id },
        itemContentType = "user_preview_item",
        footerKey = "related_pagination_footer",
        itemContent = { preview ->
            UserPreviewItem(
                preview = preview,
                onClick = { onUserClick(preview.user.id) },
            )
        },
    )
}
