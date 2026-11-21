package com.perol.pixez.shared.ui.skin

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Desktop 不提供 COUI 皮肤：这里只声明不支持，且不提供 LocalSkinPages，
// 于是调用方读到 null，直接走 MIUIX 原页面。
actual val isCouiSkinSupported: Boolean = false

@Composable
actual fun CouiSkinHost(enabled: Boolean, dark: Boolean, content: @Composable () -> Unit) {
    content()
}

// Desktop 没有 COUI 底栏，正文容器不需要登记任何采样源。
@Composable
actual fun Modifier.skinFloatingBarSource(): Modifier = this
