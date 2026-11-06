package com.xingclass.app.security

/**
 * Local account hint. The password is intentionally never persisted: it only lives in the
 * ViewModel for the duration of the login request, and the session itself is represented by
 * the cookie jar. See docs/SECURITY.md.
 */
class CredentialStore(private val storage: SecureStorage) {

    fun saveAccount(account: String) {
        storage.putString(KEY_ACCOUNT, account)
    }

    fun getAccount(): String? =
        storage.getString(KEY_ACCOUNT)?.takeIf { it.isNotBlank() }

    fun clearAccount() {
        storage.remove(KEY_ACCOUNT)
    }

    private companion object {
        const val KEY_ACCOUNT = "chaoxing.account"
    }
}
