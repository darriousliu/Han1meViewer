package io.github.darriousliu.han1meviewer.util

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import io.github.darriousliu.han1meviewer.core.common.util.requireComponentActivity

private val activityResultRequestId = AtomicInteger()

suspend fun <I, O> Context.awaitActivityResult(
    contract: ActivityResultContract<I, O>,
    input: I,
): O {
    val activity = requireComponentActivity()
    val lifecycle = activity.lifecycle
    val key = "han1me_rq#${activityResultRequestId.getAndIncrement()}"
    var launcher: ActivityResultLauncher<I>? = null
    val observer = object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if (event == Lifecycle.Event.ON_DESTROY) {
                launcher?.unregister()
                lifecycle.removeObserver(this)
            }
        }
    }

    return withContext(Dispatchers.Main) {
        lifecycle.addObserver(observer)
        suspendCoroutine { continuation ->
            var resumed = false
            launcher = activity.activityResultRegistry.register(key, contract) { result ->
                if (!resumed) {
                    resumed = true
                    launcher?.unregister()
                    lifecycle.removeObserver(observer)
                    continuation.resume(result)
                }
            }.apply { launch(input) }
        }
    }
}

suspend fun Context.requestPermission(permission: String): Boolean {
    if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
        return true
    }
    return awaitActivityResult(ActivityResultContracts.RequestPermission(), permission)
}
