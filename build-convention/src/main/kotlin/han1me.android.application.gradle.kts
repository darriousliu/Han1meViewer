@file:Suppress("UnstableApiUsage")

import com.yenaly.han1meviewer.convention.compileSdkVersion
import com.yenaly.han1meviewer.convention.libs
import com.yenaly.han1meviewer.convention.minSdkVersion
import com.yenaly.han1meviewer.convention.targetSdkVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * Android 宿主模块（`:app`）的基线配置。
 * 签名、buildTypes、splits、launcher 这些一个项目只有一份的东西留在 `:app` 自己那儿。
 */
plugins {
    // AGP 9 起 Kotlin 支持是内置的，再 apply org.jetbrains.kotlin.android 会直接报错
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    compileSdk = compileSdkVersion

    defaultConfig {
        minSdk = minSdkVersion
        targetSdk = targetSdkVersion
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.RequiresOptIn",
            "-jvm-default=enable"
        )
    }
}

dependencies {
    coreLibraryDesugaring(libs.findLibrary("desugar-jdk-libs").get())
}
