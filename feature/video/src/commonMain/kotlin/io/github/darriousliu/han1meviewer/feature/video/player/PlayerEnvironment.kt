package io.github.darriousliu.han1meviewer.feature.video.player

/**
 * 播放器要展示/判断的设备环境信息。方法全带默认实现：
 * [batteryPercent] 返回 null 时顶栏的电量列整个不显示。
 */
interface PlayerEnvironment {

    /** 当前电量百分比（0..100）；平台不提供时返回 null。 */
    fun batteryPercent(): Int? = null

    /** 系统时间文本（HH:mm）。 */
    fun currentTimeText(): String = ""

    /** 是否在计费网络（移动数据）上；流量提醒用。 */
    fun isNetworkMetered(): Boolean = false
}

object NoopPlayerEnvironment : PlayerEnvironment
