package com.xingclass.app.data.repository

import com.xingclass.app.core.AppClock
import com.xingclass.app.core.AppError
import com.xingclass.app.core.AppResult
import com.xingclass.app.core.DiagnosticsLog
import com.xingclass.app.core.LogEvents
import com.xingclass.app.core.Logger
import com.xingclass.app.data.local.CourseCacheStore
import com.xingclass.app.domain.model.Course
import com.xingclass.app.domain.model.ExportDocument
import com.xingclass.app.domain.repository.DiagnosticsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Exports the persistent diagnostics log and the locally cached schedule.
 *
 * The course export is the app's parsed course response data (week metadata + courses + period
 * times), not a raw HTTP dump; raw bodies may contain identity fields and are intentionally not
 * retained. Cookies/credentials never enter either export.
 */
class DiagnosticsRepositoryImpl(
    private val cache: CourseCacheStore,
    private val diagnostics: DiagnosticsLog,
    private val logger: Logger,
    private val clock: AppClock,
    private val versionName: String,
) : DiagnosticsRepository {

    override suspend fun exportLogs(): AppResult<ExportDocument> = withContext(Dispatchers.IO) {
        logger.event(LogEvents.EXPORT_LOGS)
        val now = clock.now()
        val header = buildString {
            appendLine("XingClass 运行日志")
            appendLine("version: $versionName")
            append(
                "exportedAt: " +
                    now.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            )
        }
        AppResult.Success(
            ExportDocument(
                fileName = "xingclass-log-${now.format(FILE_TIME)}.txt",
                mimeType = "text/plain",
                content = diagnostics.exportText(header),
            ),
        )
    }

    override suspend fun exportCourses(): AppResult<ExportDocument> = withContext(Dispatchers.IO) {
        val snapshot = cache.read()
        if (snapshot == null) {
            logger.event(LogEvents.EXPORT_FAILED)
            return@withContext AppResult.Failure(AppError.NoData)
        }

        logger.event(LogEvents.EXPORT_COURSES)
        val now = clock.now()
        val payload = CourseExportFile(
            app = "XingClass",
            version = versionName,
            exportedAt = now.atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            week = snapshot.week,
            realCurrentWeek = snapshot.realCurrentWeek,
            maxWeek = snapshot.maxWeek,
            savedAtMillis = snapshot.savedAtMillis,
            lessonTimes = snapshot.lessonTimes,
            courses = snapshot.courses,
        )
        AppResult.Success(
            ExportDocument(
                fileName = "xingclass-courses-${now.format(FILE_TIME)}.json",
                mimeType = "application/json",
                content = EXPORT_JSON.encodeToString(payload),
            ),
        )
    }

    @Serializable
    private data class CourseExportFile(
        val app: String,
        val version: String,
        val exportedAt: String,
        val week: Int,
        val realCurrentWeek: Int,
        val maxWeek: Int,
        val savedAtMillis: Long,
        val lessonTimes: List<String>,
        val courses: List<Course>,
    )

    private companion object {
        val FILE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
        val EXPORT_JSON: Json = Json {
            prettyPrint = true
            encodeDefaults = true
        }
    }
}
