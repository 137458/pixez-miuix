plugins {
    // 在 libs.versions.toml 中统一定义版本
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.sqldelight) apply false
}

// AGP 8.13 的 AarMetadata 与 local lint AAR 校验任务在 KMP 多目标工程下会因 Kotlin 元数据版本误报，
// 统一在 root 关闭一次，避免各模块各复制一份 workaround（历史上 shared 两份、composeApp 一份）。
subprojects {
    tasks.matching {
        it.name.contains("AarMetadata") ||
            it.name.contains("bundleReleaseLocalLintAar") ||
            it.name.contains("bundleDebugLocalLintAar")
    }.configureEach { enabled = false }
}
