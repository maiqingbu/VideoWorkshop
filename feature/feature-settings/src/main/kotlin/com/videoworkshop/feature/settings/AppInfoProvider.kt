package com.videoworkshop.feature.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 应用元信息提供者：负责向 [SettingsViewModel] 暴露版本号、构建时间与缓存目录。
 *
 * 抽象此层便于在测试中替换为 fake，避免直接依赖 BuildConfig。
 */
interface AppInfoProvider {
    /** 应用版本名（如 1.0.0）。 */
    val versionName: String

    /** 构建时间描述。 */
    val buildTime: String

    /** 应用缓存目录。 */
    val cacheDir: File

    /** filesDir 下的 cache 子目录（临时产物）。 */
    val filesCacheDir: File

    /** GitHub 仓库链接。 */
    val githubUrl: String
}

/**
 * 默认实现：从 [Context] 读取 [versionName]；从 [BuildConfig] 读取 [buildTime] / [githubUrl]。
 *
 * [BuildConfig] 由 feature-settings 模块在构建时生成，避免依赖 app 模块。
 */
@Singleton
class DefaultAppInfoProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : AppInfoProvider {

    override val versionName: String by lazy {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
        }.getOrDefault("unknown")
    }

    override val buildTime: String by lazy {
        runCatching { BuildConfig.BUILD_TIME }.getOrDefault("—")
    }

    override val githubUrl: String by lazy {
        runCatching { BuildConfig.GITHUB_URL }.getOrDefault("https://github.com/maiqingbu")
    }

    override val cacheDir: File
        get() = context.cacheDir

    override val filesCacheDir: File
        get() = File(context.filesDir, "cache")
}

/**
 * 便捷扩展：返回 [AboutInfo] 当前快照。
 */
fun AppInfoProvider.about(): AboutInfo = AboutInfo(
    versionName = versionName,
    buildTime = buildTime,
    githubUrl = githubUrl
)

/**
 * Hilt 模块：将 [AppInfoProvider] 绑定为 [DefaultAppInfoProvider]。
 */
@dagger.Module
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
abstract class SettingsAppInfoModule {
    @dagger.Binds
    @Singleton
    abstract fun bindAppInfoProvider(impl: DefaultAppInfoProvider): AppInfoProvider
}
