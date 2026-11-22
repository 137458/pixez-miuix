package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.itos.coui.theme.LocalCouiTextStyle
import com.perol.pixez.shared.ui.components.LocalToastSink
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.components.ToastType
import com.perol.pixez.shared.ui.coui.core.CouiItemTitle
import com.perol.pixez.shared.ui.coui.core.CouiRowDivider
import com.perol.pixez.shared.ui.coui.core.CouiSettingsScaffold
import com.perol.pixez.shared.ui.coui.core.CouiTextButton
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.screens.DEFAULT_IMAGE_HOST
import com.perol.pixez.shared.ui.screens.MIRROR_IMAGE_HOST
import com.perol.pixez.shared.ui.screens.NETWORK_MODE_STANDARD

/** 解析当前图片源在预设/自定义下拉列表中的选中下标（0 默认 / 1 镜像 / 2 自定义）。 */
internal fun resolveCouiPictureSourceOptionIndex(pictureSource: String): Int = when (pictureSource) {
    DEFAULT_IMAGE_HOST -> 0
    MIRROR_IMAGE_HOST -> 1
    else -> 2
}

/**
 * COUI 网络设置页：设置骨架 + 官方 Group 卡片（OAuth/API 模式与图片源下拉弹窗）。
 *
 * 模式切换与图片源重置、自定义 Host 校验口径完全保留；
 * 切到 standard 时清空自定义输入并回落到默认 Host；
 * 单选切换由共享下拉弹窗 [CouiMenuSelectRow] 承担，自定义 Host 输入框仍为 COUI 自绘。
 */
@Composable
internal fun CouiNetworkSettingScreen(
    onBack: () -> Unit,
) {
    val settingsRepository = LocalSettingsRepository.current
    val strings = LocalStrings.current
    val toastSink = LocalToastSink.current

    var oauthNetworkMode by remember { mutableStateOf(settingsRepository.oauthNetworkMode) }
    var apiNetworkMode by remember { mutableStateOf(settingsRepository.apiNetworkMode) }
    var pictureSource by remember { mutableStateOf(settingsRepository.pictureSource) }

    var customHostInput by rememberSaveable(apiNetworkMode) {
        mutableStateOf(
            if (pictureSource == DEFAULT_IMAGE_HOST || pictureSource == MIRROR_IMAGE_HOST) {
                ""
            } else {
                pictureSource
            },
        )
    }

    val allowsImageSource = apiNetworkMode != NETWORK_MODE_STANDARD

    fun setOAuthNetworkMode(mode: String) {
        oauthNetworkMode = mode
        settingsRepository.oauthNetworkMode = mode
    }

    fun setApiNetworkMode(mode: String) {
        apiNetworkMode = mode
        settingsRepository.apiNetworkMode = mode
        if (mode == NETWORK_MODE_STANDARD) {
            pictureSource = DEFAULT_IMAGE_HOST
            settingsRepository.pictureSource = DEFAULT_IMAGE_HOST
            customHostInput = ""
        }
    }

    fun setPresetPictureSource(host: String) {
        pictureSource = host
        settingsRepository.pictureSource = host
        customHostInput = ""
    }

    fun setCustomPictureSource(host: String) {
        val trimmed = host.trim()
        if (trimmed.isEmpty()) {
            toastSink(ToastData(strings.hostNotEmpty, ToastType.Error))
            return
        }
        if (trimmed.contains(" ")) {
            toastSink(ToastData(strings.hostNoSpace, ToastType.Error))
            return
        }
        pictureSource = trimmed
        settingsRepository.pictureSource = trimmed
    }

    val networkModes = listOf(
        Triple("ech", "ECH", strings.networkModeEchSummary),
        Triple("compat", strings.networkModeCompat, strings.networkModeCompatSummary),
        Triple("standard", strings.networkModeStandard, strings.networkModeStandardSummary),
    )

    val oauthSelectedIndex = networkModes.indexOfFirst { it.first == oauthNetworkMode }.coerceAtLeast(0)
    val apiSelectedIndex = networkModes.indexOfFirst { it.first == apiNetworkMode }.coerceAtLeast(0)
    val pictureSourceOptions = remember(strings) {
        listOf(DEFAULT_IMAGE_HOST, MIRROR_IMAGE_HOST, strings.customHost)
    }
    val pictureSourceIndex = resolveCouiPictureSourceOptionIndex(pictureSource)
    val customHostFocusRequester = remember { FocusRequester() }

    CouiSettingsScaffold(
        title = strings.settingNetwork,
        onBack = onBack,
    ) {
        item(key = "coui_network_modes", contentType = "section") {
            CouiSettingSection(title = strings.settingNetwork) {
                CouiMenuSelectRow(
                    title = strings.oauthNetworkMode,
                    items = networkModes.map { it.second },
                    selectedIndex = oauthSelectedIndex,
                    summary = networkModes[oauthSelectedIndex].third,
                    onSelect = { index ->
                        networkModes.getOrNull(index)?.first?.let(::setOAuthNetworkMode)
                    },
                )
                CouiRowDivider()
                CouiMenuSelectRow(
                    title = strings.apiNetworkMode,
                    items = networkModes.map { it.second },
                    selectedIndex = apiSelectedIndex,
                    summary = networkModes[apiSelectedIndex].third,
                    onSelect = { index ->
                        networkModes.getOrNull(index)?.first?.let(::setApiNetworkMode)
                    },
                )
            }
        }

        if (allowsImageSource) {
            item(key = "coui_network_picture_source", contentType = "section") {
                CouiSettingSection(title = strings.pictureSource) {
                    CouiMenuSelectRow(
                        title = strings.pictureSource,
                        items = pictureSourceOptions,
                        selectedIndex = pictureSourceIndex,
                        summary = if (pictureSourceIndex == 2) pictureSource else null,
                        onSelect = { index ->
                            when (index) {
                                0 -> setPresetPictureSource(DEFAULT_IMAGE_HOST)
                                1 -> setPresetPictureSource(MIRROR_IMAGE_HOST)
                                2 -> customHostFocusRequester.requestFocus()
                            }
                        },
                    )
                    CouiRowDivider()
                    // 自定义 Host：分组内独立区块，留白与官方 Item 的 rowPadding 对齐。
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CouiItemTitle(text = strings.customHost)
                        CouiNetworkHostField(
                            value = customHostInput,
                            placeholder = strings.customHost,
                            onValueChange = { customHostInput = it },
                            focusRequester = customHostFocusRequester,
                        )
                        CouiTextButton(
                            text = strings.confirm,
                            onClick = { setCustomPictureSource(customHostInput) },
                            filled = false,
                        )
                    }
                }
            }
        }
    }
}

/** 自定义 Host 输入框：COUI 容器底色 + 圆角。 */
@Composable
private fun CouiNetworkHostField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
) {
    val scheme = CouiTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(scheme.container)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                kind = CouiTextKind.ItemSummary,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = LocalCouiTextStyle.current.copy(color = scheme.onSurface),
            cursorBrush = SolidColor(scheme.accent),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
        )
    }
}
