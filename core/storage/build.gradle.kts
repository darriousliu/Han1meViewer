plugins {
    id("han1me.kmp.compose")
    alias(libs.plugins.org.jetbrains.kotlin.plugin.serialization)
    alias(libs.plugins.androidx.room)
}

kotlin {
    android {
        namespace = "io.github.darriousliu.han1meviewer.core.storage"
    }

    sourceSets {
        commonMain.dependencies {
            // Entity / Preferences 的类型遍布公开签名
            api(project(":core:common"))
            api(project(":core:model"))
            // Database 类继承 RoomDatabase，消费方（BackupManager 等）会直接调基类方法，
            // 用 api 暴露，否则报 "Cannot access RoomDatabase which is a supertype of ..."
            api(libs.room.runtime)
            implementation(libs.sqlite.bundled)
            implementation(libs.sqlite.async)
            implementation(libs.mmkv.kotlin)
            implementation(libs.filekit.core)
        }

        androidMain.dependencies {
            implementation(libs.mmkv)
            // MmkvMigration 读旧的 PreferenceManager 默认 SP，把存量设置搬进 MMKV
            implementation(libs.preference.ktx)
        }
    }
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
    add("kspJvm", libs.room.compiler)
}

room3 {
    schemaDirectory("$projectDir/schemas")
}
