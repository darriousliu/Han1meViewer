package com.yenaly.han1meviewer.platform

import kotlin.math.absoluteValue
import kotlin.math.roundToLong

/** Platform boundary for invariant and user-locale numeric text used by shared UI. */
expect object NumberFormatter {
    fun formatFixed(value: Double, fractionDigits: Int): String

    fun formatFixedLocalized(value: Double, fractionDigits: Int): String

    fun formatInteger(value: Long, minimumDigits: Int = 1): String
}

internal fun formatFixedInvariant(value: Double, fractionDigits: Int): String {
    require(fractionDigits in 0..6) { "fractionDigits must be between 0 and 6." }
    require(value.isFinite()) { "value must be finite." }

    val scale = POWERS_OF_TEN[fractionDigits]
    val scaled = (value.absoluteValue * scale).roundToLong()
    val sign = if (value < 0.0) "-" else ""
    val whole = scaled / scale
    if (fractionDigits == 0) return "$sign$whole"

    val fraction = (scaled % scale).toString().padStart(fractionDigits, '0')
    return "$sign$whole.$fraction"
}

internal fun formatFixedWithSeparator(
    value: Double,
    fractionDigits: Int,
    decimalSeparator: String,
): String {
    require(decimalSeparator.isNotEmpty()) { "decimalSeparator cannot be empty." }
    val invariant = formatFixedInvariant(value, fractionDigits)
    return if (fractionDigits == 0 || decimalSeparator == ".") {
        invariant
    } else {
        invariant.replace(".", decimalSeparator)
    }
}

internal fun formatIntegerInvariant(value: Long, minimumDigits: Int): String {
    require(minimumDigits >= 1) { "minimumDigits must be positive." }
    val text = value.toString()
    return if (text.startsWith('-')) {
        "-${text.drop(1).padStart(minimumDigits, '0')}"
    } else {
        text.padStart(minimumDigits, '0')
    }
}

private val POWERS_OF_TEN = longArrayOf(1L, 10L, 100L, 1_000L, 10_000L, 100_000L, 1_000_000L)
