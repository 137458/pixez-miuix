package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.repository.AccountRepository
import com.perol.pixez.shared.data.repository.IllustRepository
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.utils.runCatchingNonCancel
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.IllustStaggeredGrid
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar

/**
 * 首页/推荐页：顶部标题栏 + 真实推荐插画瀑布流。
 */
@Composable
fun HelloScreen(
    onIllustClick: (Int) -> Unit,
    onSettingsClick: () -> Unit,
    onLoginClick: () -> Unit,
    repository: IllustRepository,
    accountRepository: AccountRepository,
) {
    // retryCount 作为 produceState 的 key，点击重试时自增触发重新加载。
    var retryCount by rememberSaveable { mutableIntStateOf(0) }

    // 登录状态：页面进入时检测一次，未登录显示登录入口。
    var isLoggedIn by rememberSaveable { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) {
        isLoggedIn = runCatchingNonCancel { accountRepository.currentAccount() != null }.getOrDefault(false)
    }

    // 页面进入时加载数据；已登录用推荐接口，未登录用 walkthrough 匿名接口。
    val state = produceState<Result<List<Illust>>?>(
        initialValue = null,
        repository,
        retryCount,
        isLoggedIn,
    ) {
        value = when (isLoggedIn) {
            true -> runCatchingNonCancel { repository.getRecommended() }
            false -> runCatchingNonCancel { repository.getWalkthroughIllusts() }
            null -> null
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "首页",
                actions = {
                    if (isLoggedIn == false) {
                        IconButton(
                            onClick = onLoginClick,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "登录",
                            )
                        }
                    }
                    IconButton(
                        onClick = onSettingsClick,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "设置",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        val result = state.value
        when {
            result == null -> LoadingPlaceholder(modifier = Modifier.padding(paddingValues))
            result.isSuccess -> {
                val illusts = result.getOrNull().orEmpty()
                if (illusts.isEmpty()) {
                    EmptyPlaceholder(
                        message = if (isLoggedIn == true) "暂无推荐内容" else "暂无内容",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                    )
                } else {
                    IllustStaggeredGrid(
                        illusts = illusts,
                        onIllustClick = onIllustClick,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                    )
                }
            }
            else -> ErrorPlaceholder(
                error = result.exceptionOrNull(),
                onRetry = { retryCount++ },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            )
        }
    }
}
