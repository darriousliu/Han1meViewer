@file:Suppress("UnstableApiUsage")
@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import io.github.darriousliu.han1meviewer.convention.createAndroidJvmMain
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    // targets 与编译选项在 convention 里；依赖全在下面的 sourceSets 自己声明
    id("han1me.kmp.compose")
    alias(libs.plugins.org.jetbrains.kotlin.plugin.parcelize)
    alias(libs.plugins.org.jetbrains.kotlin.plugin.serialization)
    alias(libs.plugins.aboutlibraries)
    alias(libs.plugins.ben.manes)
}

// plugin 是跟着「app 逻辑搬进 shared」那次带过来的，但配置块没带过来，
// 结果 :shared 产出的 aboutlibraries.json 一直是空的
// （{"libraries":[],"licenses":{}}，30 字节），开源许可列表什么都不显示。
// app/build 下那份 148KB 是 plugin 还在 :app 时的陈旧产物，现在不会再生成。
//
// 导到 composeResources 而不是 androidMain/res/raw：LicenseDialog 在 commonMain 里
// 用 Res.readBytes 读它。**plugin 生成的那份 androidMain/res/raw/aboutlibraries.json
// 在 KMP 模块下收集不到东西，仍然是 30 字节的空壳，别再依赖它。**
//
// ⚠️ exportLibraryDefinitions 不参与常规构建，是手动任务。依赖有增减之后要重跑：
//     ./gradlew :shared:exportLibraryDefinitions
// 生成的 json 是提交进版本库的，不重跑就会过期。
aboutLibraries {
    collect {
        all = true
    }
    export {
        outputFile = rootProject.file(
            "core/resource/src/commonMain/composeResources/files/aboutlibraries.json"
        )
        prettyPrint = false
    }
}


kotlin {
    android {
        namespace = "io.github.darriousliu.han1meviewer"

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        optimization {
            consumerKeepRules.apply {
                publish = true
                file("androidMain/keepRules/rules.keep")
            }
        }
        compilerOptions {
            freeCompilerArgs.addAll(
                "-P",
                "plugin:org.jetbrains.kotlin.parcelize:additionalAnnotation=io.github.darriousliu.han1meviewer.core.common.util.Parcelize",
            )
        }
    }

    // targets（android / jvm / iosArm64 / iosSimulatorArm64）与 applyDefaultHierarchyTemplate
    // 都由 han1me.kmp.library 建好，这里只补 iOS 的 framework 产物。
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
            binaryOption("bundleId", "io.github.darriousliu.han1meviewer.shared")
        }
    }


    // Firebase 的 iOS SDK 走 Kotlin 2.4 内置的 swiftPM 集成。
    //
    // ⚠️ 这段**不能**下沉到 :core:firebase。swiftPM 会给模块挂上
    // dumpXcodebuildArgs* 任务，而依赖方解析该模块的 KMP 元数据时会拉起它们——
    // 于是在没装完整 Xcode 的机器上，连 :shared:compileKotlinJvm 都会被
    // "xcodebuild requires Xcode" 打断。放在 umbrella 里只影响 umbrella 自己。
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
            // convention 不声明依赖，基础几组都在这
            implementation(libs.bundles.compose)
            implementation(libs.bundles.lifecycle)
            implementation(libs.bundles.koin)
            implementation(libs.bundles.koin.compose)
            implementation(libs.coroutines.core)
            implementation(libs.serialization.json)
            implementation(libs.datetime)
            implementation(libs.kermit)

            // Res 出现在很多公开签名里（StringResource / DrawableResource 参数），
            // 用 api 传递出去，消费方不用各自再声明一遍。
            api(project(":core:resource"))
            // 异常、状态、格式化这些类型遍布公开签名，用 api 传下去
            api(project(":core:common"))
            // 模型类遍布公开签名
            api(project(":core:model"))
            // Preferences / Room 实体遍布公开签名
            api(project(":core:storage"))
            implementation(project(":core:parse"))
            // Firebase 门面（Android 接真身，JVM 空实现）
            api(project(":core:firebase"))
            // HttpClient / 各 service 接口遍布公开签名
            api(project(":core:network"))
            // 各 Repo 遍布 ViewModel 的公开签名
            api(project(":core:repository"))
            // 路由类型遍布各 route 与屏幕的签名
            api(project(":core:navigation"))
            // 公共组件/主题遍布各屏幕
            api(project(":core:ui"))
            // 通知门面（渠道 id 被 worker 用）
            implementation(project(":core:notification"))

            // feature 模块（拆出去的域；MainNavDisplay/AppModule 还要引它们）
            api(project(":feature:checkin"))
            api(project(":feature:history"))
            api(project(":feature:subscription"))
            api(project(":feature:comment"))
            api(project(":feature:preview"))
            api(project(":feature:mylist"))
            api(project(":feature:download"))
            api(project(":feature:search"))
            implementation(libs.kotlinx.io.core)
            implementation(libs.ksoup)
            implementation(libs.htmlconverter)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.mp.stools)
            implementation(libs.aboutlibraries.core)
            implementation(libs.aboutlibraries.compose.m3)
            implementation(libs.sonner)
            implementation(libs.composewebview)
            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs.compose)
        }

        getByName("androidHostTest").dependencies {
            implementation(libs.junit)
            implementation(libs.koin.test)
        }

        getByName("androidDeviceTest").dependencies {
            implementation(project.dependencies.platform(libs.compose.compose.bom))
            implementation(libs.androidx.ui.test.junit4)
            implementation(libs.test.junit)
            implementation(libs.test.espresso.core)
        }

        androidMain.dependencies {
            implementation(libs.koin.android)
            implementation(libs.appcompat)
            implementation(libs.androidx.window)
            implementation(libs.androidx.window.java)
            implementation(libs.androidx.biometric)
            implementation(libs.androidx.core.splashscreen)
            implementation(libs.androidx.swiperefreshlayout)
            implementation(libs.androidx.material.icons.extended)
            // android related

            implementation(libs.bundles.android.base)
            implementation(libs.bundles.android.jetpack)
            implementation(libs.palette)
            implementation(libs.material)
            //compose
            implementation(project.dependencies.platform(libs.compose.compose.bom))
            implementation(libs.compose.ui.graphics)
            implementation(libs.compose.material3)
            implementation(libs.androidx.activity.compose)
            implementation(libs.compose.ui.ui.tooling.preview)
            implementation(libs.androidx.ui)

            implementation(libs.navigation3.ui.androidx)
            implementation(libs.androidx.material.icons.core)
            // parse


            // network

            // Coil 2 的 ImageLoader 还直接用 OkHttpClient（HImageMeower），
            // 其余网络请求都走 commonMain 的 Ktor，okhttp 由 androidJvmMain 带下来。
            // coil3 的图片请求也走 Ktor（commonMain 的 coil-network-ktor3）。

            // pic

            implementation(libs.coil)


            // video

            implementation(libs.jiaozi.video.player)
            implementation(libs.media3.exoplayer)
            implementation(libs.media3.exoplayer.hls)
            implementation(libs.media3.ui.compose)
            implementation(libs.mpv.lib)

            // view

            implementation(libs.multitype)
            implementation(libs.base.recyclerview.adapter.helper4)
            implementation(libs.expandable.textview)
            implementation(libs.spannable.x)
            implementation(libs.about)
            implementation(libs.circular.reveal.switch)
            implementation(libs.drawerlayout)

            // firebase

            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.analytics)
            implementation(libs.firebase.crashlytics)
            implementation(libs.firebase.perf)
            implementation(libs.firebase.config)

            implementation(libs.firebase.database)

            implementation(libs.crashx)
            // debugImplementation(libs.leak.canary)
        }

        getByName("iosMain").dependencies {
            implementation(libs.ktor.client.darwin)
        }

        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }

    }

    // android + jvm 共用的中间源集（helper 在 convention 里，见 KotlinMultiplatform.kt）
    createAndroidJvmMain().dependencies {
        implementation(libs.ktor.client.okhttp)
        // OkHttp engine 上那些 Ktor 没有对应物的能力（自定义 DNS/DoH、ProxySelector、
        // 磁盘缓存、Throttler 限速）要直接用 okhttp 的 API。
        implementation(libs.okhttp)
        implementation(libs.okhttp.dns.over.https)
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.ui.ui.tooling)

    // Koin 的注解处理器（@Module/@KoinViewModel → 生成 module）。只挂 commonMain
    // metadata，生成一次各目标共用；任务排序在 han1me.kmp.library 里。
    add("kspCommonMainMetadata", libs.koin.ksp.compiler)

    coreLibraryDesugaring(libs.desugar.jdk.libs)
}


// KSP 的源集挂载与任务排序在 han1me.kmp.library 里（Koin/Ktorfit 共用同一套接线）。
// composeResources 与 generateSharedHKeyframeIndex 都在 :core:resource。
