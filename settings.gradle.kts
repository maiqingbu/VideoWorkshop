pluginManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        google()
        mavenCentral()
    }
}

rootProject.name = "VideoWorkshop"

include(":app")

// Core modules
include(":core:core-common")
include(":core:core-ui")
include(":core:core-designsystem")
include(":core:core-datastore")
include(":core:core-database")
include(":core:core-network")
include(":core:core-ffmpeg")
include(":core:core-media")

// Domain
include(":domain")

// Data
include(":data:data-alliance")
include(":data:data-ai")
include(":data:data-repository")
include(":data:data-publish")

// Feature
include(":feature:feature-home")
include(":feature:feature-dedup")
include(":feature:feature-videoenhance")
include(":feature:feature-imageeditor")
include(":feature:feature-goods")
include(":feature:feature-material")
include(":feature:feature-publish")
