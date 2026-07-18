package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.perol.pixez.shared.data.model.UserDetail
import com.perol.pixez.shared.ui.FakeData
import com.perol.pixez.shared.ui.components.IllustCard
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 用户详情页：头部信息 + 用户作品网格。
 *
 * 使用单个 LazyVerticalStaggeredGrid 承载头部与作品卡片，避免 LazyColumn 嵌套
 * LazyVerticalStaggeredGrid 导致的滚动回收异常与固定高度问题。
 */
@Composable
fun UserDetailScreen(
    userId: Int,
    onBack: () -> Unit,
    onIllustClick: (Int) -> Unit,
) {
    // M3 使用 mock 数据；M4 通过 userId 从 Repository 查询真实数据。
    val userDetail = FakeData.userDetail()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = userDetail.user.name,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalItemSpacing = 8.dp,
        ) {
            item {
                UserProfileHeader(
                    userDetail = userDetail,
                    modifier = Modifier.padding(16.dp),
                )
            }

            item {
                Text(
                    text = "作品",
                    style = MiuixTheme.textStyles.title2,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            items(
                items = FakeData.illusts(count = 12),
                key = { it.id },
            ) { illust ->
                IllustCard(
                    illust = illust,
                    onClick = { onIllustClick(illust.id) },
                )
            }
        }
    }
}

@Composable
private fun UserProfileHeader(
    userDetail: UserDetail,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AsyncImage(
            model = userDetail.user.profileImageUrls.medium,
            contentDescription = userDetail.user.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = userDetail.user.name,
            style = MiuixTheme.textStyles.title1,
        )
        Text(
            text = "@${userDetail.user.account}",
            style = MiuixTheme.textStyles.footnote1,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = userDetail.user.comment ?: "",
            style = MiuixTheme.textStyles.body2,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { /* M4: 调用关注 API */ },
            colors = ButtonDefaults.buttonColorsPrimary(),
        ) {
            Text(text = if (userDetail.user.isFollowed == true) "已关注" else "关注")
        }
    }
}
