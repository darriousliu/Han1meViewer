package com.yenaly.han1meviewer.datetime

internal fun yearMonthCode(year: Int, month: Int): String =
    year.zeroPadded(width = 4) + month.zeroPadded(width = 2)

internal fun shiftYearMonthCode(code: String, delta: Int): String {
    var year = code.substring(0, 4).toInt()
    var month = code.substring(4, 6).toInt() + delta
    while (month < 1) {
        month += 12
        year -= 1
    }
    while (month > 12) {
        month -= 12
        year += 1
    }
    return yearMonthCode(year, month)
}

private fun Int.zeroPadded(width: Int): String {
    val value = toString()
    return if (value.startsWith('-')) {
        "-" + value.drop(1).padStart(width - 1, '0')
    } else {
        value.padStart(width, '0')
    }
}
