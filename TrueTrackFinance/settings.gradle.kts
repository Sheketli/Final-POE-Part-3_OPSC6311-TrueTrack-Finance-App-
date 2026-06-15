// settings.gradle.kts (Project level)
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Jitpack for MPAndroidChart
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "TrueTrackFinance"
include(":app")
