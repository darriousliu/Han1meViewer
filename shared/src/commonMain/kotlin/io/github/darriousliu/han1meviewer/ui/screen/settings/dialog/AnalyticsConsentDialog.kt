package io.github.darriousliu.han1meviewer.ui.screen.settings.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import be.digitalia.compose.htmlconverter.htmlToAnnotatedString
import han1meviewer.shared.generated.resources.Res
import han1meviewer.shared.generated.resources.about_analytics
import han1meviewer.shared.generated.resources.about_analytics_summary
import han1meviewer.shared.generated.resources.deny
import han1meviewer.shared.generated.resources.ok
import org.jetbrains.compose.resources.stringResource

/**
 * 数据统计的退出确认。
 *
 * 原来这段用 `parseAsHtml()` + `AndroidView { TextView(movementMethod = LinkMovementMethod) }`
 * 渲染 `about_analytics_summary` 里的 `<br>` 和那一个 `<a href>`——那是整个
 * `HomeSettingsRoute` 里**唯一一处 View 互操作**。CMP 1.12 的 commonMain 没有
 * `AnnotatedString.fromHtml`，改用 htmlconverter（它把链接标成 `LinkAnnotation.Url`，
 * 点击默认交给 `UriHandler`）。
 *
 * [onDismissRequest] 故意为空：这个弹窗和原来一样不能点外部关掉，必须二选一。
 */
@Composable
fun AnalyticsConsentDialog(
    onAccept: () -> Unit,
    onDeny: () -> Unit,
) {
    val summary = stringResource(Res.string.about_analytics_summary)
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(Res.string.about_analytics)) },
        text = { Text(htmlToAnnotatedString(summary)) },
        confirmButton = {
            TextButton(onClick = onAccept) { Text(stringResource(Res.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDeny) { Text(stringResource(Res.string.deny)) }
        },
    )
}
