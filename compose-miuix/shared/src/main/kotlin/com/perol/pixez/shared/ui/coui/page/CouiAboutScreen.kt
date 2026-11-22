package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.Dialog
import com.itos.coui.layout.Item
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.AppInfo
import com.perol.pixez.shared.data.model.Contributor
import com.perol.pixez.shared.data.model.CONTRIBUTORS
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.components.LocalToastSink
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.components.ToastType
import com.perol.pixez.shared.ui.coui.component.couiDialogMaterialOrNull
import com.perol.pixez.shared.ui.coui.component.couiDialogPlacement
import com.perol.pixez.shared.ui.coui.component.couiPrimaryActionColors
import com.perol.pixez.shared.ui.coui.core.CouiItemSummary
import com.perol.pixez.shared.ui.coui.core.CouiItemTitle
import com.perol.pixez.shared.ui.coui.core.CouiRowDivider
import com.perol.pixez.shared.ui.coui.core.CouiSettingsScaffold
import com.perol.pixez.shared.ui.coui.core.CouiValueRow
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.utils.openSafeUrl
import androidx.compose.ui.res.painterResource
import com.perol.pixez.shared.R

/**
 * COUI 关于页：折叠大标题顶栏 + 应用标识头 + 开发者 / 贡献者 / 项目三组入口。
 *
 * 骨架改用 [CouiSettingsScaffold]：居上限宽、overscroll、底部悬浮底栏留白、顶栏覆盖区
 * 补偿全部由骨架代付，页面不再自铺底色、不加状态栏内边距、也不再二次扣底部安全区。
 * 组内条目全部换成 core 行原语：开发者行是 44dp 头像的 [Item]（头像槽 + 标题摘要槽由官方
 * 行几何排版，点击与反馈由 Item 统一安装），项目入口是 [CouiValueRow]（副标题走 summary），
 * 行间统一插 [CouiRowDivider]；只有贡献者横滚条与品牌标识头是纯排版块，无行语义可用。
 *
 * 外链统一经 openSafeUrl，失败走 LocalToastSink 报错；免责声明弹窗仍走官方 Dialog。
 */
@Composable
internal fun CouiAboutScreen(
    onBack: () -> Unit,
    onThanksClick: () -> Unit,
    onUpdateClick: () -> Unit = {},
) {
    val strings = LocalStrings.current
    val toastSink = LocalToastSink.current
    val showErrorToast: (String) -> Unit = { message ->
        toastSink(ToastData(message, ToastType.Error))
    }
    var showDisclaimerDialog by remember { mutableStateOf(false) }

    CouiSettingsScaffold(
        title = strings.settingAbout,
        onBack = onBack,
    ) {
        item(key = "coui_about_header", contentType = "header") {
            CouiAboutHeader()
        }

        item(key = "coui_about_developers", contentType = "section") {
            CouiSettingSection(title = strings.aboutDevelopers) {
                CouiDeveloperRow(
                    name = "Perol_Notsfsssf",
                    message = strings.roleFounderMaintainer,
                    avatarUrl = "https://avatars.githubusercontent.com/u/31962397?v=4",
                    onClick = {
                        openSafeUrl(
                            AppConstants.Urls.AUTHOR_NOTSFSSSF,
                            strings,
                            onError = showErrorToast,
                        )
                    },
                )
                // 分隔线缩进对齐头像右侧文字起点：行内留白 16 + 头像 44 + 图标间距 16。
                CouiRowDivider(startIndent = 76.dp)
                CouiDeveloperRow(
                    name = "137458 (Rosemary)",
                    message = strings.roleMiuixMaintainer,
                    avatarUrl = "https://avatars.githubusercontent.com/u/138956834?v=4",
                    onClick = {
                        openSafeUrl(
                            AppConstants.Urls.AUTHOR_ROSEMARY,
                            strings,
                            onError = showErrorToast,
                        )
                    },
                )
            }
        }

        item(key = "coui_about_contributors", contentType = "section") {
            CouiSettingSection(title = strings.aboutContributors) {
                CouiContributorsRow(
                    contributors = CONTRIBUTORS,
                    onUrlClick = { url ->
                        openSafeUrl(url, strings, onError = showErrorToast)
                    },
                )
            }
        }

        item(key = "coui_about_project", contentType = "section") {
            CouiSettingSection(title = strings.aboutProject) {
                CouiValueRow(
                    title = strings.aboutRepo,
                    summary = "github.com/137458/pixez-miuix",
                    onClick = {
                        openSafeUrl(AppConstants.Urls.GITHUB_REPO, strings, onError = showErrorToast)
                    },
                )
                CouiRowDivider()
                CouiValueRow(
                    title = strings.aboutFeedback,
                    summary = AppConstants.Urls.FEEDBACK_EMAIL,
                    onClick = {
                        openSafeUrl(AppConstants.Urls.FEEDBACK_MAILTO, strings, onError = showErrorToast)
                    },
                )
                CouiRowDivider()
                CouiValueRow(
                    title = strings.settingThanks,
                    summary = strings.aboutThanksDesc,
                    onClick = onThanksClick,
                )
                CouiRowDivider()
                CouiValueRow(
                    title = strings.aboutDisclaimer,
                    summary = strings.aboutDisclaimerSummary,
                    onClick = { showDisclaimerDialog = true },
                )
                CouiRowDivider()
                CouiValueRow(
                    title = strings.settingUpdate,
                    summary = "${strings.version} v${AppInfo.VERSION_NAME}",
                    onClick = onUpdateClick,
                )
            }
        }
    }

    if (showDisclaimerDialog) {
        Dialog(
            onDismissRequest = { showDisclaimerDialog = false },
            visible = true,
            material = couiDialogMaterialOrNull(),
            title = strings.aboutDisclaimer,
            message = strings.aboutDisclaimerContent,
            placement = couiDialogPlacement(),
            confirmAction = CouiDialogAction(
                text = strings.confirm,
                style = CouiDialogActionStyle.Primary,
                colors = couiPrimaryActionColors(),
                onClick = { showDisclaimerDialog = false },
            ),
        )
    }
}

/** 品牌标识头：应用图标、名称、版本胶囊与一句话简介，整块居中。 */
@Composable
private fun CouiAboutHeader() {
    val scheme = CouiTheme.colorScheme
    val strings = LocalStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(scheme.container),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_pixez_logo),
                contentDescription = strings.logoContentDescription,
                modifier = Modifier.size(72.dp),
            )
        }
        Text(
            text = "PixEz MIUIX",
            kind = CouiTextKind.HomeTitle,
            color = scheme.onSurface,
            modifier = Modifier.padding(top = 14.dp),
        )
        Row(
            modifier = Modifier
                .padding(top = 6.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(scheme.container)
                .padding(horizontal = 10.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "v${AppInfo.VERSION_NAME}",
                kind = CouiTextKind.ItemSummary,
                color = scheme.onSurfaceVariant,
            )
        }
        Text(
            text = strings.couiAboutAppDesc,
            kind = CouiTextKind.ItemSummary,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp),
        )
    }
}

/** 开发者信息行：头像槽 + 姓名 / 职责；点击与行内留白反馈由 [Item] 承担。 */
@Composable
private fun CouiDeveloperRow(
    name: String,
    message: String,
    avatarUrl: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Item(
        icon = { CouiAvatar(name = name, url = avatarUrl) },
        iconSize = 44.dp,
        onClick = onClick,
        summary = { CouiItemSummary(text = message) },
        title = { CouiItemTitle(text = name) },
    )
}

/** 圆形头像；无图时退化为首字母色块。 */
@Composable
private fun CouiAvatar(
    name: String,
    url: String?,
    size: Dp = 44.dp,
) {
    val scheme = CouiTheme.colorScheme
    if (!url.isNullOrBlank()) {
        PixivAsyncImage(
            model = url,
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(CircleShape),
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(scheme.accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.firstOrNull()?.uppercase() ?: "?",
                kind = CouiTextKind.ItemTitle,
                color = scheme.accent,
            )
        }
    }
}

/** 贡献者横向滚动行。 */
@Composable
private fun CouiContributorsRow(
    contributors: List<Contributor>,
    onUrlClick: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        contributors.forEach { contributor ->
            CouiContributorItem(
                contributor = contributor,
                onClick = { onUrlClick(contributor.url) },
            )
        }
    }
}

/** 单个贡献者头像项。 */
@Composable
private fun CouiContributorItem(
    contributor: Contributor,
    onClick: () -> Unit,
) {
    val scheme = CouiTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .width(56.dp)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CouiAvatar(name = contributor.name, url = contributor.avatar)
        Text(
            text = contributor.name,
            kind = CouiTextKind.ItemSummary,
            color = scheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
