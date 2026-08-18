package io.github.darriousliu.han1meviewer.core.ui.component

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.dokar.sonner.ToastType
import com.dokar.sonner.Toaster
import com.dokar.sonner.ToasterDefaults
import com.dokar.sonner.ToasterState
import com.dokar.sonner.rememberToasterState

/*
 * commonMain 的轻提示。替代 androidMain 那套 `showShortToast` / `showLongToast`
 * （`util/Toast.kt`，靠 `applicationContext` 拿 Context，只有 Android 有）。
 *
 * 用的是 sonner（io.github.dokar3:sonner）——依赖早就声明在 commonMain 里，
 * 只是一直没接线。它的 `show` 是**普通函数**（非 composable 非 suspend），
 * 所以拿到 state 之后可以在任意回调里调，调用点形状和原来的 showShortToast 一样。
 *
 * ⚠️ 观感与原生 Toast 的差别：这是 App 内的 Compose 浮层，**盖不住 App 之外**，
 * App 退到后台就看不见。没有 Compose 作用域的地方（worker、util）**继续用原生 Toast**，
 * 两套会共存一段时间，这是有意的。
 *
 * ⚠️ 不做全局单例（`object HanimeToast { var state … }`）——那就是第二十节明令
 * 废除的「lambda 注入」：赋值点难追踪、忘了注入只有运行时才暴露。一律走 [LocalToaster]。
 */

/** App 根部由 [HanimeToastHost] 提供。没套宿主就用会直接报错，而不是静默不弹。 */
val LocalToaster = staticCompositionLocalOf<ToasterState> {
    error("没有 Toaster 宿主：把根内容套进 HanimeToastHost { … }")
}

/**
 * 挂在 App 根部：内容之上盖一层 Toaster，并把 [ToasterState] 通过 [LocalToaster] 往下传。
 *
 * `safeDrawingPadding` 是为了不被底部导航栏/手势条盖住——sonner 默认
 * `alignment = Alignment.BottomCenter`，不避让就会压在导航栏上。
 */
@Composable
fun HanimeToastHost(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val toaster = rememberToasterState()
    CompositionLocalProvider(LocalToaster provides toaster) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
            Toaster(
                state = toaster,
                modifier = Modifier.safeDrawingPadding(),
                darkTheme = darkTheme,
                richColors = true,
            )
        }
    }
}

/** 对齐原生 `Toast.LENGTH_SHORT`（约 2 秒）。 */
fun ToasterState.showShort(message: String) {
    show(message = message, type = ToastType.Normal, duration = ToasterDefaults.DurationShort)
}

/**
 * 对齐原生 `Toast.LENGTH_LONG`（约 3.5 秒）。
 *
 * 用 `DurationDefault`(4s) 而不是 `DurationLong`(8s)——后者比原生长一倍多。
 */
fun ToasterState.showLong(message: String) {
    show(message = message, type = ToastType.Normal, duration = ToasterDefaults.DurationDefault)
}
