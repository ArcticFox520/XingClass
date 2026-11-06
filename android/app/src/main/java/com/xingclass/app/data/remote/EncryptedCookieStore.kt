package com.xingclass.app.data.remote

import com.xingclass.app.security.SecureStorage
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Cookie

/** Persistence contract for the session cookie jar. Values are encrypted at rest. */
interface CookieStore {
    fun load(): List<Cookie>
    fun save(cookies: List<Cookie>)
    fun clear()
}

/**
 * Stores the Chaoxing session cookies inside [SecureStorage].
 *
 * Cookies are bearer credentials: they are never logged, never written to the course cache and
 * never leave the device. Persisting them is what makes "reopen the app without logging in"
 * work across process deaths.
 */
class EncryptedCookieStore(
    private val storage: SecureStorage,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true },
) : CookieStore {

    override fun load(): List<Cookie> {
        val raw = storage.getString(KEY_COOKIES) ?: return emptyList()
        return try {
            json.decodeFromString<List<StoredCookie>>(raw).mapNotNull { it.toCookie() }
        } catch (t: Throwable) {
            // Unreadable payload is useless; drop it instead of keeping garbage around.
            storage.remove(KEY_COOKIES)
            emptyList()
        }
    }

    override fun save(cookies: List<Cookie>) {
        if (cookies.isEmpty()) {
            storage.remove(KEY_COOKIES)
            return
        }
        val payload = json.encodeToString(cookies.map { StoredCookie.from(it) })
        storage.putString(KEY_COOKIES, payload)
    }

    override fun clear() {
        storage.remove(KEY_COOKIES)
    }

    private companion object {
        const val KEY_COOKIES = "chaoxing.cookies.v1"
    }
}

@Serializable
private data class StoredCookie(
    val name: String,
    val value: String,
    val expiresAt: Long,
    val domain: String,
    val path: String,
    val secure: Boolean,
    val httpOnly: Boolean,
    val hostOnly: Boolean,
) {
    fun toCookie(): Cookie? = runCatching {
        Cookie.Builder()
            .name(name)
            .value(value)
            .apply {
                if (hostOnly) hostOnlyDomain(domain) else domain(domain)
                path(path)
                if (expiresAt != Long.MIN_VALUE) expiresAt(expiresAt)
                if (secure) secure()
                if (httpOnly) httpOnly()
            }
            .build()
    }.getOrNull()

    companion object {
        fun from(cookie: Cookie): StoredCookie = StoredCookie(
            name = cookie.name,
            value = cookie.value,
            expiresAt = cookie.expiresAt,
            domain = cookie.domain,
            path = cookie.path,
            secure = cookie.secure,
            httpOnly = cookie.httpOnly,
            hostOnly = cookie.hostOnly,
        )
    }
}
