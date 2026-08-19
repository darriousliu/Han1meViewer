package io.github.darriousliu.han1meviewer.feature.video.player

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Android 的 [PlayerEnvironment]：
 * 电量走 sticky 广播一次性读（控件每次展开时取一次，jzvd `setSystemTimeAndBattery`
 * 同款时机，不需要常驻 receiver）；计费网络看 `NET_CAPABILITY_NOT_METERED`。
 */
class AndroidPlayerEnvironment(private val context: Context) : PlayerEnvironment {

    override fun batteryPercent(): Int? {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return null
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        return if (level >= 0 && scale > 0) level * 100 / scale else null
    }

    override fun currentTimeText(): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

    override fun isNetworkMetered(): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }
}

@Composable
fun rememberAndroidPlayerEnvironment(): PlayerEnvironment {
    val context = LocalContext.current.applicationContext
    return remember(context) { AndroidPlayerEnvironment(context) }
}
