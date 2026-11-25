package com.perol.pixez.shared.ui.navigation.animation.system

import com.itgsa.animation.sharedview.ViewAnimationType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SystemCardTransitionPolicyTest {

    @Test
    fun `officialBoundsUseWindowCoordinates 按第二段协议声明解析坐标系`() {
        assertTrue(officialBoundsUseWindowCoordinates("desc|WINDOW"))
        assertFalse(officialBoundsUseWindowCoordinates("desc|SCREEN"))
        assertFalse(officialBoundsUseWindowCoordinates(null))
        assertTrue(officialBoundsUseWindowCoordinates("window coordinates"))
    }

    @Test
    fun `supportsOfficialBackType 遵循设备声明列表`() {
        assertFalse(supportsOfficialBackType(null, "VIEW_ANIM_FADE_OUT"))
        assertTrue(supportsOfficialBackType("1|VIEW_ANIM_DEFAULT,VIEW_ANIM_FADE_OUT", "VIEW_ANIM_FADE_OUT"))
        assertFalse(supportsOfficialBackType("1|VIEW_ANIM_DEFAULT", "VIEW_ANIM_FADE_OUT"))
        assertTrue(supportsOfficialBackType("1|legacy", "VIEW_ANIM_FADE_OUT"))
    }

    @Test
    fun `resolveOfficialReturnAnimType 在来源失效时退化为非锚点退场`() {
        assertEquals(
            ViewAnimationType.VIEW_ANIM_FADE_OUT,
            resolveOfficialReturnAnimType(
                metadata = "1|VIEW_ANIM_DEFAULT,VIEW_ANIM_FADE_OUT",
                motionEnabled = true,
                sourceAvailable = false,
            ),
        )
        assertEquals(
            ViewAnimationType.VIEW_ANIM_DEFAULT,
            resolveOfficialReturnAnimType(
                metadata = "1|VIEW_ANIM_DEFAULT,VIEW_ANIM_FADE_OUT",
                motionEnabled = true,
                sourceAvailable = true,
            ),
        )
        assertEquals(
            ViewAnimationType.VIEW_ANIM_NONE,
            resolveOfficialReturnAnimType(
                metadata = "1|VIEW_ANIM_NONE,VIEW_ANIM_DEFAULT",
                motionEnabled = false,
                sourceAvailable = true,
            ),
        )
    }

    @Test
    fun `resolveOfficialCardReturnPlan 仅在准备关闭时提交返回计划`() {
        assertNull(
            resolveOfficialCardReturnPlan(
                metadata = "1|VIEW_ANIM_DEFAULT",
                oplusSdkWindow = false,
                preparingToFinish = false,
                motionEnabled = true,
                sourceAvailable = true,
            ),
        )
        val oplusPlan = resolveOfficialCardReturnPlan(
            metadata = "1|VIEW_ANIM_DEFAULT,VIEW_ANIM_FADE_OUT",
            oplusSdkWindow = true,
            preparingToFinish = true,
            motionEnabled = true,
            sourceAvailable = true,
        )
        assertEquals(
            OfficialCardReturnPlan(
                backAnimType = ViewAnimationType.VIEW_ANIM_DEFAULT,
                useSourceBounds = true,
                updateRadius = false,
                updateEffects = false,
            ),
            oplusPlan,
        )
    }

    @Test
    fun `shouldAttemptSystemCardTransition 在分屏或缺少锚点时回退应用内转场`() {
        assertFalse(shouldAttemptSystemCardTransition(isMultiWindowOrFloating = true, hasSourceAnchor = true, capabilityAvailable = true))
        assertFalse(shouldAttemptSystemCardTransition(isMultiWindowOrFloating = false, hasSourceAnchor = false, capabilityAvailable = true))
        assertFalse(shouldAttemptSystemCardTransition(isMultiWindowOrFloating = false, hasSourceAnchor = true, capabilityAvailable = false))
        assertTrue(shouldAttemptSystemCardTransition(isMultiWindowOrFloating = false, hasSourceAnchor = true, capabilityAvailable = true))
    }

    @Test
    fun `shouldUseSourceReturnLanding 在详情页横滑切换作品后禁止飞回原卡片`() {
        assertTrue(
            shouldUseSourceReturnLanding(
                originIllustId = 100L,
                displayedIllustId = 100L,
                sameWindow = true,
                anchorMatchesGeometry = true,
            ),
        )
        assertFalse(
            shouldUseSourceReturnLanding(
                originIllustId = 100L,
                displayedIllustId = 200L,
                sameWindow = true,
                anchorMatchesGeometry = true,
            ),
        )
        assertFalse(
            shouldUseSourceReturnLanding(
                originIllustId = 100L,
                displayedIllustId = 100L,
                sameWindow = false,
                anchorMatchesGeometry = true,
            ),
        )
        assertFalse(
            shouldUseSourceReturnLanding(
                originIllustId = 100L,
                displayedIllustId = 100L,
                sameWindow = true,
                anchorMatchesGeometry = false,
            ),
        )
    }
}
