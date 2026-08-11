package com.yenaly.han1meviewer.platform

import java.text.DecimalFormatSymbols

actual object NumberFormatter {
    actual fun formatFixed(value: Double, fractionDigits: Int): String =
        formatFixedInvariant(value, fractionDigits)

    actual fun formatFixedLocalized(value: Double, fractionDigits: Int): String {
        val symbols = DecimalFormatSymbols.getInstance()
        return formatFixedWithSeparator(
            value = value,
            fractionDigits = fractionDigits,
            decimalSeparator = symbols.decimalSeparator.toString(),
        ).localizeAsciiDigits(symbols.zeroDigit)
    }

    actual fun formatInteger(value: Long, minimumDigits: Int): String =
        formatIntegerInvariant(value, minimumDigits)
}

private fun String.localizeAsciiDigits(zeroDigit: Char): String {
    if (zeroDigit == '0') return this
    return buildString(length) {
        for (character in this@localizeAsciiDigits) {
            append(
                if (character in '0'..'9') {
                    (zeroDigit.code + character.code - '0'.code).toChar()
                } else {
                    character
                },
            )
        }
    }
}
