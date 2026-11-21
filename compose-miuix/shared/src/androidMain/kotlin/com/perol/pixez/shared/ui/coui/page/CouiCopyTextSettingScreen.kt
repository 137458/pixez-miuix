package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.coui.component.CouiAssistChip
import com.perol.pixez.shared.ui.coui.component.CouiTextInput
import com.perol.pixez.shared.ui.coui.core.CouiSettingsScaffold
import com.perol.pixez.shared.ui.coui.core.CouiTopBarIconButton
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.screens.DEFAULT_COPY_TEXT_FORMAT
import com.perol.pixez.shared.ui.utils.insertTextAtSelection
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Refresh

/**
 * COUI 分享格式设置页：折叠大标题顶栏（重置 + 保存两个顶栏动作）+ 模板编辑卡片 + 占位符胶囊。
 *
 * 骨架改用 [CouiSettingsScaffold]：居上限宽、overscroll、底部悬浮底栏留白与顶栏覆盖区补偿
 * 都由骨架代付，页面因此不再自铺底色、不加状态栏内边距、也不再二次扣底部安全区。
 * 顶栏动作走 core 的 [CouiTopBarIconButton]（API 34+ 官方材质按钮，31-33 自绘平替），
 * 它依赖顶栏下发的内容色与材质，只允许在顶栏槽内调用。
 *
 * 模板语义、占位符插入光标逻辑与保存时机不变：保存仍是把输入框文本写回
 * settingsRepository.copyInfoText 后再 onBack()；COUI 库没有多行输入原语，
 * 模板输入框继续走 COUI 侧统一的 CouiTextInput（多行 + 120dp 起始高度）。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CouiCopyTextSettingScreen(
    onBack: () -> Unit,
) {
    val settingsRepository = LocalSettingsRepository.current
    val strings = LocalStrings.current

    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = settingsRepository.copyInfoText,
                selection = TextRange(settingsRepository.copyInfoText.length),
            ),
        )
    }

    val placeholderChips = remember(strings) {
        listOf(
            strings.copyTextChipTitle to "{title}",
            strings.copyTextChipIllustId to "{illust_id}",
            strings.copyTextChipUserId to "{user_id}",
            strings.copyTextChipUserName to "{user_name}",
            strings.copyTextChipTags to "{tags}",
            strings.copyTextChipArtworkUrl to AppConstants.Share.ARTWORK_URL_PLACEHOLDER,
            strings.copyTextChipUserUrl to AppConstants.Share.USER_URL_PLACEHOLDER,
        )
    }

    CouiSettingsScaffold(
        title = strings.copyTextFormatTitle,
        onBack = onBack,
        actions = {
            CouiTopBarIconButton(
                onClick = {
                    textFieldValue = TextFieldValue(
                        text = DEFAULT_COPY_TEXT_FORMAT,
                        selection = TextRange(DEFAULT_COPY_TEXT_FORMAT.length),
                    )
                },
                icon = MiuixIcons.Refresh,
                contentDescription = strings.copyTextFormatReset,
            )
            CouiTopBarIconButton(
                onClick = {
                    settingsRepository.copyInfoText = textFieldValue.text
                    onBack()
                },
                icon = MiuixIcons.Ok,
                contentDescription = strings.complete,
            )
        },
    ) {
        item(key = "coui_copy_text_template", contentType = "section") {
            CouiSettingSection(title = strings.copyTextFormatSectionTemplate) {
                CouiCopyTextField(
                    value = textFieldValue,
                    onValueChange = { textFieldValue = it },
                )
            }
        }

        item(key = "coui_copy_text_chips", contentType = "section") {
            CouiSettingSection(title = strings.copyTextFormatSectionChips) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    placeholderChips.forEach { (label, text) ->
                        CouiAssistChip(
                            text = label,
                            selected = false,
                            onClick = {
                                textFieldValue = insertTextAtSelection(textFieldValue, text)
                            },
                        )
                    }
                }
            }
        }
    }
}

/** 多行模板输入框：统一走 CouiTextInput，仅保留 120dp 起始高度与多行光标。 */
@Composable
private fun CouiCopyTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
) {
    CouiTextInput(
        value = value,
        onValueChange = onValueChange,
        // 多行模板文本：保留原先 120dp 起的高度，光标从左上开始。
        singleLine = false,
        minHeight = 120.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}
