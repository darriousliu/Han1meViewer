@file:Suppress("UnstableApiUsage")
@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.INT
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import io.github.darriousliu.han1meviewer.convention.Config
import io.github.darriousliu.han1meviewer.convention.Config.Version.createVersion
import io.github.darriousliu.han1meviewer.convention.createAndroidJvmMain
import io.github.darriousliu.han1meviewer.convention.Config.Version.source
import io.github.darriousliu.han1meviewer.convention.Config.isRelease
import io.github.darriousliu.han1meviewer.convention.Config.lastCommitSha
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    // targets / 编译选项 / Compose 那圈公共依赖都在 convention 里
    id("han1me.kmp.compose")
    alias(libs.plugins.org.jetbrains.kotlin.plugin.parcelize)
    alias(libs.plugins.org.jetbrains.kotlin.plugin.serialization)
    alias(libs.plugins.ktorfit)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.aboutlibraries)
    alias(libs.plugins.ben.manes)
    alias(libs.plugins.buildkonfig)
}

val releaseBuild = isRelease
val commitSha = if (releaseBuild) lastCommitSha else "b8eace8"
val githubToken = System.getenv("HA_GITHUB_TOKEN") ?: rootProject
    .file("app/ha1_github_token.txt")
    .takeIf { it.isFile }
    ?.readText()
    .orEmpty()
val (versionCode, versionName) = createVersion(major = 1, minor = 0, patch = 2)
val applicationId = "io.github.darriousliu.han1meviewer${if (releaseBuild) "" else ".debug"}"

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

buildkonfig {
    packageName = "io.github.darriousliu.han1meviewer"
    exposeObjectWithName = "BuildConfig"

    defaultConfigs {
        buildConfigField(
            BOOLEAN, "DEBUG", (!releaseBuild).toString(),
            nullable = false,
            const = true
        )
        buildConfigField(STRING, "APPLICATION_ID", applicationId, nullable = false, const = true)
        buildConfigField(STRING, "COMMIT_SHA", commitSha, nullable = false, const = true)
        buildConfigField(STRING, "VERSION_NAME", versionName, nullable = false, const = true)
        buildConfigField(
            INT, "VERSION_CODE", versionCode.toString(),
            nullable = false,
            const = true
        )
        buildConfigField(STRING, "HA_GITHUB_TOKEN", githubToken, nullable = false, const = true)
        buildConfigField(STRING, "VERSION_SOURCE", source, nullable = false, const = true)
        buildConfigField(
            INT, "SEARCH_YEAR_RANGE_END", Config.thisYear.toString(), nullable = false,
            const = true
        )
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
                "plugin:org.jetbrains.kotlin.parcelize:additionalAnnotation=io.github.darriousliu.han1meviewer.util.Parcelize",
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
        // 下面这些由 convention 提供，不再重复声明：
        // coroutines-core / serialization-json / datetime / kermit（han1me.kmp.library）
        // compose runtime|ui|foundation|backhandler|tooling-preview / compose-resources /
        // jetbrains material3|icons-core / lifecycle viewmodel|savedstate|compose|runtime-compose
        //   （han1me.kmp.compose）
        commonMain.dependencies {
            // Res 出现在很多公开签名里（StringResource / DrawableResource 参数），
            // 用 api 传递出去，消费方不用各自再声明一遍。
            api(project(":core:resource"))
            implementation(libs.kotlinx.io.core)
            implementation(libs.ksoup)
            implementation(libs.mmkv.kotlin)
            implementation(libs.htmlconverter)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktorfit.lib.light)
            implementation(libs.mp.stools)
            implementation(libs.aboutlibraries.core)
            implementation(libs.aboutlibraries.compose.m3)
            implementation(libs.sonner)
            implementation(libs.composewebview)
            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)
            implementation(libs.sqlite.async)
            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs.compose)
            implementation(libs.navigation3.ui.jb)
            implementation(libs.lifecycle.viewmodel.navigation3)
        }

        getByName("androidHostTest").dependencies {
            implementation(libs.junit)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.test)
        }

        getByName("androidDeviceTest").dependencies {
            implementation(project.dependencies.platform(libs.compose.compose.bom))
            implementation(libs.androidx.ui.test.junit4)
            implementation(libs.test.junit)
            implementation(libs.test.espresso.core)
        }

        androidMain.dependencies {
            implementation(libs.appcompat)
            implementation(libs.mmkv)
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

    // Ktorfit 的 Gradle 插件只注册 compiler-plugin（负责把 ktorfit.create<T>() 重写成
    // createT()），真正生成实现类的 KSP 处理器要自己加。生成一次进 commonMain metadata，
    // 各目标共用。
    add("kspCommonMainMetadata", libs.ktorfit.ksp)

    add("kspAndroid", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
    add("kspJvm", libs.room.compiler)

    coreLibraryDesugaring(libs.desugar.jdk.libs)
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

// KSP 的源集挂载与任务排序在 han1me.kmp.library 里（Koin/Ktorfit 共用同一套接线）。
// composeResources 与 generateSharedHKeyframeIndex 都在 :core:resource。
