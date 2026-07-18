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
    }
}

rootProject.name = "pixez-miuix"
include(":shared")
include(":composeApp")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
