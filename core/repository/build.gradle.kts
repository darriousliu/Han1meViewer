plugins {
    id("han1me.kmp.compose")
    alias(libs.plugins.org.jetbrains.kotlin.plugin.serialization)
}

/**
 * 各数据源的汇合处：网络、数据库、解析各出一半，拼成 feature 直接能用的东西。
 * feature 模块通常只依赖这一个。
 */
kotlin {
    android {
        namespace = "io.github.darriousliu.han1meviewer.core.repository"
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core:common"))
            api(project(":core:model"))
            api(project(":core:storage"))
            api(project(":core:network"))
            implementation(project(":core:parse"))
            implementation(libs.ktor.client.core)
            implementation(libs.filekit.core)
            implementation(libs.kotlinx.io.core)
        }
    }
}
