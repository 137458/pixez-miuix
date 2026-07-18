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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.UserDetail
import com.perol.pixez.shared.data.repository.UserRepository
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.utils.runCatchingNonCancel
import com.perol.pixez.shared.ui.components.IllustCard
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 用户详情页：头部信息 + 真实用户作品网格。
 */
@Composable
fun UserDetailScreen(
    userId: Int,
    onBack: () -> Unit,
    onIllustClick: (Int) -> Unit,
    repository: UserRepository,
) {
    // 重试计数，作为 produceState 的 key 触发用户资料与作品列表重新加载。
    var retryCount by rememberSaveable(userId) { mutableIntStateOf(0) }

    // 同时加载用户资料与作品列表；任一失败都会进入错误态。
    val detailState = produceState<Result<Pair<UserDetail, List<Illust>>>?>(
        initialValue = null,
        userId,
        repository,
        retryCount,
    ) {
        value = runCatchingNonCancel {
            coroutineScope {
                val detailDeferred = async { repository.getUserDetail(userId) }
                val illustsDeferred = async { repository.getUserIllusts(userId) }
                detailDeferred.await() to illustsDeferred.await()
            }
        }
    }

    val result = detailState.value
    val userDetail = result?.getOrNull()?.first

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = userDetail?.user?.name ?: "用户详情",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        when {
            result == null -> LoadingPlaceholder(modifier = Modifier.padding(paddingValues))
            result.isSuccess && userDetail != null -> {
                val illusts = result.getOrNull()?.second.orEmpty()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                ) {
                    UserProfileHeader(
                        userDetail = userDetail,
                        modifier = Modifier.padding(16.dp),
                    )
                    Text(
                        text = "作品",
                        style = MiuixTheme.textStyles.title2,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalItemSpacing = 8.dp,
                    ) {
                        items(
                            items = illusts,
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
            else -> ErrorPlaceholder(
                error = result.exceptionOrNull(),
                onRetry = { retryCount++ },
                modifier = Modifier.padding(paddingValues),
            )
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
        PixivAsyncImage(
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
