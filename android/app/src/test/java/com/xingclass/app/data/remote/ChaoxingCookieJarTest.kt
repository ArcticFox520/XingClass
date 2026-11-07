package com.xingclass.app.data.remote

import com.xingclass.app.testutil.FakeCookieStore
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChaoxingCookieJarTest {

    private val passportUrl = "https://passport2.chaoxing.com/fanyalogin".toHttpUrl()
    private val kbUrl = "https://kb.chaoxing.com/pc/curriculum/getMyLessons".toHttpUrl()

    private fun domainCookie() = Cookie.Builder()
        .name("_uid")
        .value("abc")
        .domain("chaoxing.com")
        .path("/")
        .build()

    @Test
    fun `stores cookies and sends them to matching subdomains`() {
        val store = FakeCookieStore()
        val jar = ChaoxingCookieJar(store, now = { 1_000_000L })

        jar.saveFromResponse(passportUrl, listOf(domainCookie()))

        assertTrue(jar.hasCookies())
        assertEquals(1, jar.loadForRequest(kbUrl).size)
        assertEquals(1, store.cookies.size)
    }

    @Test
    fun `does not leak host only cookies to other hosts`() {
        val jar = ChaoxingCookieJar(FakeCookieStore(), now = { 1_000_000L })
        val hostOnly = Cookie.Builder()
            .name("passport")
            .value("1")
            .hostOnlyDomain("passport2.chaoxing.com")
            .path("/")
            .build()

        jar.saveFromResponse(passportUrl, listOf(hostOnly))

        assertEquals(1, jar.loadForRequest(passportUrl).size)
        assertEquals(0, jar.loadForRequest(kbUrl).size)
    }

    @Test
    fun `drops expired cookies`() {
        var now = 1_000_000L
        val jar = ChaoxingCookieJar(FakeCookieStore(), now = { now })
        val shortLived = Cookie.Builder()
            .name("temp")
            .value("x")
            .domain("chaoxing.com")
            .path("/")
            .expiresAt(now + 1_000)
            .build()

        jar.saveFromResponse(passportUrl, listOf(shortLived))
        assertTrue(jar.hasCookies())

        now += 5_000
        assertFalse(jar.hasCookies())
        assertEquals(0, jar.loadForRequest(passportUrl).size)
    }

    @Test
    fun `replaces cookies with the same name domain and path`() {
        val jar = ChaoxingCookieJar(FakeCookieStore(), now = { 1_000_000L })
        val first = Cookie.Builder().name("a").value("1").domain("chaoxing.com").path("/").build()
        val second = Cookie.Builder().name("a").value("2").domain("chaoxing.com").path("/").build()

        jar.saveFromResponse(passportUrl, listOf(first))
        jar.saveFromResponse(passportUrl, listOf(second))

        val cookies = jar.loadForRequest(passportUrl)
        assertEquals(1, cookies.size)
        assertEquals("2", cookies.single().value)
    }

    @Test
    fun `restores cookies from persistent storage`() {
        val store = FakeCookieStore().apply { cookies = listOf(domainCookie()) }
        val jar = ChaoxingCookieJar(store, now = { 1_000_000L })

        assertTrue(jar.hasCookies())
        assertEquals(1, jar.loadForRequest(kbUrl).size)
    }

    @Test
    fun `loads persisted cookies lazily`() {
        val store = FakeCookieStore().apply { cookies = listOf(domainCookie()) }
        val jar = ChaoxingCookieJar(store, now = { 1_000_000L })

        assertEquals(0, store.loadCalls)
        assertTrue(jar.hasCookies())
        assertEquals(1, store.loadCalls)
        assertTrue(jar.hasCookies())
        assertEquals(1, store.loadCalls)
    }

    @Test
    fun `clear removes cookies from memory and storage`() {
        val store = FakeCookieStore()
        val jar = ChaoxingCookieJar(store, now = { 1_000_000L })
        jar.saveFromResponse(passportUrl, listOf(domainCookie()))

        jar.clear()

        assertFalse(jar.hasCookies())
        assertTrue(store.cookies.isEmpty())
        assertEquals(1, store.clearCalls)
    }
}
