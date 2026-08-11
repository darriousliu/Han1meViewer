package com.yenaly.han1meviewer.platform

actual fun firebasePlatform(): FirebasePlatform = IosFirebasePlatform

private val IosFirebasePlatform = DefaultOnlyFirebasePlatform()
