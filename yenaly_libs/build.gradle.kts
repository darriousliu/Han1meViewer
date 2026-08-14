import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.com.android.library)
    alias(libs.plugins.com.google.devtools.ksp)
}

android {
    compileSdk = property("compile.sdk")?.toString()?.toIntOrNull()

    defaultConfig {
        minSdk = property("min.sdk")?.toString()?.toIntOrNull()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        //noinspection DataBindingWithoutKapt
        dataBinding = true
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        buildConfig = true
    }
    resourcePrefix = "yenaly_"
    namespace = "com.yenaly.yenaly_libs"
}
kotlin {
    compilerOptions {
        jvmTarget.value(JvmTarget.JVM_21)
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xskip-prerelease-check",
            "-opt-in=kotlin.ExperimentalStdlibApi"
        )
    }
}
dependencies {

    implementation(libs.recyclerview)
    implementation(libs.core)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.coroutines.android)

    implementation(libs.navigation.fragment)
    implementation(libs.lifecycle.livedata)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.preference.ktx)
    implementation(libs.startup.runtime)
    implementation(libs.gson)

    testImplementation(libs.junit)

    androidTestImplementation(libs.test.junit)
    androidTestImplementation(libs.test.espresso.core)
}
