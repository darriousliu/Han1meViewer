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
