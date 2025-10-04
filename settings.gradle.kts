// settings.gradle.kts

pluginManagement {
    repositories {
        // Ensure these three are present and in this order
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
    }
}

rootProject.name = "Agentic Notes"
include(":app")
