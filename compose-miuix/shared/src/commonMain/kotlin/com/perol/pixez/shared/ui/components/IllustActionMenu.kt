package com.perol.pixez.shared.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.perol.pixez.shared.data.model.Illust
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.extra.SuperBottomSheet

/**
 * 作品详情页底部操作菜单。
 *
 * 基于 [SuperBottomSheet] 实现，必须在 [top.yukonga.miuix.kmp.basic.Scaffold] 内使用。
 * 当前提供「复制信息」「复制链接」与「分享链接」三项操作，与原 Flutter PixEz 作品详情页「更多」菜单保持一致。
 *
 * @param show 是否显示菜单
 * @param onDismissRequest 用户请求关闭菜单时的回调
 * @param onCopyInfo 点击「复制信息」时的回调
 * @param onCopyLink 点击「复制链接」时的回调
 * @param onShareLink 点击「分享链接」时的回调
 */
@Composable
fun IllustActionMenu(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onCopyInfo: () -> Unit,
    onCopyLink: () -> Unit,
    onShareLink: () -> Unit,
) {
    SuperBottomSheet(
        show = show,
        title = "更多操作",
        onDismissRequest = onDismissRequest,
    ) {
        Column {
            BasicComponent(
                title = "复制信息",
                onClick = onCopyInfo,
            )
            BasicComponent(
                title = "复制链接",
                onClick = onCopyLink,
            )
            BasicComponent(
                title = "分享链接",
                onClick = onShareLink,
            )
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
