package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.CONTRIBUTORS
import com.perol.pixez.shared.data.model.Contributor
import com.perol.pixez.shared.platform.openBrowser
import com.perol.pixez.shared.ui.AppInfo
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.components.ToastMessage
import com.perol.pixez.shared.ui.i18n.AppStrings
import com.perol.pixez.shared.ui.i18n.LocalStrings
import io.github.aakira.napier.Napier
import org.jetbrains.compose.resources.painterResource
import pixez_miuix.shared.generated.resources.Res
import pixez_miuix.shared.generated.resources.ic_pixez_logo
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.i18n.LocalStrings

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
    onUpdateClick: (() -> Unit)? = null,
) {
    val strings = LocalStrings.current
    // 用于提示打开浏览器失败等信息。
    var toastMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = strings.settingAbout,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = strings.back,
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
            Spacer(modifier = Modifier.height(20.dp))

            // 顶部 Squircle 应用图标
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MiuixTheme.colorScheme.surfaceContainer),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_pixez_logo),
                    contentDescription = "PixEz Logo",
                    modifier = Modifier.size(72.dp),
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "PixEz MIUIX",
                style = MiuixTheme.textStyles.title2.copy(fontWeight = FontWeight.Bold),
            )
            Row(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(MiuixTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "v${AppInfo.VERSION_NAME}",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            Text(
                text = strings.aboutDesc,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp),
            )

            SmallTitle(text = strings.aboutDevelopers, modifier = Modifier.fillMaxWidth())
            Card(modifier = Modifier.fillMaxWidth()) {
                DeveloperRow(
                    name = "Perol_Notsfsssf",
                    message = "Founder & Maintainer",
                    avatarUrl = "https://avatars.githubusercontent.com/u/31962397?v=4",
                    onClick = { openUrlOrToast("https://github.com/Notsfsssf", strings) { toastMessage = it } },
                )
                DeveloperRow(
                    name = "Right now",
                    message = "Design & Multiplatform",
                    avatarUrl = "https://avatars.githubusercontent.com/u/104149371?v=4",
                    onClick = { openUrlOrToast("https://github.com/137458", strings) { toastMessage = it } },
                )
            }

            SmallTitle(text = strings.aboutContributors, modifier = Modifier.fillMaxWidth())
            Card(modifier = Modifier.fillMaxWidth()) {
                ContributorsRow(
                    contributors = CONTRIBUTORS,
                    onUrlClick = { url ->
                        openUrlOrToast(url, strings) { toastMessage = it }
                    },
                )
            }

            SmallTitle(text = strings.aboutProject, modifier = Modifier.fillMaxWidth())
            Card(modifier = Modifier.fillMaxWidth()) {
                BasicComponent(
                    title = strings.aboutRepo,
                    summary = "github.com/137458/pixez-miuix",
                    onClick = {
                        openUrlOrToast(AppConstants.Urls.GITHUB_REPO, strings) { toastMessage = it }
                    },
                )
                BasicComponent(
                    title = strings.aboutFeedback,
                    summary = "PxezFeedBack@outlook.com",
                    onClick = {
                        openUrlOrToast("mailto:PxezFeedBack@outlook.com", strings) { toastMessage = it }
                    },
                )
                BasicComponent(
                    title = strings.settingThanks,
                    summary = strings.aboutThanksDesc,
                    onClick = onThanksClick,
                )
                if (onUpdateClick != null) {
                    BasicComponent(
                        title = strings.settingUpdate,
                        summary = "${strings.version} v${AppInfo.VERSION_NAME}",
                        onClick = onUpdateClick,
                    )
                }
            }

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
    avatarUrl: String? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.noRippleClickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!avatarUrl.isNullOrBlank()) {
            PixivAsyncImage(
                model = avatarUrl,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = name.firstOrNull()?.uppercase() ?: "?",
                    style = MiuixTheme.textStyles.body1.copy(fontWeight = FontWeight.Bold),
                    color = MiuixTheme.colorScheme.primary,
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = name,
                style = MiuixTheme.textStyles.body1.copy(fontWeight = FontWeight.SemiBold),
                color = MiuixTheme.colorScheme.onSurface,
            )
            Text(
                text = message,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

/**
 * 贡献者列表横向滚动行。
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        contributors.forEach { contributor ->
            ContributorAvatarItem(
                contributor = contributor,
                onClick = { onUrlClick(contributor.url) },
            )
        }
    }
}

/**
 * 单个贡献者头像项。
 */
@Composable
private fun ContributorAvatarItem(
    contributor: Contributor,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .noRippleClickable(onClick = onClick)
            .width(56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (!contributor.avatar.isNullOrBlank()) {
            PixivAsyncImage(
                model = contributor.avatar,
                contentDescription = contributor.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = contributor.name.firstOrNull()?.uppercase() ?: "?",
                    style = MiuixTheme.textStyles.body1.copy(fontWeight = FontWeight.Bold),
                    color = MiuixTheme.colorScheme.primary,
                )
            }
        }
        Text(
            text = contributor.name,
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

/**
 * 无涟漪点击修饰符（保持 MIUIX 统一触摸反馈规范）。
 */
private fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick,
    )
}

/**
 * 安全打开外部链接或展示 Toast 错误提示。
 */
private fun openUrlOrToast(url: String, strings: AppStrings, onError: (String) -> Unit) {
    try {
        openBrowser(url)
    } catch (e: Exception) {
        Napier.w(tag = "AboutScreen", throwable = e) { "Failed to open url: $url" }
        onError("${strings.loadFailed}: ${e.message ?: strings.loadFailed}")
    }
}
