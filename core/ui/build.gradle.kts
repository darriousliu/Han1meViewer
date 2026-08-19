plugins {
    id("han1me.kmp.compose")
}

/**
 * 两个及以上页面共用的 UI。
 *
 * **不放业务**：这里的组件不认识 Repository，也不认识具体页面；要做什么由调用方
 * 通过 onXxx 回调传进来。依赖只到 :core:common / :core:model / :core:resource
 * 为止——真要用到 :core:repository 就说明它不该待在这儿。
 */
kotlin {
    android {
        namespace = "io.github.darriousliu.han1meviewer.core.ui"
    }

    sourceSets {
        commonMain.dependencies {
            // convention 不声明依赖，基础几组在模块里自己写
            implementation(libs.bundles.compose)
            implementation(libs.bundles.lifecycle)
            implementation(libs.coroutines.core)
            implementation(libs.datetime)
            implementation(libs.kermit)
            api(project(":core:common"))
            api(project(":core:model"))
            api(project(":core:resource"))
            // 少数组件读偏好（网格列数、卡片尺寸等展示配置）
            implementation(project(":core:storage"))
            implementation(libs.coil.compose)
            implementation(libs.sonner)
            implementation(libs.htmlconverter)
            implementation(libs.filekit.core)
        }
    }
}
