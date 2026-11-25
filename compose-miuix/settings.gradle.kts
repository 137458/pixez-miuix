pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        // COUI-Kit 0.0.1-alpha18+ 发布仓库
        maven {
            url = uri("https://jitpack.io")
            content { includeGroup("com.github.itoseo.coui-kit") }
        }
        // 金标联盟系统卡片转场 SDK 仓库
        exclusiveContent {
            forRepository {
                maven {
                    url = uri("http://nexus.itgsa.com:5566/repository/sharedViewKit/")
                    isAllowInsecureProtocol = true
                }
            }
            filter { includeGroup("gsai.sdk") }
        }
    }
}

rootProject.name = "pixez-miuix"
include(":shared")
include(":composeApp")
include(":system-animation-stubs")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
