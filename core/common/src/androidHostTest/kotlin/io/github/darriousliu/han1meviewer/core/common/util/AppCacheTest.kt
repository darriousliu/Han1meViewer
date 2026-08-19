package io.github.darriousliu.han1meviewer.core.common.util

import io.github.vinceglb.filekit.PlatformFile
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * FileKit 只给单个文件的 `size()`，目录累加要 [calculateSizeRecursively] 自己走。
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
