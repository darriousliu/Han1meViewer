package com.yenaly.han1meviewer.playback.platform

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.net.ConnectivityManager
import android.util.Rational
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.activity.AndroidMainActivity

/**
 * Owns Android-only playback behavior so the route and playback engines do not
 * reach into window, orientation, audio or picture-in-picture APIs directly.
 */
class PlaybackPlatformBridge(
    private val activity: AndroidMainActivity,
) {
    private val audioManager =
        activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val connectivityManager =
        activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val originalOrientation = activity.requestedOrientation
    private val originalBrightness = activity.window.attributes.screenBrightness
    private var isFullscreen = false
    private var isKeepingScreenOn = false

    fun setFullscreen(
        fullscreen: Boolean,
        videoWidth: Int = 0,
        videoHeight: Int = 0,
    ) {
        if (isFullscreen == fullscreen) return
        isFullscreen = fullscreen

        val insetsController = WindowCompat.getInsetsController(
            activity.window,
            activity.window.decorView,
        )
        if (fullscreen) {
            activity.requestedOrientation = if (videoHeight > videoWidth && videoWidth > 0) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            activity.requestedOrientation = originalOrientation
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        if (isKeepingScreenOn == enabled) return
        isKeepingScreenOn = enabled
        if (enabled) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    fun currentVolume(): Float {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
    }

    fun isActiveNetworkMetered(): Boolean = connectivityManager.isActiveNetworkMetered

    /** Sets the media stream from an absolute normalized value and returns the applied value. */
    fun setVolume(fraction: Float): Float {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val next = (fraction.coerceIn(0f, 1f) * max).toInt().coerceIn(0, max)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, next, 0)
        return next.toFloat() / max
    }

    fun currentBrightness(): Float {
        val attributes = activity.window.attributes
        return attributes.screenBrightness.takeIf { it >= 0f } ?: DEFAULT_BRIGHTNESS
    }

    /** Sets the activity brightness from an absolute normalized value. */
    fun setBrightness(fraction: Float): Float {
        val attributes = activity.window.attributes
        val next = fraction.coerceIn(MIN_BRIGHTNESS, 1f)
        attributes.screenBrightness = next
        activity.window.attributes = attributes
        return next
    }

    fun enterPictureInPicture(
        isPlaying: Boolean,
        sourceRect: Rect?,
        videoWidth: Int,
        videoHeight: Int,
    ): Boolean = activity.enterPictureInPictureMode(
        pictureInPictureParams(isPlaying, sourceRect, videoWidth, videoHeight)
    )

    fun updatePictureInPictureAction(
        isPlaying: Boolean,
        sourceRect: Rect?,
        videoWidth: Int,
        videoHeight: Int,
    ) {
        if (!activity.isInPictureInPictureMode) return
        activity.setPictureInPictureParams(
            pictureInPictureParams(isPlaying, sourceRect, videoWidth, videoHeight)
        )
    }

    fun release() {
        setKeepScreenOn(false)
        setFullscreen(false)
        activity.window.attributes = activity.window.attributes.apply {
            screenBrightness = originalBrightness
        }
    }

    private fun pictureInPictureParams(
        isPlaying: Boolean,
        sourceRect: Rect?,
        videoWidth: Int,
        videoHeight: Int,
    ): PictureInPictureParams {
        val toggleIntent = PendingIntent.getBroadcast(
            activity,
            PIP_TOGGLE_REQUEST_CODE,
            Intent(AndroidMainActivity.ACTION_TOGGLE_PLAY).setPackage(activity.packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val icon = Icon.createWithResource(
            activity,
            if (isPlaying) R.drawable.ic_pip_pause_24 else R.drawable.ic_pip_play_arrow_24,
        )
        val actionLabel = activity.getString(R.string.play_pause)
        val action = RemoteAction(icon, actionLabel, actionLabel, toggleIntent)
        val aspectRatio = if (videoWidth > 0 && videoHeight > 0) {
            Rational(videoWidth, videoHeight)
        } else {
            Rational(DEFAULT_ASPECT_WIDTH, DEFAULT_ASPECT_HEIGHT)
        }

        return PictureInPictureParams.Builder()
            .setAspectRatio(aspectRatio.coerceForPip())
            .setActions(listOf(action))
            .apply { sourceRect?.takeUnless(Rect::isEmpty)?.let(::setSourceRectHint) }
            .build()
    }

    private fun Rational.coerceForPip(): Rational {
        val ratio = toFloat()
        return when {
            ratio < MIN_PIP_ASPECT_RATIO -> Rational(100, 239)
            ratio > MAX_PIP_ASPECT_RATIO -> Rational(239, 100)
            else -> this
        }
    }

    private companion object {
        const val PIP_TOGGLE_REQUEST_CODE = 389
        const val DEFAULT_ASPECT_WIDTH = 16
        const val DEFAULT_ASPECT_HEIGHT = 9
        const val MIN_BRIGHTNESS = 0.01f
        const val DEFAULT_BRIGHTNESS = 0.5f
        const val MIN_PIP_ASPECT_RATIO = 100f / 239f
        const val MAX_PIP_ASPECT_RATIO = 239f / 100f
    }
}
