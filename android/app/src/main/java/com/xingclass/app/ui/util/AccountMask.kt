package com.xingclass.app.ui.util

/**
 * Masks the account before it is displayed in Settings.
 * The full account is only ever read from encrypted storage; UI shows this masked form.
 */
fun maskAccount(account: String): String {
    val trimmed = account.trim()
    return when {
        trimmed.isEmpty() -> ""
        trimmed.length <= 4 -> "***" + trimmed.takeLast(1)
        trimmed.length >= 8 -> trimmed.take(3) + "****" + trimmed.takeLast(2)
        else -> trimmed.take(1) + "***" + trimmed.takeLast(1)
    }
}
