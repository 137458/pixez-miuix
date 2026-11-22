package com.perol.pixez.shared

/**
 * 应用级静态信息，避免版本号、名称等散落在多个屏幕中。
 * 注意：VERSION_NAME 是应用内自报版本（更新检查、关于页、User-Agent 的唯一来源），
 * 发版时必须与 composeApp/build.gradle.kts 的 versionName 同步升级，
 * 守护测试见 shared/src/test 的 AppInfoVersionParityTest。
 */
object AppInfo {
    const val VERSION_NAME = "0.9.111.1-miuix"

    val CURRENT_CHANGELOG = """
        ## 修复

        - 修复了检查更新在官方接口限流时直接失败的问题：接口不可用时改为自动从发布页继续获取版本信息，检查成功与失败分别呈现，不再把检查失败误显示为「已是最新版本」；同时为官方接口请求加入缓存校验，减少重复查询消耗的配额。
""".trimIndent()
}
