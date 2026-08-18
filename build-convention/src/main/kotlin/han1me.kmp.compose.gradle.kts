import com.yenaly.han1meviewer.convention.libs

/**
 * 带 Compose 的 KMP 模块（`:core:ui`、`:feature:*`、umbrella）的基线配置。
 *
 * 在 `han1me.kmp.library` 之上加 CMP 与它那一圈跑不掉的依赖。
 * 注意 commonMain 里用的是 JB 的制品（`org.jetbrains.compose.*`），
 * 不是 androidx 的——后者只有 Android 变体。
 */
plugins {
    id("han1me.kmp.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.findLibrary("compose-multiplatform-runtime").get())
            implementation(libs.findLibrary("compose-multiplatform-foundation").get())
            implementation(libs.findLibrary("compose-multiplatform-ui").get())
            implementation(libs.findLibrary("compose-multiplatform-ui-backhandler").get())
            implementation(libs.findLibrary("compose-multiplatform-ui-tooling-preview").get())
            implementation(libs.findLibrary("compose-resources").get())
            implementation(libs.findLibrary("jetbrains-compose-material3").get())
            implementation(libs.findLibrary("jetbrains-compose-material-icons-core").get())
            implementation(libs.findLibrary("lifecycle-viewmodel").get())
            implementation(libs.findLibrary("lifecycle-viewmodel-savedstate").get())
            implementation(libs.findLibrary("lifecycle-viewmodel-compose").get())
            implementation(libs.findLibrary("lifecycle-runtime-compose").get())
        }
    }
}

dependencies {
    // @Preview 的渲染实现只在 Android 侧需要，且只进运行时 classpath
    androidRuntimeClasspath(libs.findLibrary("compose-ui-ui-tooling").get())
}
