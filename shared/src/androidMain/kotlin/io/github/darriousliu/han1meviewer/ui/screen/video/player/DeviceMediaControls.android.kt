package io.github.darriousliu.han1meviewer.ui.screen.video.player

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import io.github.darriousliu.han1meviewer.util.activity

/**
 * 亮度走窗口属性、音量走 `AudioManager.STREAM_MUSIC`——和 `HJzvdStd` 逐条对齐。
 *
 * ⚠️ 两处语义是照抄来的，不是随手写的：
 * - **亮度下限 0.01f 不是 0f**。`HJzvdStd` 那里写着「这和声音有区别，必须自己过滤一下负值」，
 *   因为 `screenBrightness = 0f` 在系统里表示「跟随系统」而不是「最暗」。
 * - 进播放页时先把系统亮度**快照**下来当基线，离开时还回去
 *   （`HJzvdStd` 只在退出全屏时还原，导致内联调完亮度直接离开页面会把改过的值留下——
 *   这里改成 `onDispose` 也还原，顺手修掉）。
 */
private class AndroidDeviceMediaControls(
    private val context: Context,
    private val activity: Activity?,
) : DeviceMediaControls {

    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    override var brightness: Float
        get() {
            val attrs = activity?.window?.attributes ?: return systemBrightness()
            return attrs.screenBrightness.takeIf { it >= 0f } ?: systemBrightness()
        }
        set(value) {
            val window = activity?.window ?: return
            window.attributes = window.attributes.apply {
                screenBrightness = value.coerceIn(DeviceMediaControls.MIN_BRIGHTNESS, 1f)
            }
        }

    override var volumePercent: Float
        get() = if (maxVolume <= 0) 0f
        else audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume
        set(value) {
            if (maxVolume <= 0) return
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                (value.coerceIn(0f, 1f) * maxVolume).toInt(),
                0,
            )
        }

    override fun restoreSystemBrightness() {
        val window = activity?.window ?: return
        window.attributes = window.attributes.apply {
            screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
    }

    /** 系统当前亮度（0..255 → 0f..1f）。取不到就给默认值。 */
    private fun systemBrightness(): Float = runCatching {
        Settings.System.getInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
        ) / 255f
    }.getOrDefault(DeviceMediaControls.DEFAULT_BRIGHTNESS)
}

@Composable
actual fun rememberDeviceMediaControls(): DeviceMediaControls {
    val context = LocalContext.current
    val activity = context.activity
    val controls = remember(context, activity) {
        AndroidDeviceMediaControls(context, activity)
    }
    DisposableEffect(controls) {
        onDispose { controls.restoreSystemBrightness() }
    }
    return controls
}
