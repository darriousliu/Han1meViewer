@file:Suppress("DEPRECATION")

package io.github.darriousliu.han1meviewer.ui.navigation.settings

/*
 * 设置页里**真正的平台能力**——按第三节分类属 B/C 类，不迁 commonMain。
 *
 * 纯摘要文案（A 类）已拆到 commonMain 的 `SettingsSummaries.kt`；
 * 这个文件原名 `SettingsRouteUtils.kt`，改名是因为**两个源集不能有同名顶层文件**
 * （否则 `Duplicate JVM class name`），顺便名字也更诚实。
 */

import android.app.Activity
import android.app.AppOpsManager
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textview.MaterialTextView
import io.github.darriousliu.han1meviewer.core.storage.Preferences
import io.github.darriousliu.han1meviewer.R
import io.github.darriousliu.han1meviewer.core.storage.dao.download.HanimeDownloadDao
import io.github.darriousliu.han1meviewer.core.storage.SafFileManager.checkSafPermissions
import io.github.darriousliu.han1meviewer.core.storage.SafFileManager.migratePrivateToSaf
import io.github.darriousliu.han1meviewer.util.showAlertDialog
import io.github.darriousliu.han1meviewer.util.showLongToast
import io.github.darriousliu.han1meviewer.util.showShortToast

internal fun isDeviceSecureCompat(context: Context): Boolean {
    val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    return km.isDeviceSecure
}

internal fun isPipPermissionGranted(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
            Process.myUid(),
            context.packageName,
        )
        mode == AppOpsManager.MODE_ALLOWED
    } else {
        true
    }
}

internal fun openPipPermissionSettings(context: Context) {
    val intent = Intent(
        "android.settings.PICTURE_IN_PICTURE_SETTINGS",
        "package:${context.packageName}".toUri()
    )
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    context.startActivity(intent)
}

@RequiresApi(Build.VERSION_CODES.S)
internal fun showApplyDeepLinksDialog(context: Context, activity: Activity) {
    context.showAlertDialog {
        setTitle(R.string.apply_deep_links)
        setView(R.layout.dialog_apply_deep_links)
        setPositiveButton(R.string.go_to_settings) { _, _ ->
            try {
                val intent = Intent().apply {
                    action = Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS
                    addCategory(Intent.CATEGORY_DEFAULT)
                    data = "package:${context.packageName}".toUri()
                    flags =
                        Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                }
                activity.startActivity(intent)
            } catch (e: Exception) {
                showShortToast(R.string.action_app_open_by_default_settings_not_support)
                e.printStackTrace()
            }
        }
        setNegativeButton(R.string.cancel, null)
    }
}
