@file:Suppress("UnstableApiUsage")
@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import Config.Version.createVersion
import Config.Version.source
import Config.isRelease
import Config.lastCommitSha
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.INT
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.org.jetbrains.kotlin.plugin.parcelize)
    alias(libs.plugins.org.jetbrains.kotlin.plugin.serialization)
    alias(libs.plugins.ktorfit)
    alias(libs.plugins.com.google.devtools.ksp)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
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
val applicationId = "com.yenaly.han1meviewer${if (releaseBuild) "" else ".debug"}"

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
        outputFile = file("src/commonMain/composeResources/files/aboutlibraries.json")
        prettyPrint = false
    }
}

buildkonfig {
    packageName = "com.yenaly.han1meviewer"
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
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    android {
        namespace = "com.yenaly.han1meviewer"
        compileSdk = property("compile.sdk").toString().toInt()
        minSdk = property("min.sdk").toString().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
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
                "plugin:org.jetbrains.kotlin.parcelize:additionalAnnotation=com.yenaly.han1meviewer.util.Parcelize",
            )
        }
    }

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    val appleTargets = listOf(
        iosArm64(),
        iosSimulatorArm64(),
    )

    appleTargets.forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
            binaryOption("bundleId", "com.yenaly.han1meviewer.shared")
        }
    }

    applyDefaultHierarchyTemplate()

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
            implementation(libs.kotlinx.io.core)
            implementation(libs.ksoup)
            implementation(libs.mmkv.kotlin)
            implementation(libs.compose.multiplatform.runtime)
            implementation(libs.compose.multiplatform.ui)
            implementation(libs.compose.multiplatform.foundation)
            implementation(libs.compose.resources)
            implementation(libs.jetbrains.compose.material3)
            implementation(libs.compose.multiplatform.ui.backhandler)
            implementation(libs.compose.multiplatform.ui.tooling.preview)
            implementation(libs.jetbrains.compose.material.icons.core)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
            implementation(libs.lifecycle.viewmodel)
            implementation(libs.lifecycle.viewmodel.savedstate)
            implementation(libs.lifecycle.runtime.compose)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.coroutines.core)
            implementation(libs.datetime)
            implementation(libs.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktorfit.lib.light)
            implementation(libs.kermit)
            implementation(libs.mp.stools)
            implementation(libs.aboutlibraries.core)
            implementation(libs.aboutlibraries.compose.m3)
            implementation(libs.sonner)
            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)
            implementation(libs.sqlite.async)
            implementation(libs.filekit.core)
        }

        getByName("androidHostTest").dependencies {
            implementation(libs.junit)
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

            implementation(libs.androidx.navigation.compose)
            implementation(libs.androidx.material.icons.core)
            implementation(libs.compose.avatar.cropper)
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

        val androidJvmMain = create("androidJvmMain") {
            dependsOn(commonMain.get())
        }
        androidJvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            // OkHttp engine 上那些 Ktor 没有对应物的能力（自定义 DNS/DoH、ProxySelector、
            // 磁盘缓存、Throttler 限速）要直接用 okhttp 的 API。
            implementation(libs.okhttp)
            implementation(libs.okhttp.dns.over.https)
        }
        androidMain.get().dependsOn(androidJvmMain)
        jvmMain.get().dependsOn(androidJvmMain)
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

// compose-resources 的 Res.readBytes 只能按名字读，**没有列目录的能力**，
// 而共享关键 H 帧是「一个视频一个 json」的布局（README_TECH 第 15 节说明了这是为了
// 方便贡献者直接丢一个文件进来，不要改成单个大数组）。所以在构建期扫一遍目录，
// 生成一份 videoCode 清单给 DatabaseRepo.loadAllShared() 用。
val sharedHKeyframeDir =
    layout.projectDirectory.dir("src/commonMain/composeResources/files/h_keyframes")
val generateSharedHKeyframeIndex = tasks.register("generateSharedHKeyframeIndex") {
    description = "生成共享关键 H 帧的索引"
    val srcDir = sharedHKeyframeDir
    val outDir = layout.buildDirectory.dir("generated/sharedHKeyframeIndex/kotlin")
    inputs.dir(srcDir).withPropertyName("sharedHKeyframes")
    outputs.dir(outDir)
    doLast {
        val codes = srcDir.asFile.listFiles()
            .orEmpty()
            .filter { it.isFile && it.extension == "json" }
            .map { it.nameWithoutExtension }
            .sorted()
        val target =
            outDir.get().file("com/yenaly/han1meviewer/logic/SharedHKeyframeIndex.kt").asFile
        target.parentFile.mkdirs()
        target.writeText(
            buildString {
                appendLine("// 由 :shared:generateSharedHKeyframeIndex 生成，不要手改。")
                appendLine("package com.yenaly.han1meviewer.logic")
                appendLine()
                appendLine("/** `composeResources/files/h_keyframes/` 下所有共享关键 H 帧的 videoCode。 */")
                appendLine("internal val SHARED_H_KEYFRAME_CODES: List<String> = listOf(")
                codes.forEach { appendLine("    \"$it\",") }
                appendLine(")")
            }
        )
    }
}

kotlin.sourceSets.commonMain {
    kotlin.srcDir(generateSharedHKeyframeIndex)
}

// KSP 在 commonMain 上生成的代码要手动挂进源集，并保证所有编译任务都排在它后面。
kotlin.sourceSets.commonMain {
    kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
}

// 各目标的编译任务和 KSP 任务都会读 commonMain 的生成目录，都要排在它后面，
// 否则 Gradle 会报 "uses this output ... without declaring an explicit dependency"。
tasks.withType<KotlinCompilationTask<*>>().configureEach {
    if (name != "kspCommonMainKotlinMetadata") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}

tasks.matching { it.name.startsWith("ksp") && it.name != "kspCommonMainKotlinMetadata" }
    .configureEach { dependsOn("kspCommonMainKotlinMetadata") }
