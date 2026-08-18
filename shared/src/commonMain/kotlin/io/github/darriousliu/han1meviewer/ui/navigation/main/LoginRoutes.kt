package io.github.darriousliu.han1meviewer.ui.navigation.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import io.github.darriousliu.han1meviewer.logic.NetworkRepo
import io.github.darriousliu.han1meviewer.logic.state.WebsiteState
import io.github.darriousliu.han1meviewer.login
import io.github.darriousliu.han1meviewer.ui.component.LocalToaster
import io.github.darriousliu.han1meviewer.ui.component.showShort
import io.github.darriousliu.han1meviewer.ui.screen.login.LoginScreen
import io.github.darriousliu.han1meviewer.ui.screen.login.ManualInputCookiesScreen
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.account_or_password_wrong
import io.github.darriousliu.han1meviewer.core.resource.login_failed
import io.github.darriousliu.han1meviewer.core.resource.login_success
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

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
    val toaster = LocalToaster.current
    // stringResource 是 composable，回调里用不了，先在这里解开
    val wrongCredentials = stringResource(Res.string.account_or_password_wrong)
    val loginFailed = stringResource(Res.string.login_failed)
    val loginSuccess = stringResource(Res.string.login_success)

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
                                toaster.showShort(wrongCredentials)
                            } else {
                                toaster.showShort(loginFailed)
                            }
                        }

                        is WebsiteState.Success -> {
                            login(state.info)
                            toaster.showShort(loginSuccess)
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
