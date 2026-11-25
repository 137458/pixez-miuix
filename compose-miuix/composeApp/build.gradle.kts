import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

// 发布签名配置：keystore.properties（已 gitignore）提供 storeFile/storePassword/keyAlias/keyPassword。
// 缺失时回退 debug 签名，保证本地与 CI 构建（assembleDebug）始终可用；正式发版须先配置该文件。
val releaseSigningProps = rootProject.file("keystore.properties").takeIf { it.exists() }?.let { file ->
    Properties().apply { file.inputStream().use { load(it) } }
}

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

// 回退分支不静默：缺配置时明确告警，并给 release 产物打上 debug-signed 版本标注（见下方 androidComponents）。
val releaseSigningConfigured = releaseSigningProps != null
if (!releaseSigningConfigured) {
    logger.warn(
        "release 构建回退到 debug 签名：未找到 compose-miuix/keystore.properties。" +
            "该 release 产物不可用于正式发版，版本名会追加 -debug-signed 后缀以便追溯。",
    )
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

android {
    namespace = "com.perol.pixez"
    // MIUIX 0.9.5+ 要求 compileSdk >= 36
    compileSdk = 36

    defaultConfig {
        applicationId = "com.perol.pixez.miuix"
        // COUI AAR 清单固定 minSdkVersion=31（Android 12），整体抬齐而非使用 overrideLibrary 门控。
        minSdk = 31
        targetSdk = 35
        versionCode = 10010101
        versionName = "0.9.111.1-miuix"
    }

    buildFeatures {
        compose = true
    }

    signingConfigs {
        if (releaseSigningProps != null) {
            create("release") {
                storeFile = rootProject.file(releaseSigningProps.getProperty("storeFile"))
                storePassword = releaseSigningProps.getProperty("storePassword")
                keyAlias = releaseSigningProps.getProperty("keyAlias")
                keyPassword = releaseSigningProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 有 keystore.properties 时用正式签名，否则回退 debug，避免无密钥环境构建失败
            signingConfig = signingConfigs.getByName(
                if (releaseSigningConfigured) "release" else "debug"
            )
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

dependencies {
    implementation(platform(libs.androidx.compose.bom.alpha))
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)

    // decompose/decompose-compose 由 shared 以 api 暴露，经 projects.shared 传递可达
    implementation(projects.shared)
    implementation(libs.miuix.ui)
    implementation(libs.napier)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.profileinstaller)

    testImplementation(kotlin("test"))
    testImplementation(libs.junit)
}

androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        if (!releaseSigningConfigured) {
            // 产物自证：debug 签名的 release 包在清单版本名上带 -debug-signed，安装后可直接辨认。
            // 基础版本仍取 defaultConfig 的字面量，避免在脚本里再写一份版本号。
            val baseVersionName = android.defaultConfig.versionName
            if (baseVersionName != null) {
                variant.outputs.forEach { output ->
                    output.versionName.set(baseVersionName + "-debug-signed")
                }
            }
        }
    }
}
