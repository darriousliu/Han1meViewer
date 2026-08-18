package com.yenaly.han1meviewer.ui.navigation.settings

import androidx.compose.runtime.Composable
import com.yenaly.han1meviewer.HanimeConstants.HANIME_HOSTNAME
import com.yenaly.han1meviewer.HanimeConstants.HANIME_URL
import com.yenaly.han1meviewer.PlayerDefaults
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.util.formatBytesPerSecond
import com.yenaly.han1meviewer.util.formatFileSizeV2
import han1meviewer.shared.generated.resources.Res
import han1meviewer.shared.generated.resources.alternative
import han1meviewer.shared.generated.resources.at_any_time
import han1meviewer.shared.generated.resources.cache_usage_summary
import han1meviewer.shared.generated.resources.current_slide_sensitivity
import han1meviewer.shared.generated.resources.default_
import han1meviewer.shared.generated.resources.extremely_low
import han1meviewer.shared.generated.resources.high
import han1meviewer.shared.generated.resources.last_update_popup_check_time
import han1meviewer.shared.generated.resources.low
import han1meviewer.shared.generated.resources.moderate
import han1meviewer.shared.generated.resources.moderately_high
import han1meviewer.shared.generated.resources.no_limit
import han1meviewer.shared.generated.resources.no_update_popup_yet
import han1meviewer.shared.generated.resources.slightly_low
import han1meviewer.shared.generated.resources.very_low
import han1meviewer.shared.generated.resources.which_days
import han1meviewer.shared.generated.resources.will_remind_before_d_seconds
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/*
 * 设置页的摘要文案。全部属于第三节分类里的 **A 类**（全平台通用）——
 * 只是把 Preferences 的值拼成人话，没有任何平台能力。
 *
 * 从 androidMain 的 `SettingsRouteUtils.kt` 拆出来，那边只剩真平台能力
 * （改名 `SettingsPlatformActions.kt`）。
 *
 * ⚠️ 为什么是 `@Composable` 而不是普通函数：原来靠 `Context.getString`，
 * CMP 侧取字符串只有两条路——组合内的 `stringResource`（composable）
 * 或非组合的 `getString`（**suspend**）。设置页的调用点在
 * `remember(refreshKey) { … }` 里，两者都用不了，所以整条链改成 composable，
 * 调用点从 `remember(refreshKey, context) { buildXxx(context) }`
 * 变成直接 `buildXxx(refreshKey)`。
 *
 * 副作用是每次重组都会重算一遍（原来只在 refreshKey 变时算）。
 * 这些函数只读 MMKV + 资源，代价可忽略，而且**顺带修掉一个隐患**：
 * 原写法只在 refreshKey 变化时刷新，Preferences 从别处被改时 UI 不会跟。
 */

/** 镜像站下拉的选项：`域名 (默认/备用/av)` → URL */
@Composable
fun buildDomainOptions(): List<Pair<String, String>> {
    val default = stringResource(Res.string.default_)
    val alternative = stringResource(Res.string.alternative)
    return listOf(
        "${HANIME_HOSTNAME[0]} ($default)" to HANIME_URL[0],
        "${HANIME_HOSTNAME[1]} ($alternative)" to HANIME_URL[1],
        "${HANIME_HOSTNAME[2]} ($alternative)" to HANIME_URL[2],
        "${HANIME_HOSTNAME[3]} (av)" to HANIME_URL[3],
    )
}

/**
 * 缓存占用摘要。
 *
 * 原来结尾有个 `parseAsHtml()` 返回 `Spanned`，但**唯一调用点拿到就 `.toString()`**，
 * Span 全丢——等价于纯文本，所以这里直接不做 HTML 解析，行为不变。
 */
@Composable
fun generateClearCacheSummary(size: Long): String =
    stringResource(Res.string.cache_usage_summary, size.formatFileSizeV2())

/** 更新弹窗间隔 + 上次检查时间 */
@OptIn(ExperimentalTime::class)
@Composable
fun toIntervalDaysPrettyString(value: Int): String {
    val lastUpdatePopupTime = Preferences.lastUpdatePopupTime
    val msg = if (lastUpdatePopupTime == 0L) {
        stringResource(Res.string.no_update_popup_yet)
    } else {
        stringResource(
            Res.string.last_update_popup_check_time,
            Instant.fromEpochSeconds(lastUpdatePopupTime)
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .format(LocalDateTime.Formats.ISO),
        )
    }
    val head = if (value == 0) {
        stringResource(Res.string.at_any_time)
    } else {
        stringResource(Res.string.which_days, value)
    }
    return "$head\n$msg"
}

/** 滑动灵敏度 1–9 → 「高 / 较高 / 中 / … / 极低」 */
@Composable
fun toPrettySensitivityString(value: Int): String {
    val pretty = when (value) {
        1, 2 -> stringResource(Res.string.high)
        3, 4 -> stringResource(Res.string.moderately_high)
        5 -> stringResource(Res.string.moderate)
        6 -> stringResource(Res.string.slightly_low)
        7 -> stringResource(Res.string.low)
        8 -> stringResource(Res.string.very_low)
        9 -> stringResource(Res.string.extremely_low)
        else -> error("Invalid sensitivity value: $value")
    }
    return stringResource(Res.string.current_slide_sensitivity, pretty)
}

/**
 * 倒计时提醒秒数摘要，等于默认值时加「(默认)」。
 *
 * 默认值原来读的是 androidMain 的 `HJzvdStd.DEF_COUNTDOWN_SEC`，
 * 而那个常量的值就是 commonMain 的 [PlayerDefaults.COUNTDOWN_SEC]，直接引后者。
 */
@Composable
fun toPrettyCountdownRemindString(value: Int): String {
    val text = stringResource(Res.string.will_remind_before_d_seconds, value)
    val default = stringResource(Res.string.default_)
    return if (value == PlayerDefaults.COUNTDOWN_SEC) "$text ($default)" else text
}

/** 0 表示不限速 */
@Composable
fun Long.toDownloadSpeedPrettyString(): String =
    if (this == 0L) stringResource(Res.string.no_limit) else formatBytesPerSecond()

/** 0 表示不限并发数 */
@Composable
fun toDownloadCountLimitPrettyString(value: Int): String =
    if (value == 0) stringResource(Res.string.no_limit) else value.toString()
