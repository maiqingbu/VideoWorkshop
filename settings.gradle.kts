pluginManagement {
    repositories {
        // 直连 Maven Central 与 Gradle Plugin Portal（阿里云 central 镜像对部分 plugin marker POM 返回 502）
        mavenCentral()
        gradlePluginPortal()
        // 阿里云 Google 镜像（dl.google.com 在当前网络不可达，AGP 等 Google-only 工件必须走镜像）
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        google()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        google()
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
include(":feature:feature-abtransport")
include(":feature:feature-history")
include(":feature:feature-settings")
