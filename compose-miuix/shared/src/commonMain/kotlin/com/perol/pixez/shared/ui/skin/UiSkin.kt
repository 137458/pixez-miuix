package com.perol.pixez.shared.ui.skin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

/**
 * 界面皮肤：同一套业务逻辑下的两套系统视觉。
 *
 * - [HYPEROS]：MIUIX 原生组件（默认）。
 * - [COLOROS]：COUI 原语（Android 皮肤切换）。
 */
enum class UiSkin(val storageIndex: Int) {
    HYPEROS(0),
    COLOROS(1),
    ;

    companion object {
        fun fromIndex(index: Int): UiSkin = entries.firstOrNull { it.storageIndex == index } ?: HYPEROS
    }
}

/** 当前生效皮肤，由根组合在 [com.perol.pixez.shared.ui.navigation.RootContent] 下发。 */
val LocalUiSkin = compositionLocalOf { UiSkin.HYPEROS }

/** 当前平台是否提供 COUI 皮肤。 */
expect val isCouiSkinSupported: Boolean

/**
 * COUI 主题宿主：Android 在 [enabled] 时叠加 COUI 配色与排版，其余情况直接透传。
 * 必须放在 MiuixTheme 内部调用，以保证 COUI 配色由当前 MIUIX 主题派生。
 *
 * @param dark 当前实际明暗，取自 AppThemeSpec.resolvedDark。COUI 组件的明暗分支（顶栏渐隐色、
 * 底栏选中色、弹层材质）全部以它为准，不允许组件内部再从底色亮度反推。
 */
@Composable
expect fun CouiSkinHost(enabled: Boolean, dark: Boolean, content: @Composable () -> Unit)

/**
 * 皮肤浮动栏的正文采样源。
 *
 * COUI 浮动底栏的玻璃材质是把「被它遮挡的正文」采样后重建的，采样源对象是 Android 侧库类型，
 * 无法在 commonMain 创建，因此由本扩展在各平台落地：Android 在皮肤开启时把正文登记为采样源，
 * 其余平台原样返回。挂在正文容器上，且不能覆盖浮动栏自身所在的节点。
 */
@Composable
expect fun Modifier.skinFloatingBarSource(): Modifier
