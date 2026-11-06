package com.xingclass.app.data.remote

import com.xingclass.app.core.AppResult
import com.xingclass.app.core.NoopLogger
import com.xingclass.app.data.mapper.CourseMapper
import com.xingclass.app.testutil.FakeCookieStore
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Opt-in live integration test against the real Chaoxing endpoints.
 *
 * It is skipped unless both environment variables are present, so CI never needs credentials:
 *
 * ```bash
 * export XINGCLASS_TEST_ACCOUNT=...
 * export XINGCLASS_TEST_PASSWORD=...
 * ./gradlew testDebugUnitTest --tests '*LiveChaoxingApiTest'
 * ```
 *
 * No credential, cookie, response body, course name or teacher name is printed by this test.
 * Never hard-code credentials here.
 */
class LiveChaoxingApiTest {

    @Test
    fun `real login and schedule fetch`() = runBlocking {
        val account = System.getenv("XINGCLASS_TEST_ACCOUNT")
        val password = System.getenv("XINGCLASS_TEST_PASSWORD")
        assumeTrue(
            "Set XINGCLASS_TEST_ACCOUNT / XINGCLASS_TEST_PASSWORD to run the live integration test",
            !account.isNullOrBlank() && !password.isNullOrBlank(),
        )

        val cookieJar = ChaoxingCookieJar(FakeCookieStore())
        val api = ChaoxingApiClient(
            client = OkHttpClient.Builder()
                .cookieJar(cookieJar)
                .callTimeout(40, TimeUnit.SECONDS)
                .build(),
            logger = NoopLogger(),
        )

        val login = api.login(account!!, password!!)
        assertTrue("live login failed", login is AppResult.Success)
        assertTrue("live login did not produce cookies", cookieJar.hasCookies())

        val lessons = api.getMyLessons(System.currentTimeMillis())
        assertTrue("live lesson fetch failed", lessons is AppResult.Success)
        val dtos = (lessons as AppResult.Success).value
        assertTrue("live payload contained no lessons", dtos.isNotEmpty())

        val courses = CourseMapper().toCourses(dtos)
        assertTrue("no lesson could be mapped into the grid", courses.isNotEmpty())
        // Every lesson in the verified payload must resolve to a grid slot. A mismatch means
        // the parser/mapper lost a field, which is exactly what this live test exists to catch.
        assertTrue(
            "parser mapped fewer lessons than it parsed (${courses.size}/${dtos.size})",
            courses.size == dtos.size,
        )
        assertTrue(
            "mapped course outside the 7x10 grid",
            courses.all { it.day in 0..6 && it.start in 1..10 && it.start + it.span - 1 <= 10 },
        )
    }
}
