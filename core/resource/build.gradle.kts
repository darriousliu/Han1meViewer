plugins {
    id("han1me.kmp.compose")
}

kotlin {
    android {
        namespace = "io.github.darriousliu.han1meviewer.core.resource"
    }
}

compose.resources {
    // 别的模块要能拿到 Res，默认是 internal
    publicResClass = true
    packageOfResClass = "io.github.darriousliu.han1meviewer.core.resource"
}

// compose-resources 的 Res.readBytes 只能按名字读，**没有列目录的能力**，
// 而共享关键 H 帧是「一个视频一个 json」的布局（README_TECH 第 15 节说明了这是为了
// 方便贡献者直接丢一个文件进来，不要改成单个大数组）。所以在构建期扫一遍目录，
// 生成一份 videoCode 清单给 DatabaseRepo.loadAllShared() 用。
val sharedHKeyframeDir =
    layout.projectDirectory.dir("src/commonMain/composeResources/files/h_keyframes")
val generateSharedHKeyframeIndex = tasks.register("generateSharedHKeyframeIndex") {
    description = "生成共享关键 H 帧的索引"
    val srcDir = sharedHKeyframeDir
    val outDir = layout.buildDirectory.dir("generated/sharedHKeyframeIndex/kotlin")
    inputs.dir(srcDir).withPropertyName("sharedHKeyframes")
    outputs.dir(outDir)
    doLast {
        val codes = srcDir.asFile.listFiles()
            .orEmpty()
            .filter { it.isFile && it.extension == "json" }
            .map { it.nameWithoutExtension }
            .sorted()
        val target = outDir.get()
            .file("io/github/darriousliu/han1meviewer/core/resource/SharedHKeyframeIndex.kt")
            .asFile
        target.parentFile.mkdirs()
        target.writeText(
            buildString {
                appendLine("// 由 :core:resource:generateSharedHKeyframeIndex 生成，不要手改。")
                appendLine("package io.github.darriousliu.han1meviewer.core.resource")
                appendLine()
                appendLine("/** `composeResources/files/h_keyframes/` 下所有共享关键 H 帧的 videoCode。 */")
                appendLine("val SHARED_H_KEYFRAME_CODES: List<String> = listOf(")
                codes.forEach { appendLine("    \"$it\",") }
                appendLine(")")
            }
        )
    }
}

kotlin.sourceSets.commonMain {
    kotlin.srcDir(generateSharedHKeyframeIndex)
}
