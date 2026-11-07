package com.xingclass.app.domain.model

import com.xingclass.app.security.ChaoxingCrypto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChaoxingKeyTest {

    @Test
    fun `default is the published Chaoxing key and is valid`() {
        assertEquals("u2oh6Vu^HWe4_AES", ChaoxingKey.DEFAULT)
        assertEquals(ChaoxingKey.LENGTH, ChaoxingKey.DEFAULT.length)
        assertTrue(ChaoxingKey.isValid(ChaoxingKey.DEFAULT))
    }

    @Test
    fun `crypto uses the same default`() {
        assertEquals(ChaoxingKey.DEFAULT, ChaoxingCrypto.KEY)
    }

    @Test
    fun `normalize trims surrounding whitespace`() {
        assertEquals("0123456789abcdef", ChaoxingKey.normalize("  0123456789abcdef \n"))
    }

    @Test
    fun `requires exactly sixteen characters`() {
        assertFalse(ChaoxingKey.isValid("0123456789abcde")) // 15
        assertTrue(ChaoxingKey.isValid("0123456789abcdef")) // 16
        assertFalse(ChaoxingKey.isValid("0123456789abcdefg")) // 17
        assertFalse(ChaoxingKey.isValid("  0123456789abcde  "))
        assertTrue(ChaoxingKey.isValid("  0123456789abcdef  "))
    }

    @Test
    fun `empty key is invalid`() {
        assertFalse(ChaoxingKey.isValid(""))
        assertFalse(ChaoxingKey.isValid("   "))
    }
}
