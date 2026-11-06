package com.xingclass.app.domain.repository

import com.xingclass.app.core.AppResult
import com.xingclass.app.domain.model.CourseCacheSnapshot
import com.xingclass.app.domain.model.ThemeMode
import com.xingclass.app.domain.model.SessionStatus
import kotlinx.coroutines.flow.StateFlow

/** The single authentication entry point for the UI layer. */
interface AuthRepository {
    /** Performs a Chaoxing login and persists the resulting cookies / account locally. */
    suspend fun login(account: String, password: String): AppResult<Unit>

    /** Verifies the persisted session. Must never log or expose credentials. */
    suspend fun checkSession(): SessionStatus

    /** Account previously used on this device (encrypted at rest), for pre-filling the form. */
    fun savedAccount(): String?

    /** True when a local session cookie exists, regardless of server validation. */
    fun hasLocalSession(): Boolean

    /** User-initiated logout: clear cookies and the saved account. */
    suspend fun logout()

    /** Session rejected by the server: clear invalid cookies but keep the account hint. */
    suspend fun invalidateSession()
}

/** The only data entry point for the schedule UI. */
interface CourseRepository {
    suspend fun getCachedCourses(): CourseCacheSnapshot?

    /**
     * Remote-first refresh; keeps the cache unchanged on failure.
     *
     * @param targetWeek 1-based teaching week, or null to ask for the server's current week.
     */
    suspend fun refreshCourses(targetWeek: Int?): AppResult<CourseCacheSnapshot>

    suspend fun clearCourses()
}

interface SettingsRepository {
    val themeMode: StateFlow<ThemeMode>

    fun setThemeMode(mode: ThemeMode)
}
