package io.github.darriousliu.han1meviewer.feature.settings.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.mikepenz.aboutlibraries.ui.compose.rememberLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.close
import io.github.darriousliu.han1meviewer.core.resource.open_source_license
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicenseDialog(
    onDismiss: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss, modifier = Modifier
            .fillMaxWidth()
            .sizeIn(maxHeight = 500.dp),
        properties = DialogProperties(),
        content = {
            Surface(
                shape = AlertDialogDefaults.shape,
                color = AlertDialogDefaults.containerColor,
                tonalElevation = AlertDialogDefaults.TonalElevation
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 24.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.open_source_license),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 16.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 24.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        // Android-only 的 produceLibraries(@RawRes Int) 换成 common 重载
                        // rememberLibraries { suspend () -> String }。json 由 :shared 的
                        // aboutLibraries { export } 生成到 composeResources/files/，
                        // 见 build.gradle.kts 里的注释。
                        val libraries by rememberLibraries {
                            Res.readBytes("files/aboutlibraries.json").decodeToString()
                        }

                        LibrariesContainer(
                            libraries = libraries,
                            modifier = Modifier,
                            libraryModifier = Modifier
                                .clip(MaterialTheme.shapes.large)
                                .background(MaterialTheme.colorScheme.surface),
                            dimensions = LibraryDefaults.libraryDimensions(
                                itemSpacing = 8.dp
                            ),
                            showDescription = true,
                            showFundingBadges = true
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(
                                text = stringResource(Res.string.close),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        })
}