package com.perol.pixez.shared

/**
 * 应用级静态信息，避免版本号、名称等散落在多个屏幕中。
 * 注意：VERSION_NAME 是应用内自报版本（更新检查、关于页、User-Agent 的唯一来源），
 * 发版时必须与 composeApp/build.gradle.kts 的 versionName 同步升级，
 * 守护测试见 shared/src/test 的 AppInfoVersionParityTest。
 */
object AppInfo {
    const val VERSION_NAME = "0.9.110.1-miuix"

    val CURRENT_CHANGELOG = """
        ## 优化

        - 优化了 ColorOS 皮肤的应用更新弹窗版式与交互：对齐弹窗内容水平边距，更新说明卡片接入半透明凝光轮廓与限定高度的边缘羽化滚动视口；收起态默认展示标题与首批更新条目并提示剩余行数，展开/收起按钮固定在卡片底缘便于随时收起。
        - 优化了 ColorOS 更新弹窗与更新设置页的下载体验：下载进度条支持平滑填充与总长未知时的不定态动画，下载期间「取消下载」接入弹窗原生底部按钮栏，下载失败的「浏览器下载」与「忽略此版本」统一为次级胶囊按钮样式。
    """.trimIndent()
}
