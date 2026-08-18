package io.github.darriousliu.han1meviewer.core.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val SpacingSmall = 4.dp
val SpacingNormal = 8.dp
val SpacingLarge = 16.dp

val VideoNormalCardMinWidth = 145.dp
val VideoSimplifiedCardMinWidth = 95.dp

val ArtistIconSize = 72.dp

// 视频卡片上「观看数 / 时间 / 时长」那一行。原本是 R.dimen.video_view_and_time_and_duration
// 和 R.dimen.view_view_and_time_icon_size，compose-resources 不支持 dimen，改成常量。
val VideoMetaTextSize = 12.sp
val VideoMetaIconSize = 14.dp