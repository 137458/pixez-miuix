package com.perol.pixez.shared.ui.state

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.readText

/**
 * 页面级状态算子（rememberXxxState）的源码读取工具。
 *
 * 六个算子均为 @Composable 且依赖 CompositionLocal 中的具体仓储类（LocalIllustRepository 等），
 * shared 模块未引入 compose ui-test 依赖，纯 JVM 测试无法驱动组合，故这些算子的「接线」
 * （状态机如何被构造、注入何种合并/过滤策略、何时 reset）只能以源码文本守护测试锁定。
 * 状态机本身的行为（追加/去重/失败保留/重置）由各 XxxStateTest 直接驱动真状态机验证。
 */
internal object StateSourceReader {

    private val mainUiRoot = "shared/src/main/kotlin/com/perol/pixez/shared/ui"

    /** 从 Gradle 测试工作目录向上定位仓库根：以 shared/src 目录为锚点。 */
    private fun repoRoot(): Path {
        var dir: Path? = Path.of(System.getProperty("user.dir")).toAbsolutePath()
        while (dir != null) {
            if (dir.resolve("shared/src").isDirectory()) return dir
            dir = dir.parent
        }
        error("未定位到仓库根目录（working dir=${System.getProperty("user.dir")}）")
    }

    /** 读取 main ui 下的源码文件，[relativePath] 形如 "state/NewFeedState.kt"。 */
    fun readUi(relativePath: String): String {
        val file = repoRoot().resolve(mainUiRoot).resolve(relativePath)
        check(file.exists()) { "缺少待守护源码：$file" }
        return file.readText()
    }
}
