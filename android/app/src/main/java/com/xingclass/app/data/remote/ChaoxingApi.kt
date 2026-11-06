package com.xingclass.app.data.remote

import com.xingclass.app.core.AppResult
import com.xingclass.app.data.remote.dto.ChaoxingLessonDto

/** Low level Chaoxing HTTP contract. All methods are session-aware through the CookieJar. */
interface ChaoxingApi {
    suspend fun login(account: String, password: String): AppResult<Unit>

    suspend fun getAuthStatus(): AppResult<AuthStatus>

    suspend fun getMyLessons(curTimeMillis: Long): AppResult<List<ChaoxingLessonDto>>
}

/**
 * Interpretation of GET /getauthstatus/v2.
 *
 * The captured failure response ("参数为空") does not prove that a cookie is invalid, so the
 * client distinguishes VALID / INVALID / UNKNOWN instead of treating every non-true answer as a
 * logout. UNKNOWN keeps the user on the schedule and lets the lesson request decide.
 */
enum class AuthStatus {
    VALID,
    INVALID,
    UNKNOWN,
}
