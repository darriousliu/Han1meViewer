package io.github.darriousliu.han1meviewer.core.storage.mmkv

import com.ctrip.flight.mmkv.MMKV_KMP
import com.ctrip.flight.mmkv.mmkvWithID
import kotlinx.serialization.serializer

/**
 * MMKV 命名空间的持有者。
 *
 * 本项目保留了迁移前三个 SharedPreferences 文件的边界，没有合并成一个库，
 * 对应关系见 [SettingsStore] / [AccountStore] / [MiscStore] 的注释。
 *
 * 委托默认用 **Kotlin 属性名**当 key（[MMKVProperty]），不再使用旧的下划线字符串。
 * 旧数据由 androidMain 的 `MmkvMigration` 做一次性映射导入。
 */
interface MMKVOwner {
    val id: String
    val kv: MMKV_KMP get() = mmkvWithID(id)
}

/** 原 `<packageName>_preferences`（PreferenceManager 默认 SP），全部设置项。 */
object SettingsStore : MMKVOwner {
    override val id: String get() = "han1me.settings"
}

/** 原 `<packageName>`（yenaly_libs 的 getSpValue/putSpValue 默认 SP），登录态与更新相关。 */
object AccountStore : MMKVOwner {
    override val id: String get() = "han1me.account"
}

/** 原 `setting_pref`，目前只有首页公告的 last dismiss time。 */
object MiscStore : MMKVOwner {
    override val id: String get() = "han1me.misc"
}

internal operator fun MMKV_KMP.set(key: String, value: ByteArray?): Boolean = if (value == null) {
    removeValueForKey(key)
    true
} else {
    set(key, value)
}

fun MMKVOwner.mmkvInt(default: Int = 0, key: String? = null) =
    MMKVProperty(this, MMKV_KMP::getInt, MMKV_KMP::set, default, key)

fun MMKVOwner.mmkvLong(default: Long = 0L, key: String? = null) =
    MMKVProperty(this, MMKV_KMP::getLong, MMKV_KMP::set, default, key)

fun MMKVOwner.mmkvBool(default: Boolean = false, key: String? = null) =
    MMKVProperty(this, MMKV_KMP::getBoolean, MMKV_KMP::set, default, key)

fun MMKVOwner.mmkvFloat(default: Float = 0f, key: String? = null) =
    MMKVProperty(this, MMKV_KMP::getFloat, MMKV_KMP::set, default, key)

fun MMKVOwner.mmkvDouble(default: Double = 0.0, key: String? = null) =
    MMKVProperty(this, MMKV_KMP::getDouble, MMKV_KMP::set, default, key)

fun MMKVOwner.mmkvString(default: String = "", key: String? = null) =
    MMKVProperty(this, MMKV_KMP::getString, MMKV_KMP::set, default, key)

fun MMKVOwner.mmkvStringSet(default: Set<String> = emptySet(), key: String? = null) =
    MMKVProperty(this, MMKV_KMP::getStringSet, MMKV_KMP::set, default, key)

fun MMKVOwner.mmkvBytes(default: ByteArray = byteArrayOf(), key: String? = null) =
    MMKVProperty(this, MMKV_KMP::getByteArray, MMKV_KMP::set, default, key)

/**
 * `""` 视作「未设置」的可空字符串，用于 `themeColor` / `safDownloadPath` 这类
 * 原本 SharedPreferences 里默认值就是 `null` 的项。
 */
fun MMKVOwner.mmkvNullableString(key: String? = null) = MMKVProperty<String?>(
    owner = this,
    decode = { k, _ -> getString(k, "").takeIf(String::isNotEmpty) },
    encode = { k, v -> if (v.isNullOrEmpty()) removeValueForKey(k).let { true } else set(k, v) },
    defaultValue = null,
    key = key,
)

inline fun <reified V : Any?> MMKVOwner.mmkvSerializable(defaultValue: V, key: String? = null) =
    MMKVSerializableProperty(this, serializer(), defaultValue, key)

fun <V> MMKVProperty<V>.asMutableStateFlow() = MMKVStateFlowProperty(this)

fun <V> MMKVSerializableProperty<V>.asMutableStateFlow() = MMKVStateFlowSerializableProperty(this)

fun MMKVOwner.clearAll() = kv.clearAll()
