package com.yenaly.han1meviewer.logic.network

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.yenaly.han1meviewer.logic.network.plugin.CloudflareSolver
import com.yenaly.han1meviewer.ui.activity.CloudflareActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 拉起 [CloudflareActivity] 用 WebView 过盾。替代迁移前的 `CloudflareInterceptor`。
 *
 * ⚠️ 与旧实现的区别：旧的是 `CountDownLatch.await()` **阻塞** okhttp 的工作线程等 WebView，
 * 这里是真正的挂起，请求所在的协程被取消时也能跟着退出。
 */
class AndroidCloudflareSolver(private val context: Context) : CloudflareSolver {

    override suspend fun solve(url: String) = suspendCancellableCoroutine { continuation ->
        CloudflareActivity.onFinished = {
            if (continuation.isActive) continuation.resume(Unit)
        }
        val intent = Intent(context, CloudflareActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(CloudflareActivity.EXTRA_URL, url)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            ContextCompat.getMainExecutor(context).execute {
                Toast.makeText(
                    context.applicationContext,
                    "cf启动失败错误: ${e::class.java.simpleName}",
                    Toast.LENGTH_LONG,
                ).show()
            }
            CloudflareActivity.onFinished = null
            if (continuation.isActive) continuation.resume(Unit)
        }
        continuation.invokeOnCancellation { CloudflareActivity.onFinished = null }
    }
}
