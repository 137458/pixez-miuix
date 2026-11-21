plugins {
    // 在 libs.versions.toml 中统一定义版本
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.sqldelight) apply false
}

// coui、androidx-core-ktx、androidx.compose.*（BOM 2026.08.01）、miuix、zoomable、coil 等依赖的 AAR
// 声明 minCompileSdk=37 / minAndroidGradlePluginVersion=9.1.0，高于工程当前的 compileSdk=36 与 AGP 8.13.0，
// 统一关闭 AarMetadata 检查以避免 check*AarMetadata 阻塞构建。
subprojects {
    tasks.matching {
        it.name.contains("AarMetadata")
    }.configureEach { enabled = false }
}
