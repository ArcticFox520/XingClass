package com.xingclass.app.security

import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Chaoxing's web login encrypts the account and password in the browser before submitting.
 *
 * Algorithm (reverse engineered from Chaoxing's login JavaScript, see docs/API.md):
 *  - AES / CBC / PKCS#7 padding
 *  - 128-bit key, the first 16 bytes of the published obfuscation string
 *  - IV = the same 16 bytes
 *  - output is standard Base64 with padding
 *
 * This key is a public constant of the Chaoxing web client, not a user secret. It is kept in
 * exactly one place so the login ViewModel/ApiClient never deal with crypto primitives.
 */
object ChaoxingCrypto {

    /** Public obfuscation string used by the Chaoxing login page. */
    const val KEY = "u2oh6Vu^HWe4_AES"

    /**
     * JCE names PKCS#5 padding, but for a 16-byte block cipher it is identical to PKCS#7.
     * Verified against an independent implementation in ChaoxingCryptoTest.
     */
    private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
    private const val KEY_LENGTH_BYTES = 16

    fun encrypt(plainText: String, key: String = KEY): String {
        val keyBytes = key.toByteArray(StandardCharsets.UTF_8)
        require(keyBytes.size >= KEY_LENGTH_BYTES) {
            "Chaoxing AES key must be at least $KEY_LENGTH_BYTES bytes"
        }
        val secret = SecretKeySpec(keyBytes.copyOfRange(0, KEY_LENGTH_BYTES), "AES")
        val iv = IvParameterSpec(keyBytes.copyOfRange(0, KEY_LENGTH_BYTES))
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secret, iv)
        val cipherText = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
        return Base64.getEncoder().encodeToString(cipherText)
    }
}
