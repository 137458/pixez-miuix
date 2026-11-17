package com.perol.pixez.shared.ui.components

import com.perol.pixez.shared.ui.i18n.AppStrings

/**
 * 统一拼装「操作名 + 失败后缀 + 具体原因」的错误提示文案。
 *
 * 各处失败提示原先各自手拼，出现了全角冒号、漏拼失败后缀等漂移；
 * 统一从这里产出，保证同一动作失败时的措辞与标点全应用一致。
 *
 * @param action 已本地化的动作名（如 [AppStrings.btnDelete]）。
 * @param error 失败原因，交给 [PixivApiErrorFormatter] 归一为用户可读文案。
 */
internal fun actionFailureMessage(action: String, error: Throwable?, strings: AppStrings): String =
    action + strings.failedSuffix + ": " + PixivApiErrorFormatter.format(error, strings)

/**
 * [actionFailureMessage] 的 Toast 包装：错误样式由调用点统一交给本函数决定。
 */
internal fun actionFailureToast(action: String, error: Throwable?, strings: AppStrings): ToastData =
    ToastData(actionFailureMessage(action, error, strings), ToastType.Error)
