package com.gamilo.app.core

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * All monetary fields are stored as [BigDecimal] (Room persists them as TEXT — see
 * data/Converters.kt) and every arithmetic step here goes through [BigDecimal] too.
 * [Double]/[Float] must never touch a money value anywhere in this codebase: binary
 * floating point can't represent 0.10 exactly and drifts under repeated addition, which
 * is unacceptable for figures that end up on a tax filing.
 */
object Money {
    private const val SCALE = 2

    fun scale(amount: BigDecimal): BigDecimal = amount.setScale(SCALE, RoundingMode.HALF_UP)

    /** Applies an FX rate (CAD-per-foreign-unit) to a foreign-currency amount, returning CAD. */
    fun convertToCad(amount: BigDecimal, fxRateApplied: BigDecimal): BigDecimal =
        scale(amount.multiply(fxRateApplied))

    fun sum(amounts: List<BigDecimal>): BigDecimal =
        amounts.fold(BigDecimal.ZERO) { acc, v -> acc.add(v) }

    fun formatMajor(amount: BigDecimal, currencyCode: String): String =
        "$currencyCode ${scale(amount).toPlainString()}"
}
