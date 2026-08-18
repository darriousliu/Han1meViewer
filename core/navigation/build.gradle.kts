plugins {
    id("han1me.kmp.compose")
    alias(libs.plugins.org.jetbrains.kotlin.plugin.serialization)
}

/**
 * 路由定义与导航基建。
 *
 * 路由类全是 `@Serializable sealed interface HanimeRoute : NavKey` 的实现，
 * **必须收在同一个包**（sealed 的硬要求），这样 kotlinx 能自动多态、
 * `rememberNavBackStack` 的进程死亡恢复零注册代码。
 */
kotlin {
    android {
        namespace = "io.github.darriousliu.han1meviewer.core.navigation"
    }

    sourceSets {
        commonMain.dependencies {
            // NavKey / NavBackStack 出现在公开签名里
            api(libs.navigation3.ui.jb)
            api(libs.lifecycle.viewmodel.navigation3)
        }

        androidMain.dependencies {
            // JB 的 -android 变体是空壳，转发到 androidx；这条只是把版本顶上去
            implementation(libs.navigation3.ui.androidx)
        }
    }
}
