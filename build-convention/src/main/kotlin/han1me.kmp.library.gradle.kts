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
 * 只管配置：目标（android / jvm / ios）、编译选项、KSP 接线。
 * **不声明任何依赖**——每个模块用到什么自己在 build.gradle.kts 里写，
 * 常用组合在 catalog 里有 bundle（libs.bundles.compose / lifecycle / koin…）。
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

    // KSP（Koin/Ktorfit）生成到 commonMain metadata，各目标共用一份。
    // 目录不存在时无害，所以对没挂处理器的模块也是零成本。
    sourceSets.commonMain {
        kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
    }
}

// koin-annotations 钉死在 KSP 处理器那条版本线上（2.3.x）。
// 处理器生成的 KoinMeta 文件 import 的 `org.koin.meta.annotations` 只存在于 2.3.x；
// 谁要是引回 koin-bom 或某个传递依赖把它顶上 4.2.x，生成的代码当场编译不过
// （Unresolved reference 'meta'）。catalog 里已是显式版本，这条是保险。
configurations.configureEach {
    resolutionStrategy {
        force(libs.findLibrary("koin-annotations").get())
    }
}

// 所有编译任务和其它 ksp 任务都会读 commonMain 的生成目录，都要排在它后面，
// 否则 Gradle 报 "uses this output ... without declaring an explicit dependency"。
// 用 tasks.matching 惰性引用：模块没挂 kspCommonMainMetadata 处理器时该任务不存在，
// 按名字 dependsOn 会直接报错，matching 出来的空集合则无害。
val kspMetadataTask = tasks.matching { it.name == "kspCommonMainKotlinMetadata" }
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
    if (name != "kspCommonMainKotlinMetadata") {
        dependsOn(kspMetadataTask)
    }
}
tasks.matching { it.name.startsWith("ksp") && it.name != "kspCommonMainKotlinMetadata" }
    .configureEach { dependsOn(kspMetadataTask) }
