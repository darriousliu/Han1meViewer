plugins {
    id("han1me.kmp.compose")
}

kotlin {
    android {
        namespace = "io.github.darriousliu.han1meviewer.feature.video"
    }

    sourceSets {
        commonMain.dependencies {
            // convention 不声明依赖，基础几组在模块里自己写
            implementation(libs.bundles.compose)
            implementation(libs.bundles.lifecycle)
            implementation(libs.bundles.koin)
            implementation(libs.bundles.koin.compose)
            implementation(libs.coroutines.core)
            implementation(libs.datetime)
            implementation(libs.kermit)
            implementation(libs.coil.compose)
            implementation(project(":core:repository"))
            implementation(project(":core:ui"))
            implementation(project(":core:navigation"))
            implementation(project(":feature:comment"))
            implementation(project(":feature:checkin"))
            implementation(libs.serialization.json)
            implementation(libs.mp.stools)
        }

        androidMain.dependencies {
            // Media3 Compose 内核（VideoPlayerController 的 android actual）
            implementation(libs.media3.exoplayer)
            implementation(libs.media3.exoplayer.hls)
            implementation(libs.media3.ui.compose)
            // mpv Compose 内核（libmpv 封装）
            implementation(libs.mpv.lib)
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", libs.koin.ksp.compiler)
}
