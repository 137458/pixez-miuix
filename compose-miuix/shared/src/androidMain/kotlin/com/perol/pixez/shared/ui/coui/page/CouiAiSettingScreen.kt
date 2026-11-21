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
import com.perol.pixez.shared.ui.coui.core.CouiRadioOptionRow
import com.perol.pixez.shared.ui.coui.core.CouiRowDivider
import com.perol.pixez.shared.ui.coui.core.CouiSettingsScaffold
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch

/**
 * COUI AI 作品显示设置页：设置骨架 + 单选分组卡片。
 *
 * 两个互斥选项与服务器同步逻辑与 MIUIX 版一致；选中态由官方的 RadioButton 指示器承担，
 * 整行 selectable 提供点击与无障碍语义，提交期间两行同时禁用以防重复提交。
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

    CouiSettingsScaffold(
        title = strings.userAISettings,
        onBack = onBack,
    ) {
        item(key = "coui_ai_display", contentType = "section") {
            CouiSettingSection(title = strings.aiDisplayOptions) {
                CouiRadioOptionRow(
                    title = strings.aiDisplayShow,
                    selected = currentShowAI,
                    onClick = { changeShowAI(true) },
                    summary = strings.aiDisplayShowSummary,
                    enabled = !isUpdating,
                )
                CouiRowDivider()
                CouiRadioOptionRow(
                    title = strings.aiDisplayHidePartial,
                    selected = !currentShowAI,
                    onClick = { changeShowAI(false) },
                    summary = strings.aiDisplayHidePartialSummary,
                    enabled = !isUpdating,
                )
            }
        }
    }
}
