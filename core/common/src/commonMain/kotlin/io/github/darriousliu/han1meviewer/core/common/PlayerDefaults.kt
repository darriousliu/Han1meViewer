package io.github.darriousliu.han1meviewer.core.common


/**
 * 播放器默认值。`HJzvdStd.DEF_*` 是指向这里的别名。
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
     * Media3 + Compose 的播放器内核。
     *
     * 它**不是** `HMediaKernel.Type` 的成员——那个枚举每一项都要带一个
     * `Class<out JZMediaInterface>`，而这条路根本不经过 jzvd。
     * `HMediaKernel.Type.fromString` 对未知名字已经回落到 `ExoPlayer`，
     * 所以万一哪条分支漏了，走的是原来的 jzvd 路径，不会崩。
     */
    const val KERNEL_EXO_COMPOSE = "ExoPlayer (Compose)"

    /** mpv 走 Compose 的播放器内核（与 jzvd 的 `MpvPlayer` 并存，删 jzvd 后归一）。 */
    const val KERNEL_MPV_COMPOSE = "MpvPlayer (Compose)"

    /** 速度列表；[SPEED_LABELS] 与之同索引。默认档是 [DEF_SPEED_INDEX]（1.0x）。 */
    val SPEED_ARRAY = floatArrayOf(
        0.5F, 0.75F,
        1.0F, 1.25F, 1.5F, 1.75F,
        2.0F, 2.25F, 2.5F, 2.75F,
        3.0F,
    )

    val SPEED_LABELS: List<String> = SPEED_ARRAY.map { "${it}x" }

    /** 默認速度的索引 */
    const val DEF_SPEED_INDEX = 2

    /**
     * 將靈敏度轉換為實際數值。高靈敏度（1~5）照舊，低靈敏度差別放大。
     * 偏好值域是 1..9；越界值钳到边界（原实现是抛异常，这里对脏数据宽容）。
     */
    fun toRealSensitivity(sensitivity: Int): Int = when (sensitivity.coerceIn(1, 9)) {
        6 -> 7
        7 -> 10
        8 -> 20
        9 -> 40
        else -> sensitivity.coerceIn(1, 5)
    }
}
