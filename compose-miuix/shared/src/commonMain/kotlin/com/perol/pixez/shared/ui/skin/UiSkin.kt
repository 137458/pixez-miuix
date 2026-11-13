package com.perol.pixez.shared.ui.skin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

/**
 * 界面皮肤：同一套业务逻辑下的两套系统视觉。
 *
 * - [HYPEROS]：MIUIX 原生组件（默认，全平台可用）。
 * - [COLOROS]：COUI 原语，仅在 Android 提供（COUI AAR 无 Desktop 产物）。
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

/** 当前平台是否提供 COUI 皮肤。Desktop 恒为 false。 */
expect val isCouiSkinSupported: Boolean

/**
 * COUI 主题宿主：Android 在 [enabled] 时叠加 COUI 配色与排版，其余情况直接透传。
 * 必须放在 MiuixTheme 内部调用，以保证 COUI 配色由当前 MIUIX 主题派生。
 */
@Composable
expect fun CouiSkinHost(enabled: Boolean, content: @Composable () -> Unit)
