package com.yenaly.han1meviewer.platform

actual fun fileAccess(): FileAccess = UnsupportedFileAccess

actual fun backgroundJobScheduler(): BackgroundJobScheduler = UnsupportedBackgroundJobScheduler
