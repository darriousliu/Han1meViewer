plugins {
    id("han1me.kmp.compose")
}

kotlin {
    android {
        namespace = "io.github.darriousliu.han1meviewer.feature.settings"
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
            // 网格配置对话框的 %.1f 文案
            implementation(libs.mp.stools)
            implementation(project(":core:repository"))
            implementation(project(":core:ui"))
            implementation(project(":core:navigation"))
            // SettingsScaffold 的 onScreenView 走 LocalMainHostActions
            implementation(project(":feature:main"))
            // 下载设置的「同时下载数」要同步任务引擎(LocalDownloadTaskEngine)
            implementation(project(":feature:download"))
            implementation(libs.serialization.json)
            implementation(libs.aboutlibraries.core)
            implementation(libs.aboutlibraries.compose.m3)
            implementation(libs.htmlconverter)
            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs.compose)
        }

        androidMain.dependencies {
            // 下载设置的 SAF 目录选择与运行时权限 launcher
            implementation(libs.androidx.activity.compose)
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", libs.koin.ksp.compiler)
}
