package com.perol.pixez.shared.platform

/**
 * 触觉反馈类型定义。
 */
enum class HapticType {
    Confirm,
    Reject,
    GestureStart,
    GestureEnd,
    Tick,
}

/**
 * 触发系统物理级触觉反馈（触感振动）。
 */
expect fun performHapticFeedback(type: HapticType)
