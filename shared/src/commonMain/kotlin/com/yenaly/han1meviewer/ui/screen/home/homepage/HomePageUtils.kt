package com.yenaly.han1meviewer.ui.screen.home.homepage

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * 将首页分类转换为高级搜索请求参数。
 *
 * 仅写入分类中存在的参数，避免向搜索页传递空值。
 *
 * @receiver 首页分类数据
 * @return 可直接用于高级搜索的参数映射
 */
internal fun HomeCategory.toAdvancedSearchParams(): Map<String, String> = buildMap {
    genre?.let { put("genre", it) }
    sort?.let { put("sort", it) }
    tags?.let { put("tags", it) }
}

/* yyyy-MM-dd HH:mm:ss，比 LOCAL_DATE_TIME_FORMAT 多一个秒 */
private val ANNOUNCEMENT_TIME_FORMAT = LocalDateTime.Format {
    date(LocalDate.Formats.ISO); char(' ')
    hour(); char(':'); minute(); char(':'); second()
}

/**
 * 将公告秒级时间戳格式化为本地时间字符串。
 *
 * @param timestamp 秒级 Unix 时间戳。
 * @return 本地日期时间字符串。
 */
@OptIn(ExperimentalTime::class)
fun formatTimestamp(timestamp: Long): String =
    Instant.fromEpochSeconds(timestamp)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .format(ANNOUNCEMENT_TIME_FORMAT)
