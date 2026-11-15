package com.perol.pixez.shared.ui.skin

import androidx.compose.runtime.Composable

// Desktop 不提供 COUI 皮肤：这里只声明不支持，且不提供 LocalSkinPages，
// 于是调用方读到 null，直接走 MIUIX 原页面。
actual val isCouiSkinSupported: Boolean = false

@Composable
actual fun CouiSkinHost(enabled: Boolean, content: @Composable () -> Unit) {
    content()
}
