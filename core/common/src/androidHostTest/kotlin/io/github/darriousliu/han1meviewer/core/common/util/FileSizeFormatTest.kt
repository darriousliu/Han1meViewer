package io.github.darriousliu.han1meviewer.core.common.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [formatFileSizeV2] 从 androidMain 搬进 commonMain 时，把
 * `"%.Nf %s".format(Locale.getDefault(), …)` 换成了 mp_stools 的 `sprintf`。
 *
 * 这些断言原样搬自 `yenaly_libs/src/test/java/com/yenaly/yenaly_libs/UtilUnitTest.kt`，
 * 用来钉死换实现前后的输出一致。以后再动这个函数也能挡住回归。
 */
class FileSizeFormatTest {

    @Test
    fun formatFileSizeV2_matchesLegacyOutput() {
        // 默认参数
        assertEquals("1 kB", 1000L.formatFileSizeV2(useSi = true))
        assertEquals("1 KiB", 1024L.formatFileSizeV2())

        // decimalPlaces
        assertEquals(
            "1.00 kB",
            1000L.formatFileSizeV2(useSi = true, decimalPlaces = 2, stripTrailingZeros = false)
        )
        assertEquals(
            "1.00 KiB",
            1024L.formatFileSizeV2(decimalPlaces = 2, stripTrailingZeros = false)
        )

        // stripTrailingZeros = false
        assertEquals("1.0 kB", 1000L.formatFileSizeV2(useSi = true, stripTrailingZeros = false))
        assertEquals("1.0 KiB", 1024L.formatFileSizeV2(stripTrailingZeros = false))

        // 各量级
        assertEquals(
            "1.0 MB",
            1_000_000L.formatFileSizeV2(useSi = true, stripTrailingZeros = false)
        )
        assertEquals("1.0 MiB", 1_048_576L.formatFileSizeV2(stripTrailingZeros = false))
        assertEquals(
            "1.0 GB",
            1_000_000_000L.formatFileSizeV2(useSi = true, stripTrailingZeros = false)
        )
        assertEquals("1.0 GiB", 1_073_741_824L.formatFileSizeV2(stripTrailingZeros = false))

        // 边界
        assertEquals("999 B", 999L.formatFileSizeV2(useSi = true))
        assertEquals("1023 B", 1023L.formatFileSizeV2())
    }

    /** 生产代码实际只走默认参数这一条路径（4 个调用点都没覆盖默认值）。 */
    @Test
    fun formatFileSizeV2_defaultPath() {
        assertEquals("0 B", 0L.formatFileSizeV2())
        assertEquals("1 B", 1L.formatFileSizeV2())
        assertEquals("1.5 KiB", 1536L.formatFileSizeV2())
        assertEquals("1 MiB", 1_048_576L.formatFileSizeV2())
        assertEquals("1.5 MiB", 1_572_864L.formatFileSizeV2())
        assertEquals("5 GiB", 5_368_709_120L.formatFileSizeV2())
    }

    @Test
    fun formatBytesPerSecond_appendsSuffix() {
        assertEquals("1 KiB/s", 1024L.formatBytesPerSecond())
        assertEquals("1.5 MiB/s", 1_572_864L.formatBytesPerSecond())
    }
}
