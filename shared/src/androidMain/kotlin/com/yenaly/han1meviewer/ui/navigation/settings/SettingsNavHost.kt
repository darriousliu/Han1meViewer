package com.yenaly.han1meviewer.ui.navigation.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavBackStack
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.navigation.HanimeRoute
import com.yenaly.han1meviewer.util.findActivity
import com.yenaly.han1meviewer.util.logScreenViewEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScaffold(
    backStack: NavBackStack<HanimeRoute>,
    fallbackDestination: HanimeRoute,
    actions: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val activity = context.findActivity<MainActivity>()
    val currentDestination = SettingsDestinationSpec.fromKey(backStack.lastOrNull())
        ?: SettingsDestinationSpec.Home

    fun navigateBack() {
        // nav2 是 popBackStack() 返回 false（栈底、退无可退）才正向 navigate 到逻辑父页，
        // size > 1 正好对上「栈里除了自己还有别人」
        if (backStack.size > 1) backStack.removeLastOrNull() else backStack.add(fallbackDestination)
    }

    LaunchedEffect(currentDestination) {
        activity.logScreenViewEvent(currentDestination.screenClassName)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (currentDestination.showToolbar) {
                TopAppBar(
                    title = { Text(stringResource(currentDestination.titleRes)) },
                    navigationIcon = {
                        FilledIconButton(onClick = ::navigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    },
                    actions = { actions() },
                    modifier = Modifier.statusBarsPadding(),
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            content()
        }
    }
}
