@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    android {
        namespace = "com.yenaly.han1meviewer.shared"
        compileSdk = property("compile.sdk").toString().toInt()
        minSdk = property("min.sdk").toString().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
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

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.multiplatform.runtime)
            implementation(libs.compose.multiplatform.ui)
            implementation(libs.compose.multiplatform.foundation)
        }

        val androidJvmMain = create("androidJvmMain") {
            dependsOn(commonMain.get())
        }
        androidMain.get().dependsOn(androidJvmMain)
        jvmMain.get().dependsOn(androidJvmMain)
    }
}
