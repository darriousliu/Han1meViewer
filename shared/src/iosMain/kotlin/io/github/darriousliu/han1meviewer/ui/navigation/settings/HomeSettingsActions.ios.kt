package io.github.darriousliu.han1meviewer.ui.navigation.settings

import androidx.compose.runtime.Composable

/**
 * 目前一个能力都还没落地，所以全走默认空实现——对应
 * `HomeSettingsCapabilities()` 全 false，那些设置项在这个平台上根本不显示。
 * 每落地一个能力，这里加一个 override、那边翻一个 flag。
 */
@Composable
actual fun rememberHomeSettingsActions(): HomeSettingsActions = NoopHomeSettingsActions
