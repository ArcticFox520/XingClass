package com.xingclass.app.core

import android.util.Log
import com.xingclass.app.BuildConfig

/**
 * Deliberately minimal logging facade.
 *
 * SECURITY: only fixed event names and exception *types* may be logged. Never log accounts,
 * passwords, cookies, tokens, full URLs or response bodies. See docs/SECURITY.md.
 */
interface Logger {
    fun event(name: String)
    fun failure(name: String, error: Throwable? = null)
}

object LogEvents {
    const val LOGIN_REQUEST_STARTED = "LOGIN_REQUEST_STARTED"
    const val LOGIN_SUCCESS = "LOGIN_SUCCESS"
    const val LOGIN_FAILED = "LOGIN_FAILED"
    const val SESSION_CHECK_STARTED = "SESSION_CHECK_STARTED"
    const val SESSION_CHECK_VALID = "SESSION_CHECK_VALID"
    const val SESSION_CHECK_INVALID = "SESSION_CHECK_INVALID"
    const val SESSION_CHECK_UNKNOWN = "SESSION_CHECK_UNKNOWN"
    const val SESSION_CLEARED = "SESSION_CLEARED"
    const val COURSE_FETCH_STARTED = "COURSE_FETCH_STARTED"
    const val COURSE_FETCH_SUCCESS = "COURSE_FETCH_SUCCESS"
    const val COURSE_FETCH_FAILED = "COURSE_FETCH_FAILED"
    const val COURSE_CACHE_HIT = "COURSE_CACHE_HIT"
    const val COURSE_CACHE_MISS = "COURSE_CACHE_MISS"
    const val COURSE_CACHE_WRITTEN = "COURSE_CACHE_WRITTEN"
    const val COURSE_CACHE_CLEARED = "COURSE_CACHE_CLEARED"
    const val SECURE_STORAGE_FAILED = "SECURE_STORAGE_FAILED"
}

class AndroidLogger : Logger {
    override fun event(name: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, name)
    }

    override fun failure(name: String, error: Throwable?) {
        if (BuildConfig.DEBUG) {
            // Only the class name is logged. Exception messages of HTTP stacks may embed URLs
            // or response fragments and must therefore never be written out.
            Log.w(TAG, "$name: ${error?.javaClass?.simpleName ?: "unknown"}")
        }
    }

    private companion object {
        const val TAG = "XingClass"
    }
}

class NoopLogger : Logger {
    override fun event(name: String) = Unit
    override fun failure(name: String, error: Throwable?) = Unit
}
