package com.videoworkshop.core.common

/**
 * 统一的操作结果封装。
 *
 * 用于包装异步/同步操作的最终状态，便于 UI 层基于状态渲染界面。
 */
sealed class Result<out T> {

    /** 操作成功，携带结果数据。 */
    data class Success<out T>(val data: T) : Result<T>()

    /** 操作失败，携带错误信息与可选原因。 */
    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : Result<Nothing>()

    /** 操作进行中。 */
    data object Loading : Result<Nothing>()

    /**
     * 当结果为 [Success] 时执行 [action]，并返回自身以支持链式调用。
     */
    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }

    /**
     * 当结果为 [Error] 时执行 [action]。
     */
    inline fun onError(action: (String, Throwable?) -> Unit): Result<T> {
        if (this is Error) action(message, cause)
        return this
    }

    /**
     * 当结果为 [Loading] 时执行 [action]。
     */
    inline fun onLoading(action: () -> Unit): Result<T> {
        if (this is Loading) action()
        return this
    }

    /**
     * 将 [Success] 中的数据映射为新的类型，其他状态原样返回。
     */
    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
        is Loading -> this
    }

    /**
     * 获取数据，若非 [Success] 则返回 null。
     */
    fun getOrNull(): T? = (this as? Success)?.data

    companion object {
        fun <T> success(data: T): Result<T> = Success(data)
        fun error(message: String, cause: Throwable? = null): Result<Nothing> = Error(message, cause)
        fun loading(): Result<Nothing> = Loading
    }
}
