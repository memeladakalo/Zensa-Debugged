package com.zensa.app.core.network

/**
 * Represents the result of an operation in the Zensa application.
 */
sealed class ApiResult<out T> {

    /**
     * Operation completed successfully.
     */
    data class Success<out T>(
        val data: T
    ) : ApiResult<T>()

    /**
     * Operation failed.
     */
    data class Error(
        val message: String,
        val throwable: Throwable? = null
    ) : ApiResult<Nothing>()

    /**
     * Operation is currently in progress.
     */
    data object Loading : ApiResult<Nothing>()
}