package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.perol.pixez.shared.data.repository.UserRepository
import com.perol.pixez.shared.ui.components.ToastMessage
import com.perol.pixez.shared.ui.utils.suspendRunCatchingNonCancel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp

/**
 * AI 作品显示设置页：展示并更新 Pixiv 账号级 AI 作品显示偏好。
 *
 * 提供「显示」与「部分隐藏」两个互斥选项，切换时同步到服务器。
 *
 * @param showAI 页面进入时的初始设置值。
 * @param onBack 返回上一级页面回调。
 * @param userRepository 用户仓库，用于调用 AI 显示设置接口。
 */
@Composable
fun UserShowAISettingScreen(
    showAI: Boolean,
    onBack: () -> Unit,
    userRepository: UserRepository,
) {
    val coroutineScope = rememberCoroutineScope()

    // 当前选中的 AI 显示状态，以服务器返回结果为准。
    var currentShowAI by remember { mutableStateOf(showAI) }

    // 更新操作加载态，防止重复提交。
    var isUpdating by remember { mutableStateOf(false) }

    // 提示信息。
    var toastMessage by rememberSaveable { mutableStateOf<String?>(null) }

    /**
     * 调用服务器接口更新 AI 显示设置，并以返回结果刷新本地状态。
     *
     * @param value 目标设置值。
     */
    fun changeShowAI(value: Boolean) {
        if (isUpdating || currentShowAI == value) return
        coroutineScope.launch {
            isUpdating = true
            // 当前处于协程 launch 挂起上下文，需要调用挂起函数，使用 suspendRunCatchingNonCancel 捕获异常并保留取消语义。
            suspendRunCatchingNonCancel {
                userRepository.updateUserAISettings(value)
            }.onSuccess { response ->
                currentShowAI = response.showAI
            }.onFailure { e ->
                Napier.e("更新 AI 显示设置失败", e)
                toastMessage = "更新失败：${e.message}"
            }
            isUpdating = false
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "AI 作品显示设置",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = "返回",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues,
        ) {
            item {
                SmallTitle(text = "显示选项")
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    BasicComponent(
                        title = "显示",
                        summary = "展示所有 AI 生成作品",
                        onClick = { changeShowAI(true) },
                        endActions = {
                            if (currentShowAI) {
                                Text(text = "✓")
                            }
                        },
                    )
                    BasicComponent(
                        title = "部分隐藏",
                        summary = "隐藏部分 AI 生成作品",
                        onClick = { changeShowAI(false) },
                        endActions = {
                            if (!currentShowAI) {
                                Text(text = "✓")
                            }
                        },
                    )
                }
            }
        }

        ToastMessage(
            message = toastMessage,
            onDismiss = { toastMessage = null },
        )
    }
}
