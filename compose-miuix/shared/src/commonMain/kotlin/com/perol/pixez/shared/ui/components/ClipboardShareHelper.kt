/**
 * 「复制 / 分享 → 成功或失败提示」的统一反馈流程。
 *
 * 收敛的部分：异常捕获（`CancellationException` 原样抛出，不并入 Result）、
 * 成功与失败 `ToastData` 的产出、失败文案的拼接规则（`"前缀: 异常信息"`）。
 *
 * 刻意不收敛的部分：复制目标、缩略图与磁盘缓存的降级顺序、触感反馈与埋点时机——
 * 这些在各入口语义不同，留在调用方决定。
 *
 * 本文件不持有 AppStrings：全部文案由调用方以参数传入，因此可脱离 CompositionLocal 直接单测。
 *
 * 文案兼容说明：异常 message 为 null 时仍会拼出「前缀: null」，与收敛前逐字一致，
 * 避免重构顺带静默改动现网提示内容。
 */
package com.perol.pixez.shared.ui.components

import com.perol.pixez.shared.utils.runCatchingNonCancel
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel

/**
 * 执行同步的复制 / 分享动作（剪贴板写入、系统分享面板），并把结果回调为 [ToastData]。
 *
 * @param success 动作成功时展示的 Toast，由调用方决定 [ToastType]
 * @param failurePrefix 失败文案前缀，通常是操作名或「操作名 + 失败」的组合
 * @param appendCauseMessage 是否在失败前缀后拼接异常信息；面向外部的错误提示可置为 false
 * @param onToast 结果回调，无论成功失败都恰好触发一次
 * @return 动作的执行结果，调用方可据此追加日志等额外副作用
 */
fun runClipboardShare(
    success: ToastData,
    failurePrefix: String,
    appendCauseMessage: Boolean = true,
    onToast: (ToastData) -> Unit,
    action: () -> Unit,
): Result<Unit> = runCatchingNonCancel(action).reportFeedback(success, failurePrefix, appendCauseMessage, onToast)

/**
 * [runClipboardShare] 的挂起版本：动作内部需要切换 IO 调度器或调用挂起 API 时使用。
 */
suspend fun suspendRunClipboardShare(
    success: ToastData,
    failurePrefix: String,
    appendCauseMessage: Boolean = true,
    onToast: (ToastData) -> Unit,
    action: suspend () -> Unit,
): Result<Unit> =
    suspendRunCatchingNonCancel(action).reportFeedback(success, failurePrefix, appendCauseMessage, onToast)

private fun Result<Unit>.reportFeedback(
    success: ToastData,
    failurePrefix: String,
    appendCauseMessage: Boolean,
    onToast: (ToastData) -> Unit,
): Result<Unit> = also { result ->
    result.fold(
        onSuccess = { onToast(success) },
        onFailure = { e ->
            onToast(
                ToastData(
                    message = if (appendCauseMessage) "$failurePrefix: ${e.message}" else failurePrefix,
                    type = ToastType.Error,
                ),
            )
        },
    )
}
