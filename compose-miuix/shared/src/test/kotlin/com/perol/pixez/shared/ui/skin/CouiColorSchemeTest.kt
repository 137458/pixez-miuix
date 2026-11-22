package com.perol.pixez.shared.ui.skin

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.ui.coui.core.withExtra
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * COUI 皮肤色板派生（种子色 / AMOLED）与活 PaddingValues 延迟求值验证。
 */
class CouiColorSchemeTest {

    @Test
    fun `关闭动态取色时不同种子色派生不同的强调色`() {
        val base = lightColorScheme()
        val pinkScheme = couiColorSchemeOf(
            colors = base,
            isDark = false,
            isAmoled = false,
            useDynamicColor = false,
            seedColor = 0xFFE91E63.toInt(),
        )
        val greenScheme = couiColorSchemeOf(
            colors = base,
            isDark = false,
            isAmoled = false,
            useDynamicColor = false,
            seedColor = 0xFF4CAF50.toInt(),
        )

        assertEquals(Color(0xFFE91E63), pinkScheme.accent)
        assertEquals(Color(0xFF4CAF50), greenScheme.accent)
        assertEquals(Color(0xFFE91E63), pinkScheme.checkboxAccent)
        assertEquals(Color(0xFF4CAF50), greenScheme.checkboxAccent)
        assertNotEquals(pinkScheme.accentOnSurface, greenScheme.accentOnSurface)
    }

    @Test
    fun `开启动态取色且开启 AMOLED 深色模式时仍保持纯黑背景且卡片与背景有层次`() {
        val darkBase = darkColorScheme()
        val amoledDynamic = couiColorSchemeOf(
            colors = darkBase,
            isDark = true,
            isAmoled = true,
            useDynamicColor = true,
            seedColor = 0xFFE91E63.toInt(),
        )
        val amoledSeeded = couiColorSchemeOf(
            colors = darkBase,
            isDark = true,
            isAmoled = true,
            useDynamicColor = false,
            seedColor = 0xFFE91E63.toInt(),
        )

        assertEquals(Color.Black, amoledDynamic.background, "开启动态取色时 AMOLED 背景也必须为纯黑")
        assertNotEquals(Color.Black, amoledDynamic.card, "AMOLED 下卡片不能与纯黑背景融为一体")
        assertEquals(Color.Black, amoledSeeded.background)
        assertNotEquals(Color.Black, amoledSeeded.card)
    }

    @Test
    fun `PaddingValues withExtra 在底层边距动态变化时实时返回最新值`() {
        var dynamic by mutableStateOf(PaddingValues(top = 52.dp, bottom = 10.dp))
        val liveSource = object : PaddingValues {
            override fun calculateLeftPadding(layoutDirection: LayoutDirection) = dynamic.calculateLeftPadding(layoutDirection)
            override fun calculateTopPadding() = dynamic.calculateTopPadding()
            override fun calculateRightPadding(layoutDirection: LayoutDirection) = dynamic.calculateRightPadding(layoutDirection)
            override fun calculateBottomPadding() = dynamic.calculateBottomPadding()
        }
        val wrapped = liveSource.withExtra(top = 8.dp, bottom = 12.dp)

        assertEquals(60.dp, wrapped.calculateTopPadding())
        assertEquals(22.dp, wrapped.calculateBottomPadding())

        dynamic = PaddingValues(top = 110.dp, bottom = 24.dp)
        assertEquals(118.dp, wrapped.calculateTopPadding(), "活 PaddingValues 不应冻结初始值")
        assertEquals(36.dp, wrapped.calculateBottomPadding())
    }
}
