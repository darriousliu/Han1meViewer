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
