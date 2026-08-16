package com.yenaly.han1meviewer.util

import android.util.Base64
import java.io.File
import java.util.Locale

private val siFileSizeUnits = arrayOf("B", "kB", "MB", "GB", "TB")
private val iecFileSizeUnits = arrayOf("B", "KiB", "MiB", "GiB", "TiB")

fun Long.formatFileSizeV2(
    useSi: Boolean = false,
    decimalPlaces: Int = 1,
    stripTrailingZeros: Boolean = true,
): String {
    require(decimalPlaces >= 0) { "decimalPlaces must not be negative" }
    val unit = if (useSi) 1000 else 1024
    if (this < unit) return "$this B"

    val units = if (useSi) siFileSizeUnits else iecFileSizeUnits
    var value = toDouble()
    var unitIndex = 0
    while (value >= unit && unitIndex < units.lastIndex) {
        value /= unit
        unitIndex++
    }

    return if (decimalPlaces == 0 || (stripTrailingZeros && value % 1 == 0.0)) {
        "%.0f %s".format(Locale.getDefault(), value, units[unitIndex])
    } else {
        "%.${decimalPlaces}f %s".format(Locale.getDefault(), value, units[unitIndex])
    }
}

fun Long.formatBytesPerSecond(
    useSi: Boolean = false,
    decimalPlaces: Int = 1,
    stripTrailingZeros: Boolean = true,
): String = formatFileSizeV2(useSi, decimalPlaces, stripTrailingZeros) + "/s"

val File?.folderSize: Long
    get() = this?.listFiles()?.sumOf { file ->
        if (file.isDirectory) file.folderSize else file.length()
    } ?: 0L

fun String.decodeFromStringByBase64(flag: Int = Base64.DEFAULT): String =
    String(Base64.decode(toByteArray(), flag))
