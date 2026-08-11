@file:Suppress("UnstableApiUsage")

import Config.Version.createVersion
import Config.Version.source
import Config.isRelease
import Config.lastCommitSha
import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.BuiltArtifactsLoader
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import javax.inject.Inject

plugins {
    alias(libs.plugins.com.android.application)
    alias(libs.plugins.com.google.gms.google.services)
    alias(libs.plugins.com.google.firebase.crashlytics)
    alias(libs.plugins.com.google.firebase.firebase.pref)
    alias(libs.plugins.ben.manes.versions)
}

android {
    namespace = "com.yenaly.han1meviewer.androidapp"
    compileSdk = property("compile.sdk").toString().toInt()

    val commitSha = if (isRelease) lastCommitSha else "b8eace8"
    val githubToken = System.getenv("HA_GITHUB_TOKEN") ?: File(
        projectDir,
        "ha1_github_token.txt",
    ).checkIfExists()?.readText().orEmpty()

    defaultConfig {
        applicationId = "com.yenaly.han1meviewer"
        minSdk = property("min.sdk").toString().toInt()
        targetSdk = property("target.sdk").toString().toInt()
        val (code, name) = createVersion(major = 1, minor = 0, patch = 1)
        versionCode = code
        versionName = name

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "COMMIT_SHA", "\"$commitSha\"")
        buildConfigField("String", "VERSION_NAME", "\"$versionName\"")
        buildConfigField("int", "VERSION_CODE", "$versionCode")
        buildConfigField("String", "HA_GITHUB_TOKEN", "\"$githubToken\"")
        buildConfigField("String", "VERSION_SOURCE", "\"$source\"")
        buildConfigField("int", "SEARCH_YEAR_RANGE_END", "${Config.thisYear}")
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
            isEnable = gradle.startParameter.taskRequests.toString().contains("Release")
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
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
            manifestPlaceholders["appIcon"] = "@mipmap/ic_launcher_new"
        }

        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            applicationIdSuffix = ".debug"
            manifestPlaceholders["appIcon"] = "@mipmap/ic_launcher_debug"
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    lint {
        disable += "EnsureInitializerMetadata"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.RequiresOptIn",
            "-jvm-default=enable",
        )
    }
}

androidComponents {
    onVariants { variant ->
        val taskSuffix = variant.name.replaceFirstChar { it.uppercase() }
        val copyTask = tasks.register<CopyApkTask>("copy${taskSuffix}Apk") {
            apkDirectory.set(variant.artifacts.get(SingleArtifact.APK))
            builtArtifactsLoader.set(variant.artifacts.getBuiltArtifactsLoader())
            outputDirectory.set(layout.buildDirectory.dir("outputs/renamedApk/${variant.name}"))
            outputFileName.set(
                variant.outputs.single().versionName.map { versionName ->
                    "Han1meViewer-v$versionName.apk"
                },
            )
        }
        tasks.configureEach {
            if (name == "assemble$taskSuffix") {
                dependsOn(copyTask)
            }
        }
    }
}

dependencies {
    implementation(project(":composeApp"))
    implementation(libs.appcompat)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    debugImplementation(platform(libs.compose.compose.bom))
    debugImplementation(libs.compose.ui.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    androidTestImplementation(libs.test.junit)
    androidTestImplementation(libs.test.espresso.core)
}

abstract class CopyApkTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val apkDirectory: DirectoryProperty

    @get:Internal
    abstract val builtArtifactsLoader: Property<BuiltArtifactsLoader>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Input
    abstract val outputFileName: Property<String>

    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations

    @TaskAction
    fun copyApk() {
        val builtArtifacts = checkNotNull(
            builtArtifactsLoader.get().load(apkDirectory.get()),
        ) { "Unable to load APK metadata from ${apkDirectory.get().asFile}" }
        val apk = builtArtifacts.elements.single()
        fileSystemOperations.copy {
            from(apk.outputFile)
            into(outputDirectory)
            rename { outputFileName.get() }
        }
    }
}

fun File.checkIfExists(): File? = if (exists() && isFile) this else null
