@file:Suppress("UnstableApiUsage")

import Config.Version.createVersion
import Config.Version.source
import Config.isRelease
import Config.lastCommitSha
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.INT
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.org.jetbrains.kotlin.plugin.parcelize)
    alias(libs.plugins.org.jetbrains.kotlin.plugin.serialization)
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

buildkonfig {
    packageName = "com.yenaly.han1meviewer"
    exposeObjectWithName = "BuildConfig"

    defaultConfigs {
        buildConfigField(BOOLEAN, "DEBUG", (!releaseBuild).toString(), false, true)
        buildConfigField(STRING, "APPLICATION_ID", applicationId, false, true)
        buildConfigField(STRING, "COMMIT_SHA", commitSha, false, true)
        buildConfigField(STRING, "VERSION_NAME", versionName, false, true)
        buildConfigField(INT, "VERSION_CODE", versionCode.toString(), false, true)
        buildConfigField(STRING, "HA_GITHUB_TOKEN", githubToken, false, true)
        buildConfigField(STRING, "VERSION_SOURCE", source, false, true)
        buildConfigField(INT, "SEARCH_YEAR_RANGE_END", Config.thisYear.toString(), false, true)
    }
}

kotlin {
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
            ),
        )
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.multiplatform.runtime)
            implementation(libs.compose.multiplatform.ui)
            implementation(libs.compose.multiplatform.foundation)
            implementation(libs.coroutines.core)
            implementation(libs.datetime)
            implementation(libs.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)
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
            // parse

            implementation(libs.jsoup)

            // network

            implementation(libs.ktor.client.okhttp)
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

        getByName("iosMain").dependencies {
            implementation(libs.ktor.client.darwin)
        }

        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
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
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
    add("kspJvm", libs.room.compiler)

    coreLibraryDesugaring(libs.desugar.jdk.libs)
}

room {
    schemaDirectory("$projectDir/schemas")
}
