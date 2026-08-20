import io.github.darriousliu.han1meviewer.convention.createAndroidJvmMain

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
            // 自定义镜像站测试要对首页 HTML 跑一遍解析
            implementation(project(":core:parse"))
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
            // SAF 迁移检查要数 DocumentFile 树里的文件
            implementation(libs.androidx.documentfile)
        }
    }

    // 网络设置的 DNS/DoH/延迟测试依赖 okhttp 侧能力(HDns/HProxySelector),
    // android 和 jvm 共用一份 actual,iOS 走门控 Noop。
    createAndroidJvmMain().dependencies {
        // HDns 实现 okhttp3.Dns,编译期解析超类型要看得见 okhttp
        implementation(libs.okhttp)
    }
}

dependencies {
    add("kspCommonMainMetadata", libs.koin.ksp.compiler)
}
