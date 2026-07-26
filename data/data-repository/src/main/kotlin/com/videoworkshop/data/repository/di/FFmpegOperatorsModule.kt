package com.videoworkshop.data.repository.di

import com.videoworkshop.core.ffmpeg.operators.AVStreamSwapper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * FFmpeg operators Hilt 模块。
 *
 * 为 core-ffmpeg 中的 operators（如 [AVStreamSwapper]）提供 DI 实例。
 *
 * 这些 operators 内部已通过默认参数注入了 [com.videoworkshop.core.ffmpeg.FfmpegEngine]
 * 与 [com.videoworkshop.core.ffmpeg.FfprobeHelper]，无需额外依赖；
 * 此处仅负责实例化并将其暴露为 [Singleton]。
 *
 * 与 [RepositoryModule] 分离的原因：
 * [RepositoryModule] 使用 [@dagger.Binds] 抽象方法绑定接口到实现，
 * 而 operators 是具体类、需通过 [@dagger.Provides] 提供实例，
 * 二者风格不同，独立模块更清晰。
 */
@Module
@InstallIn(SingletonComponent::class)
object FFmpegOperatorsModule {

    /**
     * 提供 [AVStreamSwapper] 单例。
     *
     * 使用其默认构造参数：
     * - executor = [com.videoworkshop.core.ffmpeg.operators.DefaultFfmpegExecutor]（委托 [com.videoworkshop.core.ffmpeg.FfmpegEngine]）
     * - probeVideo = [com.videoworkshop.core.ffmpeg.FfprobeHelper.getVideoInfo]
     *
     * 单例化原因：[AVStreamSwapper] 无内部状态，可安全复用；
     * 同时与 [DedupRepositoryImpl] 的单例生命周期对齐。
     */
    @Provides
    @Singleton
    fun provideAVStreamSwapper(): AVStreamSwapper = AVStreamSwapper()
}
