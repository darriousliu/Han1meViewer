package io.github.darriousliu.han1meviewer.core.storage.mmkv

import com.ctrip.flight.mmkv.MMKV_KMP
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * MMKV 读写委托。
 *
 * key **默认取 Kotlin 属性名**，惯例上不要传 [key]。需要显式传的只有两种场合：
 * - [asMutableStateFlow]：委托拿到的 `property.name` 是外层属性名，
 *   `val fooStateFlow by ....asMutableStateFlow()` 会得到 key `"fooStateFlow"`，
 *   这时传 `key = "foo"` 让它和登记表里的名字对上；
 * - 一个属性由另一个派生属性对外暴露（如 `loginCookie` / `loginCookieStateFlow` 共用一份存储）。
 *
 * 与参考实现（PiPixiv2）的区别：owner 由工厂函数在**创建时**捕获，而不是靠 `thisRef`。
 * 因为 [io.github.darriousliu.han1meviewer.core.storage.Preferences] 一个对象要同时读写三个命名空间
 * （settings / account / misc），`ReadWriteProperty<MMKVOwner, V>` 那种写法做不到。
 */
class MMKVProperty<V>(
    private val owner: MMKVOwner,
    private val decode: MMKV_KMP.(String, V) -> V,
    private val encode: MMKV_KMP.(String, V) -> Boolean,
    private val defaultValue: V,
    private val key: String? = null,
) : ReadWriteProperty<Any?, V> {

    internal fun keyOf(property: KProperty<*>): String = key ?: property.name

    override fun getValue(thisRef: Any?, property: KProperty<*>): V =
        owner.kv.decode(keyOf(property), defaultValue)

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: V) {
        owner.kv.encode(keyOf(property), value)
    }
}
