package com.xingclass.app.data.repository

import com.xingclass.app.core.AppError
import com.xingclass.app.core.AppResult
import com.xingclass.app.core.NoopLogger
import com.xingclass.app.data.remote.AuthStatus
import com.xingclass.app.data.remote.ChaoxingCookieJar
import com.xingclass.app.domain.model.SessionStatus
import com.xingclass.app.security.CredentialStore
import com.xingclass.app.security.InMemorySecureStorage
import com.xingclass.app.testutil.FakeChaoxingApi
import com.xingclass.app.testutil.FakeCookieStore
import kotlinx.coroutines.test.runTest
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryImplTest {

    private val loginUrl = "https://passport2.chaoxing.com/fanyalogin".toHttpUrl()
    private val cookieStore = FakeCookieStore()
    private val api = FakeChaoxingApi()
    private val credentialStore = CredentialStore(InMemorySecureStorage())
    private val cookieJar = ChaoxingCookieJar(cookieStore, now = { 1_000_000L })

    private val repository = AuthRepositoryImpl(
        api = api,
        cookieJar = cookieJar,
        credentialStore = credentialStore,
        logger = NoopLogger(),
    )

    private fun seedCookie() {
        val cookie = Cookie.Builder()
            .name("_uid")
            .value("abc")
            .domain("chaoxing.com")
            .path("/")
            .build()
        cookieJar.saveFromResponse(loginUrl, listOf(cookie))
    }

    @Test
    fun `successful login stores the trimmed account but not the password`() = runTest {
        api.loginResult = AppResult.Success(Unit)

        val result = repository.login("  13800000000  ", "secret-password")

        assertTrue(result is AppResult.Success)
        assertEquals("13800000000", api.lastAccount)
        assertEquals("secret-password", api.lastPassword)
        assertEquals("13800000000", repository.savedAccount())
        assertEquals("13800000000", credentialStore.getAccount())
    }

    @Test
    fun `failed login does not store the account`() = runTest {
        api.loginResult = AppResult.Failure(AppError.InvalidCredentials)

        repository.login("13800000000", "wrong")

        assertNull(repository.savedAccount())
        assertNull(credentialStore.getAccount())
    }

    @Test
    fun `checkSession without cookies is absent and performs no request`() = runTest {
        assertEquals(SessionStatus.ABSENT, repository.checkSession())
        assertEquals(0, api.authStatusCalls)
    }

    @Test
    fun `valid auth status reports a valid session`() = runTest {
        seedCookie()
        api.authStatusResult = AppResult.Success(AuthStatus.VALID)

        assertEquals(SessionStatus.VALID, repository.checkSession())
        assertTrue(repository.hasLocalSession())
    }

    @Test
    fun `invalid auth status clears the session`() = runTest {
        seedCookie()
        api.authStatusResult = AppResult.Success(AuthStatus.INVALID)

        assertEquals(SessionStatus.INVALID, repository.checkSession())
        assertFalse(repository.hasLocalSession())
        assertTrue(cookieStore.cookies.isEmpty())
    }

    @Test
    fun `unparseable auth status stays unknown`() = runTest {
        seedCookie()
        api.authStatusResult = AppResult.Success(AuthStatus.UNKNOWN)

        assertEquals(SessionStatus.UNKNOWN, repository.checkSession())
        assertTrue(repository.hasLocalSession())
    }

    @Test
    fun `network failure during session check stays unknown and keeps cookies`() = runTest {
        seedCookie()
        api.authStatusResult = AppResult.Failure(AppError.Network)

        assertEquals(SessionStatus.UNKNOWN, repository.checkSession())
        assertTrue(repository.hasLocalSession())
    }

    @Test
    fun `invalidateSession clears cookies but keeps the account hint`() = runTest {
        seedCookie()
        credentialStore.saveAccount("13800000000")

        repository.invalidateSession()

        assertFalse(repository.hasLocalSession())
        assertEquals("13800000000", repository.savedAccount())
    }

    @Test
    fun `logout clears cookies and the account`() = runTest {
        seedCookie()
        credentialStore.saveAccount("13800000000")

        repository.logout()

        assertFalse(repository.hasLocalSession())
        assertNull(repository.savedAccount())
        assertEquals(1, cookieStore.clearCalls)
    }
}
