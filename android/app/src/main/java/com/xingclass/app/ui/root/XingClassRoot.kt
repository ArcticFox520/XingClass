package com.xingclass.app.ui.root

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xingclass.app.di.AppContainer
import com.xingclass.app.ui.components.neumorphic.NeumorphicElevation
import com.xingclass.app.ui.components.neumorphic.NeumorphicLoadingDots
import com.xingclass.app.ui.components.neumorphic.NeumorphicSurface
import com.xingclass.app.ui.screens.login.LoginScreen
import com.xingclass.app.ui.screens.login.LoginViewModel
import com.xingclass.app.ui.screens.schedule.ScheduleScreen
import com.xingclass.app.ui.screens.schedule.ScheduleViewModel
import com.xingclass.app.ui.screens.settings.SettingsScreen
import com.xingclass.app.ui.screens.settings.SettingsViewModel
import com.xingclass.app.ui.theme.LocalNeumorphicColors
import com.xingclass.app.ui.theme.XingClassType
import com.xingclass.app.ui.theme.XingClassTheme

/**
 * Single activity root. Navigation is a tiny state machine (checking / login / schedule /
 * settings); no navigation library is needed for four destinations.
 */
@Composable
fun XingClassRoot(container: AppContainer) {
    val rootViewModel: RootViewModel = viewModel(factory = RootViewModel.factory(container))
    val rootState by rootViewModel.state.collectAsStateWithLifecycle()
    val view = LocalView.current

    SideEffect {
        val activity = view.context.findActivity() ?: return@SideEffect
        WindowCompat.getInsetsController(activity.window, view).apply {
            isAppearanceLightStatusBars = !rootState.darkTheme
            isAppearanceLightNavigationBars = !rootState.darkTheme
        }
    }

    XingClassTheme(darkTheme = rootState.darkTheme) {
        val colors = LocalNeumorphicColors.current
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .safeDrawingPadding(),
        ) {
            when (rootState.destination) {
                RootDestination.CHECKING -> BootScreen()

                RootDestination.LOGIN -> LoginRoute(
                    container = container,
                    rootState = rootState,
                    rootViewModel = rootViewModel,
                )

                RootDestination.SCHEDULE -> ScheduleRoute(
                    container = container,
                    rootState = rootState,
                    rootViewModel = rootViewModel,
                )

                RootDestination.SETTINGS -> SettingsRoute(
                    container = container,
                    rootState = rootState,
                    rootViewModel = rootViewModel,
                )
            }
        }
    }
}

@Composable
private fun LoginRoute(
    container: AppContainer,
    rootState: RootUiState,
    rootViewModel: RootViewModel,
) {
    val viewModel: LoginViewModel = viewModel(
        key = "login-${rootState.sessionKey}",
        factory = LoginViewModel.factory(
            authRepository = container.authRepository,
            initialAccount = rootState.savedAccount,
        ),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    LoginScreen(
        state = state,
        sessionNotice = rootState.sessionNotice,
        onAccountChange = viewModel::onAccountChange,
        onPasswordChange = viewModel::onPasswordChange,
        onTogglePasswordVisible = viewModel::togglePasswordVisibility,
        onSubmit = viewModel::submit,
        onLoggedIn = rootViewModel::onLoginSuccess,
    )
}

@Composable
private fun ScheduleRoute(
    container: AppContainer,
    rootState: RootUiState,
    rootViewModel: RootViewModel,
) {
    val viewModel: ScheduleViewModel = viewModel(
        key = "schedule-${rootState.sessionKey}",
        factory = ScheduleViewModel.factory(
            courseRepository = container.courseRepository,
            clock = container.clock,
        ),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    ScheduleScreen(
        state = state,
        themeMode = rootState.themeMode,
        onCycleTheme = rootViewModel::cycleThemeMode,
        onRefresh = { viewModel.refresh(manual = true) },
        onSelectWeek = viewModel::selectWeek,
        onGoToCurrentWeek = viewModel::goToCurrentWeek,
        onDayTabClick = viewModel::toggleDay,
        onCourseClick = { course -> viewModel.openDay(course.day) },
        onCloseDay = viewModel::closeDay,
        onOpenSettings = rootViewModel::onOpenSettings,
        onDismissMessage = viewModel::dismissMessage,
        onSessionExpired = rootViewModel::onSessionExpired,
    )
}

@Composable
private fun SettingsRoute(
    container: AppContainer,
    rootState: RootUiState,
    rootViewModel: RootViewModel,
) {
    val viewModel: SettingsViewModel = viewModel(
        key = "settings-${rootState.sessionKey}",
        factory = SettingsViewModel.factory(
            authRepository = container.authRepository,
            courseRepository = container.courseRepository,
            settingsRepository = container.settingsRepository,
            versionName = container.appVersionName,
        ),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val today = remember { container.clock.now().toLocalDate() }

    SettingsScreen(
        state = state,
        todayDate = today,
        onBack = rootViewModel::onBackFromSettings,
        onThemeChange = viewModel::setThemeMode,
        onClearCache = viewModel::clearCache,
        onLogout = { viewModel.logout(rootViewModel::onLoggedOut) },
        onDismissMessage = viewModel::dismissMessage,
    )
}

@Composable
private fun BootScreen() {
    val colors = LocalNeumorphicColors.current
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            NeumorphicSurface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                elevation = NeumorphicElevation.LARGE,
                contentColor = colors.accent,
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "X",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.accent,
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "XingClass",
                style = XingClassType.wordmark,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(20.dp))
            NeumorphicLoadingDots()
        }
    }
}

private fun Context.findActivity(): Activity? {
    var context: Context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
