package com.xingclass.app.di

import android.content.Context
import com.xingclass.app.BuildConfig
import com.xingclass.app.core.AndroidLogger
import com.xingclass.app.core.AppClock
import com.xingclass.app.core.DiagnosticsLog
import com.xingclass.app.core.Logger
import com.xingclass.app.core.SystemAppClock
import com.xingclass.app.data.local.FileCourseCacheStore
import com.xingclass.app.data.mapper.CourseMapper
import com.xingclass.app.data.remote.ChaoxingApi
import com.xingclass.app.data.remote.ChaoxingApiClient
import com.xingclass.app.data.remote.ChaoxingCookieJar
import com.xingclass.app.data.remote.ChaoxingCourseRemoteDataSource
import com.xingclass.app.data.remote.CourseRemoteDataSource
import com.xingclass.app.data.remote.EncryptedCookieStore
import com.xingclass.app.data.repository.AuthRepositoryImpl
import com.xingclass.app.data.repository.CourseRepositoryImpl
import com.xingclass.app.data.repository.DiagnosticsRepositoryImpl
import com.xingclass.app.data.repository.SharedPreferencesSettingsRepository
import com.xingclass.app.domain.repository.AuthRepository
import com.xingclass.app.domain.repository.CourseRepository
import com.xingclass.app.domain.repository.DiagnosticsRepository
import com.xingclass.app.domain.repository.SettingsRepository
import com.xingclass.app.security.CredentialStore
import com.xingclass.app.security.KeystoreSecureStorage
import com.xingclass.app.security.SecureStorage
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Manual dependency graph.
 *
 * A DI framework (Hilt/Koin) would add a code generator or a runtime dependency for a graph with
 * fewer than ten objects. Manual wiring keeps builds fast and the ownership obvious.
 */
interface AppContainer {
    val logger: Logger
    val clock: AppClock
    val authRepository: AuthRepository
    val courseRepository: CourseRepository
    val settingsRepository: SettingsRepository
    val diagnosticsRepository: DiagnosticsRepository
    val appVersionName: String
}

class DefaultAppContainer(context: Context) : AppContainer {

    private val appContext = context.applicationContext

    override val clock: AppClock = SystemAppClock()

    private val diagnosticsLog = DiagnosticsLog(
        file = File(appContext.filesDir, "logs/diagnostics.log"),
        now = clock::nowMillis,
    )

    override val logger: Logger = AndroidLogger(diagnosticsLog)

    private val secureStorage: SecureStorage = KeystoreSecureStorage(appContext, logger)
    private val credentialStore = CredentialStore(secureStorage)

    private val cookieJar = ChaoxingCookieJar(EncryptedCookieStore(secureStorage))

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .followRedirects(true)
        .build()

    private val api: ChaoxingApi = ChaoxingApiClient(httpClient, logger)

    private val remote: CourseRemoteDataSource = ChaoxingCourseRemoteDataSource(
        api = api,
        mapper = CourseMapper(),
        clock = clock,
        logger = logger,
    )

    private val cacheStore = FileCourseCacheStore(
        file = File(appContext.filesDir, "course-cache.json"),
    )

    override val authRepository: AuthRepository = AuthRepositoryImpl(
        api = api,
        cookieJar = cookieJar,
        credentialStore = credentialStore,
        logger = logger,
    )

    override val courseRepository: CourseRepository = CourseRepositoryImpl(
        remote = remote,
        cache = cacheStore,
        authRepository = authRepository,
        clock = clock,
        logger = logger,
    )

    override val appVersionName: String = BuildConfig.VERSION_NAME.removeSuffix("-debug")

    override val diagnosticsRepository: DiagnosticsRepository = DiagnosticsRepositoryImpl(
        cache = cacheStore,
        diagnostics = diagnosticsLog,
        logger = logger,
        clock = clock,
        versionName = appVersionName,
    )

    override val settingsRepository: SettingsRepository =
        SharedPreferencesSettingsRepository(appContext)
}
