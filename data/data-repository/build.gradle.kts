plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.videoworkshop.data.repository"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // ===== 项目模块 =====
    implementation(project(":core:core-common"))
    implementation(project(":core:core-database"))
    implementation(project(":core:core-ffmpeg"))
    implementation(project(":core:core-media"))
    implementation(project(":domain"))

    // ===== AndroidX =====
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)

    // FFmpegKit (community-maintained) — compileOnly: AAR 由 app 模块在运行时提供
    compileOnly(libs.ffmpegkit.full)
    compileOnly(libs.androidx.annotation)

    // ===== Hilt =====
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // ===== 测试 =====
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
