plugins {
    id("han1me.kmp.compose")
}

kotlin {
    android {
        namespace = "io.github.darriousliu.han1meviewer.feature.checkin"
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

            // 打卡弹窗里的成就图
            implementation(libs.coil.compose)
            implementation(project(":core:repository"))
            implementation(project(":core:ui"))
        }

        androidMain.dependencies {
            // rememberCheckInActions 的 LocalActivity
            implementation(libs.androidx.activity.compose)
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", libs.koin.ksp.compiler)
}
