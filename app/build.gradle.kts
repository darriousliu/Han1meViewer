@file:Suppress("UnstableApiUsage")

import com.android.build.api.variant.impl.VariantOutputImpl
import com.yenaly.han1meviewer.convention.Config.Version.createVersion

plugins {
    // compileSdk/minSdk/targetSdk、Java 21 + desugaring、compose、jvmTarget 都在 convention 里
    id("han1me.android.application")
    alias(libs.plugins.org.jetbrains.kotlin.plugin.parcelize)
    alias(libs.plugins.org.jetbrains.kotlin.plugin.serialization)
    alias(libs.plugins.com.google.devtools.ksp)
    alias(libs.plugins.com.google.gms.google.services)
    alias(libs.plugins.com.google.firebase.crashlytics)
    alias(libs.plugins.com.google.firebase.firebase.pref)
}

android {
    defaultConfig {
        applicationId = "com.yenaly.han1meviewer"
        val (code, name) = createVersion(major = 1, minor = 0, patch = 2)
        versionCode = code
        versionName = name
    }
    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("HOME") + "/.android/keystore.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEYSTORE_PASSWORD")
        }
    }

    splits {
        abi {
            isEnable = (gradle.startParameter.taskRequests.toString().contains("Release"))
            reset()
            include("arm64-v8a")
            isUniversalApk = false
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            manifestPlaceholders["appIcon"] = "@mipmap/ic_launcher_new"

        }

        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            applicationIdSuffix = ".debug"
            manifestPlaceholders["appIcon"] = "@mipmap/ic_launcher_debug"
        }
    }
    buildFeatures {
        buildConfig = false
    }
    lint {
        disable += setOf("EnsureInitializerMetadata")
    }
    namespace = "com.yenaly.han1meviewer.app"
}


androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->

            //  val apkName = "你的应用名_V${output.versionName.get()}_Build${output.versionCode.get()}_${variant.buildType}.apk"
            val apkName = "Han1meViewer-v${output.versionName.get()}.apk"
            (output as VariantOutputImpl).outputFileName = apkName
        }
    }
}

dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)

    implementation(project.dependencies.platform(libs.compose.compose.bom))
    implementation(libs.compose.ui.ui.tooling.preview)
    debugImplementation(project.dependencies.platform(libs.compose.compose.bom))
    debugImplementation(libs.compose.ui.ui.tooling)
    // coreLibraryDesugaring 在 han1me.android.application 里
}
