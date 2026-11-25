package com.perol.pixez.shared.ui.navigation.animation

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween

/**
 * 固定时长的松手速度交接曲线：端点为 0/1，起始斜率来自手指释放速度，末端斜率为 0。
 *
 * 使用单调三次 Hermite 插值；归一化斜率限制在 `0..3` 之间，保证不越界且无回弹过冲，
 * 朝目标方向释放时平滑接续手指惯性。
 */
internal class NavVelocityHandoffEasing(
    distance: Float,
    initialVelocity: Float,
    durationMillis: Int,
) : Easing {
    private val slope = if (
        distance.isFinite() &&
        distance != 0f &&
        initialVelocity.isFinite() &&
        durationMillis > 0
    ) {
        (initialVelocity * (durationMillis / 1000f) / distance).coerceIn(0f, 3f)
    } else {
        0f
    }

    override fun transform(fraction: Float): Float {
        if (fraction <= 0f) return 0f
        if (fraction >= 1f) return 1f
        val t = fraction.coerceIn(0f, 1f)
        return (slope * t + (3f - 2f * slope) * t * t + (slope - 2f) * t * t * t).coerceIn(0f, 1f)
    }
}

/** 根据剩余行程与松手速度构造无缝交接的补间规格；零速度时退回默认曲线。 */
internal fun resolveVelocityHandoffSpec(
    remainingDistance: Float,
    initialVelocityPerSec: Float,
    durationMillis: Int,
    fallbackEasing: Easing = HyperOSDecelerateEasing,
): TweenSpec<Float> {
    val hasVelocity = initialVelocityPerSec.isFinite() &&
        initialVelocityPerSec != 0f &&
        remainingDistance.isFinite() &&
        remainingDistance != 0f
    return tween(
        durationMillis = durationMillis,
        easing = if (hasVelocity) {
            NavVelocityHandoffEasing(
                distance = remainingDistance,
                initialVelocity = initialVelocityPerSec,
                durationMillis = durationMillis,
            )
        } else {
            fallbackEasing
        },
    )
}
