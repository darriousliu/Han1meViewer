package io.github.darriousliu.han1meviewer

interface PermissionRequester {
    fun requestStoragePermission(
        onGranted: () -> Unit,
        onDenied: () -> Unit,
        onPermanentlyDenied: () -> Unit
    )
}