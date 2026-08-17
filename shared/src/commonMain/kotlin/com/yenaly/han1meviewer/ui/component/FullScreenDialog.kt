package com.yenaly.han1meviewer.ui.component

import androidx.compose.ui.window.DialogProperties

/**
 * 全屏沉浸弹窗（图片查看器）的 [DialogProperties]。
 *
 * Android 上要靠 `decorFitsSystemWindows = false` 才能把内容画到系统栏下面，
 * 再由弹窗内容自己用 `statusBarsPadding()` / `navigationBarsPadding()` 让位；
 * 但 CMP common 的 [DialogProperties] 没有这个字段（只有 `dismissOnBackPress` /
 * `dismissOnClickOutside` / `usePlatformDefaultWidth`），所以做成 expect/actual。
 *
 * 光去掉那个字段而留着两个 padding 会双重留白，比原样更糟。
 */
expect fun fullScreenDialogProperties(): DialogProperties
