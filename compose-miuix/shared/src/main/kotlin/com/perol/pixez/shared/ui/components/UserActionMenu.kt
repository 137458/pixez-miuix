package com.perol.pixez.shared.ui.components

import com.perol.pixez.shared.data.model.UserDetail

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
