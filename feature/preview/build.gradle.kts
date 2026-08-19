plugins {
    id("han1me.kmp.compose")
}

kotlin {
    android {
        namespace = "io.github.darriousliu.han1meviewer.feature.preview"
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
            // Getchu 图要带 Referer/Cookie，复用 HClientSpec 的 Ktor client
            implementation(libs.coil.network.ktor3)
            implementation(libs.ktor.client.core)
            implementation(project(":core:navigation"))
            implementation(project(":core:repository"))
            implementation(project(":core:ui"))
            implementation(project(":feature:comment"))
            implementation(libs.filekit.core)
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", libs.koin.ksp.compiler)
}
