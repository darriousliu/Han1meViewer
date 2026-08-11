package com.yenaly.han1meviewer.platform

actual fun firebasePlatform(): FirebasePlatform = JvmFirebasePlatform

private val JvmFirebasePlatform = DefaultOnlyFirebasePlatform()
