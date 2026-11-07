package com.xingclass.app.domain.model

/**
 * The public obfuscation string Chaoxing's web login page uses as its AES-128 key.
 *
 * This is not a user secret: the value ships with the Chaoxing web client. It is still a setting
 * because Chaoxing can rotate the key without shipping a new app release. The published key is
 * exactly [LENGTH] characters; AES still uses its first 16 UTF-8 bytes
 * (see [com.xingclass.app.security.ChaoxingCrypto]).
 */
object ChaoxingKey {
    /** Default key published by the Chaoxing login page. */
    const val DEFAULT = "u2oh6Vu^HWe4_AES"

    /** The user-editable key must be exactly this many characters. */
    const val LENGTH = 16

    /** AES-128 needs 16 key bytes; a [LENGTH]-character key always satisfies this. */
    const val MIN_BYTES = 16

    fun normalize(raw: String): String = raw.trim()

    fun isValid(raw: String): Boolean = normalize(raw).length == LENGTH
}
