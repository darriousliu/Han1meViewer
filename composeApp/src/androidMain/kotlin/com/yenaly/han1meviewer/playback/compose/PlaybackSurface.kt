package com.yenaly.han1meviewer.playback.compose

import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.ContentFrame
import com.yenaly.han1meviewer.playback.core.PlaybackController
import com.yenaly.han1meviewer.playback.core.PlaybackRenderHandle
import com.yenaly.han1meviewer.playback.core.renderHandle
import com.yenaly.han1meviewer.playback.media3.Media3PlaybackRenderHandle
import com.yenaly.han1meviewer.playback.model.PlaybackEngineType

/**
 * The single rendering entry point used by the Compose player.
 *
 * Media3 delegates surface ownership to its official Compose [ContentFrame]. MPV and other native
 * engines receive only an Android [Surface] through the internal render handle.
 */
@OptIn(UnstableApi::class)
@Composable
fun PlaybackSurface(
    controller: PlaybackController,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    when (controller.engineType) {
        PlaybackEngineType.Media3 -> {
            val handle = controller.renderHandle
            check(handle is Media3PlaybackRenderHandle) {
                "Media3 controller must expose a Media3PlaybackRenderHandle."
            }
            ContentFrame(
                player = handle.player,
                modifier = modifier,
                contentScale = contentScale,
                keepContentOnReset = true,
                shutter = { Box(Modifier.fillMaxSize().background(Color.Black)) },
            )
        }

        PlaybackEngineType.Mpv -> NativeTexturePlaybackSurface(
            renderHandle = controller.renderHandle,
            modifier = modifier,
        )
    }
}

@Composable
private fun NativeTexturePlaybackSurface(
    renderHandle: PlaybackRenderHandle,
    modifier: Modifier,
) {
    var textureView by remember { mutableStateOf<TextureView?>(null) }

    AndroidView(
        factory = ::TextureView,
        update = { textureView = it },
        modifier = modifier,
    )

    textureView?.let { view ->
        DisposableEffect(view, renderHandle) {
            var attachedSurface: Surface? = null

            fun attach(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                attachedSurface?.let { oldSurface ->
                    renderHandle.detachSurface(oldSurface)
                    oldSurface.release()
                }
                attachedSurface = Surface(surfaceTexture).also { surface ->
                    renderHandle.attachSurface(surface)
                    renderHandle.updateSurfaceSize(width, height)
                }
            }

            val listener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(
                    surface: SurfaceTexture,
                    width: Int,
                    height: Int,
                ) {
                    attach(surface, width, height)
                }

                override fun onSurfaceTextureSizeChanged(
                    surface: SurfaceTexture,
                    width: Int,
                    height: Int,
                ) {
                    renderHandle.updateSurfaceSize(width, height)
                }

                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                    attachedSurface?.let(renderHandle::detachSurface)
                    attachedSurface?.release()
                    attachedSurface = null
                    return true
                }

                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
            }

            view.surfaceTextureListener = listener
            if (view.isAvailable) {
                view.surfaceTexture?.let { attach(it, view.width, view.height) }
            }

            onDispose {
                if (view.surfaceTextureListener === listener) {
                    view.surfaceTextureListener = null
                }
                attachedSurface?.let(renderHandle::detachSurface)
                attachedSurface?.release()
                attachedSurface = null
            }
        }
    }
}
