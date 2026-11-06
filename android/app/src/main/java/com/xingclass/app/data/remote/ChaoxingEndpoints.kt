package com.xingclass.app.data.remote

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

/**
 * Chaoxing hosts used by the MVP. They are injectable so unit tests can point them at a
 * MockWebServer instead of the real service.
 */
data class ChaoxingEndpoints(
    val passportBase: HttpUrl = "https://passport2.chaoxing.com/".toHttpUrl(),
    val wwwBase: HttpUrl = "https://www.chaoxing.com/".toHttpUrl(),
    val kbBase: HttpUrl = "https://kb.chaoxing.com/".toHttpUrl(),
) {
    val login: HttpUrl get() = passportBase.appendPath("fanyalogin")
    val authStatus: HttpUrl get() = passportBase.appendPath("getauthstatus/v2")
    val readCookie: HttpUrl get() = wwwBase.appendPath("entry/cookie/get")
    val myLessons: HttpUrl get() = kbBase.appendPath("pc/curriculum/getMyLessons")

    private fun HttpUrl.appendPath(path: String): HttpUrl =
        newBuilder().addPathSegments(path.trim('/')).build()
}
