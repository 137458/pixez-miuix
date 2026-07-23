package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.composed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.CONTRIBUTORS
import com.perol.pixez.shared.data.model.Contributor
import com.perol.pixez.shared.platform.openBrowser
import com.perol.pixez.shared.ui.AppInfo
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.components.ToastMessage
import io.github.aakira.napier.Napier
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 关于页：展示应用信息、开发者、贡献者、项目仓库与反馈入口。
 *
 * @param onBack 返回上一级页面。
 * @param onThanksClick 打开致谢页。
 */
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onThanksClick: () -> Unit,
) {
    // 用于提示打开浏览器失败等信息。
    var toastMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "关于 PixEz",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = "返回",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "PixEz",
                style = MiuixTheme.textStyles.title1,
            )
            Text(
                text = "版本 ${AppInfo.VERSION_NAME}",
                style = MiuixTheme.textStyles.body1,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = "使用 MIUIX + Compose Multiplatform 重构的 PixEz 第三方客户端，保留原应用核心功能与交互。",
                style = MiuixTheme.textStyles.body2,
                modifier = Modifier.padding(top = 16.dp),
            )

            SmallTitle(text = "开发者")
            DeveloperRow(name = "Perol_Notsfsssf", message = "最初开发与维护")
            DeveloperRow(name = "Right now", message = "设计与开发支持")

            SmallTitle(text = "贡献者")
            ContributorsRow(
                contributors = CONTRIBUTORS,
                onUrlClick = { url ->
                    openUrlOrToast(url) { toastMessage = it }
                },
            )

            SmallTitle(text = "项目")
            BasicComponent(
                title = "项目仓库",
                summary = "github.com/Notsfsssf/pixez-flutter",
                onClick = {
                    openUrlOrToast("https://github.com/Notsfsssf/pixez-flutter") { toastMessage = it }
                },
            )
            BasicComponent(
                title = "反馈邮箱",
                summary = "PxezFeedBack@outlook.com",
                onClick = {
                    openUrlOrToast("mailto:PxezFeedBack@outlook.com") { toastMessage = it }
                },
            )
            BasicComponent(
                title = "支持 / 感谢",
                summary = "感谢帮助、支持与鼓励我的朋友们",
                onClick = onThanksClick,
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        ToastMessage(
            message = toastMessage,
            onDismiss = { toastMessage = null },
        )
    }
}

/**
 * 开发者信息行。
 */
@Composable
private fun DeveloperRow(
    name: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.first().toString(),
                style = MiuixTheme.textStyles.title2,
                color = MiuixTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, style = MiuixTheme.textStyles.title3)
            Text(
                text = message,
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

/**
 * 贡献者横向列表。
 */
@Composable
private fun ContributorsRow(
    contributors: List<Contributor>,
    onUrlClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        contributors.forEach { contributor ->
            ContributorCard(
                contributor = contributor,
                onClick = { onUrlClick(contributor.url) },
            )
        }
    }
}

/**
 * 贡献者卡片。
 */
@Composable
private fun ContributorCard(
    contributor: Contributor,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(80.dp)
            .background(
                color = MiuixTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(12.dp),
            )
            .clip(RoundedCornerShape(12.dp))
            .padding(8.dp)
            .then(Modifier.noRippleClickable(onClick = onClick)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        PixivAsyncImage(
            model = contributor.avatar,
            contentDescription = contributor.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
        )
        Text(
            text = contributor.name,
            style = MiuixTheme.textStyles.footnote1,
            maxLines = 1,
        )
        Text(
            text = contributor.content,
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            maxLines = 1,
        )
    }
}

/**
 * 打开 URL，失败时通过回调返回提示信息。
 */
private fun openUrlOrToast(url: String, onError: (String) -> Unit) {
    try {
        openBrowser(url)
    } catch (e: Exception) {
        Napier.e("打开链接失败 url=$url", e)
        onError("打开失败: ${e.message ?: "未知错误"}")
    }
}

/**
 * 无涟漪点击 Modifier。
 */
@Composable
private fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier = composed {
    clickable(
        indication = null,
        interactionSource = remember { MutableInteractionSource() },
        onClick = onClick,
    )
}
