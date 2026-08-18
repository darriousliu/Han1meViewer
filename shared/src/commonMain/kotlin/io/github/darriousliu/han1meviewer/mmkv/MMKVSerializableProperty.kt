package io.github.darriousliu.han1meviewer.mmkv

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private val mmkvJson = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
    isLenient = true
}

/**
 * 可序列化类型的 MMKV 委托，统一以 JSON 字符串落盘。
 *
 * 不走 protobuf——参考实现里的 protobuf 分支和 `_is_json` 标记位是那个项目自己的历史包袱，
 * 本项目是全新写入，不需要兼容。
 */
class MMKVSerializableProperty<V>(
    private val owner: MMKVOwner,
    private val serializer: KSerializer<V>,
    private val defaultValue: V,
    private val key: String? = null,
) : ReadWriteProperty<Any?, V> {

    override fun getValue(thisRef: Any?, property: KProperty<*>): V {
        val json = owner.kv.getString(key ?: property.name, "")
        if (json.isEmpty()) return defaultValue
        return runCatching { mmkvJson.decodeFromString(serializer, json) }.getOrDefault(defaultValue)
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: V) {
        val k = key ?: property.name
        if (value == null) {
            owner.kv.removeValueForKey(k)
        } else {
            owner.kv.set(k, mmkvJson.encodeToString(serializer, value))
        }
    }
}
