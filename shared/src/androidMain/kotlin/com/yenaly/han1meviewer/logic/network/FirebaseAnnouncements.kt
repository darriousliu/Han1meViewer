package com.yenaly.han1meviewer.logic.network

import com.google.firebase.database.FirebaseDatabase
import com.yenaly.han1meviewer.FIREBASE_REALTIME_DATABASE
import com.yenaly.han1meviewer.logic.model.Announcement
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 公告的 Android 实现：Firebase Realtime Database。
 *
 * 在 `HanimeApplication.onCreate` 里注册到
 * [com.yenaly.han1meviewer.ui.screen.home.homepage.AnnouncementSource]。
 * 「24 小时内不重复弹」「按 priority 排序」「过滤 isActive」这些策略在 common 侧，
 * 这里只负责把原始列表取回来。
 */
suspend fun fetchAnnouncementsFromFirebase(): List<Announcement> =
    suspendCancellableCoroutine { continuation ->
        FirebaseDatabase.getInstance(FIREBASE_REALTIME_DATABASE)
            .getReference("announcements").get()
            .addOnSuccessListener { snapshot ->
                if (!continuation.isActive) return@addOnSuccessListener
                // getValue 走反射，Announcement 那个空构造就是给它用的
                val list = snapshot.children.mapNotNull {
                    it.getValue(Announcement::class.java)
                }
                continuation.resume(list)
            }
            .addOnFailureListener { e ->
                // 往上抛，让 common 侧统一 runCatching + 打日志 + 退化成空列表
                if (continuation.isActive) continuation.resumeWithException(e)
            }
    }
