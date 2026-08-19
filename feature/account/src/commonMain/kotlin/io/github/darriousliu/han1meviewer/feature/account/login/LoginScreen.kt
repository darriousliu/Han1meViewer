package io.github.darriousliu.han1meviewer.feature.account.login

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.unit.dp
import io.github.darriousliu.han1meviewer.core.storage.HANIME_LOGIN_URL
import io.github.darriousliu.han1meviewer.core.common.HanimeConstants.HANIME_URL
import io.github.darriousliu.han1meviewer.core.common.USER_AGENT
import io.github.darriousliu.han1meviewer.core.ui.component.appbar.HanimeScaffold
import io.github.darriousliu.han1meviewer.core.ui.preview.ComponentPreview
import dev.nucleusframework.webview.request.RequestInterceptor
import dev.nucleusframework.webview.request.WebRequest
import dev.nucleusframework.webview.request.WebRequestInterceptResult
import dev.nucleusframework.webview.web.WebView
import dev.nucleusframework.webview.web.WebViewNavigator
import dev.nucleusframework.webview.web.rememberWebViewNavigator
import dev.nucleusframework.webview.web.rememberWebViewState
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.cancel
import io.github.darriousliu.han1meviewer.core.resource.email
import io.github.darriousliu.han1meviewer.core.resource.ic_baseline_scan_24
import io.github.darriousliu.han1meviewer.core.resource.login
import io.github.darriousliu.han1meviewer.core.resource.password
import io.github.darriousliu.han1meviewer.core.resource.scan_for_cookies
import io.github.darriousliu.han1meviewer.core.resource.try_login_here
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * WebView 登录页。
 *
 * 行为约定：进入先清全部 cookie、清完才加载登录页；重定向回站内任一基础域时拦截，
 * 抓当前 cookie 串交给 [onCookiesCaptured]；主框架加载失败弹账密登录 [LoginDialog] 兜底；
 * 返回键网页能后退就后退，不能才退出本页；下拉刷新重新加载登录页。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalComposeUiApi::class)
@Composable
fun LoginScreen(
    isLoggingIn: Boolean,
    onBack: () -> Unit,
    onCookiesCaptured: (String) -> Unit,
    onPasswordLogin: (username: String, password: String) -> Unit,
    onOpenManualCookies: () -> Unit,
) {
    var showLoginDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // 先落在空白页：清 cookie 完成后才加载登录页，避免登录页先种下将被清掉的会话 cookie
    val state = rememberWebViewState("about:blank") {
        customUserAgentString = USER_AGENT
        isJavaScriptEnabled = true
        // domStorage 是 Android 专属设置，在平台子块里
        androidWebSettings.domStorageEnabled = true
    }

    // 重定向命中站内域 = 登录成功。只触发一次（重定向可能带出多个请求）。
    var captured by remember { mutableStateOf(false) }
    val navigator = rememberWebViewNavigator(
        requestInterceptor = remember {
            object : RequestInterceptor {
                override fun onInterceptUrlRequest(
                    request: WebRequest,
                    navigator: WebViewNavigator,
                ): WebRequestInterceptResult {
                    val isSameUrl = HANIME_URL.contains(request.url)
                    if (request.isRedirect && isSameUrl && !captured) {
                        captured = true
                        scope.launch {
                            val cookies = state.cookieManager.getCookies(request.url)
                                .joinToString("; ") { "${it.name}=${it.value}" }
                            onCookiesCaptured(cookies)
                        }
                        return WebRequestInterceptResult.Reject
                    }
                    return WebRequestInterceptResult.Allow
                }
            }
        },
    )

    LaunchedEffect(Unit) {
        state.cookieManager.removeAllCookies()
        navigator.loadUrl(HANIME_LOGIN_URL)
    }

    // 主框架加载失败 → 账密登录兜底
    LaunchedEffect(state.errorsForCurrentRequest.size) {
        if (state.errorsForCurrentRequest.any { it.isFromMainFrame }) {
            showLoginDialog = true
        }
    }

    BackHandler(enabled = navigator.canGoBack) {
        navigator.navigateBack()
    }

    if (showLoginDialog) {
        LoginDialog(
            isLoggingIn = isLoggingIn,
            onDismiss = { showLoginDialog = false },
            onLogin = onPasswordLogin,
        )
    }

    val refreshingState = rememberPullToRefreshState()
    HanimeScaffold(
        title = stringResource(Res.string.login),
        onBack = onBack,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(stringResource(Res.string.scan_for_cookies)) },
                icon = {
                    Icon(
                        painter = painterResource(Res.drawable.ic_baseline_scan_24),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                },
                onClick = onOpenManualCookies,
            )
        },
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { navigator.loadUrl(HANIME_LOGIN_URL) },
            state = refreshingState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    state = refreshingState,
                    isRefreshing = state.isLoading,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        ) {
            WebView(
                state = state,
                modifier = Modifier.fillMaxSize(),
                navigator = navigator,
            )
        }
    }
}

@Composable
fun LoginDialog(
    isLoggingIn: Boolean,
    onDismiss: () -> Unit,
    onLogin: (username: String, password: String) -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.try_login_here)) },
        text = {
            Column {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(Res.string.email)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoggingIn,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(Res.string.password)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoggingIn,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onLogin(username, password) },
                enabled = username.isNotBlank() && password.isNotBlank() && !isLoggingIn,
            ) {
                Text(stringResource(Res.string.login))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoggingIn) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun LoginDialogPreview() {
    ComponentPreview {
        LoginDialog(
            isLoggingIn = false,
            onDismiss = {},
            onLogin = { _, _ -> },
        )
    }
}
