package com.xingclass.app.security

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Expected values were produced by an independent Python implementation of
 * AES/CBC/PKCS7 + Base64 using the same public Chaoxing key. They are not captured from the
 * network and contain no real credentials.
 */
class ChaoxingCryptoTest {

    @Test
    fun `matches independent AES CBC vectors`() {
        assertEquals("jgrbIjqqgztEEOVJia2UQA==", ChaoxingCrypto.encrypt("xingclass"))
        assertEquals("ZBjZ8C7FsVyCJ12TKWWRSQ==", ChaoxingCrypto.encrypt("13800000000"))
        assertEquals(
            "7rCi79vmnuHkz9L5cze8eg0i0+b46Mcuwdc83Q20Kpc=",
            ChaoxingCrypto.encrypt("Chaoxing-Test-2024!"),
        )
    }

    @Test
    fun `encrypts unicode and special characters`() {
        assertEquals("6Hzxr9z21jWH8a7krVur2g==", ChaoxingCrypto.encrypt("密码123"))
    }

    @Test
    fun `is deterministic`() {
        assertEquals(
            ChaoxingCrypto.encrypt("13800000000"),
            ChaoxingCrypto.encrypt("13800000000"),
        )
    }

    @Test
    fun `output is valid base64 without whitespace`() {
        val encrypted = ChaoxingCrypto.encrypt("abc")
        assertEquals(encrypted.trim(), encrypted)
        assertEquals(0, encrypted.count { it.isWhitespace() })
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects a key that is too short`() {
        ChaoxingCrypto.encrypt("abc", key = "short")
    }
}
