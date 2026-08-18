package io.github.darriousliu.han1meviewer.util

import io.github.vinceglb.filekit.PlatformFile
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * [calculateSizeRecursively] 是 Step 23 把「清缓存」从 androidMain 的
 * `File.folderSize` 换成 commonMain + FileKit 时新写的递归累加。
 *
 * FileKit 只给单个文件的 `size()`，目录累加要自己走——这是本轮唯一
 * 能用单测钉住的纯函数，所以钉住它。
 */
class AppCacheTest {

    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun calculateSizeRecursively_sumsNestedFiles() {
        // root/a.txt(3) + root/sub/b.txt(5) + root/sub/deep/c.txt(7) = 15
        temp.newFile("a.txt").writeBytes(ByteArray(3))
        val sub = temp.newFolder("sub")
        sub.resolve("b.txt").writeBytes(ByteArray(5))
        sub.resolve("deep").apply { mkdirs() }.resolve("c.txt").writeBytes(ByteArray(7))

        assertEquals(15L, PlatformFile(temp.root).calculateSizeRecursively())
    }

    @Test
    fun calculateSizeRecursively_emptyDirIsZero() {
        assertEquals(0L, PlatformFile(temp.newFolder("empty")).calculateSizeRecursively())
    }

    @Test
    fun calculateSizeRecursively_singleFileIsItsOwnSize() {
        val file = temp.newFile("only.bin").apply { writeBytes(ByteArray(42)) }
        assertEquals(42L, PlatformFile(file).calculateSizeRecursively())
    }
}
