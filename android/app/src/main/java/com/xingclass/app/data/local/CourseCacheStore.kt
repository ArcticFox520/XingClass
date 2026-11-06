package com.xingclass.app.data.local

import com.xingclass.app.domain.model.Course
import com.xingclass.app.domain.model.CourseCacheSnapshot
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** Local schedule cache. Not sensitive: it contains course titles, rooms and teachers only. */
interface CourseCacheStore {
    suspend fun read(): CourseCacheSnapshot?
    suspend fun write(snapshot: CourseCacheSnapshot)
    suspend fun clear()
}

/**
 * JSON file cache in the app's private files directory.
 *
 * A single small file is enough for one student's schedule and keeps the app free of a database
 * dependency. Writes go through a temporary file so a process death cannot leave a half-written
 * cache behind; unreadable caches are discarded and treated as a cache miss.
 */
class FileCourseCacheStore(
    private val file: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    },
) : CourseCacheStore {

    override suspend fun read(): CourseCacheSnapshot? = withContext(ioDispatcher) {
        if (!file.isFile) return@withContext null
        try {
            val envelope = json.decodeFromString<CacheEnvelope>(file.readText())
            val validCourses = envelope.courses.filter(::isPlausible)
            CourseCacheSnapshot(courses = validCourses, savedAtMillis = envelope.savedAtMillis)
        } catch (t: Throwable) {
            runCatching { file.delete() }
            null
        }
    }

    override suspend fun write(snapshot: CourseCacheSnapshot) = withContext(ioDispatcher) {
        file.parentFile?.mkdirs()
        val payload = json.encodeToString(
            CacheEnvelope(
                version = CACHE_VERSION,
                savedAtMillis = snapshot.savedAtMillis,
                courses = snapshot.courses,
            ),
        )
        val temporary = File(file.parentFile, "${file.name}.tmp")
        temporary.writeText(payload)
        if (!temporary.renameTo(file)) {
            // Extremely defensive: rename can fail on unusual storage; fall back to copy.
            temporary.copyTo(file, overwrite = true)
            temporary.delete()
        }
        Unit
    }

    override suspend fun clear() = withContext(ioDispatcher) {
        runCatching { file.delete() }
        runCatching { File(file.parentFile, "${file.name}.tmp").delete() }
        Unit
    }

    private fun isPlausible(course: Course): Boolean =
        course.day in 0..6 &&
            course.start in 1..10 &&
            course.span >= 1 &&
            course.start + course.span - 1 <= 10 &&
            course.title.isNotBlank()

    @Serializable
    private data class CacheEnvelope(
        val version: Int,
        val savedAtMillis: Long,
        val courses: List<Course>,
    )

    private companion object {
        const val CACHE_VERSION = 1
    }
}
