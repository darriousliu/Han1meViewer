plugins {
    id("han1me.kmp.compose")
}

kotlin {
    android {
        namespace = "io.github.darriousliu.han1meviewer.feature.home"
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
            // HomeRouteScreen 的 onExitApp 走 LocalMainHostActions;退出弹窗的打卡记录
            implementation(project(":feature:main"))
            implementation(project(":feature:checkin"))
            implementation(libs.serialization.json)
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", libs.koin.ksp.compiler)
}
