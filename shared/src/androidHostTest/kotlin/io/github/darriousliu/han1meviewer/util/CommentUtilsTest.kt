package io.github.darriousliu.han1meviewer.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [parseTimeStrToMinutes] 和 [safeSortedBy] 随 Step 24 从 androidMain 搬进 commonMain。
 * 两个都是纯 Kotlin，正好趁搬迁把行为钉住——这是本轮唯一能单测的地方。
 *
 * ⚠️ 注意 `parseTimeStrToMinutes` 的分支顺序是**有意义**的：`"分鐘前"` 必须排在
 * `"鐘前"` 之类的子串之前。这里逐个分支都测，改动顺序会被挡住。
 */
class CommentUtilsTest {

    @Test
    fun parseTimeStrToMinutes_coversEveryUnit() {
        assertEquals(45, parseTimeStrToMinutes("45分鐘前"))
        assertEquals(2 * 60, parseTimeStrToMinutes("2小時前"))
        assertEquals(5 * 60 * 24, parseTimeStrToMinutes("5天前"))
        assertEquals(1 * 60 * 24 * 7, parseTimeStrToMinutes("1週前"))
        assertEquals(2 * 60 * 24 * 30, parseTimeStrToMinutes("2個月前"))
        assertEquals(1 * 60 * 24 * 365, parseTimeStrToMinutes("1年前"))
    }

    @Test
    fun parseTimeStrToMinutes_unknownFormatIsZero() {
        assertEquals(0, parseTimeStrToMinutes("剛剛"))
        assertEquals(0, parseTimeStrToMinutes(""))
    }

    @Test
    fun safeSortedBy_sortsAscendingAndDescending() {
        val src = listOf("bb", "a", "ccc")
        assertEquals(listOf("a", "bb", "ccc"), src.safeSortedBy({ it.length }))
        assertEquals(listOf("ccc", "bb", "a"), src.safeSortedBy({ it.length }, descending = true))
    }

    /**
     * ⚠️ 名字叫 "safe" 容易让人以为「selector 返回 null 就放弃排序、原样返回」——**不是**。
     * `sortedBy` 本身就吃可空 selector，null 按「最小」排在最前面，
     * `runCatching` 那层兜的是 selector **抛异常**的情况，不是返回 null。
     */
    @Test
    fun safeSortedBy_putsNullSelectorResultsFirst() {
        val src = listOf("bb", "a", "ccc")
        assertEquals(
            listOf("a", "bb", "ccc"),
            src.safeSortedBy({ if (it == "a") null else it.length })
        )
    }

    @Test
    fun safeSortedBy_returnsOriginalWhenSelectorThrows() {
        val src = listOf("bb", "a", "ccc")
        assertEquals(src, src.safeSortedBy<String, Int>({ error("boom") }))
    }
}
