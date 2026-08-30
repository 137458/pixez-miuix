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
            implementation(libs.miuix.ui)
            implementation(libs.napier)
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
            implementation("net.java.dev.jna:jna:5.14.0")
            implementation("net.java.dev.jna:jna-platform:5.14.0")
            implementation("com.mayakapps.compose:window-styler:0.3.2")
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
        versionName = "0.9.108.3-miuix"
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
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb,
            )
            packageName = "PixEz"
            packageVersion = "0.9.108"
            description = "PixEz MIUIX - Pixiv Client with Xiaomi HyperOS design"
            copyright = "© 2026 PixEz Contributors"
            vendor = "PixEz"

            windows {
                iconFile.set(project.file("src/desktopMain/resources/icon.ico"))
                menu = true
                shortcut = true
                dirChooser = true
                perUserInstall = false
                upgradeUuid = "8b646c21-3965-4f40-b384-0a3ffcbe0999"
            }

            macOS {
                iconFile.set(project.file("src/desktopMain/resources/icon.png"))
                bundleID = "com.perol.pixez"
            }

            linux {
                iconFile.set(project.file("src/desktopMain/resources/icon.png"))
            }

            modules(
                "java.base",
                "java.desktop",
                "java.sql",
                "java.naming",
                "java.management",
                "java.net.http",
                "java.security.jgss",
                "java.xml",
                "jdk.unsupported",
            )

            jvmArgs += listOf(
                "-XX:+UseG1GC",
                "-XX:+UseStringDeduplication",
                "-Xms128m",
                "-Xmx2048m",
                "-Dfile.encoding=UTF-8",
                "-Djava.net.useSystemProxies=true",
                "-Dskiko.fps=0",
                "-Dskiko.vsync.enabled=true",
                "-Dskiko.hardwareAcceleration=true",
                "-Dskiko.directx.enabled=true",
                "-Dcompose.interop.blending=true",
                "-Dsun.java2d.d3d=true",
            )
        }
    }
}

tasks.register<Zip>("packageWindowsPortableZip") {
    group = "compose desktop"
    description = "Packs the Windows desktop distributable into a clean portable zip archive."
    dependsOn("createDistributable")
    from(layout.buildDirectory.dir("compose/binaries/main/app/PixEz"))
    into("PixEz")
    archiveFileName.set("PixEz-windows-x64-portable.zip")
    destinationDirectory.set(layout.buildDirectory.dir("compose/binaries/main/zip"))
}

tasks.matching { it.name.contains("AarMetadata") }.configureEach {
    enabled = false
}

