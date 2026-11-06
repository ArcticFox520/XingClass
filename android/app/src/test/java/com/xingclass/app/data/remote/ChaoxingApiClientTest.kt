package com.xingclass.app.data.remote

import com.xingclass.app.core.AppError
import com.xingclass.app.core.AppResult
import com.xingclass.app.core.NoopLogger
import com.xingclass.app.security.ChaoxingCrypto
import com.xingclass.app.testutil.FakeCookieStore
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.URLDecoder

class ChaoxingApiClientTest {

    private lateinit var server: MockWebServer
    private lateinit var cookieJar: ChaoxingCookieJar
    private lateinit var client: ChaoxingApiClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val endpoints = ChaoxingEndpoints(
            passportBase = server.url("/"),
            wwwBase = server.url("/"),
            kbBase = server.url("/"),
        )
        cookieJar = ChaoxingCookieJar(FakeCookieStore(), now = { 1_000_000L })
        val httpClient = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .build()
        client = ChaoxingApiClient(httpClient, NoopLogger(), endpoints)
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
    }

    private fun json(body: String): MockResponse = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json; charset=utf-8")
        .setBody(body)

    private fun formValue(body: String, key: String): String? {
        val pair = body.split("&").firstOrNull { it.startsWith("$key=") } ?: return null
        return URLDecoder.decode(pair.substringAfter('='), "UTF-8")
    }

    @Test
    fun `login sends encrypted credentials and the documented form fields`() = runTest {
        server.enqueue(json("""{"status":true,"url":"https://www.chaoxing.com/"}"""))

        val result = client.login("13800000000", "secret-password")

        assertTrue(result is AppResult.Success)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/fanyalogin", request.path)
        assertEquals(
            "application/x-www-form-urlencoded; charset=UTF-8",
            request.headers["Content-Type"],
        )

        val body = request.body.readUtf8()
        assertEquals("-1", formValue(body, "fid"))
        assertEquals(ChaoxingCrypto.encrypt("13800000000"), formValue(body, "uname"))
        assertEquals(ChaoxingCrypto.encrypt("secret-password"), formValue(body, "password"))
        assertEquals("true", formValue(body, "t"))
        assertEquals("0", formValue(body, "forbidotherlogin"))
        assertEquals("0", formValue(body, "doubleFactorLogin"))
        assertEquals("0", formValue(body, "independentId"))
        assertEquals("0", formValue(body, "independentNameId"))
        assertFalse(body.contains("secret-password"))
        assertFalse(body.contains("13800000000"))
    }

    @Test
    fun `login failure maps password errors to invalid credentials`() = runTest {
        server.enqueue(json("""{"msg2":"密码错误","status":false}"""))

        val result = client.login("13800000000", "wrong")

        assertEquals(AppError.InvalidCredentials, (result as AppResult.Failure).error)
    }

    @Test
    fun `generic login failure maps to login failed`() = runTest {
        server.enqueue(json("""{"msg2":"参数为空","status":false}"""))

        val result = client.login("", "")

        assertEquals(AppError.LoginFailed, (result as AppResult.Failure).error)
    }

    @Test
    fun `auth status true is valid`() = runTest {
        server.enqueue(json("""{"status":true}"""))

        val result = client.getAuthStatus()

        assertEquals(AuthStatus.VALID, (result as AppResult.Success).value)
    }

    @Test
    fun `auth status parameter empty is unknown rather than invalid`() = runTest {
        server.enqueue(json("""{"mes":"参数为空","type":"1","status":false}"""))

        val result = client.getAuthStatus()

        assertEquals(AuthStatus.UNKNOWN, (result as AppResult.Success).value)
    }

    @Test
    fun `auth status explains an expired session`() = runTest {
        server.enqueue(json("""{"mes":"未登录","status":false}"""))

        val result = client.getAuthStatus()

        assertEquals(AuthStatus.INVALID, (result as AppResult.Success).value)
    }

    @Test
    fun `lessons request sends curTime and parses the payload`() = runTest {
        server.enqueue(
            json(
                """{"status":200,"data":[{"id":"1","name":"高等数学","teacher":"张老师",
                   "time":"周一第1-2节","location":"A101"}]}""",
            ),
        )

        val result = client.getMyLessons(1793946884693L, null)

        val lessons = (result as AppResult.Success).value.lessons
        assertEquals(1, lessons.size)
        assertEquals("高等数学", lessons.single().title)

        val request = server.takeRequest()
        assertEquals("/pc/curriculum/getMyLessons?curTime=1793946884693", request.path)
    }

    @Test
    fun `week query parameter selects the teaching week and metadata is parsed`() = runTest {
        server.enqueue(
            json(
                """{"data":{"curriculum":{"currentWeek":10,"realCurrentWeek":10,"maxWeek":22},
                   "lessonArray":[{"name":"示例课程","dayOfWeek":1,"beginNumber":1,"length":2}]}}""",
            ),
        )

        val result = client.getMyLessons(123L, 9)

        val schedule = (result as AppResult.Success).value
        assertEquals(1, schedule.lessons.size)
        assertEquals(10, schedule.currentWeek)
        assertEquals(10, schedule.realCurrentWeek)
        assertEquals(22, schedule.maxWeek)

        val request = server.takeRequest()
        assertEquals("/pc/curriculum/getMyLessons?curTime=123&week=9", request.path)
    }

    @Test
    fun `login page html is treated as an expired session`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/html")
                .setBody("<!DOCTYPE html><html><body>请登录</body></html>"),
        )

        val result = client.getMyLessons(1L, null)

        assertEquals(AppError.SessionExpired, (result as AppResult.Failure).error)
    }

    @Test
    fun `http 401 is treated as an expired session`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))

        val result = client.getMyLessons(1L, null)

        assertEquals(AppError.SessionExpired, (result as AppResult.Failure).error)
    }

    @Test
    fun `no lesson container is a course fetch failure`() = runTest {
        server.enqueue(json("""{"status":200,"data":{"unexpected":true}}"""))

        val result = client.getMyLessons(1L, null)

        assertEquals(AppError.CourseFetch, (result as AppResult.Failure).error)
    }

    @Test
    fun `session cookies from login are replayed on the next request`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Set-Cookie", "session=abc123; Path=/")
                .setHeader("Content-Type", "application/json")
                .setBody("""{"status":true}"""),
        )

        val login = client.login("13800000000", "secret")
        assertTrue(login is AppResult.Success)
        assertTrue(cookieJar.hasCookies())

        server.enqueue(json("""{"status":200,"data":[]}"""))
        client.getMyLessons(1L, null)

        server.takeRequest() // login
        val lessonsRequest = server.takeRequest()
        val cookieHeader = lessonsRequest.headers["Cookie"]
        assertNotNull(cookieHeader)
        assertTrue(cookieHeader!!.contains("session=abc123"))
    }

    @Test
    fun `unreachable host maps to a network error`() = runTest {
        val endpoints = ChaoxingEndpoints(
            passportBase = "http://127.0.0.1:1/".toHttpUrl(),
            wwwBase = "http://127.0.0.1:1/".toHttpUrl(),
            kbBase = "http://127.0.0.1:1/".toHttpUrl(),
        )
        val offlineClient = ChaoxingApiClient(
            client = OkHttpClient(),
            logger = NoopLogger(),
            endpoints = endpoints,
        )

        val result = offlineClient.login("a", "b")

        assertEquals(AppError.Network, (result as AppResult.Failure).error)
    }
}
