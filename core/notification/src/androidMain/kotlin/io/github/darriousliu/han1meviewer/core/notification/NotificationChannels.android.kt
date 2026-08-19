package io.github.darriousliu.han1meviewer.core.notification

import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import io.github.darriousliu.han1meviewer.core.common.util.applicationContext

actual fun createAppNotificationChannels() {
    val nm = NotificationManagerCompat.from(applicationContext)

    val hanimeDownloadChannel = NotificationChannelCompat.Builder(
        DOWNLOAD_NOTIFICATION_CHANNEL,
        NotificationManagerCompat.IMPORTANCE_HIGH
    ).setName("Hanime Download").build()
    nm.createNotificationChannel(hanimeDownloadChannel)

    val appUpdateChannel = NotificationChannelCompat.Builder(
        UPDATE_NOTIFICATION_CHANNEL,
        NotificationManagerCompat.IMPORTANCE_HIGH
    ).setName("App Update").build()
    nm.createNotificationChannel(appUpdateChannel)
}
