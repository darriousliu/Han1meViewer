package com.yenaly.han1meviewer.platform

import java.text.DecimalFormatSymbols

actual object NumberFormatter {
    actual fun formatFixed(value: Double, fractionDigits: Int): String =
        formatFixedInvariant(value, fractionDigits)

    actual fun formatFixedLocalized(value: Double, fractionDigits: Int): String =
        formatFixedWithSeparator(
            value = value,
            fractionDigits = fractionDigits,
            decimalSeparator = DecimalFormatSymbols.getInstance().decimalSeparator.toString(),
        )

    actual fun formatInteger(value: Long, minimumDigits: Int): String =
        formatIntegerInvariant(value, minimumDigits)
}
