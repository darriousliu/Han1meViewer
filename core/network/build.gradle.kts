import io.github.darriousliu.han1meviewer.convention.createAndroidJvmMain

plugins {
    id("han1me.kmp.compose")
    alias(libs.plugins.org.jetbrains.kotlin.plugin.serialization)
    alias(libs.plugins.ktorfit)
}

kotlin {
    android {
        namespace = "io.github.darriousliu.han1meviewer.core.network"
    }

    sourceSets {
        commonMain.dependencies {
            // convention 不声明依赖，基础几组在模块里自己写
            implementation(libs.bundles.compose)
            implementation(libs.coroutines.core)
            implementation(libs.serialization.json)
            implementation(libs.datetime)
            implementation(libs.kermit)
            api(project(":core:common"))
            api(project(":core:model"))
            implementation(project(":core:storage"))
            implementation(project(":core:parse"))
            // 公告的 Android 数据源是 Firebase Realtime Database
            implementation(project(":core:firebase"))

            api(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktorfit.lib.light)
            implementation(libs.filekit.core)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }

    // DoH / ProxySelector / 磁盘缓存 / 限速这些 Ktor 没有对应物的能力只能直接用
    // okhttp 的 API，android 和 jvm 共用一份实现。
    createAndroidJvmMain().dependencies {
        implementation(libs.ktor.client.okhttp)
        implementation(libs.okhttp)
        implementation(libs.okhttp.dns.over.https)
    }
}

dependencies {
    // Ktorfit 的 Gradle 插件只注册 compiler-plugin（负责把 ktorfit.create<T>() 重写成
    // createT()），真正生成实现类的 KSP 处理器要自己加。
    add("kspCommonMainMetadata", libs.ktorfit.ksp)
}
