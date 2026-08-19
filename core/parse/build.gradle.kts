plugins {
    id("han1me.kmp.compose")
    alias(libs.plugins.org.jetbrains.kotlin.plugin.serialization)
}

kotlin {
    android {
        namespace = "io.github.darriousliu.han1meviewer.core.parse"
    }

    sourceSets {
        commonMain.dependencies {
            // convention 不声明依赖，基础几组在模块里自己写
            implementation(libs.bundles.compose)
            implementation(libs.coroutines.core)
            implementation(libs.serialization.json)
            implementation(libs.datetime)
            implementation(libs.kermit)
            api(project(":core:common"))
            api(project(":core:model"))
            // 解析里会读登录态等偏好
            implementation(project(":core:storage"))
            // 网页解析
            implementation(libs.ksoup)
            implementation(libs.ktor.client.core)
        }
    }
}
