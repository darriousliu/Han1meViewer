package io.github.darriousliu.han1meviewer.core.common.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [computeSampleSize] 是「大图不 OOM」这件事唯一能纯逻辑验证的部分，
 * 三个平台的 actual 都拿它算采样率，算错就直接体现为内存翻倍或图糊。
 */
class ImageCodecTest {

    @Test
    fun noSamplingWhenAlreadySmallEnough() {
        assertEquals(1, computeSampleSize(1024, 768, 2048))
        // 正好等于上限也不该采样
        assertEquals(1, computeSampleSize(2048, 2048, 2048))
    }

    @Test
    fun samplesByPowerOfTwo() {
        // 4096 / 2 = 2048，正好到上限
        assertEquals(2, computeSampleSize(4096, 3072, 2048))
        // 4097 / 2 = 2048（整除截断），仍然够
        assertEquals(2, computeSampleSize(4097, 3072, 2048))
        // 8000 / 4 = 2000 <= 2048，而 /2 = 4000 太大
        assertEquals(4, computeSampleSize(8000, 6000, 2048))
    }

    @Test
    fun usesLongestSide() {
        // 竖图，高才是长边
        assertEquals(4, computeSampleSize(6000, 8000, 2048))
        // 极端长条：20000/8 = 2500 还超，要到 16 才是 1250
        assertEquals(16, computeSampleSize(20000, 100, 2048))
    }

    @Test
    fun resultAlwaysFitsWithinMaxDimension() {
        val maxDimension = 2048
        // 覆盖一批真实相机/截图尺寸，逐个验「采样后确实不超上限」这个不变式
        val sizes = listOf(
            640 to 480, 1080 to 1920, 3024 to 4032, 4000 to 3000,
            6000 to 4000, 8000 to 6000, 12000 to 9000, 1 to 30000,
        )
        for ((width, height) in sizes) {
            val sample = computeSampleSize(width, height, maxDimension)
            val longest = maxOf(width, height) / sample
            assertTrue(
                "$width x $height 采样 $sample 后最长边 $longest 仍超过 $maxDimension",
                longest <= maxDimension,
            )
            // 采样率不能过头：降一档就该超上限（1 除外）
            if (sample > 1) {
                assertTrue(
                    "$width x $height 的采样率 $sample 过大，图会糊",
                    maxOf(width, height) / (sample / 2) > maxDimension,
                )
            }
        }
    }

    @Test
    fun illegalInputFallsBackToOne() {
        assertEquals(1, computeSampleSize(0, 0, 2048))
        assertEquals(1, computeSampleSize(-1, 100, 2048))
        assertEquals(1, computeSampleSize(4000, 3000, 0))
    }
}
