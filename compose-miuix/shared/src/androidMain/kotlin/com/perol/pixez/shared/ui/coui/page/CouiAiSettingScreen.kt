package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.components.LocalToastSink
import com.perol.pixez.shared.ui.components.PixivApiErrorFormatter
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.components.ToastType
import com.perol.pixez.shared.ui.coui.component.CouiFixedTopBar
import com.perol.pixez.shared.ui.coui.component.CouiRadioRow
import com.perol.pixez.shared.ui.coui.component.CouiReadableColumn
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.LocalUserRepository
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.MiuixIcons

/**
 * COUI AI 作品显示设置页：固定顶栏 + 分组卡片。
 *
 * 两个互斥选项与服务器同步逻辑与 MIUIX 版一致；COUI 库无单选控件，
 * 选中态用行尾勾选图标表达，提交期间禁用两行以防重复提交。
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CouiTheme.colorScheme.background),
    ) {
        CouiFixedTopBar(
            title = strings.userAISettings,
            onBack = onBack,
        )

        CouiReadableColumn(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    top = 8.dp,
                    end = 12.dp,
                    bottom = LocalBottomBarContentPadding.current,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                overscrollEffect = rememberCouiOverscrollEffect(),
            ) {
                item(key = "coui_ai_display", contentType = "section") {
                    CouiSettingSection(title = strings.aiDisplayOptions) {
                        CouiRadioRow(
                            title = strings.aiDisplayShow,
                            summary = strings.aiDisplayShowSummary,
                            selected = currentShowAI,
                            enabled = !isUpdating,
                            onClick = { changeShowAI(true) },
                        )
                        CouiRadioRow(
                            title = strings.aiDisplayHidePartial,
                            summary = strings.aiDisplayHidePartialSummary,
                            selected = !currentShowAI,
                            enabled = !isUpdating,
                            onClick = { changeShowAI(false) },
                        )                    }
                }
            }
        }
    }
}
