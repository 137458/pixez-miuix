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
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.IllustStaggeredGrid
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.utils.suspendRunCatchingNonCancel
import androidx.compose.runtime.remember
import androidx.compose.ui.input.nestedscroll.nestedScroll
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.extra.SuperDialog

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
    settingsRepository: SettingsRepository,
) {
    // 登录状态：页面进入时检测一次，未登录显示登录入口。
    var isLoggedIn by rememberSaveable { mutableStateOf<Boolean?>(null) }

    // 未登录提示弹窗：仅当首次检测到未登录时主动弹出一次，避免旋转屏幕等场景反复打扰。
    var showLoginDialog by rememberSaveable { mutableStateOf(false) }
    var hasPromptedLogin by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // 当前处于 LaunchedEffect 挂起上下文，需要调用挂起函数，使用 suspendRunCatchingNonCancel 捕获异常并保留取消语义。
        isLoggedIn = suspendRunCatchingNonCancel { accountRepository.currentAccount() != null }.getOrDefault(false)
    }

    // 当登录状态检测完成且为未登录时，触发一次性登录提示弹窗。
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn == false && !hasPromptedLogin) {
            showLoginDialog = true
            hasPromptedLogin = true
        }
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
        settingsRepository,
    ) {
        value = when (isLoggedIn) {
            true -> {
                val illustsResult = suspendRunCatchingNonCancel { repository.getFollowIllusts(currentRestrict) }
                val bannedIds = suspendRunCatchingNonCancel { banRepository.getBannedIllustIds() }
                    .getOrDefault(emptySet())
                val bannedUserIds = suspendRunCatchingNonCancel { banRepository.getBannedUserIds() }
                    .getOrDefault(emptySet())
                val banTags = suspendRunCatchingNonCancel { banRepository.getAllBanTags() }
                    .getOrDefault(emptyList())
                val banAIIllust = settingsRepository.banAIIllust
                illustsResult.map { illusts ->
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
            false -> Result.success(emptyList())
            null -> null 
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

    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "最新",
                scrollBehavior = scrollBehavior,
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
                .padding(top = paddingValues.calculateTopPadding()),
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
                        result == null -> LoadingPlaceholder(modifier = Modifier.weight(1f))
                        result.isSuccess -> {
                            val illusts = result.getOrNull().orEmpty()
                            if (illusts.isEmpty()) {
                                EmptyPlaceholder(
                                    message = "暂无关注作品",
                                    modifier = Modifier.weight(1f),
                                )
                            } else {
                                IllustStaggeredGrid(
                                    illusts = illusts,
                                    onIllustClick = onIllustClick,
                                    modifier = Modifier
                                        .weight(1f)
                                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                                )
                            }
                        }

                        else -> ErrorPlaceholder(
                            error = result.exceptionOrNull(),
                            onRetry = { retryCount++ },
                            modifier = Modifier.weight(1f),
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
