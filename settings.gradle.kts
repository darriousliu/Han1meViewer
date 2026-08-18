pluginManagement {
    includeBuild("build-convention")
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io/") }
        // net.sergeych:mp_stools（跨平台 String.sprintf）只发布在作者自建的这个源上，
        // 不在 Maven Central；jitpack 也构建不了（1.6.3 及之后的 tag 全是 Error），
        // 所以只能直接引官方源。限定 group 免得它参与解析别的依赖。
        maven("https://maven.universablockchain.com/") {
            content { includeGroup("net.sergeych") }
        }
    }
}
rootProject.name = "Han1meViewer"
include(":app", ":desktopApp", ":shared")
