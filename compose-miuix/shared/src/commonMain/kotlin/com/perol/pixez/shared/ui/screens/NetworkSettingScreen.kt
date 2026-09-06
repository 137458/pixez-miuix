package com.perol.pixez.shared.ui.screens

import com.perol.pixez.shared.ui.components.BlurredBar
import com.perol.pixez.shared.ui.components.rememberBlurBackdrop

import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.perol.pixez.shared.ui.components.LocalBackdrop
import com.perol.pixez.shared.ui.components.blurBackdropSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.components.CheckIndicator
import com.perol.pixez.shared.ui.components.ToastMessage
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.perol.pixez.shared.ui.AppConstants
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import androidx.compose.foundation.background
import top.yukonga.miuix.kmp.blur.layerBackdrop

/**
 * 网络设置页：提供 OAuth / API 服务网络模式切换与图片源选择。
 *
 * @param settingsRepository 设置仓库，用于读写网络相关偏好。
 * @param onBack 返回上一级页面。
 */
@Composable
fun NetworkSettingScreen(
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
) {
    // 页面状态：从 SettingsRepository 读取当前网络设置。
    var oauthNetworkMode by remember { mutableStateOf(settingsRepository.oauthNetworkMode) }
    var apiNetworkMode by remember { mutableStateOf(settingsRepository.apiNetworkMode) }
    var pictureSource by remember { mutableStateOf(settingsRepository.pictureSource) }

    // 自定义 Host 输入框状态。
    var customHostInput by rememberSaveable(apiNetworkMode) {
        mutableStateOf(
            if (pictureSource == DEFAULT_IMAGE_HOST || pictureSource == MIRROR_IMAGE_HOST) {
                ""
            } else {
                pictureSource
            },
        )
    }

    // 提示信息状态。
    var toastMessage by rememberSaveable { mutableStateOf<String?>(null) }

    // 当前 API 网络模式是否允许选择图片源（standard 模式不显示）。
    val allowsImageSource = apiNetworkMode != NETWORK_MODE_STANDARD

    /**
     * 设置 OAuth 网络模式。
     */
    fun setOAuthNetworkMode(mode: String) {
        oauthNetworkMode = mode
        settingsRepository.oauthNetworkMode = mode
    }

    /**
     * 设置 API 服务网络模式；切换到 standard 时自动将图片源重置为默认 Host。
     */
    fun setApiNetworkMode(mode: String) {
        apiNetworkMode = mode
        settingsRepository.apiNetworkMode = mode
        if (mode == NETWORK_MODE_STANDARD) {
            pictureSource = DEFAULT_IMAGE_HOST
            settingsRepository.pictureSource = DEFAULT_IMAGE_HOST
            customHostInput = ""
        }
    }

    /**
     * 设置图片源为预设 Host。
     */
    fun setPresetPictureSource(host: String) {
        pictureSource = host
        settingsRepository.pictureSource = host
        customHostInput = ""
    }

    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current

    /**
     * 设置图片源为自定义 Host，并进行基础校验。
     */
    fun setCustomPictureSource(host: String) {
        val trimmed = host.trim()
        if (trimmed.isEmpty()) {
            toastMessage = strings.hostNotEmpty
            return
        }
        if (trimmed.contains(" ")) {
            toastMessage = strings.hostNoSpace
            return
        }
        pictureSource = trimmed
        settingsRepository.pictureSource = trimmed
        toastMessage = null
    }

    // 网络模式选项数据：code -> (label, description)。
    val networkModes = listOf(
        Triple("ech", "ECH", strings.networkModeEchSummary),
        Triple("compat", strings.networkModeCompat, strings.networkModeCompatSummary),
        Triple("standard", strings.networkModeStandard, strings.networkModeStandardSummary),
    )

    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val colorScheme = MiuixTheme.colorScheme

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            BlurredBar(
                backdrop = backdrop,
                scrollBehavior = scrollBehavior,
            ) {
                TopAppBar(
                    title = strings.settingNetwork,
                    scrollBehavior = scrollBehavior,
                    color = if (backdrop != null) Color.Transparent else colorScheme.surface,
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = strings.back,
                            )
                        }
                    },
                )
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.surface)
                .blurBackdropSource(backdrop),
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
                    SmallTitle(text = strings.oauthNetworkMode)
                    top.yukonga.miuix.kmp.basic.Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        networkModes.forEach { (code, label, description) ->
                            NetworkModeOption(
                                label = label,
                                description = description,
                                selected = oauthNetworkMode == code,
                                onClick = { setOAuthNetworkMode(code) },
                            )
                        }
                    }
                }

                item {
                    SmallTitle(text = strings.apiNetworkMode)
                    top.yukonga.miuix.kmp.basic.Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        networkModes.forEach { (code, label, description) ->
                            NetworkModeOption(
                                label = label,
                                description = description,
                                selected = apiNetworkMode == code,
                                onClick = { setApiNetworkMode(code) },
                            )
                        }
                    }
                }

                if (allowsImageSource) {
                    item {
                        SmallTitle(text = strings.pictureSource)
                        top.yukonga.miuix.kmp.basic.Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            BasicComponent(
                                title = DEFAULT_IMAGE_HOST,
                                summary = "i.pximg.net",
                                onClick = { setPresetPictureSource(DEFAULT_IMAGE_HOST) },
                                endActions = {
                                    CheckIndicator(selected = pictureSource == DEFAULT_IMAGE_HOST)
                                },
                            )
                            BasicComponent(
                                title = MIRROR_IMAGE_HOST,
                                summary = "pixiv.re",
                                onClick = { setPresetPictureSource(MIRROR_IMAGE_HOST) },
                                endActions = {
                                    CheckIndicator(selected = pictureSource == MIRROR_IMAGE_HOST)
                                },
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(text = strings.customHost)
                                TextField(
                                    value = customHostInput,
                                    onValueChange = { customHostInput = it },
                                    label = strings.customHost,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    TextButton(
                                        text = strings.confirm,
                                        onClick = { setCustomPictureSource(customHostInput) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.textButtonColorsPrimary(),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        ToastMessage(
            message = toastMessage,
            onDismiss = { toastMessage = null },
        )
    }
}

/**
 * 网络模式单选项：标题 + 描述 + 选中指示器。
 */
@Composable
private fun NetworkModeOption(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    BasicComponent(
        title = label,
        summary = description,
        onClick = onClick,
        endActions = {
            CheckIndicator(selected = selected)
        },
    )
}

/**
 * 默认图片 Host：对齐 Flutter 版 ImageCatHost。
 */
private const val DEFAULT_IMAGE_HOST = AppConstants.Network.HOST_PXIMG

/**
 * 镜像图片 Host：对齐 Flutter 版 ImageCatHost。
 */
private const val MIRROR_IMAGE_HOST = AppConstants.Network.HOST_PIXIV_RE

/**
 * standard 网络模式 code：该模式下不显示图片源选择。
 */
private const val NETWORK_MODE_STANDARD = "standard"
