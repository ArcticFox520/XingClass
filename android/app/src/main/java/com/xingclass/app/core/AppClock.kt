package com.xingclass.app.core

import java.time.LocalDateTime

/** Injectable time source so auto-theme and cache timestamps are testable. */
interface AppClock {
    fun now(): LocalDateTime
    fun nowMillis(): Long
}

class SystemAppClock : AppClock {
    override fun now(): LocalDateTime = LocalDateTime.now()
    override fun nowMillis(): Long = System.currentTimeMillis()
}
