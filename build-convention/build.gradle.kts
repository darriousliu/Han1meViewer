plugins {
    `kotlin-dsl`
}

// 项目放在 SMB 共享上时，`generatePrecompiledScriptPluginAccessors` 会在本模块的
// build 目录里起一个**嵌套构建**去生成类型安全访问器，而 macOS 的 SMB 不支持
// `FileChannel.lock()`，那个嵌套构建建 FileHasher 时直接 `IOException: Operation not
// supported`。命令行的 `--project-cache-dir` 只作用于根构建，管不到它，所以只能
// 把本模块的 build 目录整个挪到本地盘。
// 想放回默认位置就传 -Phan1me.convention.buildDir=build。
val conventionBuildDir = providers.gradleProperty("han1me.convention.buildDir")
    .getOrElse(
        File(System.getProperty("java.io.tmpdir"), "han1me-build-convention").absolutePath
    )
layout.buildDirectory.set(file(conventionBuildDir))

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

// precompiled script plugin（src/main/kotlin/*.gradle.kts）里要 apply 的插件，
// 必须先作为**实现依赖**出现在这里，否则脚本编译期就找不到它们的 DSL。
// 坐标是「插件 marker」：<plugin id>:<plugin id>.gradle.plugin，
// 少数插件有官方约定的 gradle-plugin 制品（kotlin/AGP/compose），直接用制品更稳。
dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.compose.compiler.gradle.plugin)
    implementation(libs.android.gradle.plugin)
    implementation(libs.compose.multiplatform.gradle.plugin)
    implementation(libs.ksp.gradle.plugin)
}
