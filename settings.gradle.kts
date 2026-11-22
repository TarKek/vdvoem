pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
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

rootProject.name = "Vdvoem"

include(":app")
include(":core:ui")
include(":core:domain")
include(":core:data")
include(":core:sync")
include(":feature:calendar")
include(":feature:wishes")
include(":feature:gifts")
include(":feature:store")
include(":feature:settings")
include(":feature:quizzes")
include(":feature:timetable")
include(":feature:notes")
include(":feature:cycle")
include(":feature:dates")
include(":feature:voice")
include(":feature:tasks")
include(":feature:diary")
