package com.perol.pixez.shared.ui.screens

import androidx.compose.runtime.Composable
import com.perol.pixez.shared.data.model.appendDistinct
import com.perol.pixez.shared.ui.LocalUserRepository
import com.perol.pixez.shared.ui.components.UserPagedListScreen
import com.perol.pixez.shared.ui.components.UserPreviewItem
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.state.PagedListPage
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Contacts

/**
 * 用户好P友/粉丝列表页：账号守卫、分页、四态占位与下拉刷新由 [UserPagedListScreen] 统一提供，
 * 本页只描述数据源、条目渲染与文案。
 */
@Composable
fun UserFollowerListScreen(
    userId: Long,
    onBack: () -> Unit,
    onUserClick: (Long) -> Unit,
) {
    // 中-9：数据仓储统一经 CompositionLocal 注入（RootContent 根部 provide）。
    val repository = LocalUserRepository.current
    val strings = LocalStrings.current

    UserPagedListScreen(
        title = strings.userFollowerTitle,
        emptyText = strings.userFollowerEmpty,
        emptyIcon = MiuixIcons.Contacts,
        pageKey = userId,
        onBack = onBack,
        fetch = { nextUrl ->
            val response = repository.getUserFollowersResponse(userId = userId, nextUrl = nextUrl)
            PagedListPage(response.userPreviews, response.nextUrl)
        },
        mergeItems = { old, new -> old.appendDistinct(new) },
        itemKey = { it.user.id },
        itemContentType = "user_preview_item",
        footerKey = "follower_pagination_footer",
        itemContent = { preview ->
            UserPreviewItem(
                preview = preview,
                onClick = { onUserClick(preview.user.id) },
            )
        },
    )
}
