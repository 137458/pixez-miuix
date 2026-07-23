package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.repository.AccountRepository
import com.perol.pixez.shared.data.repository.BanRepository
import com.perol.pixez.shared.data.repository.IllustRepository
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.utils.runCatchingNonCancel
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.IllustStaggeredGrid
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.AddCircle
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.Settings

/**
 * 首页/推荐页：顶部标题栏 + 真实推荐插画瀑布流。
 */
@Composable
fun HelloScreen(
    onIllustClick: (Int) -> Unit,
    onSettingsClick: () -> Unit,
    onLoginClick: () -> Unit,
    onRecomUserClick: () -> Unit,
    repository: IllustRepository,
    accountRepository: AccountRepository,
    banRepository: BanRepository,
    settingsRepository: SettingsRepository,
) {
    // retryCount 作为 produceState 的 key，点击重试时自增触发重新加载。
    var retryCount by rememberSaveable { mutableIntStateOf(0) }

    // 登录状态：页面进入时检测一次，未登录显示登录入口。
    var isLoggedIn by rememberSaveable { mutableStateOf<Boolean?>(null) }

    // 未登录提示弹窗：仅当首次检测到未登录时主动弹出一次，避免旋转屏幕等场景反复打扰。
    var showLoginDialog by rememberSaveable { mutableStateOf(false) }
    var hasPromptedLogin by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isLoggedIn = runCatchingNonCancel { accountRepository.currentAccount() != null }.getOrDefault(false)
    }

    // 当登录状态检测完成且为未登录时，触发一次性登录提示弹窗。
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn == false && !hasPromptedLogin) {
            showLoginDialog = true
            hasPromptedLogin = true
        }
    }

    // 页面进入时加载数据；已登录用推荐接口，未登录用 walkthrough 匿名接口。
    // 加载完成后用本地屏蔽列表过滤，被屏蔽作品不展示。
    val state = produceState<Result<List<Illust>>?>(
        initialValue = null,
        repository,
        banRepository,
        settingsRepository,
        retryCount,
        isLoggedIn,
    ) {
        val illustsResult = when (isLoggedIn) {
            true -> runCatchingNonCancel { repository.getRecommended() }
            false -> runCatchingNonCancel { repository.getWalkthroughIllusts() }
            null -> null
        }
        val bannedIds = runCatchingNonCancel { banRepository.getBannedIllustIds() }
            .getOrDefault(emptySet())
        val bannedUserIds = runCatchingNonCancel { banRepository.getBannedUserIds() }
            .getOrDefault(emptySet())
        val banTags = runCatchingNonCancel { banRepository.getAllBanTags() }
            .getOrDefault(emptyList())
        val banAIIllust = settingsRepository.banAIIllust
        value = illustsResult?.map { illusts ->
            illusts.filter {
                it.id !in bannedIds &&
                    it.user.id !in bannedUserIds &&
                    (!banAIIllust || it.illustAIType != 2) &&
                    !banRepository.isBannedByTags(
                        banTags,
                        it.tags.flatMap { tag -> listOfNotNull(tag.name, tag.translatedName) }
                    )
            }
        }
    }

    // 未登录提示对话框：位于 Scaffold 外层，确保能覆盖整个页面。
    SuperDialog(
        title = "需要登录",
        summary = "当前未登录，登录后可使用完整功能",
        show = showLoginDialog,
        onDismissRequest = { showLoginDialog = false },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                text = "暂不登录",
                onClick = { showLoginDialog = false },
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = {
                    showLoginDialog = false
                    onLoginClick()
                },
                modifier = Modifier.weight(1f),
            ) {
                Text("去登录")
            }
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
                                imageVector = MiuixIcons.Contacts,
                                contentDescription = "登录",
                            )
                        }
                    }
                    if (isLoggedIn == true) {
                        IconButton(
                            onClick = onRecomUserClick,
                        ) {
                            Icon(
                                imageVector = MiuixIcons.AddCircle,
                                contentDescription = "推荐用户",
                            )
                        }
                    }
                    IconButton(
                        onClick = onSettingsClick,
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Settings,
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
