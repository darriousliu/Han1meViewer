package io.github.darriousliu.han1meviewer.core.common.util

import cn.jzvd.JZUtils
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [formatVideoTime] 是 `cn.jzvd.JZUtils.stringForTime` 的等价替换。
 *
 * 这个测试**直接和真身对拍**——androidHostTest 还能看到 Jzvd，所以期望值不是我算的，
 * 是原实现跑出来的。播放器域迁完、Jzvd 依赖去掉之后，把 `expected` 换成硬编码字符串
 * （届时 `assertEquals(JZUtils.stringForTime(x), formatVideoTime(x))` 会编不过）。
 */
class TimeFormatTest {

    private fun assertSameAsJzvd(timeMs: Long) {
        assertEquals("timeMs=$timeMs", JZUtils.stringForTime(timeMs), formatVideoTime(timeMs))
    }

    @Test
    fun matchesJzvdOnBoundaries() {
        // 非正数和越界都是 "00:00"
        assertSameAsJzvd(Long.MIN_VALUE)
        assertSameAsJzvd(-1L)
        assertSameAsJzvd(0L)
        assertSameAsJzvd(86_400_000L)          // 恰好 24h，越界
        assertSameAsJzvd(86_400_001L)
        assertSameAsJzvd(Long.MAX_VALUE)
        // 越界前一刻还是正常格式
        assertSameAsJzvd(86_399_999L)
    }

    @Test
    fun matchesJzvdUnderOneHour() {
        assertSameAsJzvd(1L)
        assertSameAsJzvd(999L)
        assertSameAsJzvd(1_000L)
        assertSameAsJzvd(9_000L)
        assertSameAsJzvd(59_000L)
        assertSameAsJzvd(60_000L)
        assertSameAsJzvd(123_000L)
        assertSameAsJzvd(540_000L)
        assertSameAsJzvd(3_599_000L)
    }

    @Test
    fun matchesJzvdOverOneHour() {
        assertSameAsJzvd(3_600_000L)
        assertSameAsJzvd(3_723_000L)           // 1:02:03
        assertSameAsJzvd(36_000_000L)          // 10:00:00
        assertSameAsJzvd(45_296_000L)
    }

    /** 小时位不补零，这是最容易写错的一条，单独钉一下。 */
    @Test
    fun hourIsNotZeroPadded() {
        assertEquals("1:02:03", formatVideoTime(3_723_000L))
        assertEquals("10:00:00", formatVideoTime(36_000_000L))
        assertEquals("02:03", formatVideoTime(123_000L))
        assertEquals("00:00", formatVideoTime(0L))
    }
}
