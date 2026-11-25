package com.perol.pixez.shared.ui.navigation.animation.system

import com.itgsa.animation.sharedview.ViewAnimationType

internal enum class SystemCardTransitionMethod {
    OFFICIAL,
    OPLUS_LEGACY,
    VIVO_ONESHOT,
    XIAOMI_ROUNDED,
    NONE,
}

internal data class SystemCardCapability(
    val method: SystemCardTransitionMethod = SystemCardTransitionMethod.NONE,
    val configurableBlur: Boolean = false,
    /** EXTRA_BACK_ANIM_TYPE 原始声明；null = 设备未声明，按协议不假定类型可写。 */
    val backAnimMetadata: String? = null,
    /** 标准 SDK 与原生无缝门面并存，使用该门面的返回更新契约。 */
    val oplusSdkWindow: Boolean = false,
    val systemConditionSummary: String? = null,
    val officialSupport: Int? = null,
    val canConfigureBackgroundScale: Boolean = false,
    val canConfigureColor: Boolean = false,
    val canRefreshBitmap: Boolean = false,
    val officialBoundsWindowCoordinates: Boolean = false,
) {
    val available: Boolean get() = method != SystemCardTransitionMethod.NONE
}

internal data class SystemCardWindowGeometry(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val displayId: Int,
    val rotation: Int,
    val densityDpi: Int,
)

internal data class OfficialCardReturnPlan(
    val backAnimType: Int?,
    val useSourceBounds: Boolean,
    val updateRadius: Boolean,
    val updateEffects: Boolean,
)

/** 第二段机器可读声明优先；旧描述未声明坐标系时按协议使用屏幕坐标。 */
internal fun officialBoundsUseWindowCoordinates(metadata: String?): Boolean {
    val token = metadata?.split('|')?.getOrNull(1)?.trim()
    if (token.equals("WINDOW", ignoreCase = true)) return true
    if (token.equals("SCREEN", ignoreCase = true)) return false
    return metadata?.contains("WINDOW", ignoreCase = true) == true
}

/** 只使用设备声明可写的参数；旧版 int 声明允许公开合法值。 */
internal fun supportsOfficialBackType(metadata: String?, type: String): Boolean {
    if (metadata == null) return false
    val names = metadata.split('|').getOrNull(1)?.split(',')?.map(String::trim).orEmpty()
    val declared = names.filter { it.startsWith("VIEW_ANIM_") }
    return declared.isEmpty() || type in declared
}

/** 来源失效后只选择不依赖卡片落点的退场；未声明可用类型时保留设备策略。 */
internal fun resolveOfficialReturnAnimType(
    metadata: String?,
    motionEnabled: Boolean,
    sourceAvailable: Boolean,
): Int? {
    val preferredTypes = when {
        !motionEnabled -> listOf("VIEW_ANIM_NONE", "VIEW_ANIM_FOLLOW_SYSTEM")
        !sourceAvailable -> listOf(
            "VIEW_ANIM_FOLLOW_SYSTEM",
            "VIEW_ANIM_FADE_OUT",
            "VIEW_ANIM_EXPLODE_OUT",
            "VIEW_ANIM_NONE",
        )
        else -> listOf("VIEW_ANIM_DEFAULT")
    }
    return when (preferredTypes.firstOrNull { supportsOfficialBackType(metadata, it) }) {
        "VIEW_ANIM_NONE" -> ViewAnimationType.VIEW_ANIM_NONE
        "VIEW_ANIM_FOLLOW_SYSTEM" -> ViewAnimationType.VIEW_ANIM_FOLLOW_SYSTEM
        "VIEW_ANIM_FADE_OUT" -> ViewAnimationType.VIEW_ANIM_FADE_OUT
        "VIEW_ANIM_EXPLODE_OUT" -> ViewAnimationType.VIEW_ANIM_EXPLODE_OUT
        "VIEW_ANIM_DEFAULT" -> ViewAnimationType.VIEW_ANIM_DEFAULT
        else -> null
    }
}

/** 此门面的默认类型不清除跳过标记；只在关闭前提交返回控制，且不更新仅支持打开的字段。 */
internal fun resolveOfficialCardReturnPlan(
    metadata: String?,
    oplusSdkWindow: Boolean,
    preparingToFinish: Boolean,
    motionEnabled: Boolean,
    sourceAvailable: Boolean,
): OfficialCardReturnPlan? {
    if (!preparingToFinish) return null
    val useSourceBounds = motionEnabled && sourceAvailable
    return OfficialCardReturnPlan(
        backAnimType = resolveOfficialReturnAnimType(metadata, motionEnabled, sourceAvailable),
        useSourceBounds = useSourceBounds,
        updateRadius = useSourceBounds && !oplusSdkWindow,
        updateEffects = !oplusSdkWindow,
    )
}

/** 分屏/小窗、缺少来源锚点或设备协议不可用时回退应用内 CardExpandLayer。 */
internal fun shouldAttemptSystemCardTransition(
    isMultiWindowOrFloating: Boolean,
    hasSourceAnchor: Boolean,
    capabilityAvailable: Boolean,
): Boolean = !isMultiWindowOrFloating && hasSourceAnchor && capabilityAvailable

/** 仅同窗、未切图且来源几何仍与打开时一致时才回飞原卡片。 */
internal fun shouldUseSourceReturnLanding(
    originIllustId: Long,
    displayedIllustId: Long,
    sameWindow: Boolean,
    anchorMatchesGeometry: Boolean,
): Boolean = originIllustId == displayedIllustId && sameWindow && anchorMatchesGeometry
