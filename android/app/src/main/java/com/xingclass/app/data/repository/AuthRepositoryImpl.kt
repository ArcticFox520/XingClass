package com.xingclass.app.data.repository

import com.xingclass.app.core.AppResult
import com.xingclass.app.core.LogEvents
import com.xingclass.app.core.Logger
import com.xingclass.app.data.remote.AuthStatus
import com.xingclass.app.data.remote.ChaoxingApi
import com.xingclass.app.data.remote.ChaoxingCookieJar
import com.xingclass.app.domain.model.SessionStatus
import com.xingclass.app.domain.repository.AuthRepository
import com.xingclass.app.security.CredentialStore

/**
 * Login + session lifecycle.
 *
 * The password is never stored: after a successful login only the encrypted account hint and
 * the session cookies remain on the device.
 */
class AuthRepositoryImpl(
    private val api: ChaoxingApi,
    private val cookieJar: ChaoxingCookieJar,
    private val credentialStore: CredentialStore,
    private val logger: Logger,
) : AuthRepository {

    override suspend fun login(account: String, password: String): AppResult<Unit> {
        val normalizedAccount = account.trim()
        val result = api.login(normalizedAccount, password)
        if (result is AppResult.Success) {
            credentialStore.saveAccount(normalizedAccount)
        }
        return result
    }

    override suspend fun checkSession(): SessionStatus {
        if (!cookieJar.hasCookies()) return SessionStatus.ABSENT

        logger.event(LogEvents.SESSION_CHECK_STARTED)
        return when (val result = api.getAuthStatus()) {
            is AppResult.Success -> when (result.value) {
                AuthStatus.VALID -> {
                    logger.event(LogEvents.SESSION_CHECK_VALID)
                    SessionStatus.VALID
                }

                AuthStatus.INVALID -> {
                    logger.event(LogEvents.SESSION_CHECK_INVALID)
                    cookieJar.clear()
                    logger.event(LogEvents.SESSION_CLEARED)
                    SessionStatus.INVALID
                }

                AuthStatus.UNKNOWN -> {
                    logger.event(LogEvents.SESSION_CHECK_UNKNOWN)
                    SessionStatus.UNKNOWN
                }
            }

            is AppResult.Failure -> {
                // Network problems must not log the user out: the cached schedule is shown.
                logger.event(LogEvents.SESSION_CHECK_UNKNOWN)
                SessionStatus.UNKNOWN
            }
        }
    }

    override fun savedAccount(): String? = credentialStore.getAccount()

    override fun hasLocalSession(): Boolean = cookieJar.hasCookies()

    override suspend fun logout() {
        cookieJar.clear()
        credentialStore.clearAccount()
        logger.event(LogEvents.SESSION_CLEARED)
    }

    override suspend fun invalidateSession() {
        cookieJar.clear()
        logger.event(LogEvents.SESSION_CLEARED)
    }
}
