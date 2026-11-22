package com.perol.pixez.shared.ui.coui

import com.perol.pixez.shared.ui.coui.component.couiContourSurfaceEnabled
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CouiUpdateDialogPolicyTest {

    @Test
    fun `COUI 轮廓凝光材质仅在 API 33 及以上启用`() {
        assertFalse(couiContourSurfaceEnabled(31))
        assertFalse(couiContourSurfaceEnabled(32))
        assertTrue(couiContourSurfaceEnabled(33))
        assertTrue(couiContourSurfaceEnabled(34))
    }

    @Test
    fun `COUI 更新弹窗符合原生对话框版式契约且无 MIUIX 或 Material3 残留`() {
        val dialogSource = locateSharedSourceFile("ui/coui/page/CouiUpdateDialog.kt").readText()

        assertFalse(
            "CouiUpdateDialog must not import MIUIX or Material3",
            dialogSource.contains("top.yukonga.miuix") || dialogSource.contains("androidx.compose.material3"),
        )
        assertTrue(
            "CouiUpdateDialog must align content with CouiDialogDefaults.contentPadding",
            dialogSource.contains("CouiDialogDefaults.contentPadding"),
        )
        assertTrue(
            "CouiUpdateDialog must wire neutralAction for cancelling download",
            dialogSource.contains("neutralAction ="),
        )
        assertTrue(
            "CouiUpdateDialog must use FoldableNotes and CouiContourStyle.Dialog inside notes card",
            dialogSource.contains("FoldableNotes(") &&
                dialogSource.contains("CouiContourStyle.Dialog") &&
                dialogSource.contains("couiContourSurface("),
        )
        assertTrue(
            "CouiUpdateDialog must use CouiLinearProgressBar and CouiDialogSecondaryButton",
            dialogSource.contains("CouiLinearProgressBar(") &&
                dialogSource.contains("CouiDialogSecondaryButton("),
        )
    }

    private fun locateSharedSourceFile(relativePath: String): File {
        var dir: File? = File(System.getProperty("user.dir") ?: ".").absoluteFile
        val suffix = "src/main/kotlin/com/perol/pixez/shared/$relativePath"
        while (dir != null) {
            val direct = File(dir, suffix)
            if (direct.isFile) return direct
            val nested = File(dir, "shared/$suffix")
            if (nested.isFile) return nested
            val rootNested = File(dir, "compose-miuix/shared/$suffix")
            if (rootNested.isFile) return rootNested
            dir = dir.parentFile
        }
        error("Unable to locate $relativePath from ${System.getProperty("user.dir")}")
    }
}
