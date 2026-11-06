package com.xingclass.app.domain.repository

import com.xingclass.app.core.AppResult
import com.xingclass.app.domain.model.ExportDocument

/**
 * Builds user-initiated diagnostic exports. No credential, cookie or token is ever included.
 * Course data is only read from the local cache and only when the user taps export.
 */
interface DiagnosticsRepository {
    suspend fun exportLogs(): AppResult<ExportDocument>

    suspend fun exportCourses(): AppResult<ExportDocument>
}
