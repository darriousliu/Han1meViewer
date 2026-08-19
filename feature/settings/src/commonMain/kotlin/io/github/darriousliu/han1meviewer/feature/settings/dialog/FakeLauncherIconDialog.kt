package io.github.darriousliu.han1meviewer.feature.settings.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.app_name_fake_calc
import io.github.darriousliu.han1meviewer.core.resource.app_name_fake_cornhub
import io.github.darriousliu.han1meviewer.core.resource.app_name_fake_xxt
import io.github.darriousliu.han1meviewer.core.resource.fake_app_icon
import io.github.darriousliu.han1meviewer.core.resource.hanime_app_name
import io.github.darriousliu.han1meviewer.core.resource.ic_launcher_calc
import io.github.darriousliu.han1meviewer.core.resource.ic_launcher_cornhub
import io.github.darriousliu.han1meviewer.core.resource.ic_launcher_new
import io.github.darriousliu.han1meviewer.core.resource.ic_launcher_xxt
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * 一个可选的 App 图标。
 *
 * [alias] 是 Android 那边 `activity-alias` 的类名，**必须和 `AndroidManifest.xml`
 * 以及 `HanimeApplication.switchLauncher` 里的那份列表逐字一致**。
 * 它作为纯数据留在 commonMain（用户定的：「commonMain 中设置的数据是全的」），
 * 真正去切图标的能力在 `HomeSettingsActions.switchLauncherIcon`。
 */
data class LauncherIconOption(
    val name: StringResource,
    val icon: DrawableResource,
    val alias: String,
)

val launcherIconOptions: List<LauncherIconOption> = listOf(
    LauncherIconOption(
        name = Res.string.hanime_app_name,
        icon = Res.drawable.ic_launcher_new,
        alias = "io.github.darriousliu.han1meviewer.LauncherAliasDefault",
    ),
    LauncherIconOption(
        name = Res.string.app_name_fake_calc,
        icon = Res.drawable.ic_launcher_calc,
        alias = "io.github.darriousliu.han1meviewer.LauncherFakeCalc",
    ),
    LauncherIconOption(
        name = Res.string.app_name_fake_cornhub,
        icon = Res.drawable.ic_launcher_cornhub,
        alias = "io.github.darriousliu.han1meviewer.LauncherFakeCornhub",
    ),
    LauncherIconOption(
        name = Res.string.app_name_fake_xxt,
        icon = Res.drawable.ic_launcher_xxt,
        alias = "io.github.darriousliu.han1meviewer.LauncherFakeXxt",
    ),
)

@Composable
fun FakeLauncherIconDialog(
    onSelect: (LauncherIconOption) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(Res.string.fake_app_icon),
                    style = MaterialTheme.typography.titleLarge,
                )
                launcherIconOptions.forEach { option ->
                    TextButton(
                        onClick = { onSelect(option) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                painter = painterResource(option.icon),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(30.dp),
                            )
                            Text(stringResource(option.name))
                        }
                    }
                }
            }
        }
    }
}
