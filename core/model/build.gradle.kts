plugins {
    id("han1me.kmp.compose")
    alias(libs.plugins.org.jetbrains.kotlin.plugin.parcelize)
}

kotlin {
    android {
        namespace = "io.github.darriousliu.han1meviewer.core.model"

        compilerOptions {
            freeCompilerArgs.addAll(
                "-P",
                "plugin:org.jetbrains.kotlin.parcelize:additionalAnnotation=io.github.darriousliu.han1meviewer.core.common.util.Parcelize",
            )
        }
    }

    sourceSets {
        commonMain.dependencies {
            // 模型带 Parcelize / 语言常量，且少数几个模型的显示文案直接吃 StringResource
            api(project(":core:common"))
        }
    }
}
