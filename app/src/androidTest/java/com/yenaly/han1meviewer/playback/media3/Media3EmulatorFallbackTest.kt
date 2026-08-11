package com.yenaly.han1meviewer.playback.media3

import androidx.annotation.OptIn
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(UnstableApi::class)
@RunWith(AndroidJUnit4::class)
class Media3EmulatorFallbackTest {
    @Test
    fun softwareDecoderHandlesReportedAvcFormat() {
        assumeTrue(Util.isRunningOnEmulator())
        val format = Format.Builder()
            .setSampleMimeType(MimeTypes.VIDEO_H264)
            .setCodecs("avc1.640029")
            .setWidth(720)
            .setHeight(480)
            .setFrameRate(23.976025f)
            .build()
        val decoders = MediaCodecSelector.PREFER_SOFTWARE.getDecoderInfos(
            MimeTypes.VIDEO_H264,
            false,
            false,
        )
        val rendererOrder = MediaCodecUtil.getDecoderInfosSortedByFormatSupport(
            InstrumentationRegistry.getInstrumentation().targetContext,
            decoders,
            format,
        )

        assertFalse(rendererOrder.first().name.startsWith("c2.goldfish.", ignoreCase = true))
    }
}
