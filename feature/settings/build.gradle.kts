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
            implementation(libs.serialization.json)
            implementation(libs.aboutlibraries.core)
            implementation(libs.aboutlibraries.compose.m3)
            implementation(libs.htmlconverter)
            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs.compose)
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", libs.koin.ksp.compiler)
}
