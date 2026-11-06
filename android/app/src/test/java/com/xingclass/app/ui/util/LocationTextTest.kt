package com.xingclass.app.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationTextTest {

    @Test
    fun `splits a building and an alphanumeric room`() {
        val parts = parseLocation("示例楼C201")
        assertEquals("示例楼", parts.building)
        assertEquals("C201", parts.room)
        assertTrue(parts.hasSplit)
        assertEquals("示例楼C201", parts.raw)
    }

    @Test
    fun `splits a building and a numeric room`() {
        val parts = parseLocation("综合楼302")
        assertEquals("综合楼", parts.building)
        assertEquals("302", parts.room)
    }

    @Test
    fun `splits a room code with a hyphen`() {
        val parts = parseLocation("综合楼A-101")
        assertEquals("综合楼", parts.building)
        assertEquals("A-101", parts.room)
    }

    @Test
    fun `splits a building with a trailing space`() {
        val parts = parseLocation("图书馆 302")
        assertEquals("图书馆", parts.building)
        assertEquals("302", parts.room)
    }

    @Test
    fun `keeps a room only location`() {
        val parts = parseLocation("A101")
        assertNull(parts.building)
        assertEquals("A101", parts.room)
        assertFalse(parts.hasSplit)
    }

    @Test
    fun `keeps a non numeric location as one line`() {
        val parts = parseLocation("线上")
        assertEquals("线上", parts.building)
        assertNull(parts.room)
        assertTrue(parts.hasLocation)
    }

    @Test
    fun `does not split a building name ending in a digit and a unit`() {
        val parts = parseLocation("3号楼")
        assertEquals("3号楼", parts.building)
        assertNull(parts.room)
    }

    @Test
    fun `trims a leading hyphen from a room`() {
        val parts = parseLocation("示例楼-101")
        assertEquals("示例楼", parts.building)
        assertEquals("101", parts.room)
    }

    @Test
    fun `blank location has no parts`() {
        val parts = parseLocation("   ")
        assertNull(parts.building)
        assertNull(parts.room)
        assertNull(parts.raw)
        assertFalse(parts.hasLocation)
    }
}
