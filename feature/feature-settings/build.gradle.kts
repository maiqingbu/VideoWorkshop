import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.videoworkshop.feature.settings"
    compileSdk = 35

    buildToolsVersion = "35.0.0"

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "String",
            "BUILD_TIME",
            "\"${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}\""
        )
        buildConfigField("String", "GITHUB_URL", "\"https://github.com/maiqingbu\"")
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
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // ===== 项目模块 =====
    implementation(project(":core:core-common"))
    implementation(project(":core:core-ui"))
    implementation(project(":core:core-designsystem"))
    implementation(project(":core:core-datastore"))
    implementation(project(":domain"))

    // ===== AndroidX =====
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // ===== Compose =====
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.foundation)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // ===== Navigation =====
    implementation(libs.androidx.navigation.compose)

    // ===== Hilt =====
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // ===== Coroutines =====
    implementation(libs.kotlinx.coroutines.android)

    // ===== 测试 =====
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
