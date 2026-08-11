package com.yenaly.han1meviewer.platform

import platform.Foundation.NSNumberFormatter

actual object NumberFormatter {
    actual fun formatFixed(value: Double, fractionDigits: Int): String =
        formatFixedInvariant(value, fractionDigits)

    actual fun formatFixedLocalized(value: Double, fractionDigits: Int): String =
        formatFixedWithSeparator(
            value = value,
            fractionDigits = fractionDigits,
            decimalSeparator = NSNumberFormatter().decimalSeparator,
        )

    actual fun formatInteger(value: Long, minimumDigits: Int): String =
        formatIntegerInvariant(value, minimumDigits)
}
