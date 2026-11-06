package com.xingclass.app.data.remote

import com.xingclass.app.core.AppResult
import com.xingclass.app.core.NoopLogger
import com.xingclass.app.data.mapper.CourseMapper
import com.xingclass.app.domain.model.CourseMerger
import com.xingclass.app.domain.model.PeriodTimes
import com.xingclass.app.testutil.FakeCookieStore
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
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
    fun `real login, current week and week switching`() = runBlocking {
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

        val currentResult = api.getMyLessons(System.currentTimeMillis(), null)
        assertTrue("live lesson fetch failed", currentResult is AppResult.Success)
        val current = (currentResult as AppResult.Success).value
        assertTrue("live payload contained no lessons", current.lessons.isNotEmpty())
        assertTrue(
            "live payload did not include week metadata",
            current.currentWeek != null && current.maxWeek != null,
        )

        val courses = CourseMapper().toCourses(current.lessons)
        assertTrue("no lesson could be mapped into the grid", courses.isNotEmpty())
        // Every lesson in the verified payload must resolve to a grid slot. A mismatch means
        // the parser/mapper lost a field, which is exactly what this live test exists to catch.
        assertTrue(
            "parser mapped fewer lessons than it parsed (${courses.size}/${current.lessons.size})",
            courses.size == current.lessons.size,
        )
        assertTrue(
            "mapped course outside the 7x10 grid",
            courses.all { it.day in 0..6 && it.start in 1..10 && it.start + it.span - 1 <= 10 },
        )

        assertTrue("live payload did not include period times", current.lessonTimes.isNotEmpty())
        val periodTimes = PeriodTimes.parse(current.lessonTimes)
        assertTrue("no period time could be parsed", periodTimes.any { it != null })
        val merged = CourseMerger.merge(courses, periodTimes)
        assertTrue("merging removed every course", merged.isNotEmpty())
        assertTrue("merging created more courses", merged.size <= courses.size)
        assertTrue(
            "merged course outside the 7x10 grid",
            merged.all { it.day in 0..6 && it.start in 1..10 && it.start + it.span - 1 <= 10 },
        )

        val realCurrentWeek = current.currentWeek!!
        val targetWeek = realCurrentWeek - 1
        if (targetWeek >= 1) {
            val switchedResult = api.getMyLessons(System.currentTimeMillis(), targetWeek)
            assertTrue("live week switch failed", switchedResult is AppResult.Success)
            val switched = (switchedResult as AppResult.Success).value

            // The server always reports the real current week in currentWeek, even when the
            // requested `week` is another one. The lesson `weeks` field is the proof that the
            // returned lessonArray belongs to the requested week.
            assertEquals(realCurrentWeek, switched.currentWeek)
            assertTrue("switched week returned no lessons", switched.lessons.isNotEmpty())
            assertTrue(
                "switched week payload does not belong to week $targetWeek",
                switched.lessons.any { lesson -> lesson.weeks.containsWeek(targetWeek) },
            )
            val switchedCourses = CourseMapper().toCourses(switched.lessons)
            assertEquals(switched.lessons.size, switchedCourses.size)
        }
    }

    private fun String?.containsWeek(week: Int): Boolean =
        this?.split(',', '，', ' ')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.any { it == week.toString() } == true
}
