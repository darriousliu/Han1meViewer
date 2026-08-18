plugins {
    id("han1me.kmp.library")
}

/**
 * Firebase 的跨平台门面。
 *
 * commonMain 只有 `expect object Firebase`（读 Remote Config）。
 * Android 接真身；JVM 是空实现（桌面端没有 Firebase）；iOS 目前也是空实现，
 * 原因见 Firebase.ios.kt——swiftPM 不能挂在被依赖的库模块上。
 */
kotlin {
    android {
        namespace = "io.github.darriousliu.han1meviewer.core.firebase"
    }

    sourceSets {
        commonMain.dependencies {
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
