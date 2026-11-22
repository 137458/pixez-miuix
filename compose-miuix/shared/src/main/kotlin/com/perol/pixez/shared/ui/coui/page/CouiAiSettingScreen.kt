package com.perol.pixez.shared.ui.coui.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.perol.pixez.shared.ui.LocalUserRepository
import com.perol.pixez.shared.ui.components.LocalToastSink
import com.perol.pixez.shared.ui.components.PixivApiErrorFormatter
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.components.ToastType
import com.perol.pixez.shared.ui.coui.core.CouiSettingsScaffold
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch

/**
 * COUI AI 作品显示设置页：设置骨架 + 下拉弹窗选择行。
 *
 * 两个互斥选项与服务器同步逻辑与 MIUIX 版一致；提交期间禁用触发行以防重复提交。
 */
@Composable
internal fun CouiAiSettingScreen(
    showAI: Boolean,
    onBack: () -> Unit,
) {
    val userRepository = LocalUserRepository.current
    val strings = LocalStrings.current
    val coroutineScope = rememberCoroutineScope()
    val toastSink = LocalToastSink.current

    var currentShowAI by remember { mutableStateOf(showAI) }
    var isUpdating by remember { mutableStateOf(false) }

    fun changeShowAI(value: Boolean) {
        if (isUpdating || currentShowAI == value) return
        coroutineScope.launch {
            isUpdating = true
            suspendRunCatchingNonCancel {
                userRepository.updateUserAISettings(value)
            }.onSuccess { response ->
                currentShowAI = response.showAI
            }.onFailure { e ->
                Napier.e("更新 AI 显示设置失败", e)
                toastSink(ToastData(PixivApiErrorFormatter.format(e, strings), ToastType.Error))
            }
            isUpdating = false
        }
    }

    val options = remember(strings) {
        listOf(
            true to (strings.aiDisplayShow to strings.aiDisplayShowSummary),
            false to (strings.aiDisplayHidePartial to strings.aiDisplayHidePartialSummary),
        )
    }
    val selectedIndex = if (currentShowAI) 0 else 1

    CouiSettingsScaffold(
        title = strings.userAISettings,
        onBack = onBack,
    ) {
        item(key = "coui_ai_display", contentType = "section") {
            CouiSettingSection(title = strings.aiDisplayOptions) {
                CouiMenuSelectRow(
                    title = strings.aiDisplayOptions,
                    items = options.map { it.second.first },
                    selectedIndex = selectedIndex,
                    summary = options[selectedIndex].second.second,
                    enabled = !isUpdating,
                    onSelect = { index ->
                        options.getOrNull(index)?.first?.let(::changeShowAI)
                    },
                )
            }
        }
    }
}
