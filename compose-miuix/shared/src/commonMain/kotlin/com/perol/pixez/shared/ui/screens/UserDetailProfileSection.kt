package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.UserDetail
import com.perol.pixez.shared.platform.openBrowser
import com.perol.pixez.shared.ui.components.HtmlCaptionText
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.i18n.LocalStrings
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 用户详情页头部资料区：头像与账号、关注按钮、关注数/粉丝数与外部链接、个性签名。
 *
 * 纯展示组件，所有状态均由 [UserDetailTabContent] 以参数形式传入，自身不持有任何状态。
 */
@Composable
internal fun UserProfileHeader(
    userDetail: UserDetail,
    isCurrentUser: Boolean,
    isFollowed: Boolean,
    isLoading: Boolean,
    onFollowClick: () -> Unit,
    onFollowListClick: () -> Unit,
    onFollowerListClick: () -> Unit,
    modifier: Modifier = Modifier,
    onIllustClick: (Int) -> Unit = {},
) {
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // 第一行：头像 + 名字/账号 + 关注按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PixivAsyncImage(
                model = userDetail.user.profileImageUrls.medium,
                contentDescription = userDetail.user.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = userDetail.user.name,
                    style = MiuixTheme.textStyles.title3,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "@${userDetail.user.account}",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (!isCurrentUser) {
                Button(
                    onClick = onFollowClick,
                    enabled = !isLoading,
                    colors = if (isFollowed) ButtonDefaults.buttonColors() else ButtonDefaults.buttonColorsPrimary(),
                ) {
                    Text(
                        text = if (isFollowed) strings.followed else strings.follow,
                        style = MiuixTheme.textStyles.footnote1,
                    )
                }
            }
        }

        // 第二行：关注数 / 粉丝数 / 外部链接（紧凑横向排布）
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val followCount = userDetail.profile.totalFollowUsers
            Text(
                text = strings.userFollowCount.format(followCount),
                style = MiuixTheme.textStyles.footnote1,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(enabled = followCount > 0, onClick = onFollowListClick)
                    .padding(vertical = 2.dp),
            )

            val followerCount = userDetail.profile.totalMypixivUsers
            Text(
                text = strings.userFollowerCount.format(followerCount),
                style = MiuixTheme.textStyles.footnote1,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(enabled = followerCount > 0, onClick = onFollowerListClick)
                    .padding(vertical = 2.dp),
            )

            // 外部链接
            userDetail.profile.twitterUrl?.takeIf { it.isNotBlank() }?.let { url ->
                Text(
                    text = "Twitter",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { runCatching { openBrowser(url) } }
                        .padding(vertical = 2.dp),
                )
            }
            userDetail.profile.pawooUrl?.takeIf { it.isNotBlank() }?.let { url ->
                Text(
                    text = "Pawoo",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { runCatching { openBrowser(url) } }
                        .padding(vertical = 2.dp),
                )
            }
            userDetail.profile.webpage?.takeIf { it.isNotBlank() }?.let { url ->
                Text(
                    text = strings.userWebpage,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { runCatching { openBrowser(url) } }
                        .padding(vertical = 2.dp),
                )
            }
        }

        // 第三行：个性签名/简介（若存在，支持超链接解析与折叠）
        userDetail.user.comment?.takeIf { it.isNotBlank() }?.let { bio ->
            HtmlCaptionText(
                html = bio,
                onIllustClick = onIllustClick,
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                collapsible = true,
                collapsedMaxLines = 2,
            )
        }
    }
}