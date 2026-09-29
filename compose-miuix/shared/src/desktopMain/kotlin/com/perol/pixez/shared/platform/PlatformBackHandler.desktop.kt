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
object DesktopBackDispatcher {
    private class BackEntry(
        val id: Long,
        @Volatile var enabled: Boolean,
        @Volatile var onBack: () -> Unit,
    )

    private val nextId = AtomicLong(1L)
    private val entries = CopyOnWriteArrayList<BackEntry>()

    fun register(enabled: Boolean = true, onBack: () -> Unit): () -> Unit {
        val entry = BackEntry(
            id = nextId.getAndIncrement(),
            enabled = enabled,
            onBack = onBack,
        )
        entries.add(entry)
        return {
            entries.remove(entry)
        }
    }

    internal fun registerEntry(enabled: Boolean, onBack: () -> Unit): Any {
        val entry = BackEntry(
            id = nextId.getAndIncrement(),
            enabled = enabled,
            onBack = onBack,
        )
        entries.add(entry)
        return entry
    }

    internal fun updateEntry(handle: Any, enabled: Boolean, onBack: () -> Unit) {
        val entry = handle as? BackEntry ?: return
        entry.enabled = enabled
        entry.onBack = onBack
    }

    internal fun unregisterEntry(handle: Any) {
        val entry = handle as? BackEntry ?: return
        entries.remove(entry)
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
        DesktopBackDispatcher.registerEntry(enabled) { currentOnBack() }
    }
    SideEffect {
        DesktopBackDispatcher.updateEntry(handle, enabled) { currentOnBack() }
    }
    DisposableEffect(handle) {
        onDispose {
            DesktopBackDispatcher.unregisterEntry(handle)
        }
    }
}
