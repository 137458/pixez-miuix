package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.itos.coui.theme.LocalCouiTextStyle
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.components.LocalToastSink
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.components.ToastType
import com.perol.pixez.shared.ui.coui.component.CouiActionButton
import com.perol.pixez.shared.ui.coui.component.CouiRadioRow
import com.perol.pixez.shared.ui.coui.component.CouiReadableColumn
import com.perol.pixez.shared.ui.coui.component.CouiTextAction
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.screens.DEFAULT_IMAGE_HOST
import com.perol.pixez.shared.ui.screens.MIRROR_IMAGE_HOST
import com.perol.pixez.shared.ui.screens.NETWORK_MODE_STANDARD
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back

/**
 * COUI 网络设置页：固定顶栏 + OAuth/API 模式单选卡片 + 图片源卡片。
 *
 * 模式切换与图片源重置、自定义 Host 校验口径完全保留；
 * 切到 standard 时清空自定义输入并回落到默认 Host；
 * COUI 库无单选与输入原语，分别使用共用 [CouiRadioRow] 与自绘输入框。
 */
@Composable
internal fun CouiNetworkSettingScreen(
    onBack: () -> Unit,
) {
    val settingsRepository = LocalSettingsRepository.current
    val strings = LocalStrings.current
    val toastSink = LocalToastSink.current
    val scheme = CouiTheme.colorScheme

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
                text = strings.settingNetwork,
                kind = CouiTextKind.ItemTitle,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
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
                item(key = "coui_network_oauth", contentType = "section") {
                    CouiSettingSection(title = strings.oauthNetworkMode) {
                        networkModes.forEach { (code, label, description) ->
                            CouiRadioRow(
                                title = label,
                                summary = description,
                                selected = oauthNetworkMode == code,
                                onClick = { setOAuthNetworkMode(code) },
                            )
                        }
                    }
                }

                item(key = "coui_network_api", contentType = "section") {
                    CouiSettingSection(title = strings.apiNetworkMode) {
                        networkModes.forEach { (code, label, description) ->
                            CouiRadioRow(
                                title = label,
                                summary = description,
                                selected = apiNetworkMode == code,
                                onClick = { setApiNetworkMode(code) },
                            )
                        }
                    }
                }

                if (allowsImageSource) {
                    item(key = "coui_network_picture_source", contentType = "section") {
                        CouiSettingSection(title = strings.pictureSource) {
                            CouiRadioRow(
                                title = DEFAULT_IMAGE_HOST,
                                summary = DEFAULT_IMAGE_HOST,
                                selected = pictureSource == DEFAULT_IMAGE_HOST,
                                onClick = { setPresetPictureSource(DEFAULT_IMAGE_HOST) },
                            )
                            CouiRadioRow(
                                title = MIRROR_IMAGE_HOST,
                                summary = MIRROR_IMAGE_HOST,
                                selected = pictureSource == MIRROR_IMAGE_HOST,
                                onClick = { setPresetPictureSource(MIRROR_IMAGE_HOST) },
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = strings.customHost,
                                    kind = CouiTextKind.ItemTitle,
                                    color = scheme.onSurface,
                                )
                                CouiNetworkHostField(
                                    value = customHostInput,
                                    placeholder = strings.customHost,
                                    onValueChange = { customHostInput = it },
                                )
                                CouiTextAction(
                                    text = strings.confirm,
                                    color = scheme.accent,
                                    onClick = { setCustomPictureSource(customHostInput) },
                                )
                            }
                        }
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
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
