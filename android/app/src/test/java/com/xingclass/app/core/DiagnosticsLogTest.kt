package com.xingclass.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DiagnosticsLogTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `records and exports entries with a header`() {
        var now = 1_700_000_000_000L
        val log = DiagnosticsLog(
            file = File(temporaryFolder.root, "logs/diagnostics.log"),
            now = { now },
        )

        log.info("APP_STARTED")
        now += 1_000
        log.warn("COURSE_FETCH_FAILED: UnknownHostException")

        val text = log.exportText("XingClass test header")

        assertTrue(text.contains("XingClass test header"))
        assertTrue(text.contains("APP_STARTED"))
        assertTrue(text.contains("COURSE_FETCH_FAILED: UnknownHostException"))
        assertEquals(2, log.snapshot().size)
        assertTrue(File(temporaryFolder.root, "logs/diagnostics.log").exists())
    }

    @Test
    fun `export falls back to memory when the file is not writable`() {
        val blockedParent = File(temporaryFolder.root, "blocked")
        blockedParent.writeText("not a directory")
        val log = DiagnosticsLog(
            file = File(blockedParent, "diagnostics.log"),
            now = { 1_700_000_000_000L },
        )

        log.info("SESSION_CHECK_VALID")

        val text = log.exportText("header")
        assertTrue(text.contains("SESSION_CHECK_VALID"))
    }
}
