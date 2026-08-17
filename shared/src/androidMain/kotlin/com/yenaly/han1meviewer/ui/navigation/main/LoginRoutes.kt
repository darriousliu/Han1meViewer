package com.yenaly.han1meviewer.ui.navigation.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.NetworkRepo
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.login
import com.yenaly.han1meviewer.ui.screen.login.LoginScreen
import com.yenaly.han1meviewer.ui.screen.login.ManualInputCookiesScreen
import com.yenaly.han1meviewer.util.showShortToast
import kotlinx.coroutines.launch

/*
 * 原来的 LoginActivity / ManualInputCookiesActivity，Step 17 合并成两个导航目的地。
 * 屏幕在 commonMain（composewebview），平台副作用（Toast、登录网络请求的错误提示）在这里。
 */

/**
 * @param onLoginFinished 登录成功后的收尾：pop 回上一页并刷新首页
 *   （对应原来 `loginDataLauncher` 收到 `RESULT_OK` 后的 `viewModel.getHomePage()`）
 */
@Composable
fun LoginRouteScreen(
    onBack: () -> Unit,
    onOpenManualCookies: () -> Unit,
    onLoginFinished: () -> Unit,
) {
    var isLoggingIn by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LoginScreen(
        isLoggingIn = isLoggingIn,
        onBack = onBack,
        onCookiesCaptured = { cookies ->
            login(cookies)
            onLoginFinished()
        },
        onPasswordLogin = { username, password ->
            isLoggingIn = true
            scope.launch {
                NetworkRepo.login(username, password).collect { state ->
                    when (state) {
                        WebsiteState.Loading -> Unit

                        is WebsiteState.Error -> {
                            isLoggingIn = false
                            state.throwable.printStackTrace()
                            if (state.throwable is IllegalStateException) {
                                showShortToast(R.string.account_or_password_wrong)
                            } else {
                                showShortToast(R.string.login_failed)
                            }
                        }

                        is WebsiteState.Success -> {
                            login(state.info)
                            showShortToast(R.string.login_success)
                            onLoginFinished()
                        }
                    }
                }
            }
        },
        onOpenManualCookies = onOpenManualCookies,
    )
}

@Composable
fun ManualCookiesRouteScreen(
    onBack: () -> Unit,
    onLoginFinished: () -> Unit,
) {
    ManualInputCookiesScreen(
        onBack = onBack,
        onCookieScanned = { cookie ->
            login(cookie)
            onLoginFinished()
        },
    )
}
