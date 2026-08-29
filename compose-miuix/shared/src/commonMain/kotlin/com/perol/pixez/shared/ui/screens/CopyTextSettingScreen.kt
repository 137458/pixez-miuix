package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.settings.SettingsRepository
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.perol.pixez.shared.ui.AppConstants
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior

/**
 * 分享格式设置页：编辑复制到剪贴板时的作品信息模板。
 *
 * @param settingsRepository 设置仓库，用于读写分享格式模板。
 * @param onBack 返回上一级页面。
 */
@Composable
fun CopyTextSettingScreen(
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
) {
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current

    // 使用 TextFieldValue 以便跟踪和操控光标/选区。
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = settingsRepository.copyInfoText,
                selection = TextRange(settingsRepository.copyInfoText.length),
            )
        )
    }

    val placeholderChips = remember(strings) {
        listOf(
            PlaceholderChip(label = strings.copyTextChipTitle, text = "{title}"),
            PlaceholderChip(label = strings.copyTextChipIllustId, text = "{illust_id}"),
            PlaceholderChip(label = strings.copyTextChipUserId, text = "{user_id}"),
            PlaceholderChip(label = strings.copyTextChipUserName, text = "{user_name}"),
            PlaceholderChip(label = strings.copyTextChipTags, text = "{tags}"),
            PlaceholderChip(
                label = strings.copyTextChipArtworkUrl,
                text = "https://www.pixiv.net/artworks/{illust_id}",
            ),
            PlaceholderChip(
                label = strings.copyTextChipUserUrl,
                text = "https://www.pixiv.net/users/{user_id}",
            ),
        )
    }

    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = strings.copyTextFormatTitle,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = strings.back,
                        )
                    }
                },
                actions = {
                    // 重置为默认模板。
                    IconButton(
                        onClick = {
                            textFieldValue = TextFieldValue(
                                text = DEFAULT_COPY_TEXT_FORMAT,
                                selection = TextRange(DEFAULT_COPY_TEXT_FORMAT.length),
                            )
                        }
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Refresh,
                            contentDescription = strings.copyTextFormatReset,
                        )
                    }
                    // 保存并返回。
                    IconButton(
                        onClick = {
                            settingsRepository.copyInfoText = textFieldValue.text
                            onBack()
                        }
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Ok, // 保存：MIUIX 无 Save，用 Ok 语义最接近
                            contentDescription = strings.complete,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                contentPadding = paddingValues,
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = AppConstants.Layout.TABLET_CONTENT_MAX_WIDTH_DP.dp)
                    .fillMaxWidth()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
            ) {
                item {
                    top.yukonga.miuix.kmp.basic.SmallTitle(text = strings.copyTextFormatSectionTemplate)
                    top.yukonga.miuix.kmp.basic.Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        TextField(
                            value = textFieldValue,
                            onValueChange = { textFieldValue = it },
                            label = strings.copyTextFormatLabel,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            maxLines = 10,
                        )
                    }
                }
                item {
                    top.yukonga.miuix.kmp.basic.SmallTitle(text = strings.copyTextFormatSectionChips)
                    top.yukonga.miuix.kmp.basic.Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            placeholderChips.forEach { chip ->
                                InsertChip(
                                    label = chip.label,
                                    onClick = {
                                        textFieldValue = insertTextAtSelection(
                                            textFieldValue,
                                            chip.text,
                                        )
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

/**
 * 占位符 chip 数据：展示文案与插入文本。
 */
private data class PlaceholderChip(
    val label: String,
    val text: String,
)

/**
 * 占位符 chip：点击后在文本框当前光标/选区处插入对应文本。
 */
@Composable
private fun InsertChip(
    label: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(),
        minHeight = 32.dp,
        insideMargin = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.footnote2,
        )
    }
}

/**
 * 在 TextFieldValue 的当前选区位置插入文本。
 * 若存在选区则替换选区内容，否则在光标处插入。
 * 插入完成后将光标置于插入文本末尾。
 */
private fun insertTextAtSelection(
    textFieldValue: TextFieldValue,
    insertText: String,
): TextFieldValue {
    val selection = textFieldValue.selection
    val newText = textFieldValue.text.replaceRange(
        selection.start,
        selection.end,
        insertText,
    )
    val newCursor = selection.start + insertText.length
    return textFieldValue.copy(
        text = newText,
        selection = TextRange(newCursor, newCursor),
    )
}

/**
 * 默认分享格式模板，与原 Flutter 版保持一致。
 */
private const val DEFAULT_COPY_TEXT_FORMAT = "title:{title}\npainter:{user_name}\nillust id:{illust_id}"

