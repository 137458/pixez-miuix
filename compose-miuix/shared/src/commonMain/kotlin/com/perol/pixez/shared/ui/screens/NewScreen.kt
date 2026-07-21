package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.repository.AccountRepository
import com.perol.pixez.shared.data.repository.BanRepository
import com.perol.pixez.shared.data.repository.IllustRepository
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.IllustStaggeredGrid
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.utils.runCatchingNonCancel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar

/**
 * 最新/关注页：展示已登录用户关注画师的最新插画。
 *
 * 未登录时显示登录入口；登录后支持 all/public/private 三种可见性筛选。
 */
@Composable
fun NewScreen(
    onIllustClick: (Int) -> Unit,
    onLoginClick: () -> Unit,
    repository: IllustRepository,
    accountRepository: AccountRepository,
    banRepository: BanRepository,
) {
    // 登录状态：页面进入时检测一次，未登录显示登录入口。
    var isLoggedIn by rememberSaveable { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) {
        isLoggedIn = runCatchingNonCancel { accountRepository.currentAccount() != null }.getOrDefault(false)
    }

    // 可见性筛选：0=all, 1=public, 2=private。
    var selectedRestrictIndex by rememberSaveable { mutableIntStateOf(0) }
    val restrictOptions = listOf("全部" to "all", "公开" to "public", "私密" to "private")
    val currentRestrict = restrictOptions[selectedRestrictIndex].second

    // retryCount 作为 produceState 的 key，点击重试或切换筛选时自增触发重新加载。
    var retryCount by rememberSaveable { mutableIntStateOf(0) }

    val state = produceState<Result<List<Illust>>?>(
        initialValue = null,
        repository,
        currentRestrict,
        retryCount,
        isLoggedIn,
        banRepository,
    ) {
        value = when (isLoggedIn) {
            true -> {
                val illustsResult = runCatchingNonCancel { repository.getFollowIllusts(currentRestrict) }
                val bannedIds = runCatchingNonCancel { banRepository.getBannedIllustIds() }
                    .getOrDefault(emptySet())
                val bannedUserIds = runCatchingNonCancel { banRepository.getBannedUserIds() }
                    .getOrDefault(emptySet())
                illustsResult.map { illusts ->
                    illusts.filter { it.id !in bannedIds && it.user.id !in bannedUserIds }
                }
            }
            false -> Result.success(emptyList())
            null -> null
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "最新",
                actions = {
                    if (isLoggedIn == false) {
                        Button(
                            onClick = onLoginClick,
                            colors = ButtonDefaults.buttonColorsPrimary(),
                            modifier = Modifier.padding(end = 12.dp),
                        ) {
                            Text("登录")
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when (isLoggedIn) {
                false -> {
                    EmptyPlaceholder(
                        message = "登录后可查看关注作品",
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                null -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())

                true -> {
                    // 登录后显示筛选器与列表。
                    RestrictSelector(
                        options = restrictOptions.map { it.first },
                        selectedIndex = selectedRestrictIndex,
                        onSelect = { selectedRestrictIndex = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )

                    val result = state.value
                    when {
                        result == null -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())
                        result.isSuccess -> {
                            val illusts = result.getOrNull().orEmpty()
                            if (illusts.isEmpty()) {
                                EmptyPlaceholder(
                                    message = "暂无关注作品",
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                IllustStaggeredGrid(
                                    illusts = illusts,
                                    onIllustClick = onIllustClick,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }

                        else -> ErrorPlaceholder(
                            error = result.exceptionOrNull(),
                            onRetry = { retryCount++ },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RestrictSelector(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEachIndexed { index, label ->
            Button(
                onClick = { onSelect(index) },
                colors = if (index == selectedIndex) {
                    ButtonDefaults.buttonColorsPrimary()
                } else {
                    ButtonDefaults.buttonColors()
                },
                modifier = Modifier.weight(1f),
            ) {
                Text(label)
            }
        }
    }
}
