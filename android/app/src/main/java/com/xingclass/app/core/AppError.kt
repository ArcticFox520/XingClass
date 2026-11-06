package com.xingclass.app.core

/**
 * User-visible failure categories. Raw exceptions (IOException, JSONException, HTTP 500 …)
 * never reach the UI; they are mapped to one of these values first.
 */
sealed interface AppError {
    /** No usable network connection. The cached schedule must be kept. */
    data object Network : AppError

    /** The account / password pair was rejected by Chaoxing. */
    data object InvalidCredentials : AppError

    /** Any other login failure (server error, captcha, account locked …). */
    data object LoginFailed : AppError

    /** Cookie/session is no longer accepted; the user must log in again. */
    data object SessionExpired : AppError

    /** Logged in, but the lesson payload could not be obtained or understood. */
    data object CourseFetch : AppError

    /** Unexpected HTTP status. */
    data object Server : AppError

    data object Unknown : AppError
}
