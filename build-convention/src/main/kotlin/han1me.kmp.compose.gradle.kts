/**
 * 带 Compose 的 KMP 模块（`:core:ui`、`:feature:*`、umbrella）的基线配置。
 *
 * 在 `han1me.kmp.library` 之上只多 apply 两个 Compose 插件。
 * **依赖由模块自己声明**：`implementation(libs.bundles.compose)` 起步，
 * 要 viewModel/collectAsStateWithLifecycle 再加 `libs.bundles.lifecycle`。
 */
plugins {
    id("han1me.kmp.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}
