package io.github.darriousliu.han1meviewer.logic.network

import io.github.darriousliu.han1meviewer.logic.model.Announcement

/**
 * 拉取平台的公告列表。Android 走 Firebase Realtime Database；
 * 没有公告来源的平台返回空列表，首页就不显示公告。
 *
 * 只负责取原始列表：过滤 `isActive`、按 `priority` 排序、「关掉后 24 小时内
 * 不重复弹」都在调用方 `HomePageViewModel` 里。取失败往上抛，由调用方统一
 * `runCatching` 记日志并退化成空列表。
 */
expect suspend fun fetchPlatformAnnouncements(): List<Announcement>
