@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    id("han1me.kmp.library")
}

/**
 * Firebase 的跨平台门面。
 *
 * commonMain 只有 `expect object Firebase`（读 Remote Config）。
 * Android 接真身；JVM 是空实现（桌面端没有 Firebase）；iOS 经下方 swiftPM 链
 * Analytics/Crashlytics/RemoteConfig，actual 是真实现——但要生效还需宿主 App
 * 调 `FirebaseApp.configure()`（iosApp 目前还没接）。
 */
kotlin {
    android {
        namespace = "io.github.darriousliu.han1meviewer.core.firebase"
    }

    swiftPMDependencies {
        iosMinimumDeploymentTarget.set("15.0")
        swiftPackage(
            url = url("https://github.com/firebase/firebase-ios-sdk.git"),
            version = exact(libs.versions.firebaseApple.get()),
            products = listOf(
                product("FirebaseAnalytics"),
                product("FirebaseCrashlytics"),
                product("FirebaseRemoteConfig")
            ),
        )
    }

    sourceSets {
        commonMain.dependencies {
            // convention 不声明依赖，基础几组在模块里自己写
            implementation(libs.coroutines.core)
            implementation(libs.kermit)
            api(project(":core:common"))
        }

        androidMain.dependencies {
            // BOM 要用 api：下面几个制品是 api 暴露出去的，消费方解析它们时
            // 也得看得见 BOM，否则报 "Could not find com.google.firebase:firebase-analytics:"
            // （版本号是空的）。
            api(project.dependencies.platform(libs.firebase.bom))
            api(libs.firebase.analytics)
            api(libs.firebase.crashlytics)
            api(libs.firebase.config)
            api(libs.firebase.database)
            implementation(libs.firebase.perf)
            implementation(libs.fragment)
        }
    }
}

// swiftPM import 的 dump→convert→cinterop 三层任务要完整 Xcode(CommandLineTools
// 的 xcodebuild 是必失败的 stub),而 HMPP 的 metadata transform 链让**所有模块的
// 一切编译**都在它们下游——dump 一失败,全部 Kotlin 编译被静默跳过,构建假绿。
// 没有完整 Xcode 的机器上把这三层直接 SKIP,下游改用上一次成功构建留在 build/
// 里的输出(klib/def/ld dump)。有 Xcode 的机器不受影响。
// ⚠️ 全新 clone 且无 Xcode 时没有残留输出,依旧编不过 iOS 侧——预期内。
val xcodeDeveloperDir = providers.exec {
    commandLine("/usr/bin/xcode-select", "-p")
    isIgnoreExitValue = true
}.standardOutput.asText.map { it.trim() }

tasks.matching {
    it.name.startsWith("dumpXcodebuildArgs") ||
        it.name.startsWith("convertSyntheticImportProjectIntoDefFile") ||
        it.name.startsWith("cinteropSwiftPMImport")
}.configureEach {
    onlyIf("full Xcode is required; reuse previous outputs otherwise") {
        xcodeDeveloperDir.get().contains("Xcode.app")
    }
}
