package com.perol.pixez.shared.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay

/**
 * 弹窗关闭后延迟清理数据引用，确保退场动画平滑完成且不造成内存或状态泄漏。
 *
 * 当弹窗处于关闭状态（[visible] 为 false）且目标数据 [target] 不为空时，
 * 等待退场动画结束（默认 300ms，严格对齐 MIUIX 官方弹窗退场时间），随后触发 [onClear]。
 *
 * @param visible 弹窗当前显示状态
 * @param target 待操作或弹窗关联的数据对象
 * @param delayMillis 退场动画延迟毫秒数，默认 300ms
 * @param onClear 数据清理回调函数
 */
@Composable
fun <T> DelayedClearEffect(
    visible: Boolean,
    target: T?,
    delayMillis: Long = 300L,
    onClear: () -> Unit,
) {
    LaunchedEffect(visible) {
        if (!visible && target != null) {
            delay(delayMillis)
            onClear()
        }
    }
}
