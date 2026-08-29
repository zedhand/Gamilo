package com.gamilo.app.core

/**
 * Every timestamp written to the database goes through this seam so unit/instrumented tests
 * can freeze time instead of racing [System.currentTimeMillis].
 */
interface Clock {
    fun nowMillis(): Long
}

object SystemClock : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}

class FakeClock(private var millis: Long = 0L) : Clock {
    override fun nowMillis(): Long = millis
    fun set(millis: Long) { this.millis = millis }
    fun advanceBy(deltaMillis: Long) { millis += deltaMillis }
}
