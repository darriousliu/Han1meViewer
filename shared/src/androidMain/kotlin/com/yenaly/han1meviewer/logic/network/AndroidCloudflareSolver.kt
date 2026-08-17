package com.yenaly.han1meviewer.logic.network

import android.content.Context
import android.content.Intent
import com.yenaly.han1meviewer.logic.network.plugin.CloudflareSolver
import com.yenaly.han1meviewer.ui.activity.MainActivity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 过盾请求在网络层和导航图之间的桥。
 *
 * 原来是 `startActivity(CloudflareActivity)` + companion 静态回调 `onFinished`——
 * 那套有个死锁：用户没过盾直接关掉 Activity 时 `onFinished` 永不触发，
 * `solve()` 的 continuation 永不 resume，而外层 `CloudflareChallengeHandler.mutex`
 * 一直被持有，后续所有撞盾请求排队卡死。
 *
 * 现在过盾页是导航图内的目的地（`CloudflareRoute`）：
 * 1. [AndroidCloudflareSolver.solve] 把请求挂到 [pending] 并等 [PendingChallenge.done]
 * 2. `MainActivityContent` 观察 [pending]，非空就导航到过盾页
 * 3. 过盾页 route 的 `DisposableEffect` 在 **onDispose 时必然 complete**——
 *    不管是过完自动 pop、用户按返回、还是页面被销毁，等待中的请求都会被放行，
 *    死锁从结构上消失
 *
 * 外层有全局 mutex，同一时刻最多一个挑战，所以 [MutableStateFlow] 够用。
 */
object CloudflareNavBridge {
    class PendingChallenge(val url: String) {
        val done = CompletableDeferred<Unit>()
    }

    val pending = MutableStateFlow<PendingChallenge?>(null)
}

/**
 * 替代迁移前的 `CloudflareInterceptor`。真正挂起（不阻塞线程），
 * 请求所在协程被取消时跟着退出。
 *
 * App 在后台撞盾（下载 worker）时会尝试把 MainActivity 唤到前台——
 * MainActivity 是 singleTask，前台时重复启动等于无操作。Android 10+ 的
 * 后台启动限制可能拦掉这次唤起，但请求仍挂在 [CloudflareNavBridge.pending] 里，
 * 用户下次打开 App 时观察者会立刻把过盾页推出来——比旧实现（唤起失败就只能
 * Toast + 放行）多了一条恢复路径。
 */
class AndroidCloudflareSolver(private val context: Context) : CloudflareSolver {

    override suspend fun solve(url: String) {
        val challenge = CloudflareNavBridge.PendingChallenge(url)
        CloudflareNavBridge.pending.value = challenge

        try {
            context.startActivity(
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        } catch (_: Exception) {
            // 启动被系统拦下也没关系：请求还挂在 pending 上，等 App 回前台再弹
        }

        try {
            challenge.done.await()
        } finally {
            // 只清自己那份，避免覆盖后续新挂上来的挑战
            CloudflareNavBridge.pending.compareAndSet(challenge, null)
        }
    }
}
