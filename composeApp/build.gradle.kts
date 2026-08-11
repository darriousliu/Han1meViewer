@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.org.jetbrains.kotlin.plugin.parcelize)
    alias(libs.plugins.org.jetbrains.kotlin.plugin.serialization)
    alias(libs.plugins.com.google.devtools.ksp)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.aboutlibraries)
}

kotlin {
    android {
        namespace = "com.yenaly.han1meviewer"
        compileSdk = property("compile.sdk").toString().toInt()
        minSdk = property("min.sdk").toString().toInt()

        androidResources {
            enable = true
        }
        enableCoreLibraryDesugaring = true
        localDependencySelection {
            // yenaly_libs is temporary and still variant based. Preserve the old debug behavior.
            selectBuildTypeFrom.set(listOf("debug", "release"))
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
        withDeviceTest {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            execution = "HOST"
        }
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            freeCompilerArgs.addAll(
                "-opt-in=kotlin.RequiresOptIn",
                "-jvm-default=enable",
            )
        }
    }

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            freeCompilerArgs.addAll(
                "-opt-in=kotlin.RequiresOptIn",
                "-jvm-default=enable",
            )
        }
        binaries {
            executable {
                mainClass.set("com.yenaly.han1meviewer.desktop.DesktopMainKt")
            }
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            binaryOption("bundleId", "com.yenaly.han1meviewer.composeApp")
        }
    }

    jvmToolchain(21)

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.compose.runtime.multiplatform)
            }
        }

        androidMain {
            dependencies {
                api(project(":yenaly_libs"))

                implementation(libs.appcompat)
                implementation(libs.androidx.window)
                implementation(libs.androidx.window.java)
                implementation(libs.aboutlibraries.core)
                implementation(libs.androidx.biometric)
                implementation(libs.androidx.core.splashscreen)
                implementation(libs.androidx.swiperefreshlayout)
                implementation(libs.androidx.material.icons.extended)

                implementation(libs.bundles.android.base)
                implementation(libs.bundles.android.jetpack)
                implementation(libs.palette)
                implementation(libs.material)

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

                implementation(libs.datetime)
                implementation(libs.serialization.json)
                implementation(libs.jsoup)

                implementation(libs.retrofit)
                implementation(libs.converter.serialization)
                implementation(libs.okhttp)
                implementation(libs.okhttp.dns.over.https)

                implementation(libs.coil)

                implementation(libs.media3.exoplayer)
                implementation(libs.media3.exoplayer.hls)
                implementation(libs.media3.ui.compose)
                implementation(libs.mpv.lib)

                implementation(libs.multitype)
                implementation(libs.base.recyclerview.adapter.helper4)
                implementation(libs.expandable.textview)
                implementation(libs.spannable.x)
                implementation(libs.about)
                implementation(libs.circular.reveal.switch)
                implementation(libs.drawerlayout)

                implementation(project.dependencies.platform(libs.firebase.bom))
                implementation(libs.firebase.analytics)
                implementation(libs.firebase.crashlytics)
                implementation(libs.firebase.perf)
                implementation(libs.firebase.config)
                implementation(libs.firebase.database)
            }
        }

        getByName("androidHostTest") {
            dependencies {
                implementation(libs.junit)
            }
        }

        getByName("androidDeviceTest") {
            dependencies {
                implementation(project.dependencies.platform(libs.compose.compose.bom))
                implementation(libs.androidx.ui.test.junit4)
                implementation(libs.test.junit)
                implementation(libs.test.espresso.core)
            }
        }
    }
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("coreLibraryDesugaring", libs.desugar.jdk.libs)
}
