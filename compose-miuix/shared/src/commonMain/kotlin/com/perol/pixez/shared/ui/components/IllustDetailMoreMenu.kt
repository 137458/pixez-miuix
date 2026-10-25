/**
 * [IllustDetailTopBar]「更多操作」下拉菜单条目构建。
 *
 * 原手写浮层（全屏遮罩、AnimatedVisibility 进出场、液态玻璃卡片与菜单行）已迁移至
 * Miuix 原生 OverlayDropdownPopup；本文件仅负责把参数映射为 [DropdownEntry] 列表，
 * 复制 / 分享 / 屏蔽的动作协程逻辑原样保留。
 */

package com.perol.pixez.shared.ui.components

import coil3.PlatformContext
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.repository.BanRepository
import com.perol.pixez.shared.platform.IllustClipboard
import com.perol.pixez.shared.platform.IllustShare
import com.perol.pixez.shared.ui.i18n.AppStrings
import com.perol.pixez.shared.ui.utils.openSafeUrl
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Blocklist
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.icon.extended.Share
import top.yukonga.miuix.kmp.icon.extended.Show

/**
 * 构建「更多操作」下拉菜单条目，按功能分三组（组间分隔线由 Miuix 弹层内置）：
 * 复制信息 / 复制图片 / 复制链接；分享作品 / 浏览器打开 / SauceNao 搜图；屏蔽作品与屏蔽画师（作品未屏蔽时）。
 */
internal fun buildIllustDetailMoreMenuEntries(
    strings: AppStrings,
    illust: Illust,
    isBanned: Boolean,
    banRepository: BanRepository,
    onBanSuccess: () -> Unit,
    onToast: (ToastData) -> Unit,
    clipboard: IllustClipboard,
    share: IllustShare,
    context: PlatformContext,
    coroutineScope: CoroutineScope,
): List<DropdownEntry> = buildList {
    add(
        DropdownEntry(
            items = listOf(
                DropdownItem(
                    text = strings.menuCopyInfo,
                    icon = { modifier -> Icon(imageVector = MiuixIcons.Copy, contentDescription = null, modifier = modifier) },
                    onClick = {
                        val text = buildIllustCopyInfo(illust)
                        runClipboardShare(
                            success = ToastData(strings.copiedToClipboard, ToastType.Success),
                            failurePrefix = "${strings.copy}${strings.loadFailed}",
                            onToast = onToast,
                        ) { clipboard.copy(text) }
                    },
                ),
                DropdownItem(
                    text = strings.menuCopyImage,
                    icon = { modifier -> Icon(imageVector = MiuixIcons.Show, contentDescription = null, modifier = modifier) },
                    onClick = {
                        coroutineScope.launch {
                            suspendRunClipboardShare(
                                success = ToastData(strings.imageCopySuccess, ToastType.Success),
                                failurePrefix = strings.menuCopyImage,
                                onToast = onToast,
                            ) {
                                withContext(Dispatchers.IO) {
                                    val candidateUrls = listOf(
                                        illust.imageUrls.large,
                                        illust.imageUrls.medium,
                                        illust.imageUrls.squareMedium,
                                    )
                                    val bytes = extractCachedImageBytes(context, candidateUrls)
                                    bytes?.let { clipboard.copyImage(it) } ?: throw IllegalStateException(strings.imageNoCacheFound)
                                }
                            }
                        }
                    },
                ),
                DropdownItem(
                    text = strings.menuCopyLink,
                    icon = { modifier -> Icon(imageVector = MiuixIcons.Link, contentDescription = null, modifier = modifier) },
                    onClick = {
                        val link = buildIllustShareLink(illust)
                        runClipboardShare(
                            success = ToastData(strings.copiedToClipboard, ToastType.Success),
                            failurePrefix = "${strings.copy}${strings.loadFailed}",
                            onToast = onToast,
                        ) { clipboard.copy(link) }
                    },
                ),
            ),
        ),
    )
    add(
        DropdownEntry(
            items = listOf(
                DropdownItem(
                    text = strings.share,
                    icon = { modifier -> Icon(imageVector = MiuixIcons.Share, contentDescription = null, modifier = modifier) },
                    onClick = {
                        val link = buildIllustShareLink(illust)
                        val successMsg = if (com.perol.pixez.shared.platform.isDesktopPlatform()) {
                            strings.copiedToClipboard
                        } else {
                            strings.share
                        }
                        runClipboardShare(
                            success = ToastData(successMsg, ToastType.Success),
                            failurePrefix = strings.share,
                            onToast = onToast,
                        ) { share.share(link, illust.title) }
                    },
                ),
                DropdownItem(
                    text = strings.openInBrowser,
                    icon = { modifier -> Icon(imageVector = MiuixIcons.Link, contentDescription = null, modifier = modifier) },
                    onClick = {
                        openSafeUrl(buildIllustShareLink(illust), strings, onError = { onToast(ToastData(it, ToastType.Error)) })
                    },
                ),
                DropdownItem(
                    text = strings.menuSauceNao,
                    icon = { modifier -> Icon(imageVector = MiuixIcons.Search, contentDescription = null, modifier = modifier) },
                    onClick = {
                        val imgUrl = illust.imageUrls.medium.ifEmpty { illust.imageUrls.large }
                        val sauceUrl = buildSauceNaoUrl(imgUrl)
                        openSafeUrl(sauceUrl, strings, onError = { onToast(ToastData(it, ToastType.Error)) })
                    },
                ),
            ),
        ),
    )
    if (!isBanned) {
        add(
            DropdownEntry(
                items = listOf(
                    DropdownItem(
                        text = strings.menuBanWork,
                        icon = { modifier -> Icon(imageVector = MiuixIcons.Blocklist, contentDescription = null, modifier = modifier) },
                        onClick = {
                            coroutineScope.launch {
                                suspendRunCatchingNonCancel {
                                    banRepository.insertBanIllust(illust.id, illust.title)
                                }.fold(
                                    onSuccess = {
                                        onBanSuccess()
                                        onToast(ToastData(strings.menuBanWork, ToastType.Success))
                                    },
                                    onFailure = { e -> onToast(ToastData("${strings.menuBanWork}: ${PixivApiErrorFormatter.format(e, strings)}", ToastType.Error)) },
                                )
                            }
                        },
                    ),
                    DropdownItem(
                        text = strings.menuBanUser,
                        icon = { modifier -> Icon(imageVector = MiuixIcons.Blocklist, contentDescription = null, modifier = modifier) },
                        onClick = {
                            coroutineScope.launch {
                                suspendRunCatchingNonCancel {
                                    banRepository.insertBanUser(illust.user.id, illust.user.name)
                                }.fold(
                                    onSuccess = {
                                        onBanSuccess()
                                        onToast(ToastData(strings.menuBanUser, ToastType.Success))
                                    },
                                    onFailure = { e -> onToast(ToastData("${strings.menuBanUser}: ${PixivApiErrorFormatter.format(e, strings)}", ToastType.Error)) },
                                )
                            }
                        },
                    ),
                ),
            ),
        )
    }
}
