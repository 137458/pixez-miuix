package com.perol.pixez.shared.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.perol.pixez.shared.data.model.UserDetail
import com.perol.pixez.shared.ui.i18n.LocalStrings
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.window.WindowBottomSheet

/**
 * 用户详情页底部操作菜单。
 *
 * 基于 [WindowBottomSheet] 实现，支持独立 Window 渲染，无需依赖页面级 Scaffold。
 * 当前提供「复制信息」「复制链接」与「分享链接」三项操作，与原 Flutter PixEz 用户详情页弹出菜单保持一致。
 *
 * @param show 是否显示菜单
 * @param onDismissRequest 用户请求关闭菜单时的回调
 * @param onCopyInfo 点击「复制信息」时的回调
 * @param onCopyLink 点击「复制链接」时的回调
 * @param onShareLink 点击「分享链接」时的回调
 */
@Composable
fun UserActionMenu(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onCopyInfo: () -> Unit,
    onCopyLink: () -> Unit,
    onShareLink: () -> Unit,
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
            BasicComponent(
                title = strings.menuCopyLink,
                onClick = onCopyLink,
            )
            BasicComponent(
                title = strings.menuShareLink,
                onClick = onShareLink,
            )
        }
    }
}

/**
 * 构造用户详情「复制信息」文本。
 *
 * 格式与原 Flutter 应用保持一致：
 * ```
 * painter:{user.name}
 * pid:{user.id}
 * ```
 */
fun buildUserCopyInfo(userDetail: UserDetail): String {
    return "painter:${userDetail.user.name}\npid:${userDetail.user.id}"
}

