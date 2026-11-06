package com.xingclass.app.data.remote

import com.xingclass.app.core.AppResult
import com.xingclass.app.data.remote.dto.ChaoxingScheduleDto

/** Low level Chaoxing HTTP contract. All methods are session-aware through the CookieJar. */
interface ChaoxingApi {
    suspend fun login(account: String, password: String): AppResult<Unit>

    suspend fun getAuthStatus(): AppResult<AuthStatus>

    /**
     * @param week 1-based teaching week. Verified 2026-11-06: the server switches the returned
     *   `lessonArray` to this week (the response's currentWeek still reports the real week).
     *   Null asks for the real current week.
     */
    suspend fun getMyLessons(curTimeMillis: Long, week: Int?): AppResult<ChaoxingScheduleDto>
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
