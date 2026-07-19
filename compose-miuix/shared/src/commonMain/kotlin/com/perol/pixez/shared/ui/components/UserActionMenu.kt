package com.perol.pixez.shared.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.perol.pixez.shared.data.model.UserDetail
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.extra.SuperBottomSheet

/**
 * 用户详情页底部操作菜单。
 *
 * 基于 [SuperBottomSheet] 实现，必须在 [top.yukonga.miuix.kmp.basic.Scaffold] 内使用。
 * 当前提供「复制信息」与「复制链接」两项操作，与原 Flutter PixEz 用户详情页弹出菜单保持一致。
 *
 * @param show 是否显示菜单
 * @param onDismissRequest 用户请求关闭菜单时的回调
 * @param onCopyInfo 点击「复制信息」时的回调
 * @param onCopyLink 点击「复制链接」时的回调
 */
@Composable
fun UserActionMenu(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onCopyInfo: () -> Unit,
    onCopyLink: () -> Unit,
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

/**
 * 构造用户主页链接。
 */
fun buildUserShareLink(userId: Int): String {
    return "https://www.pixiv.net/users/${userId}"
}
