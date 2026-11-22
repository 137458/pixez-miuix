package com.perol.pixez.shared.ui.coui.core

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.itos.coui.effect.CouiFloatingRenderer

/**
 * 有界借还对象池：复用高开销图形渲染器（如 [CouiFloatingRenderer] 内的 AGSL Shader 与阴影网格），
 * 同时保证同一时刻屏幕上的每个可见控件都持有独立实例。
 */
internal class BoundedObjectPool<T : Any>(
    private val maxSize: Int,
    private val factory: () -> T,
) {
    init {
        require(maxSize >= 0) { "maxSize must be non-negative: $maxSize" }
    }

    private val idle = ArrayDeque<T>(maxSize.coerceAtMost(32))

    val availableCount: Int
        get() = synchronized(this) { idle.size }

    fun acquire(): T {
        val reused = synchronized(this) {
            if (idle.isNotEmpty()) idle.removeLast() else null
        }
        return reused ?: factory()
    }

    fun release(instance: T): Boolean {
        if (maxSize == 0) return false
        return synchronized(this) {
            if (idle.size >= maxSize || idle.any { it === instance }) {
                false
            } else {
                idle.addLast(instance)
                true
            }
        }
    }

    fun clear() {
        synchronized(this) {
            idle.clear()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
internal object CouiFloatingRendererPool {
    private val pool = BoundedObjectPool(maxSize = 24) { CouiFloatingRenderer.standalone() }

    fun acquire(): CouiFloatingRenderer? = runCatching { pool.acquire() }.getOrNull()

    fun release(renderer: CouiFloatingRenderer) {
        pool.release(renderer)
    }
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Composable
internal fun rememberPooledCouiFloatingRenderer(enabled: Boolean = true): CouiFloatingRenderer? {
    val renderer = remember(enabled) {
        if (enabled) CouiFloatingRendererPool.acquire() else null
    }
    DisposableEffect(renderer) {
        onDispose {
            if (renderer != null) {
                CouiFloatingRendererPool.release(renderer)
            }
        }
    }
    return renderer
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Composable
internal fun rememberPooledCouiFloatingRenderers(count: Int): List<CouiFloatingRenderer>? {
    val renderers = remember(count) {
        if (count <= 0) {
            emptyList()
        } else {
            val acquired = ArrayList<CouiFloatingRenderer>(count)
            var failed = false
            for (i in 0 until count) {
                val item = CouiFloatingRendererPool.acquire()
                if (item == null) {
                    failed = true
                    break
                }
                acquired.add(item)
            }
            if (failed) {
                acquired.forEach { CouiFloatingRendererPool.release(it) }
                null
            } else {
                acquired
            }
        }
    }
    DisposableEffect(renderers) {
        onDispose {
            renderers?.forEach { CouiFloatingRendererPool.release(it) }
        }
    }
    return renderers
}
