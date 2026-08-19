import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm("desktop")

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    macosArm64()

    sourceSets {
        val desktopMain by getting

        commonMain.dependencies {
            // 当前 Compose 1.11.1 仍使用 compose.* DSL；后续升级至 1.12+ 后迁移到 compose.dependencies.*
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)


            // MIUIX
            implementation(libs.miuix.ui)
            implementation(libs.miuix.preference)
            implementation(libs.miuix.icons)

            // Kyant0 Backdrop (Liquid Glass 毛玻璃特效)
            implementation(libs.backdrop)

            // Ktor
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)

            // Kotlinx
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)

            // DI / Navigation / Logging
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            api(libs.decompose)
            api(libs.decompose.compose)
            implementation(libs.navigationevent.compose)
            implementation(libs.napier)

            // Settings
            api(libs.multiplatform.settings)
            api(libs.multiplatform.settings.coroutines)

            // SQLDelight runtime（提供 SqlDriver.Schema 等共享 API）与 coroutines 扩展
            api(libs.sqldelight.runtime)
            api(libs.sqldelight.coroutines.extensions)

            // Coil
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.junit)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqldelight.android.driver)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
        }

        desktopMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqldelight.sqlite.driver)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native.driver)
        }

        macosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native.driver)
        }
    }
}

android {
    namespace = "com.perol.pixez.shared"
    // MIUIX 0.9.3 要求 compileSdk >= 36
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        // AGP 8.13 lint 内嵌的 Kotlin 编译器为 2.2.0，无法读取项目/MIUIX 使用的 Kotlin 2.4.0 元数据，
        // 会在 lintAnalyze 阶段抛出 "incompatible version of Kotlin" 错误（非 lint issue，无 issue id 可禁用）。
        // 在 AGP 升级到支持 Kotlin 2.4.0 元数据之前，暂时关闭 abortOnError 和 checkReleaseBuilds，避免阻塞构建。
        abortOnError = false
        checkReleaseBuilds = false
    }
}

tasks.matching { it.name.contains("AarMetadata") || it.name.contains("bundleReleaseLocalLintAar") || it.name.contains("bundleDebugLocalLintAar") }.configureEach {
    enabled = false
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
        // TODO(M2 后续或 M3): 旧 Flutter 还有以下数据库未接入迁移，对应功能历史数据会暂时缺失：
        //  - illustpersist.db（表 illustpersist）
        //  - tag.db（表 tag）
        //  - Novelpersist.db（表 novelpersist）
        //  - NovelViewerPersist.db（表 novel_viewer_persist）
        //  - banncommentid.db（表 ban_comment_persist）
        create("AccountDatabase") {
            packageName.set("com.perol.pixez.shared.data.local.account")
            srcDirs.setFrom("src/commonMain/sqldelight/account")
        }
        // 注意：该数据库实际表名为 glanceillustpersist，对应旧 glanceillustpersist.db，
        // 因此生成类命名为 GlanceIllustPersistDatabase，避免与未来的 illustpersist.db 混淆。
        create("GlanceIllustPersistDatabase") {
            packageName.set("com.perol.pixez.shared.data.local.glanceillustpersist")
            srcDirs.setFrom("src/commonMain/sqldelight/glanceillustpersist")
        }
        create("TaskDatabase") {
            packageName.set("com.perol.pixez.shared.data.local.task")
            srcDirs.setFrom("src/commonMain/sqldelight/task")
        }
        create("KVPairDatabase") {
            packageName.set("com.perol.pixez.shared.data.local.kvpair")
            srcDirs.setFrom("src/commonMain/sqldelight/kvpair")
        }
        create("BanIllustIdDatabase") {
            packageName.set("com.perol.pixez.shared.data.local.banillustid")
            srcDirs.setFrom("src/commonMain/sqldelight/banillustid")
        }
        create("BanUserIdDatabase") {
            packageName.set("com.perol.pixez.shared.data.local.banuserid")
            srcDirs.setFrom("src/commonMain/sqldelight/banuserid")
        }
        create("BanTagDatabase") {
            packageName.set("com.perol.pixez.shared.data.local.bantag")
            srcDirs.setFrom("src/commonMain/sqldelight/bantag")
        }
        create("IllustPersistDatabase") {
            packageName.set("com.perol.pixez.shared.data.local.illustpersist")
            srcDirs.setFrom("src/commonMain/sqldelight/illustpersist")
        }
        create("NovelPersistDatabase") {
            packageName.set("com.perol.pixez.shared.data.local.novelpersist")
            srcDirs.setFrom("src/commonMain/sqldelight/novelpersist")
        }
    }
}

tasks.matching { it.name.contains("AarMetadata") }.configureEach {
    enabled = false
}

