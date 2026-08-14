@file:Suppress("UnstableApiUsage")

import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.com.google.devtools.ksp)
}

kotlin {
    android {
        namespace = "com.yenaly.han1meviewer.shared"
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
            implementation(libs.androidx.window)
            implementation(libs.androidx.window.java)
            implementation(project(":yenaly_libs"))
            implementation(libs.aboutlibraries.core)
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
            implementation(libs.coil.compose)
            implementation(libs.coil.network.okhttp)
            implementation(libs.aboutlibraries.compose.m3)
            implementation(libs.compose.avatar.cropper)
            // datetime

            implementation(libs.datetime)

            // parse

            implementation(libs.serialization.json)
            implementation(libs.jsoup)

            // network

            implementation(libs.retrofit)
            implementation(libs.converter.serialization)
            implementation(libs.okhttp)
            implementation(libs.okhttp.dns.over.https)

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

        val androidJvmMain = create("androidJvmMain") {
            dependsOn(commonMain.get())
        }
        androidMain.get().dependsOn(androidJvmMain)
        jvmMain.get().dependsOn(androidJvmMain)
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.ui.ui.tooling)

    add("kspAndroid", libs.room.compiler)
    kotlin.targets.filter { it.name.startsWith("ios") }.forEach { target ->
        add(
            "ksp${target.name.replaceFirstChar { it.uppercaseChar() }}",
            libs.room.compiler
        )
    }
    add("kspJvm", libs.room.compiler)

    coreLibraryDesugaring(libs.desugar.jdk.libs)
}
