@file:Suppress("UnstableApiUsage")
@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import com.yenaly.han1meviewer.convention.compileSdkVersion
import com.yenaly.han1meviewer.convention.libs
import com.yenaly.han1meviewer.convention.minSdkVersion
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
            implementation(libs.findLibrary("coroutines-core").get())
            implementation(libs.findLibrary("serialization-json").get())
            implementation(libs.findLibrary("datetime").get())
            implementation(libs.findLibrary("kermit").get())
        }
    }
}
