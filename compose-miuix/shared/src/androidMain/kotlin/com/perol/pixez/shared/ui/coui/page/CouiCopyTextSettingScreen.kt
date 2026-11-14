package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.itos.coui.theme.LocalCouiTextStyle
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.coui.component.CouiActionButton
import com.perol.pixez.shared.ui.coui.component.CouiAssistChip
import com.perol.pixez.shared.ui.coui.component.CouiReadableColumn
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.screens.DEFAULT_COPY_TEXT_FORMAT
import com.perol.pixez.shared.ui.screens.insertTextAtSelection
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Refresh

/**
 * COUI 分享格式设置页：固定顶栏（重置 + 保存）+ 模板编辑卡片 + 占位符胶囊。
 *
 * 模板语义、占位符插入光标逻辑与保存时机不变；COUI 库无多行输入原语，
 * 模板输入框按主题令牌自绘多行 `BasicTextField`。
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(scheme.background)
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CouiActionButton(
                icon = MiuixIcons.Back,
                contentDescription = strings.back,
                onClick = onBack,
            )
            Text(
                text = strings.copyTextFormatTitle,
                kind = CouiTextKind.ItemTitle,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
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

/** 多行模板输入框：COUI 容器底色 + 圆角，跟随主题文字样式。 */
@Composable
private fun CouiCopyTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
) {
    val scheme = CouiTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .heightIn(min = 120.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(scheme.container)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = LocalCouiTextStyle.current.copy(color = scheme.onSurface),
            cursorBrush = SolidColor(scheme.accent),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
