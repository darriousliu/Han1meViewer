package com.yenaly.han1meviewer

/**
 * 播放器默认值。原本定义在 androidMain 的 `HJzvdStd` 伴生对象里，
 * 为了让 [Preferences] 能进 commonMain 抽了出来。
 *
 * `HJzvdStd.DEF_*` 保留为指向这里的别名，现有调用点不受影响。
 */
object PlayerDefaults {

    /** 默認滑動調整進度條的靈敏度，越大播放进度条滑动越慢 */
    const val PROGRESS_SLIDE_SENSITIVITY = 5

    const val COUNTDOWN_SEC = 10

    /** 默認速度 */
    const val SPEED = 1.0F

    /** 默認長按速度是原先速度的幾倍 */
    const val LONG_PRESS_SPEED_TIMES = 2.5F

    /**
     * 默认播放器内核，对应 androidMain `HMediaKernel.Type.ExoPlayer.name`。
     * 那个枚举带 `Class<out JZMediaInterface>`，进不了 commonMain，所以这里只留名字。
     */
    const val PLAYER_KERNEL = "ExoPlayer"
}
