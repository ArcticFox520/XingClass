package com.xingclass.app.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

class AccountMaskTest {

    @Test
    fun `masks a phone number`() {
        assertEquals("138****00", maskAccount("13800000000"))
    }

    @Test
    fun `masks an alphanumeric account`() {
        assertEquals("stu****23", maskAccount("student123"))
    }

    @Test
    fun `masks short accounts completely`() {
        assertEquals("1***5", maskAccount("12345"))
        assertEquals("***1", maskAccount("1"))
    }

    @Test
    fun `handles blank input`() {
        assertEquals("", maskAccount("   "))
    }
}
