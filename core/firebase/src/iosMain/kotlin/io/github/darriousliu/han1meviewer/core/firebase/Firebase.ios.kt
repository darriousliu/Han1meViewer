package io.github.darriousliu.han1meviewer.core.firebase

// ⚠️ 暂时与 JVM 同款空实现。swiftPM import 的 convert 任务在现有环境上
// 始终失败("No clang call discovered"),产出的 cinterop klib 是空壳,
// 引用 FIRRemoteConfig 的真实现从未编译通过。等有完整 Xcode 的环境把
// cinterop 链跑通后,恢复成:
//   FIRRemoteConfig.remoteConfig().configValueForKey(key).boolValue/…
// (还需宿主 App 调 FirebaseApp.configure())。
actual object Firebase {
    actual fun getBoolean(key: String): Boolean = false

    actual fun getString(key: String): String = ""

    actual fun getLong(key: String): Long = 0L

    actual fun getDouble(key: String): Double = 0.0
}
