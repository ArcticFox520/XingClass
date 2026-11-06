package com.xingclass.app.core

/**
 * Result type used across data/domain boundaries.
 *
 * It intentionally keeps the failure side small and UI-agnostic: the UI layer translates
 * [AppError] into human readable, localized copy, while the technical cause stays in logs.
 */
sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Failure(val error: AppError, val cause: Throwable? = null) : AppResult<Nothing>
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(value))
    is AppResult.Failure -> this
}

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(value)
    return this
}

inline fun <T> AppResult<T>.onFailure(action: (AppError) -> Unit): AppResult<T> {
    if (this is AppResult.Failure) action(error)
    return this
}

fun <T> AppResult<T>.getOrNull(): T? = (this as? AppResult.Success)?.value
