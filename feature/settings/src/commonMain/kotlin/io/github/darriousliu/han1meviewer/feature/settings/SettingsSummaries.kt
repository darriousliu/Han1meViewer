package io.github.darriousliu.han1meviewer.feature.settings

import androidx.compose.runtime.Composable
import io.github.darriousliu.han1meviewer.core.common.HanimeConstants.HANIME_HOSTNAME
import io.github.darriousliu.han1meviewer.core.common.HanimeConstants.HANIME_URL
import io.github.darriousliu.han1meviewer.core.common.PlayerDefaults
import io.github.darriousliu.han1meviewer.core.storage.Preferences
import io.github.darriousliu.han1meviewer.core.common.util.formatBytesPerSecond
import io.github.darriousliu.han1meviewer.core.common.util.formatFileSizeV2
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.alternative
import io.github.darriousliu.han1meviewer.core.resource.at_any_time
import io.github.darriousliu.han1meviewer.core.resource.cache_usage_summary
import io.github.darriousliu.han1meviewer.core.resource.current_slide_sensitivity
import io.github.darriousliu.han1meviewer.core.resource.default_
import io.github.darriousliu.han1meviewer.core.resource.extremely_low
import io.github.darriousliu.han1meviewer.core.resource.high
import io.github.darriousliu.han1meviewer.core.resource.last_update_popup_check_time
import io.github.darriousliu.han1meviewer.core.resource.low
import io.github.darriousliu.han1meviewer.core.resource.moderate
import io.github.darriousliu.han1meviewer.core.resource.moderately_high
import io.github.darriousliu.han1meviewer.core.resource.no_limit
import io.github.darriousliu.han1meviewer.core.resource.no_update_popup_yet
import io.github.darriousliu.han1meviewer.core.resource.slightly_low
import io.github.darriousliu.han1meviewer.core.resource.very_low
import io.github.darriousliu.han1meviewer.core.resource.which_days
import io.github.darriousliu.han1meviewer.core.resource.will_remind_before_d_seconds
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/*
 * 设置页的摘要文案，全平台通用——只是把 Preferences 的值拼成人话，没有任何平台能力。
 *
 * ⚠️ 为什么是 `@Composable` 而不是普通函数：CMP 侧取字符串只有两条路——
 * 组合内的 `stringResource`（composable）或非组合的 `getString`（**suspend**），
 * 在 `remember { … }` 块里两者都用不了，所以整条链是 composable，
 * 调用点直接 `buildXxx(refreshKey)`、不包 remember。
 *
 * 副作用是每次重组都会重算一遍。这些函数只读 MMKV + 资源，代价可忽略，
 * 好处是 Preferences 从别处被改时 UI 也能跟上（包 remember 就不会跟）。
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

/** 缓存占用摘要，纯文本、不做 HTML 解析。 */
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

/** 倒计时提醒秒数摘要，等于默认值 [PlayerDefaults.COUNTDOWN_SEC] 时加「(默认)」。 */
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
