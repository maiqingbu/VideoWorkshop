package com.videoworkshop.core.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * 协程调度器提供者接口。
 *
 * 通过抽象 [Dispatchers]，便于在测试中替换为 [kotlinx.coroutines.test.TestDispatcher]，
 * 实现对协程调度环境的可控管理。
 */
interface DispatcherProvider {
    /** 主线程调度器，用于 UI 操作。 */
    val main: CoroutineDispatcher

    /** IO 调度器，用于磁盘读写、网络、数据库等阻塞操作。 */
    val io: CoroutineDispatcher

    /** 默认调度器，用于 CPU 密集型计算。 */
    val default: CoroutineDispatcher

    /** 不受限调度器。 */
    val unconfined: CoroutineDispatcher
}

/**
 * 默认实现，直接使用系统 [Dispatchers]。
 */
class DefaultDispatcherProvider : DispatcherProvider {
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val default: CoroutineDispatcher = Dispatchers.Default
    override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
}
