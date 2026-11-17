package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.coui.component.CouiActionButton
import com.perol.pixez.shared.ui.coui.component.CouiAssistChip
import com.perol.pixez.shared.ui.coui.component.CouiFixedTopBar
import com.perol.pixez.shared.ui.coui.component.CouiReadableColumn
import com.perol.pixez.shared.ui.coui.component.CouiSettingsLazyColumn
import com.perol.pixez.shared.ui.coui.component.CouiTextInput
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.screens.DEFAULT_COPY_TEXT_FORMAT
import com.perol.pixez.shared.ui.utils.insertTextAtSelection
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.MiuixIcons

/**
 * COUI 分享格式设置页：固定顶栏（重置 + 保存）+ 模板编辑卡片 + 占位符胶囊。
 *
 * 模板语义、占位符插入光标逻辑与保存时机不变；COUI 库无多行输入原语，
 * 模板输入框走 COUI 侧统一的 CouiTextInput（多行 + 120dp 起始高度）。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CouiCopyTextSettingScreen(
    onBack: () -> Unit,
) {
    val settingsRepository = LocalSettingsRepository.current
    val strings = LocalStrings.current
    val scheme = CouiTheme.colorScheme

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background),
    ) {
        CouiFixedTopBar(
            title = strings.copyTextFormatTitle,
            onBack = onBack,
        ) {
            CouiActionButton(
                icon = MiuixIcons.Refresh,
                contentDescription = strings.copyTextFormatReset,
                onClick = {
                    textFieldValue = TextFieldValue(
                        text = DEFAULT_COPY_TEXT_FORMAT,
                        selection = TextRange(DEFAULT_COPY_TEXT_FORMAT.length),
                    )
                },
            )
            CouiActionButton(
                icon = MiuixIcons.Ok,
                contentDescription = strings.complete,
                onClick = {
                    settingsRepository.copyInfoText = textFieldValue.text
                    onBack()
                },
            )
        }

        CouiReadableColumn(modifier = Modifier.fillMaxSize()) {
            CouiSettingsLazyColumn {
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
                                .padding(horizontal = 12.dp, vertical = 12.dp),
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
            .padding(12.dp),
    )
}
