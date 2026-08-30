// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package com.perol.pixez.shared.ui.libs.liquid

import top.yukonga.miuix.kmp.blur.BackdropEffectScope
import top.yukonga.miuix.kmp.blur.colorControls

/**
 * Lightweight saturation boost and luminance-floor lifting for Liquid Glass refraction optics.
 * Lifts the black level of text to prevent dark smudges under frosted blur,
 * while expanding saturation so illustrations and avatars remain vibrant.
 */
fun BackdropEffectScope.vibrancy(
    brightness: Float = 0.2f,
    contrast: Float = 0.8f,
    saturation: Float = 1.7f,
) {
    colorControls(
        brightness = brightness,
        contrast = contrast,
        saturation = saturation,
    )
}
