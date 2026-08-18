package io.github.darriousliu.han1meviewer.convention

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

/**
 * precompiled script plugin 里没有类型安全的 `libs` 访问器，只能这样拿。
 */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

/** SDK 版本统一从根 `gradle.properties` 读，别在各模块硬编码。 */
internal val Project.compileSdkVersion: Int
    get() = property("compile.sdk").toString().toInt()

internal val Project.minSdkVersion: Int
    get() = property("min.sdk").toString().toInt()

internal val Project.targetSdkVersion: Int
    get() = property("target.sdk").toString().toInt()

/**
 * 建一个 android 和 jvm 共用的中间源集。
 *
 * 用途是那些「JVM 上有、iOS 上没有」的东西——最典型的是 OkHttp：
 * 自定义 DNS/DoH、`ProxySelector`、磁盘缓存、限速这些 Ktor 没有对应物的能力
 * 只能直接用 okhttp 的 API，而 android 和 jvm 两端可以共用同一份实现。
 *
 * 调用点在 `kotlin { }` 块里：`val androidJvmMain = createAndroidJvmMain()`。
 */
fun KotlinMultiplatformExtension.createAndroidJvmMain(): KotlinSourceSet {
    val androidJvmMain = sourceSets.create("androidJvmMain") {
        dependsOn(sourceSets.getByName("commonMain"))
    }
    sourceSets.getByName("androidMain").dependsOn(androidJvmMain)
    sourceSets.getByName("jvmMain").dependsOn(androidJvmMain)
    return androidJvmMain
}
