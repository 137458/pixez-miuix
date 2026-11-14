package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.Dialog
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.AppInfo
import com.perol.pixez.shared.data.model.CONTRIBUTORS
import com.perol.pixez.shared.data.model.Contributor
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.components.LocalToastSink
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.components.ToastType
import com.perol.pixez.shared.ui.coui.component.CouiActionButton
import com.perol.pixez.shared.ui.coui.component.CouiNavigationRow
import com.perol.pixez.shared.ui.coui.component.CouiReadableColumn
import com.perol.pixez.shared.ui.coui.component.couiDialogMaterialOrNull
import com.perol.pixez.shared.ui.coui.component.couiDialogPlacement
import com.perol.pixez.shared.ui.coui.component.couiPrimaryActionColors
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.utils.openSafeUrl
import org.jetbrains.compose.resources.painterResource
import pixez_miuix.shared.generated.resources.Res
import pixez_miuix.shared.generated.resources.ic_pixez_logo
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back

/**
 * COUI 关于页：固定顶栏 + 品牌头图 + 开发者/贡献者/项目三组卡片。
 *
 * 外链跳转、免责声明弹窗、致谢与更新入口全部保留；
 * 项目卡片行改用共用 `CouiNavigationRow`，免责声明改用 COUI `Dialog`。
 */
@Composable
internal fun CouiAboutScreen(
    onBack: () -> Unit,
    onThanksClick: () -> Unit,
    onUpdateClick: () -> Unit = {},
) {
    val strings = LocalStrings.current
    val toastSink = LocalToastSink.current
    val scheme = CouiTheme.colorScheme
    val showErrorToast: (String) -> Unit = { toastSink(ToastData(it, ToastType.Error)) }
    var showDisclaimerDialog by remember { mutableStateOf(false) }

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
                text = strings.settingAbout,
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
                horizontalAlignment = Alignment.CenterHorizontally,
                overscrollEffect = rememberCouiOverscrollEffect(),
            ) {
                item(key = "coui_about_header", contentType = "header") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(scheme.container),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.ic_pixez_logo),
                            contentDescription = strings.logoContentDescription,
                            modifier = Modifier.size(72.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "PixEz MIUIX",
                        kind = CouiTextKind.HomeTitle,
                        color = scheme.onSurface,
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
                        text = strings.aboutDesc,
                        kind = CouiTextKind.ItemSummary,
                        color = scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp, start = 8.dp, end = 8.dp),
                    )
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
                        CouiNavigationRow(
                            title = strings.aboutRepo,
                            summary = "github.com/137458/pixez-miuix",
                            onClick = {
                                openSafeUrl(AppConstants.Urls.GITHUB_REPO, strings, onError = showErrorToast)
                            },
                        )
                        CouiNavigationRow(
                            title = strings.aboutFeedback,
                            summary = AppConstants.Urls.FEEDBACK_EMAIL,
                            onClick = {
                                openSafeUrl(AppConstants.Urls.FEEDBACK_MAILTO, strings, onError = showErrorToast)
                            },
                        )
                        CouiNavigationRow(
                            title = strings.settingThanks,
                            summary = strings.aboutThanksDesc,
                            onClick = onThanksClick,
                        )
                        CouiNavigationRow(
                            title = strings.aboutDisclaimer,
                            summary = strings.aboutDisclaimerSummary,
                            onClick = { showDisclaimerDialog = true },
                        )
                        CouiNavigationRow(
                            title = strings.settingUpdate,
                            summary = "${strings.version} v${AppInfo.VERSION_NAME}",
                            onClick = onUpdateClick,
                        )
                    }
                }
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

/** 开发者信息行。 */
@Composable
private fun CouiDeveloperRow(
    name: String,
    message: String,
    avatarUrl: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val scheme = CouiTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = { onClick?.invoke() },
            )
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
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = name,
                kind = CouiTextKind.ItemTitle,
                color = scheme.onSurface,
            )
            Text(
                text = message,
                kind = CouiTextKind.ItemSummary,
                color = scheme.onSurfaceVariant,
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
                    .background(scheme.accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = contributor.name.firstOrNull()?.uppercase() ?: "?",
                    kind = CouiTextKind.ItemTitle,
                    color = scheme.accent,
                )
            }
        }
        Text(
            text = contributor.name,
            kind = CouiTextKind.ItemSummary,
            color = scheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
