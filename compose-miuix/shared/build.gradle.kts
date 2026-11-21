import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

android {
    namespace = "com.perol.pixez.shared"
    // MIUIX 0.9.4 要求 compileSdk >= 36
    compileSdk = 36

    defaultConfig {
        // COUI AAR 清单固定 minSdkVersion=31（Android 12），整体抬齐而非使用 overrideLibrary 门控。
        minSdk = 31
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            // Compose Android runtime 等三方库在 JVM 单测中调用 android.util.Log，允许返回默认值避免 not mocked 中断
            isReturnDefaultValues = true
        }
    }

    lint {
        // AGP 8.13 lint 内嵌的 Kotlin 编译器为 2.2.0，无法读取项目/MIUIX 使用的 Kotlin 2.4.10 元数据，
        // 会在 lintAnalyze 阶段抛出 "incompatible version of Kotlin" 错误（非 lint issue，无 issue id 可禁用）。
        // 在 AGP 升级到支持 Kotlin 2.4.10 元数据之前，暂时关闭 abortOnError 和 checkReleaseBuilds，避免阻塞构建。
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom.alpha))
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)

    // MIUIX
    implementation(libs.miuix.ui)
    implementation(libs.miuix.preference)
    implementation(libs.miuix.icons)
    implementation(libs.miuix.blur)

    implementation(libs.zoomable)

    // COUI（ColorOS 皮肤，仅 Android）
    implementation(libs.androidx.core.ktx)
    implementation(libs.coui)

    // Ktor
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)
    implementation(libs.okhttp.dnsoverhttps)

    // Kotlinx
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)

    // Navigation / Logging
    api(libs.decompose)
    api(libs.decompose.compose)
    implementation(libs.navigationevent.compose)
    implementation(libs.napier)

    // Settings
    api(libs.multiplatform.settings)

    // SQLDelight（runtime 提供 SqlDriver.Schema 等共享 API）
    api(libs.sqldelight.runtime)
    implementation(libs.sqldelight.android.driver)

    // AndroidX Lifecycle / Activity
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.network.ktor3)
    implementation(libs.coil.gif)

    // Unit Tests
    testImplementation(kotlin("test"))
    testImplementation(libs.junit)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.sqldelight.sqlite.driver)
}

// SQLDelight migration 验证在 Windows 上因 native sqlite 库释放路径问题可能失败，
// 因此仅在非 Windows 平台默认启用；Windows 本地/CI 可通过 -PskipVerifyMigrations 显式跳过。
// 现在有真实 migration 文件（1.sqm），必须保留校验以捕获 schema 不一致问题。
tasks.withType<app.cash.sqldelight.gradle.VerifyMigrationTask>().configureEach {
    onlyIf {
        val skip = project.findProperty("skipVerifyMigrations")?.toString()?.toBoolean() ?: false
        !skip && !System.getProperty("os.name").orEmpty().lowercase().contains("win")
    }
}

sqldelight {
    databases {
        // 为兼容旧 Flutter 应用，每个旧 .db 文件对应一个独立数据库，
        // 文件路径与表结构均与旧版保持一致。
        // 每个数据库通过 srcDirs 限定只编译自己的 .sq 文件，避免生成类重名。
        //
        // 以下旧 Flutter 数据库仍按需预留：
        //  - tag.db（表 tag）
        //  - NovelViewerPersist.db（表 novel_viewer_persist）
        //  - banncommentid.db（表 ban_comment_persist）
        create("AccountDatabase") {
            packageName.set("com.perol.pixez.shared.data.local.account")
            srcDirs.setFrom("src/main/sqldelight/account")
        }
        // 注意：该数据库实际表名为 glanceillustpersist，对应旧 glanceillustpersist.db，
        // 因此生成类命名为 GlanceIllustPersistDatabase，避免与未来的 illustpersist.db 混淆。
        create("GlanceIllustPersistDatabase") {
            packageName.set("com.perol.pixez.shared.data.local.glanceillustpersist")
            srcDirs.setFrom("src/main/sqldelight/glanceillustpersist")
        }
        create("TaskDatabase") {
            packageName.set("com.perol.pixez.shared.data.local.task")
            srcDirs.setFrom("src/main/sqldelight/task")
        }
        create("KVPairDatabase") {
            packageName.set("com.perol.pixez.shared.data.local.kvpair")
            srcDirs.setFrom("src/main/sqldelight/kvpair")
        }
        create("BanIllustIdDatabase") {
            packageName.set("com.perol.pixez.shared.data.local.banillustid")
            srcDirs.setFrom("src/main/sqldelight/banillustid")
        }
        create("BanUserIdDatabase") {
            packageName.set("com.perol.pixez.shared.data.local.banuserid")
            srcDirs.setFrom("src/main/sqldelight/banuserid")
        }
        create("BanTagDatabase") {
            packageName.set("com.perol.pixez.shared.data.local.bantag")
            srcDirs.setFrom("src/main/sqldelight/bantag")
        }
        create("IllustPersistDatabase") {
            packageName.set("com.perol.pixez.shared.data.local.illustpersist")
            srcDirs.setFrom("src/main/sqldelight/illustpersist")
        }
        create("NovelPersistDatabase") {
            packageName.set("com.perol.pixez.shared.data.local.novelpersist")
            srcDirs.setFrom("src/main/sqldelight/novelpersist")
        }
    }
}
