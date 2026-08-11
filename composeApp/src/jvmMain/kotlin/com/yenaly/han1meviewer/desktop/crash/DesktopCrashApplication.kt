package com.yenaly.han1meviewer.desktop.crash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.LocalWindowExceptionHandlerFactory
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel

/** Application-owned scope for Desktop business jobs; fatal children use the process controller. */
internal val LocalDesktopApplicationScope = staticCompositionLocalOf<CoroutineScope> {
    error("Desktop application scope is unavailable")
}

@OptIn(ExperimentalComposeUiApi::class)
internal fun runNormalDesktopApplication(controller: DesktopFatalController) {
    val rootScope = controller.createApplicationScope()
    try {
        application(exitProcessOnExit = false) {
            DisposableEffect(controller) {
                val binding = controller.bindNormalApplicationExit(::exitApplication)
                onDispose(binding::close)
            }
            CompositionLocalProvider(
                LocalWindowExceptionHandlerFactory provides
                    controller.normalWindowExceptionHandlerFactory,
                LocalDesktopApplicationScope provides rootScope,
            ) {
                Window(
                    onCloseRequest = ::exitApplication,
                    state = rememberWindowState(size = DpSize(900.dp, 640.dp)),
                    title = "Han1meViewer",
                ) {
                    DesktopBootstrapContent()
                }
            }
        }
    } finally {
        rootScope.cancel()
    }
}

@OptIn(ExperimentalComposeUiApi::class)
internal fun runDesktopCrashApplication(
    controller: DesktopFatalController,
    incident: DesktopFatalIncident,
) {
    application(exitProcessOnExit = false) {
        CompositionLocalProvider(
            LocalWindowExceptionHandlerFactory provides
                controller.crashWindowExceptionHandlerFactory,
        ) {
            Window(
                onCloseRequest = ::exitApplication,
                state = rememberWindowState(size = DpSize(920.dp, 720.dp)),
                title = "Han1meViewer - 崩溃报告",
            ) {
                DesktopCrashPage(
                    incident = incident,
                    actions = controller.actions,
                    onExit = ::exitApplication,
                )
            }
        }
    }
}

@Composable
private fun DesktopBootstrapContent() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Han1meViewer Desktop", style = MaterialTheme.typography.headlineMedium)
                    Text("共享应用界面正在迁移中。")
                }
            }
        }
    }
}

@Composable
private fun DesktopCrashPage(
    incident: DesktopFatalIncident,
    actions: DesktopCrashActions,
    onExit: () -> Unit,
) {
    var actionMessage by remember { mutableStateOf<String?>(null) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
            ) {
                Text("应用已停止运行", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "正常应用已完全关闭。下面是已脱敏的首个异常报告，旧进程不会自动重启。",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    incident.logFile?.let { "日志：$it" }
                        ?: "日志写入失败；报告仍保留在当前窗口中。",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(16.dp))

                SelectionContainer(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x0F000000))
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Text(
                            incident.report.rendered,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                actionMessage?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = {
                            actionMessage = actions
                                .copyReportToClipboard(incident.report.rendered)
                                .toUserMessage(success = "报告已复制。")
                        },
                    ) {
                        Text("复制报告")
                    }
                    OutlinedButton(
                        onClick = {
                            actionMessage = actions
                                .openLogDirectory(incident.logDirectory)
                                .toUserMessage(success = "已打开日志目录。")
                        },
                    ) {
                        Text("打开日志目录")
                    }
                    OutlinedButton(
                        enabled = actions.canRestart,
                        onClick = {
                            when (val result = actions.restartOnUserRequest()) {
                                ActionResult.Success -> onExit()
                                is ActionResult.Failure -> {
                                    actionMessage = result.toUserMessage(success = "")
                                }
                            }
                        },
                    ) {
                        Text("手动重启")
                    }
                    Spacer(Modifier.weight(1f))
                    Button(onClick = onExit) {
                        Text("退出")
                    }
                }
            }
        }
    }
}

private fun ActionResult.toUserMessage(success: String): String = when (this) {
    ActionResult.Success -> success
    is ActionResult.Failure -> when (reason) {
        FailureReason.ClipboardUnavailable -> "系统剪贴板不可用。"
        FailureReason.LogDirectoryUnavailable -> "无法创建日志目录。"
        FailureReason.OpenLogDirectoryUnsupported -> "当前系统不支持打开日志目录。"
        FailureReason.OpenLogDirectoryFailed -> "打开日志目录失败。"
        FailureReason.RestartUnavailable -> "无法取得安全的重启命令。"
        FailureReason.RestartFailed -> "启动新进程失败；当前崩溃页会继续保留。"
    }
}
