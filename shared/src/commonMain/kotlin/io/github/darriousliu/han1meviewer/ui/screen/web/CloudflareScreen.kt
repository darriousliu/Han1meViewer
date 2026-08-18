package io.github.darriousliu.han1meviewer.ui.screen.web

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import io.github.darriousliu.han1meviewer.core.storage.Preferences
import io.github.darriousliu.han1meviewer.core.common.USER_AGENT
import io.github.darriousliu.han1meviewer.core.ui.component.appbar.HanimeScaffold
import io.github.darriousliu.han1meviewer.core.common.util.CookieString
import dev.nucleusframework.webview.web.LoadingState
import dev.nucleusframework.webview.web.WebView
import dev.nucleusframework.webview.web.rememberWebViewNavigator
import dev.nucleusframework.webview.web.rememberWebViewState
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.complete_cloudflare_verification
import io.github.darriousliu.han1meviewer.core.resource.complete_cloudflare_verification_with_warning
import io.github.darriousliu.han1meviewer.core.resource.current_webview_version
import io.github.darriousliu.han1meviewer.core.resource.version_check_failed
import io.github.darriousliu.han1meviewer.core.resource.webview_version_too_low
import io.github.darriousliu.han1meviewer.core.resource.webview_version_unknown
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import io.github.darriousliu.han1meviewer.core.navigation.CloudflareRoute

/**
 * Cloudflare 过盾页。原来是独立的 `CloudflareActivity`（`NEW_TASK` + companion 静态回调），
 * Step 17 合并成导航目的地并换 composewebview 重写，整个屏幕进 commonMain。
 *
 * 过盾判定与原实现一致：加载进度 ≥90% 后延迟 1 秒读 `document.head.innerHTML`，
 * 三个 challenge 标记（form/success-text/error-text）都不在了，且 cookie 里出现
 * `cf_clearance`，就认为过了——写入 [Preferences.cloudFlareCookie] 并回调 [onSolved]。
 *
 * 顺手修掉原实现的一个小病：UA 版本提示原来在 `loadUrl` **之前**对空白 WebView
 * 求值 `navigator.userAgent`，拿到的是空页的 UA；现在等首次进入加载态之后再求值。
 *
 * 「用户没过盾直接关掉」的兜底不在这里——route 侧的 `DisposableEffect` 保证
 * 无论怎么离开这个页面，等待中的请求都会被放行（见 `MainNavHost` 的 CloudflareRoute）。
 */
@Composable
fun CloudflareScreen(
    url: String,
    onSolved: () -> Unit,
    onClose: () -> Unit,
) {
    val state = rememberWebViewState(url) {
        customUserAgentString = USER_AGENT
        isJavaScriptEnabled = true
        androidWebSettings.domStorageEnabled = true
    }
    val navigator = rememberWebViewNavigator()

    var tipText by remember { mutableStateOf("") }
    val baseWarning = stringResource(Res.string.complete_cloudflare_verification_with_warning)

    // evaluateJavaScript 的回调不是挂起环境、线程也不定——结果先写进 state，
    // 由下面的 LaunchedEffect 在协程里消费（getString 是挂起函数）
    var pendingVersionCode by remember { mutableStateOf<String?>(null) }
    var pendingCookieCheck by remember { mutableIntStateOf(0) }

    // UA 版本提示：等真的开始加载页面再问 WebView（原实现在空白页上问，结果没意义）
    var uaChecked by remember { mutableStateOf(false) }
    LaunchedEffect(baseWarning) { tipText = baseWarning }
    LaunchedEffect(state.loadingState, uaChecked) {
        if (uaChecked || state.loadingState !is LoadingState.Loading) return@LaunchedEffect
        uaChecked = true
        navigator.evaluateJavaScript("navigator.userAgent") { output ->
            val userAgent = output
                .removeSurrounding("\"")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
            val chromePattern = "Chrome/(\\d+\\.\\d+\\.\\d+\\.\\d+)".toRegex()
            val versionCode =
                chromePattern.find(userAgent)?.groupValues?.getOrNull(1) ?: userAgent
            pendingVersionCode = versionCode
        }
    }
    // evaluateJavaScript 的回调线程不定，把版本号中转成 state 再在协程里拼文案
    LaunchedEffect(pendingVersionCode) {
        val versionCode = pendingVersionCode ?: return@LaunchedEffect
        var t = baseWarning + getString(Res.string.current_webview_version, versionCode)
        t += try {
            val parts = versionCode.split(".").map { it.toIntOrNull() ?: 0 }
            when {
                parts.size < 4 -> getString(Res.string.webview_version_unknown)
                parts[0] < 120 -> getString(Res.string.webview_version_too_low)
                else -> ""
            }
        } catch (_: Exception) {
            getString(Res.string.version_check_failed)
        }
        tipText = t
    }

    // 过盾判定：进度 >= 90% 或加载完成后，延迟 1 秒查 head 里的 challenge 标记
    var solved by remember { mutableStateOf(false) }
    LaunchedEffect(state.loadingState) {
        val loading = state.loadingState
        val ready = loading is LoadingState.Finished ||
                (loading is LoadingState.Loading && loading.progress >= 0.9f)
        if (!ready || solved) return@LaunchedEffect
        delay(1000)
        navigator.evaluateJavaScript("document.head.innerHTML") { html ->
            if (!html.contains("#challenge-form") &&
                !html.contains("#challenge-success-text") &&
                !html.contains("#challenge-error-text")
            ) {
                pendingCookieCheck++
            }
        }
    }
    LaunchedEffect(pendingCookieCheck) {
        if (pendingCookieCheck == 0 || solved) return@LaunchedEffect
        val cookies = state.cookieManager.getCookies(url)
            .joinToString("; ") { "${it.name}=${it.value}" }
        if (cookies.contains("cf_clearance")) {
            solved = true
            Preferences.cloudFlareCookie = CookieString(cookies)
            onSolved()
        }
    }

    val progress = (state.loadingState as? LoadingState.Loading)?.progress ?: 0f
    HanimeScaffold(
        title = stringResource(Res.string.complete_cloudflare_verification),
        onBack = onClose,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            WebView(
                state = state,
                modifier = Modifier.fillMaxSize(),
                navigator = navigator,
            )

            if (progress > 0f && progress < 1f) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    strokeCap = StrokeCap.Round,
                )
            }

            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Text(
                    text = tipText,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
