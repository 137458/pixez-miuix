package com.perol.pixez.shared.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.ui.i18n.LocalStrings
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.window.WindowBottomSheet

/**
 * 作品详情页底部操作菜单。
 *
 * 基于 [WindowBottomSheet] 实现，支持独立 Window 渲染，无需依赖页面级 Scaffold。
 * 当前提供「复制信息」「复制链接」「分享链接」与「屏蔽作品」四项操作，
 * 与原 Flutter PixEz 作品详情页「更多」菜单保持一致。
 *
 * @param show 是否显示菜单
 * @param showBan 是否显示「屏蔽作品」入口；作品已屏蔽时可设为 false 避免重复屏蔽
 * @param onDismissRequest 用户请求关闭菜单时的回调
 * @param onCopyInfo 点击「复制信息」时的回调
 * @param onCopyLink 点击「复制链接」时的回调
 * @param onShareLink 点击「分享链接」时的回调
 * @param onBan 点击「屏蔽作品」时的回调
 */
@Composable
fun IllustActionMenu(
    show: Boolean,
    showBan: Boolean,
    onDismissRequest: () -> Unit,
    onCopyInfo: () -> Unit,
    onCopyImage: (() -> Unit)? = null,
    onCopyLink: () -> Unit,
    onShareLink: () -> Unit,
    onSauceNao: (() -> Unit)? = null,
    onBan: () -> Unit,
) {
    val strings = LocalStrings.current

    WindowBottomSheet(
        show = show,
        title = strings.menuMoreActions,
        onDismissRequest = onDismissRequest,
    ) {
        Column {
            BasicComponent(
                title = strings.menuCopyInfo,
                onClick = onCopyInfo,
            )
            if (onCopyImage != null) {
                BasicComponent(
                    title = strings.menuCopyImage,
                    onClick = onCopyImage,
                )
            }
            BasicComponent(
                title = strings.menuCopyLink,
                onClick = onCopyLink,
            )
            BasicComponent(
                title = strings.menuShareLink,
                onClick = onShareLink,
            )
            if (onSauceNao != null) {
                BasicComponent(
                    title = strings.menuSauceNao,
                    onClick = onSauceNao,
                )
            }
            if (showBan) {
                BasicComponent(
                    title = strings.menuBanWork,
                    onClick = onBan,
                )
            }
        }
    }
}

/**
 * 构造「复制信息」文本。
 *
 * 格式与原 Flutter 应用保持一致：
 * ```
 * title:{title}
 * painter:{user.name}
 * illust id:{id}
 * ```
 */
fun buildIllustCopyInfo(illust: Illust): String {
    return "title:${illust.title}\npainter:${illust.user.name}\nillust id:${illust.id}"
}

/**
 * 构造作品详情页链接。
 */
fun buildIllustShareLink(illust: Illust): String {
    return "https://www.pixiv.net/artworks/${illust.id}"
}

/**
 * 构造 SauceNAO 以图搜图链接。
 */
fun buildSauceNaoUrl(imageUrl: String): String {
    return io.ktor.http.URLBuilder(com.perol.pixez.shared.ui.AppConstants.Urls.SAUCE_NAO_SEARCH).apply {
        parameters.append("db", "999")
        parameters.append("url", imageUrl)
    }.buildString()
}
