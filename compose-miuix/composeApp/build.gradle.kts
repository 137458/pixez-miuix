import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
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
            baseName = "ComposeApp"
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
            implementation(projects.shared)
            implementation(libs.decompose)
            implementation(libs.decompose.compose)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.core.splashscreen)
            implementation(libs.androidx.profileinstaller)
        }

        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}

android {
    namespace = "com.perol.pixez"
    // MIUIX 0.9.3 要求 compileSdk >= 36
    compileSdk = 36

    defaultConfig {
        applicationId = "com.perol.pixez.miuix"
        minSdk = 24
        targetSdk = 35
        versionCode = 10010053
        versionName = "0.9.109-miuix"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            excludes += listOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/DEPENDENCIES",
                "/META-INF/INDEX.LIST",
                "/META-INF/LICENSE*",
                "/META-INF/NOTICE*",
                "/META-INF/*.version",
                "/META-INF/*.txt",
                "/META-INF/*.properties",
                "DebugProbesKt.bin"
            )
        }
    }
}


compose.desktop {
    application {
        mainClass = "com.perol.pixez.desktop.MainKt"

        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb)
            packageName = "PixEz"
            packageVersion = "0.9.109"

            jvmArgs += listOf(
                "-XX:+UseG1GC",
                "-XX:+UseStringDeduplication",
                "-Xms64m",
                "-Dfile.encoding=UTF-8"
            )
        }
    }
}


tasks.matching { it.name.contains("AarMetadata") }.configureEach {
    enabled = false
}

