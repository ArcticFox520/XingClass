package com.xingclass.app.core

import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class DiagnosticsEntry(
    val timestampMillis: Long,
    val level: String,
    val message: String,
)

/**
 * Persistent, bounded diagnostics log.
 *
 * SECURITY: callers may only pass fixed event names and exception class names. Passwords,
 * cookies, tokens, accounts and response bodies must never be written here. The file lives in
 * the app's private storage and is only read when the user explicitly exports it.
 */
class DiagnosticsLog(
    private val file: File,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val lock = Any()
    private val recent = ArrayDeque<DiagnosticsEntry>()

    fun info(message: String) = record("INFO", message)

    fun warn(message: String) = record("WARN", message)

    fun snapshot(): List<DiagnosticsEntry> = synchronized(lock) { recent.toList() }

    fun exportText(header: String): String = synchronized(lock) {
        val persisted = runCatching {
            if (file.isFile) file.readText() else ""
        }.getOrDefault("")
        val body = persisted.ifBlank { recent.joinToString("\n") { format(it) } }
        buildString {
            appendLine(header.trimEnd())
            appendLine("----------------------------------------")
            if (body.isBlank()) {
                appendLine("(no log entries)")
            } else {
                append(body)
                if (!body.endsWith("\n")) appendLine()
            }
        }
    }

    private fun record(level: String, message: String) {
        val entry = DiagnosticsEntry(
            timestampMillis = now(),
            level = level,
            message = message.take(MAX_MESSAGE_LENGTH),
        )
        synchronized(lock) {
            recent.addLast(entry)
            while (recent.size > MAX_MEMORY_ENTRIES) recent.removeFirst()
            runCatching { appendToFile(entry) }
        }
    }

    private fun appendToFile(entry: DiagnosticsEntry) {
        file.parentFile?.mkdirs()
        if (file.isFile && file.length() > MAX_FILE_BYTES) {
            // Keep the recent half; diagnostics do not need an unbounded history.
            val tail = file.readText().takeLast((MAX_FILE_BYTES / 2).toInt())
            file.writeText(tail)
        }
        file.appendText(format(entry) + "\n")
    }

    private fun format(entry: DiagnosticsEntry): String {
        val time = Instant.ofEpochMilli(entry.timestampMillis)
            .atZone(ZoneId.systemDefault())
            .format(FORMATTER)
        return "$time ${entry.level.padEnd(5)} ${entry.message}"
    }

    private companion object {
        const val MAX_MEMORY_ENTRIES = 500
        const val MAX_FILE_BYTES = 256 * 1024L
        const val MAX_MESSAGE_LENGTH = 300
        val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    }
}
