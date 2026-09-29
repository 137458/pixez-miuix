package com.perol.pixez.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

/**
 * Desktop 平台 LIFO 返回事件分发器。
 *
 * 用于接管全屏图片查看器 (`IllustFullScreenViewer`)、弹出菜单 (`IllustDetailTopBar`) 等组件的
 * `PlatformBackHandler`，使键盘 `Esc`、`Ctrl+W` 与鼠标侧键返回优先关闭顶层浮层，而非直接退出整个详情页。
 */
internal class DesktopBackHandle(
    val id: Long,
    @Volatile var enabled: Boolean,
    @Volatile var onBack: () -> Unit,
)

object DesktopBackDispatcher {
    private val nextId = AtomicLong(1L)
    private val entries = CopyOnWriteArrayList<DesktopBackHandle>()

    fun register(enabled: Boolean = true, onBack: () -> Unit): () -> Unit {
        val entry = DesktopBackHandle(
            id = nextId.getAndIncrement(),
            enabled = enabled,
            onBack = onBack,
        )
        entries.add(entry)
        return {
            entries.remove(entry)
        }
    }

    internal fun createHandle(enabled: Boolean, onBack: () -> Unit): DesktopBackHandle =
        DesktopBackHandle(
            id = nextId.getAndIncrement(),
            enabled = enabled,
            onBack = onBack,
        )

    internal fun attachHandle(handle: DesktopBackHandle) {
        entries.add(handle)
    }

    internal fun detachHandle(handle: DesktopBackHandle) {
        entries.remove(handle)
    }

    internal fun updateHandle(handle: DesktopBackHandle, enabled: Boolean, onBack: () -> Unit) {
        handle.enabled = enabled
        handle.onBack = onBack
    }

    /**
     * 按后进先出（LIFO）顺序触发最后一个处于 enabled 状态的返回处理器。
     * @return 若有处理器消费了返回事件则返回 true，否则返回 false。
     */
    fun dispatchBack(): Boolean {
        val snapshot = entries.toTypedArray()
        for (index in snapshot.indices.reversed()) {
            val entry = snapshot[index]
            if (entry.enabled) {
                entry.onBack.invoke()
                return true
            }
        }
        return false
    }

    fun clear() {
        entries.clear()
    }
}

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    val currentOnBack by rememberUpdatedState(onBack)
    val handle = remember {
        DesktopBackDispatcher.createHandle(enabled) { currentOnBack() }
    }
    SideEffect {
        DesktopBackDispatcher.updateHandle(handle, enabled) { currentOnBack() }
    }
    DisposableEffect(handle) {
        DesktopBackDispatcher.attachHandle(handle)
        onDispose {
            DesktopBackDispatcher.detachHandle(handle)
        }
    }
}
