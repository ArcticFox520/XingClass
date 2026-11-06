package com.xingclass.app.data.remote

import com.xingclass.app.core.AppError
import com.xingclass.app.core.AppResult
import com.xingclass.app.core.LogEvents
import com.xingclass.app.core.Logger
import com.xingclass.app.data.remote.dto.ChaoxingLessonParser
import com.xingclass.app.data.remote.dto.ChaoxingScheduleDto
import com.xingclass.app.data.remote.dto.LessonPayload
import com.xingclass.app.security.ChaoxingCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.Buffer
import java.io.IOException

/**
 * Direct, server-less Chaoxing client.
 *
 * The same [OkHttpClient] (and therefore the same cookie jar) is used for login, auth check and
 * lesson fetching, which is what keeps the Chaoxing session alive. Requests are executed on
 * [Dispatchers.IO] with OkHttp's own bounded timeouts; no retry loop lives in this class so a
 * broken session can never cause an endless login storm.
 *
 * SECURITY: the login body contains AES encrypted credentials and the headers contain session
 * cookies. Neither is ever logged, echoed, or included in an exception message.
 */
class ChaoxingApiClient(
    private val client: OkHttpClient,
    private val logger: Logger,
    private val endpoints: ChaoxingEndpoints = ChaoxingEndpoints(),
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true },
) : ChaoxingApi {

    override suspend fun login(account: String, password: String): AppResult<Unit> {
        logger.event(LogEvents.LOGIN_REQUEST_STARTED)

        val result = try {
            val request = baseRequest(endpoints.login)
                .post(loginForm(account, password))
                .header("Referer", "https://passport2.chaoxing.com/login?fid=&refer=")
                .build()

            withResponse(request) { response ->
                if (!response.isSuccessful) {
                    AppResult.Failure(AppError.Server)
                } else {
                    val body = response.body?.string().orEmpty()
                    val errorObject = body.toJsonObjectOrNull()
                    when (errorObject?.booleanOf("status")) {
                        true -> AppResult.Success(Unit)
                        false -> AppResult.Failure(classifyLoginFailure(errorObject))
                        null -> AppResult.Failure(AppError.LoginFailed)
                    }
                }
            }
        } catch (e: Throwable) {
            // Encryption / form building problems are not user-caused; do not leak details.
            AppResult.Failure(AppError.Unknown, e)
        }

        when (result) {
            is AppResult.Success -> logger.event(LogEvents.LOGIN_SUCCESS)
            is AppResult.Failure -> logger.failure(LogEvents.LOGIN_FAILED, result.cause)
        }
        return result
    }

    override suspend fun getAuthStatus(): AppResult<AuthStatus> {
        val request = baseRequest(endpoints.authStatus).get().build()
        return withResponse(request) { response ->
            when {
                response.code == HTTP_UNAUTHORIZED || response.code == HTTP_FORBIDDEN ->
                    AppResult.Success(AuthStatus.INVALID)

                !response.isSuccessful -> AppResult.Failure(AppError.Server)

                else -> {
                    val body = response.body?.string().orEmpty()
                    when {
                        body.isBlank() -> AppResult.Success(AuthStatus.UNKNOWN)
                        body.looksLikeHtml() -> AppResult.Success(AuthStatus.INVALID)
                        else -> AppResult.Success(classifyAuthStatus(body.toJsonObjectOrNull()))
                    }
                }
            }
        }
    }

    override suspend fun getMyLessons(
        curTimeMillis: Long,
        week: Int?,
    ): AppResult<ChaoxingScheduleDto> {
        val urlBuilder = endpoints.myLessons.newBuilder()
            .addQueryParameter("curTime", curTimeMillis.toString())
        if (week != null) {
            // Verified 2026-11-06: "week" (1-based) is the real week selector. curTime does not
            // change the returned week; it is only a timestamp/cache-buster.
            urlBuilder.addQueryParameter("week", week.toString())
        }
        val url = urlBuilder.build()
        val request = baseRequest(url)
            .get()
            .header("Referer", "https://kb.chaoxing.com/")
            .build()

        return withResponse(request) { response ->
            when {
                response.code == HTTP_UNAUTHORIZED || response.code == HTTP_FORBIDDEN ->
                    AppResult.Failure(AppError.SessionExpired)

                !response.isSuccessful -> AppResult.Failure(AppError.Server)

                else -> {
                    val body = response.body?.string().orEmpty()
                    if (body.isBlank()) return@withResponse AppResult.Failure(AppError.CourseFetch)
                    if (body.looksLikeHtml()) return@withResponse AppResult.Failure(AppError.SessionExpired)

                    val root = body.toJsonObjectOrNull()
                    val message = root?.stringOf("msg") ?: root?.stringOf("message")
                    val statusText = (root?.get("status") as? JsonPrimitive)?.contentOrNull
                    val authFailure = message.indicatesAuthFailure()

                    if (statusText != null && statusText != "200" && statusText != "true" && authFailure) {
                        return@withResponse AppResult.Failure(AppError.SessionExpired)
                    }

                    when (val payload = ChaoxingLessonParser.parse(body)) {
                        is LessonPayload.Lessons -> AppResult.Success(payload.schedule)
                        LessonPayload.InvalidPayload -> AppResult.Failure(AppError.CourseFetch)
                        LessonPayload.NoLessonContainer ->
                            if (authFailure) {
                                AppResult.Failure(AppError.SessionExpired)
                            } else {
                                AppResult.Failure(AppError.CourseFetch)
                            }
                    }
                }
            }
        }
    }

    private suspend fun <T> withResponse(
        request: Request,
        block: (Response) -> AppResult<T>,
    ): AppResult<T> = withContext(Dispatchers.IO) {
        try {
            client.newCall(request).execute().use(block)
        } catch (e: IOException) {
            AppResult.Failure(AppError.Network, e)
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown, e)
        }
    }

    /**
     * OkHttp's [FormBody] forces `application/x-www-form-urlencoded` without a charset. The
     * Chaoxing login page sends the explicit charset variant, so the encoded form is re-wrapped
     * with the exact media type.
     */
    private fun loginForm(account: String, password: String): RequestBody {
        val form = FormBody.Builder()
            .add("fid", "-1")
            .add("uname", ChaoxingCrypto.encrypt(account))
            .add("password", ChaoxingCrypto.encrypt(password))
            .add("refer", REFER)
            .add("t", "true")
            .add("forbidotherlogin", "0")
            .add("validate", "")
            .add("doubleFactorLogin", "0")
            .add("independentId", "0")
            .add("independentNameId", "0")
            .build()
        val buffer = Buffer().apply { form.writeTo(this) }
        return buffer.readByteString().toRequestBody(FORM_MEDIA_TYPE)
    }

    private fun baseRequest(url: HttpUrl): Request.Builder = Request.Builder()
        .url(url)
        .header("User-Agent", USER_AGENT)
        .header("Accept", "application/json, text/plain, */*")
        .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
        .header("X-Requested-With", "XMLHttpRequest")

    private fun classifyLoginFailure(errorObject: JsonObject?): AppError {
        val message = errorObject?.stringOf("msg2")
            ?: errorObject?.stringOf("msg")
            ?: errorObject?.stringOf("message")
            ?: ""
        return when {
            message.contains("验证码") -> AppError.LoginFailed
            message.contains("密码") ||
                message.contains("账号") ||
                message.contains("账户") ||
                message.contains("用户不存在") -> AppError.InvalidCredentials

            else -> AppError.LoginFailed
        }
    }

    private fun classifyAuthStatus(errorObject: JsonObject?): AuthStatus {
        if (errorObject == null) return AuthStatus.UNKNOWN
        return when (errorObject.booleanOf("status")) {
            true -> AuthStatus.VALID
            false -> {
                val message = errorObject.stringOf("mes")
                    ?: errorObject.stringOf("msg")
                    ?: errorObject.stringOf("message")
                if (message.indicatesAuthFailure()) AuthStatus.INVALID else AuthStatus.UNKNOWN
            }

            null -> AuthStatus.UNKNOWN
        }
    }

    private fun JsonObject.booleanOf(key: String): Boolean? =
        (this[key] as? JsonPrimitive)?.booleanOrNull

    private fun JsonObject.stringOf(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }

    private fun String.toJsonObjectOrNull(): JsonObject? =
        runCatching { json.parseToJsonElement(this) as? JsonObject }.getOrNull()

    private fun String.looksLikeHtml(): Boolean {
        val trimmed = trimStart()
        return trimmed.startsWith("<") ||
            trimmed.contains("<html", ignoreCase = true) ||
            trimmed.contains("<!doctype", ignoreCase = true)
    }

    private fun String?.indicatesAuthFailure(): Boolean {
        if (this.isNullOrBlank()) return false
        return AUTH_FAILURE_MARKERS.any { marker -> contains(marker) }
    }

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_FORBIDDEN = 403

        /** Recruiting endpoint parameter captured in the Chaoxing login flow. */
        const val REFER = "https://www.chaoxing.com/"

        val FORM_MEDIA_TYPE = "application/x-www-form-urlencoded; charset=UTF-8".toMediaType()

        /**
         * A browser-like UA is required by Chaoxing's web endpoints; they do not expose a
         * documented native API. This is a client compatibility header, not user tracking.
         */
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

        val AUTH_FAILURE_MARKERS = listOf(
            "未登录", "登录失效", "登录已失效", "请登录", "重新登录", "会话", "过期", "没有权限", "无权限",
        )
    }
}
