@file:JvmName("SharedPreferencesUtil")
@file:Suppress("UNCHECKED_CAST", "unused")

package com.yenaly.yenaly_libs.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * 创建SharedPreferences
 *
 * @param name sp名称
 * @param mode 模式
 */
private fun Context.sp(
    name: String = packageName,
    mode: Int = Context.MODE_PRIVATE
): SharedPreferences {
    return getSharedPreferences(name, mode)
}

/**
 * 把值存入SharedPreferences内
 *
 * @param Ace   泛型
 * @param key   储存键
 * @param value 储存值
 * @param name  sp名称
 */
@JvmOverloads
fun <Ace> putSpValue(
    key: String,
    value: Ace,
    name: String = applicationContext.packageName
) {
    applicationContext.sp(name = name).edit {
        when (value) {
            is Long -> putLong(key, value)
            is String -> putString(key, value)
            is Int -> putInt(key, value)
            is Boolean -> putBoolean(key, value)
            is Float -> putFloat(key, value)
            is Set<*> -> putStringSet(key, value.requireStringSet())
            else -> error("Unsupported SharedPreferences value type: ${value?.let { it::class }}")
        }
    }
}

/**
 * 把值从SharedPreferences取出
 *
 * @param Taffy   泛型
 * @param key     储存键
 * @param default 缺省值
 * @param name    sp名称
 *
 * @return 储存值
 */
@JvmOverloads
fun <Taffy> getSpValue(
    key: String,
    default: Taffy,
    name: String = applicationContext.packageName
): Taffy {
    return applicationContext.sp(name = name).run {
        val result = when (default) {
            is Long -> getLong(key, default)
            is String -> getString(key, default)
            is Int -> getInt(key, default)
            is Boolean -> getBoolean(key, default)
            is Float -> getFloat(key, default)
            is Set<*> -> getStringSet(key, default.requireStringSet())?.toSet() ?: default
            else -> error("Unsupported SharedPreferences value type: ${default?.let { it::class }}")
        }
        result as Taffy
    }
}

/**
 * 通过委托方式懒加载获取sp值
 *
 * @param Taffy   泛型
 * @param key     储存键
 * @param default 缺省值
 * @param name    sp名称
 */
@JvmOverloads
fun <Taffy> spValue(
    key: String,
    default: Taffy,
    name: String = applicationContext.packageName
) =
    lazy(LazyThreadSafetyMode.NONE) {
        getSpValue(key, default, name)
    }

/**
 * 删除sp内特定值
 *
 * @param key  储存键
 * @param name sp名称
 */
@JvmOverloads
fun removeSpValue(
    key: String,
    name: String = applicationContext.packageName
) {
    applicationContext.sp(name = name).edit { remove(key) }
}

/**
 * 清除sp的所有内容
 *
 * @param name sp名称
 */
@JvmOverloads
fun clearSharedPreferences(
    name: String = applicationContext.packageName
) {
    applicationContext.sp(name = name).edit { clear() }
}

private fun Set<*>.requireStringSet(): Set<String> {
    require(all { it is String }) { "SharedPreferences sets may only contain strings" }
    return filterIsInstanceTo(linkedSetOf())
}
