package com.xingclass.app.testutil

import com.xingclass.app.core.AppClock
import com.xingclass.app.core.AppResult
import com.xingclass.app.data.local.CourseCacheStore
import com.xingclass.app.data.remote.AuthStatus
import com.xingclass.app.data.remote.ChaoxingApi
import com.xingclass.app.data.remote.CookieStore
import com.xingclass.app.data.remote.CourseRemoteDataSource
import com.xingclass.app.data.remote.dto.ChaoxingScheduleDto
import com.xingclass.app.domain.model.Course
import com.xingclass.app.domain.model.CourseCacheSnapshot
import com.xingclass.app.domain.model.CourseWeek
import com.xingclass.app.domain.model.SessionStatus
import com.xingclass.app.domain.repository.AuthRepository
import okhttp3.Cookie
import java.time.LocalDateTime
import java.time.ZoneId

class FixedAppClock(var current: LocalDateTime) : AppClock {
    override fun now(): LocalDateTime = current

    override fun nowMillis(): Long =
        current.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun advanceMinutes(minutes: Long) {
        current = current.plusMinutes(minutes)
    }
}

class FakeCourseRemote(
    var result: AppResult<CourseWeek>,
) : CourseRemoteDataSource {
    var calls = 0
    var lastWeek: Int? = null

    override suspend fun fetchCourses(week: Int?): AppResult<CourseWeek> {
        calls++
        lastWeek = week
        return result
    }
}

class FakeCourseCache(
    var snapshot: CourseCacheSnapshot? = null,
) : CourseCacheStore {
    var writes = 0
    var clears = 0

    override suspend fun read(): CourseCacheSnapshot? = snapshot

    override suspend fun write(snapshot: CourseCacheSnapshot) {
        this.snapshot = snapshot
        writes++
    }

    override suspend fun clear() {
        snapshot = null
        clears++
    }
}

class FakeAuthRepository : AuthRepository {
    var loginResult: AppResult<Unit> = AppResult.Success(Unit)
    var sessionStatus: SessionStatus = SessionStatus.ABSENT
    var hasSession = false
    var accountHint: String? = null
    var loginCalls = 0
    var logoutCalls = 0
    var invalidateCalls = 0

    override suspend fun login(account: String, password: String): AppResult<Unit> {
        loginCalls++
        return loginResult
    }

    override suspend fun checkSession(): SessionStatus = sessionStatus

    override fun savedAccount(): String? = accountHint

    override fun hasLocalSession(): Boolean = hasSession

    override suspend fun logout() {
        logoutCalls++
        accountHint = null
        hasSession = false
    }

    override suspend fun invalidateSession() {
        invalidateCalls++
        hasSession = false
    }
}

class FakeChaoxingApi : ChaoxingApi {
    var loginResult: AppResult<Unit> = AppResult.Success(Unit)
    var authStatusResult: AppResult<AuthStatus> = AppResult.Success(AuthStatus.VALID)
    var scheduleResult: AppResult<ChaoxingScheduleDto> =
        AppResult.Success(ChaoxingScheduleDto(emptyList()))
    var lastAccount: String? = null
    var lastPassword: String? = null
    var lastWeek: Int? = null
    var authStatusCalls = 0

    override suspend fun login(account: String, password: String): AppResult<Unit> {
        lastAccount = account
        lastPassword = password
        return loginResult
    }

    override suspend fun getAuthStatus(): AppResult<AuthStatus> {
        authStatusCalls++
        return authStatusResult
    }

    override suspend fun getMyLessons(
        curTimeMillis: Long,
        week: Int?,
    ): AppResult<ChaoxingScheduleDto> {
        lastWeek = week
        return scheduleResult
    }
}

class FakeCookieStore : CookieStore {
    var cookies: List<Cookie> = emptyList()
    var clearCalls = 0

    override fun load(): List<Cookie> = cookies

    override fun save(cookies: List<Cookie>) {
        this.cookies = cookies
    }

    override fun clear() {
        clearCalls++
        cookies = emptyList()
    }
}
