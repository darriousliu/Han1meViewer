package io.github.darriousliu.han1meviewer.core.network

import com.google.firebase.database.FirebaseDatabase
import io.github.darriousliu.han1meviewer.core.common.FIREBASE_REALTIME_DATABASE
import io.github.darriousliu.han1meviewer.core.model.Announcement
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 公告的 Android actual：Firebase Realtime Database。
 * 策略（过滤/排序/24 小时冷却）在 common 侧，这里只取原始列表。
 */
actual suspend fun fetchPlatformAnnouncements(): List<Announcement> =
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
