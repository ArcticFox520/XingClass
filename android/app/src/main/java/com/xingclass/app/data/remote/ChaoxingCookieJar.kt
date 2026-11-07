package com.xingclass.app.data.remote

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * Session-scoped cookie jar.
 *
 * Every request made through the shared OkHttpClient (login, auth check, lessons) automatically
 * carries the same Chaoxing session. Cookies are matched by domain/path using OkHttp's own
 * rules, so a cookie for passport2.chaoxing.com is never sent to kb.chaoxing.com unless the
 * server scoped it to .chaoxing.com.
 *
 * NOTE: this class never logs cookie names or values.
 */
class ChaoxingCookieJar(
    private val store: CookieStore,
    private val now: () -> Long = System::currentTimeMillis,
) : CookieJar {

    private val lock = Any()
    private val cookies = mutableListOf<Cookie>()
    private var loaded = false

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return
        synchronized(lock) {
            ensureLoadedLocked()
            // Refresh both Deletion and update: last write wins per (domain, path, name).
            cookies.forEach { incoming ->
                this.cookies.removeAll {
                    it.name == incoming.name &&
                        it.domain == incoming.domain &&
                        it.path == incoming.path
                }
                val expired = incoming.expiresAt != Long.MIN_VALUE && incoming.expiresAt <= now()
                if (!expired) this.cookies.add(incoming)
            }
            pruneExpiredLocked()
            persistLocked()
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        synchronized(lock) {
            ensureLoadedLocked()
            pruneExpiredLocked()
            return cookies.filter { it.matches(url) }
        }
    }

    fun hasCookies(): Boolean = synchronized(lock) {
        ensureLoadedLocked()
        pruneExpiredLocked()
        cookies.isNotEmpty()
    }

    fun clear() {
        synchronized(lock) {
            loaded = true
            cookies.clear()
            store.clear()
        }
    }

    private fun ensureLoadedLocked() {
        if (loaded) return
        cookies.addAll(store.load())
        loaded = true
    }

    private fun pruneExpiredLocked() {
        val current = now()
        cookies.removeAll { it.expiresAt != Long.MIN_VALUE && it.expiresAt <= current }
    }

    private fun persistLocked() {
        store.save(cookies.toList())
    }
}
