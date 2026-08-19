plugins {
    id("han1me.kmp.library")
}

/**
 * 各平台的通知能力。
 *
 * commonMain 是门面（渠道 id + 初始化入口）；Android 接 NotificationCompat，
 * iOS/JVM 目前是空实现。下载/更新的具体通知构建仍在各自的 Worker 里——
 * 那些和 WorkManager 的 ForegroundInfo 绑死，等 :feature:download 拆出来时
 * 再决定要不要收进来。
 */
kotlin {
    android {
        namespace = "io.github.darriousliu.han1meviewer.core.notification"
    }

    sourceSets {
        commonMain.dependencies {
            // convention 不声明依赖，基础几组在模块里自己写
            implementation(project(":core:common"))
        }

        androidMain.dependencies {
            // NotificationChannelCompat / NotificationManagerCompat
            implementation(libs.core)
        }
    }
}
