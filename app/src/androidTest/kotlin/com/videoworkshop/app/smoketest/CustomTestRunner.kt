package com.videoworkshop.app.smoketest

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * 自定义 Instrumentation TestRunner。
 *
 * 用 [HiltTestApplication] 替换 Application，使 androidTest 中的 `@HiltAndroidTest`
 * 用例可注入 Hilt 依赖图（含测试替身）。在 `app/build.gradle.kts` 中通过
 * `testInstrumentationRunner` 指向本类。
 */
class CustomTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        name: String?,
        context: Context?
    ): Application {
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}
