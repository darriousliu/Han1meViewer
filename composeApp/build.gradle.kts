@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
@file:Suppress("UnstableApiUsage")

import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
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

group = "com.yenaly.han1meviewer"

// Lifecycle 2.11 moved its multiplatform artifacts to androidx.lifecycle. The old
// org.jetbrains coordinates are compatibility shims whose empty JARs share the
// same filenames as the real artifacts and break Gradle's JVM distribution task.
configurations.configureEach {
    exclude(group = "org.jetbrains.androidx.lifecycle")
}

val mmkvDesktopNative = when {
    System.getProperty("os.name") == "Mac OS X" -> libs.mmkv.kotlin.nativelib.macos
    System.getProperty("os.name").startsWith("Windows") -> libs.mmkv.kotlin.nativelib.windows
    System.getProperty("os.name").startsWith("Linux") -> libs.mmkv.kotlin.nativelib.linux
    else -> error("Unsupported Desktop OS for MMKV: ${System.getProperty("os.name")}")
}

val desktopJavaHome = extensions
    .getByType<JavaToolchainService>()
    .launcherFor {
        languageVersion.set(JavaLanguageVersion.of(22))
    }
    .map { launcher -> launcher.metadata.installationPath.asFile.absolutePath }

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
            jvmTarget.set(JvmTarget.JVM_22)
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

    swiftPMDependencies {
        iosMinimumDeploymentTarget.set("15.0")
        swiftPackage(
            url = url("https://github.com/firebase/firebase-ios-sdk.git"),
            version = exact(libs.versions.firebaseApple.get()),
            products = listOf(
                product("FirebaseAnalytics"),
                product("FirebaseCrashlytics"),
                product("FirebaseRemoteConfig"),
                product("FirebaseDatabase"),
            ),
        )
    }

    jvmToolchain(22)

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.compose.runtime.multiplatform)
                implementation(libs.compose.foundation.multiplatform)
                implementation(libs.compose.material3.multiplatform)
                implementation(libs.compose.ui.multiplatform)
                implementation(libs.compose.resources.multiplatform)

                implementation(libs.coroutines.core)
                implementation(libs.serialization.json)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ksoup)
                implementation(libs.room.runtime)
                implementation(libs.sqlite.bundled)
                implementation(libs.coil.compose)
                implementation(libs.coil.network.ktor3)
                implementation(libs.lifecycle.runtime.compose.multiplatform)
                implementation(libs.lifecycle.viewmodel.compose.multiplatform)
                implementation(libs.navigation.compose.multiplatform)
                implementation(libs.mmkv.kotlin)
                implementation(libs.compose.sonner)
                implementation("io.github.n7ghtm4r3:biometrik:${libs.versions.biometrik.get()}") {
                    exclude(
                        group = "org.jetbrains.compose.desktop",
                        module = "desktop-jvm-windows-x64",
                    )
                }
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
                implementation(libs.ktor.client.okhttp)

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

        iosMain {
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }

        jvmMain {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.coroutines.swing)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.composewebview.jvm)
                runtimeOnly(mmkvDesktopNative)
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

compose.desktop {
    application {
        mainClass = "com.yenaly.han1meviewer.desktop.DesktopMainKt"
        javaHome = desktopJavaHome.get()
        jvmArgs("--enable-native-access=ALL-UNNAMED")
    }
}

tasks.withType<JavaExec>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("coreLibraryDesugaring", libs.desugar.jdk.libs)
}
