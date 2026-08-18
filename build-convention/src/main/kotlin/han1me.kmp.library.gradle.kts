@file:Suppress("UnstableApiUsage")
@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import io.github.darriousliu.han1meviewer.convention.compileSdkVersion
import io.github.darriousliu.han1meviewer.convention.libs
import io.github.darriousliu.han1meviewer.convention.minSdkVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * 非 UI 的 KMP 库模块（`:core:model`、`:core:network` 这类）的基线配置。
 *
 * 三个目标（android / jvm / ios）、编译选项、以及所有模块都要的那几个依赖都在这里，
 * 模块自己的 build.gradle.kts 只写 namespace 和它独有的依赖。
 * 要 Compose 的模块用 `han1me.kmp.compose`，那个 apply 了本插件再加 CMP。
 */
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("com.google.devtools.ksp")
}

kotlin {
    compilerOptions {
        // expect/actual class 目前仍是实验特性，全项目都要
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    android {
        compileSdk = compileSdkVersion
        minSdk = minSdkVersion

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    iosArm64()
    iosSimulatorArm64()

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            implementation(project.dependencies.platform(libs.findLibrary("koin-bom").get()))
            implementation(libs.findLibrary("koin-core").get())
            // koin-annotations 必须钉死在 KSP 处理器那条版本线上，不能跟着 BOM 走：
            // 处理器生成的 KoinMeta 文件 import 的 `org.koin.meta.annotations` 只存在于
            // 2.3.x，BOM 会把它提到 4.2.2，一提上去生成的代码就编译不过
            // （Unresolved reference 'meta'）。
            implementation(libs.findLibrary("koin-annotations").get())
            implementation(libs.findLibrary("coroutines-core").get())
            implementation(libs.findLibrary("serialization-json").get())
            implementation(libs.findLibrary("datetime").get())
            implementation(libs.findLibrary("kermit").get())
        }
        androidMain.dependencies {
            implementation(project.dependencies.platform(libs.findLibrary("koin-bom").get()))
            implementation(libs.findLibrary("koin-android").get())
        }
    }

    // KSP 生成到 commonMain metadata，各目标共用一份
    sourceSets.commonMain {
        kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
    }
}

dependencies {
    // Koin 的注解处理器。只挂 commonMain metadata：定义都写在 commonMain，
    // 生成一次各目标共用。
    add("kspCommonMainMetadata", libs.findLibrary("koin-ksp-compiler").get())
}

// koin-annotations 必须钉死在 KSP 处理器那条版本线上，不能跟着 koin-bom 走。
// 处理器生成的 KoinMeta 文件 import 的 `org.koin.meta.annotations` 只存在于 2.3.x；
// BOM 4.2.2 会把 koin-annotations 一起提上去，而 4.2.2 里没有这个包，
// 生成的代码当场编译不过（Unresolved reference 'meta'）。
// 官方新的 Kotlin compiler plugin 方案没这个问题，但它编译时链的是 kotlin-compiler
// 2.3.20，在本项目的 2.4.20-Beta2 上 IR 阶段就崩，等它跟上再换。
configurations.configureEach {
    resolutionStrategy {
        force(libs.findLibrary("koin-annotations").get())
    }
}

// 所有编译任务和其它 ksp 任务都会读 commonMain 的生成目录，都要排在它后面，
// 否则 Gradle 报 "uses this output ... without declaring an explicit dependency"。
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
    if (name != "kspCommonMainKotlinMetadata") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}
tasks.matching { it.name.startsWith("ksp") && it.name != "kspCommonMainKotlinMetadata" }
    .configureEach { dependsOn("kspCommonMainKotlinMetadata") }
