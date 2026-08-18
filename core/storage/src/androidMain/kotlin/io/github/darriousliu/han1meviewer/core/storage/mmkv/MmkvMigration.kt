package io.github.darriousliu.han1meviewer.core.storage.mmkv

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import co.touchlab.kermit.Logger
import io.github.darriousliu.han1meviewer.core.storage.Preferences

/**
 * 标记位存在 [SettingsStore] 里。它不是配置项，不会被 `Preferences.exportSettings()` 导出。
 */
private const val MIGRATED_FLAG = "__migratedFromSharedPreferences"

private const val LEGACY_MISC_SP_NAME = "setting_pref"

private val logger = Logger.withTag("MmkvMigration")

/**
 * 把三个旧 SharedPreferences 文件一次性搬进 MMKV。
 *
 * 不能用 `MMKV.importFromSharedPreferences`——它是原样复制 key 的，换不了名字。
 *
 * **旧 SP 文件不删**，留一版作回滚余地。
 */
fun migrateSharedPreferencesToMMKV(context: Context) {
    if (SettingsStore.kv.getBoolean(MIGRATED_FLAG, false)) return

    val migrated = migrate(
        sp = PreferenceManager.getDefaultSharedPreferences(context),
        owner = SettingsStore,
        keys = LegacyPreferenceKeys.settings,
    ) + migrate(
        sp = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE),
        owner = AccountStore,
        keys = LegacyPreferenceKeys.account,
    ) + migrate(
        sp = context.getSharedPreferences(LEGACY_MISC_SP_NAME, Context.MODE_PRIVATE),
        owner = MiscStore,
        keys = LegacyPreferenceKeys.misc,
    )

    SettingsStore.kv.set(MIGRATED_FLAG, true)
    logger.i { "migrated $migrated entries from SharedPreferences to MMKV" }
}

private fun migrate(sp: SharedPreferences, owner: MMKVOwner, keys: Map<String, String>): Int {
    var count = 0
    sp.all.forEach { (legacyKey, value) ->
        val name = keys[legacyKey] ?: return@forEach
        if (value == null) return@forEach
        val written = if (legacyKey in LegacyPreferenceKeys.stringToFloat) {
            (value as? String)?.toFloatOrNull()?.let { owner.kv.set(name, it) }
        } else when (value) {
            is Boolean -> owner.kv.set(name, value)
            is Int -> owner.kv.set(name, value)
            is Long -> owner.kv.set(name, value)
            is Float -> owner.kv.set(name, value)
            is String -> owner.kv.set(name, value)
            else -> null
        }
        if (written == true) {
            count++
        } else {
            logger.w { "cannot migrate \"$legacyKey\" -> $name: unexpected value $value" }
        }
    }
    return count
}
