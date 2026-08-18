package io.github.darriousliu.han1meviewer.core.firebase

/**
 * iOS 侧暂时是空实现，取到的都是默认值（Remote Config 的开关会全部为关）。
 *
 * 原来这里直接 `swiftPMImport.Han1meViewer.shared.FIRRemoteConfig`，但那个包名
 * 绑在 :shared 的 swiftPM 声明上；而 swiftPM 一旦挂到被依赖的库模块上，
 * 依赖方解析元数据时就会拉起 dumpXcodebuildArgs*，在没装完整 Xcode 的机器上
 * 连 jvm 编译都跑不了（见 :shared/build.gradle.kts 里那段说明）。
 *
 * 加上本机跑不了 iOS 任务，那份实现从来没被编译验证过。等 iOS 有真实入口时，
 * 连同 umbrella 的 swiftPM 声明一起重做——届时要么把 Remote Config 的读取
 * 收到 umbrella 再往下传，要么给这个模块单独想办法。
 */
actual object Firebase {
    actual fun getBoolean(key: String): Boolean = false

    actual fun getString(key: String): String = ""

    actual fun getLong(key: String): Long = 0L

    actual fun getDouble(key: String): Double = 0.0
}
