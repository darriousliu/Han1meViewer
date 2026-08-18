import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.INT
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import io.github.darriousliu.han1meviewer.convention.Config
import io.github.darriousliu.han1meviewer.convention.Config.Version.createVersion
import io.github.darriousliu.han1meviewer.convention.Config.Version.source
import io.github.darriousliu.han1meviewer.convention.Config.isRelease
import io.github.darriousliu.han1meviewer.convention.Config.lastCommitSha
import io.github.darriousliu.han1meviewer.convention.createAndroidJvmMain

plugins {
    id("han1me.kmp.compose")
    alias(libs.plugins.org.jetbrains.kotlin.plugin.parcelize)
    alias(libs.plugins.buildkonfig)
}

// BuildConfig 是全局的（版本号、applicationId、GitHub token…），谁都可能读，
// 所以生成在依赖图最底层的 :core:common 而不是某个上层模块。
val releaseBuild = isRelease
val commitSha = if (releaseBuild) lastCommitSha else "b8eace8"
val githubToken = System.getenv("HA_GITHUB_TOKEN") ?: rootProject
    .file("app/ha1_github_token.txt")
    .takeIf { it.isFile }
    ?.readText()
    .orEmpty()
val (versionCode, versionName) = createVersion(major = 1, minor = 0, patch = 2)
val applicationId = "io.github.darriousliu.han1meviewer${if (releaseBuild) "" else ".debug"}"

buildkonfig {
    packageName = "io.github.darriousliu.han1meviewer.core.common"
    exposeObjectWithName = "BuildConfig"

    defaultConfigs {
        buildConfigField(
            BOOLEAN, "DEBUG", (!releaseBuild).toString(),
            nullable = false,
            const = true
        )
        buildConfigField(STRING, "APPLICATION_ID", applicationId, nullable = false, const = true)
        buildConfigField(STRING, "COMMIT_SHA", commitSha, nullable = false, const = true)
        buildConfigField(STRING, "VERSION_NAME", versionName, nullable = false, const = true)
        buildConfigField(
            INT, "VERSION_CODE", versionCode.toString(),
            nullable = false,
            const = true
        )
        buildConfigField(STRING, "HA_GITHUB_TOKEN", githubToken, nullable = false, const = true)
        buildConfigField(STRING, "VERSION_SOURCE", source, nullable = false, const = true)
        buildConfigField(
            INT, "SEARCH_YEAR_RANGE_END", Config.thisYear.toString(), nullable = false,
            const = true
        )
    }
}

kotlin {
    android {
        namespace = "io.github.darriousliu.han1meviewer.core.common"

        compilerOptions {
            freeCompilerArgs.addAll(
                "-P",
                "plugin:org.jetbrains.kotlin.parcelize:additionalAnnotation=io.github.darriousliu.han1meviewer.core.common.util.Parcelize",
            )
        }
    }

    sourceSets {
        commonMain.dependencies {
            // 异常带 StringResource（LocalizedException），出现在公开签名里
            api(project(":core:resource"))
            implementation(libs.kotlinx.io.core)
            implementation(libs.filekit.core)
            // FileSizeFormat 的 %f 格式化：CMP 的 stringResource 只认 %N$d / %N$s
            implementation(libs.mp.stools)
        }

        androidMain.dependencies {
            implementation(libs.appcompat)
        }

        // SslError 的 iOS actual 要认 Darwin engine 的异常类型
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        getByName("androidHostTest").dependencies {
            implementation(libs.junit)
            // TimeFormatTest 是拿 formatVideoTime 和 JZUtils.stringForTime 直接对拍的，
            // 期望值不是算出来的是跑出来的。**只是测试依赖，不进产物。**
            // 播放器域迁完、jzvd 去掉之后，按那个文件头的说明把期望值改成硬编码，
            // 这条依赖也就能删了。
            implementation(libs.jiaozi.video.player)
        }
    }

    // Dates / EucJp / NetworkError / SslError 的 actual 是 android 和 jvm 共用的
    createAndroidJvmMain().dependencies {
        implementation(libs.okhttp)
    }
}
