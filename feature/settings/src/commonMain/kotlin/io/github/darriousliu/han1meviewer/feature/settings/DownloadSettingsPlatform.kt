package io.github.darriousliu.han1meviewer.feature.settings

import androidx.compose.runtime.Composable

/**
 * 下载设置页的平台能力可见性。用户定(2026-08-19):JVM/iOS 默认路径用 FileKit.filesDir;
 * JVM 可选路径、可迁移下载项(与 Android 一致);iOS 隐藏路径选择;
 * 「匯出下載記錄」是移动文件+改库路径的迁移语义,iOS 也隐藏。
 */
data class DownloadSettingsCapabilities(
    /** 选择下载目录(android=SAF;jvm=目录对话框)。 */
    val chooseLocation: Boolean = false,
    /** 「匯出下載記錄」:把私有目录的下载迁移(移动+改库)到所选目录。 */
    val migrateDownloads: Boolean = false,
)

expect val downloadSettingsCapabilities: DownloadSettingsCapabilities

enum class MigrateOutcome { Success, NoFiles, PermissionError }

interface DownloadSettingsActions {

    /** 当前下载位置摘要(路径或目录名)。 */
    fun downloadPathSummary(): String = ""

    /** 是否处于默认(私有)位置——决定选路径弹窗是否带「恢复默认」按钮。 */
    fun isUsingDefaultLocation(): Boolean = true

    /** 弹目录选择;选定并持久化后经工厂参数 onLocationChanged 回调。 */
    fun chooseDownloadLocation() {}

    /** 恢复默认位置(写回偏好并提示)。 */
    fun restoreDefaultLocation() {}

    /** 迁移前置是否满足(自定义目录已选且权限有效)。 */
    fun canMigrate(): Boolean = false

    /**
     * 执行迁移(移动文件+改库路径)。[onProgress] 报 (已迁移, 总数);
     * [onFinished] 报最终结果。回调线程不保证是主线程。
     */
    fun migrateDownloads(
        onProgress: (migrated: Int, total: Int) -> Unit,
        onFinished: (MigrateOutcome) -> Unit,
    ) {
        onFinished(MigrateOutcome.PermissionError)
    }

    /** 确保存储权限(android ≤P 的运行时权限);其余平台空实现。 */
    fun ensureStoragePermission() {}
}

object NoopDownloadSettingsActions : DownloadSettingsActions

@Composable
expect fun rememberDownloadSettingsActions(onLocationChanged: () -> Unit): DownloadSettingsActions
