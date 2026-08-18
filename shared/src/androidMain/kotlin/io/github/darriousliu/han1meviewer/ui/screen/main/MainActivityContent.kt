package io.github.darriousliu.han1meviewer.ui.screen.main

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import io.github.darriousliu.han1meviewer.core.storage.Preferences
import io.github.darriousliu.han1meviewer.R
import io.github.darriousliu.han1meviewer.core.common.exception.CloudFlareBlockedException
import io.github.darriousliu.han1meviewer.core.model.github.Latest
import io.github.darriousliu.han1meviewer.core.network.CloudflareNavBridge
import io.github.darriousliu.han1meviewer.core.common.state.PageState
import io.github.darriousliu.han1meviewer.ui.activity.MainActivity
import io.github.darriousliu.han1meviewer.ui.component.HanimeToastHost
import io.github.darriousliu.han1meviewer.ui.component.UpdateDialog
import io.github.darriousliu.han1meviewer.ui.component.UsageNoticeDialog
import io.github.darriousliu.han1meviewer.ui.navigation.CloudflareRoute
import io.github.darriousliu.han1meviewer.ui.navigation.HanimeRoute
import io.github.darriousliu.han1meviewer.ui.navigation.HomeRoute
import io.github.darriousliu.han1meviewer.ui.navigation.LoginRoute
import io.github.darriousliu.han1meviewer.ui.navigation.main.MainDestinationSpec
import io.github.darriousliu.han1meviewer.ui.navigation.main.MainNavDisplay
import io.github.darriousliu.han1meviewer.ui.navigation.main.handleMainIntent
import io.github.darriousliu.han1meviewer.ui.navigation.main.navigateDrawerDestination
import io.github.darriousliu.han1meviewer.ui.navigation.navigateSafely
import io.github.darriousliu.han1meviewer.ui.navigation.rememberHanimeBackStack
import io.github.darriousliu.han1meviewer.ui.screen.home.homepage.HomePageViewModel
import io.github.darriousliu.han1meviewer.ui.theme.HanimeTheme
import io.github.darriousliu.han1meviewer.ui.viewmodel.AppViewModel
import io.github.darriousliu.han1meviewer.util.getUpdateIfExists
import io.github.darriousliu.han1meviewer.util.installApkPackage
import io.github.darriousliu.han1meviewer.util.requestPostNotificationPermission
import io.github.darriousliu.han1meviewer.util.showShortToast
import io.github.darriousliu.han1meviewer.worker.HUpdateWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
fun MainActivityContent(
    activity: MainActivity,
    viewModel: HomePageViewModel,
    pendingNavigationRequests: Flow<Intent>,
    showAuthGuard: Boolean,
    onOpenAccount: () -> Unit,
    onLogoutClick: () -> Unit,
    onSwitchSiteClick: () -> Unit,
    onNavigateControllerReady: (NavBackStack<HanimeRoute>) -> Unit,
) {
    // 切语言时把整棵组合树重建，让 compose-resources 重新解析字符串。
    // Android 上 AppCompat 自己也会重建 Activity（平台行为），这层是冗余的；
    // 它真正起作用的是 desktop/iOS —— 那两端没有重建机制。
    val appLanguage by Preferences.appLanguageStateFlow.collectAsStateWithLifecycle()
    key(appLanguage) {
        HanimeTheme {
            HanimeToastHost {
                // 用自家的建栈入口而不是 nav3 的 rememberNavBackStack，理由见 rememberHanimeBackStack：
                // 栈元素收窄到 sealed 的 HanimeRoute，靠 sealed 自动多态恢复，不用手写 SerializersModule
                val backStack = rememberHanimeBackStack(HomeRoute)
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                var pendingUpdate by remember { mutableStateOf<Latest?>(null) }
                var showUsageNotice by remember { mutableStateOf(!Preferences.usageNoticeAccepted) }
                val isDrawerOpen =
                    drawerState.currentValue == DrawerValue.Open || drawerState.targetValue == DrawerValue.Open

                // nav2 时代靠 MainNavHost 的 onDestinationChanged 回调反向推送，nav3 直接从栈顶派生
                val currentMainDestination =
                    MainDestinationSpec.fromKey(backStack.lastOrNull()) ?: MainDestinationSpec.Home

                val homeState by viewModel.homePageFlow.collectAsStateWithLifecycle()
                val isLoggedIn by Preferences.loginStateFlow.collectAsStateWithLifecycle()
                val headerAvatarUrl = if (isLoggedIn) {
                    (homeState as? PageState.Success)?.info?.page?.avatarUrl
                } else {
                    null
                }
                val headerUsername = if (isLoggedIn) {
                    (homeState as? PageState.Success)?.info?.page?.username
                } else {
                    null
                }
                val headerIsLoading = isLoggedIn && homeState is PageState.Loading
                val selectedDrawerDestination = currentMainDestination.drawerDestination

                LaunchedEffect(backStack) {
                    onNavigateControllerReady(backStack)
                }
                LaunchedEffect(Unit) {
                    pendingNavigationRequests.collect { intent ->
                        backStack.handleMainIntent(intent)
                    }
                }
                LaunchedEffect(Unit) {
                    AppViewModel.pendingUpdateDialog.collect { latest ->
                        Preferences.lastUpdatePopupTime = kotlin.time.Clock.System.now().epochSeconds
                        pendingUpdate = latest
                    }
                }
                LaunchedEffect(viewModel) {
                    viewModel.sessionExpiredMessage.collect { event ->
                        event.message?.let(::showShortToast) ?: showShortToast(getString(event.fallbackRes))
                    }
                }
                // 网络层撞上 Cloudflare challenge 时（CloudflareNavBridge.pending 非空）把过盾页推出来。
                // 请求会一直挂在 pending 上等，App 从后台回前台时这里也会立刻补弹。
                LaunchedEffect(backStack) {
                    CloudflareNavBridge.pending.collect { challenge ->
                        if (challenge != null) {
                            backStack.navigateSafely(CloudflareRoute(challenge.url))
                        }
                    }
                }
                LaunchedEffect(homeState) {
                    if (homeState is PageState.Error) {
                        val throwable = (homeState as PageState.Error).throwable
                        if (throwable is CloudFlareBlockedException) {
                            Log.e("error", "被屏蔽时的处理")
                        }
                    }
                }
                MainActivityScaffold(
                    drawerState = drawerState,
                    drawerEnabled = currentMainDestination.drawerEnabled,
                    selectedDestination = selectedDrawerDestination,
                    avatarUrl = headerAvatarUrl,
                    username = headerUsername,
                    isLoggedIn = isLoggedIn,
                    isLoading = headerIsLoading,
                    currentSite = Preferences.baseUrl,
                    onAvatarClick = {
                        if (isLoggedIn) {
                            scope.launch { drawerState.close() }
                            onOpenAccount()
                        } else {
                            // 原来是 gotoLoginActivity()（StartActivityForResult），
                            // Step 17 起登录是导航图内的目的地
                            scope.launch { drawerState.close() }
                            backStack.navigateSafely(LoginRoute)
                        }
                    },
                    onAvatarLongClick = {
                        onLogoutClick()
                    },
                    onSwitchSiteClick = onSwitchSiteClick,
                    onDrawerItemSelected = { destination ->
                        val handled = backStack.navigateDrawerDestination(
                            destination = destination,
                            isLoggedIn = isLoggedIn,
                            onRequireLogin = { showShortToast(R.string.login_first) },
                        )
                        if (handled) {
                            scope.launch { drawerState.close() }
                        }
                        handled
                    },
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        MainNavDisplay(
                            activity = activity,
                            backStack = backStack,
                            isDrawerOpen = isDrawerOpen,
                            onOpenDrawer = {
                                if (currentMainDestination.drawerEnabled) {
                                    scope.launch { drawerState.open() }
                                }
                            },
                        )
                        if (showAuthGuard) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.55f)),
                            )
                        }

                        pendingUpdate?.let { latest ->
                            UpdateDialog(
                                latest = latest,
                                onDismiss = { pendingUpdate = null },
                                onConfirm = {
                                    pendingUpdate = null
                                    scope.launch {
                                        val file = activity.getUpdateIfExists(latest)
                                        if (file != null) {
                                            activity.installApkPackage(file)
                                        } else {
                                            if (activity.requestPostNotificationPermission()) {
                                                HUpdateWorker.enqueue(activity.applicationContext, latest)
                                                showShortToast(R.string.update_download_background)
                                            }
                                        }
                                    }
                                },
                            )
                        }
                        UsageNoticeDialog(
                            visible = showUsageNotice,
                            onAccepted = {
                                Preferences.usageNoticeAccepted = true
                                showUsageNotice = false
                            },
                            onDeclined = { activity.finish() },
                        )
                    }
                }
            }
        }
    }
}
