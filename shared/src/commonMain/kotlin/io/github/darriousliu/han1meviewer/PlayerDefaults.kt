package io.github.darriousliu.han1meviewer

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

    /**
     * Step 25 新增的第四个内核：Media3 + Compose 的播放器。
     *
     * 它**不是** `HMediaKernel.Type` 的成员——那个枚举每一项都要带一个
     * `Class<out JZMediaInterface>`，而这条路根本不经过 jzvd。
     * `HMediaKernel.Type.fromString` 对未知名字已经回落到 `ExoPlayer`，
     * 所以万一哪条分支漏了，走的是原来的 jzvd 路径，不会崩。
     */
    const val KERNEL_EXO_COMPOSE = "ExoPlayer (Compose)"
}
