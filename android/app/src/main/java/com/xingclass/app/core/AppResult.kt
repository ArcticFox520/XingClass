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
