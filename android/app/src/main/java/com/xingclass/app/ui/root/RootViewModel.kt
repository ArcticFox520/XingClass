package com.xingclass.app.ui.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.xingclass.app.R
import com.xingclass.app.core.AppClock
import com.xingclass.app.domain.model.AutoTheme
import com.xingclass.app.domain.model.SessionStatus
import com.xingclass.app.domain.model.ThemeMode
import com.xingclass.app.domain.repository.AuthRepository
import com.xingclass.app.domain.repository.SettingsRepository
import com.xingclass.app.ui.util.UiText
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RootDestination {
    CHECKING,
    LOGIN,
    SCHEDULE,
    SETTINGS,
}

data class RootUiState(
    val destination: RootDestination = RootDestination.CHECKING,
    val themeMode: ThemeMode = ThemeMode.AUTO,
    val darkTheme: Boolean = false,
    val savedAccount: String = "",
    val sessionNotice: UiText? = null,
    /** Changes when a new session starts/ends so screens get fresh ViewModels. */
    val sessionKey: Int = 0,
)

/**
 * Owns the session gate, the appearance mode and the top level destination.
 *
 * Session check: no local cookie -> login page; server rejects -> login page with a notice;
 * server unreachable -> go to the schedule (the cache is shown) instead of logging the user out.
 */
class RootViewModel(
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: AppClock,
) : ViewModel() {

    private val _state = MutableStateFlow(
        RootUiState(
            themeMode = settingsRepository.themeMode.value,
            savedAccount = authRepository.savedAccount().orEmpty(),
        ).let { state ->
            state.copy(darkTheme = AutoTheme.isDark(state.themeMode, clock.now()))
        },
    )
    val state: StateFlow<RootUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.themeMode.collect { mode ->
                _state.update {
                    it.copy(themeMode = mode, darkTheme = AutoTheme.isDark(mode, clock.now()))
                }
            }
        }
        viewModelScope.launch {
            // Auto theme only depends on the hour: re-evaluate once a minute, never per second.
            while (true) {
                delay(AUTO_THEME_TICK_MS)
                _state.update {
                    it.copy(darkTheme = AutoTheme.isDark(it.themeMode, clock.now()))
                }
            }
        }
        viewModelScope.launch { bootstrap() }
    }

    private suspend fun bootstrap() {
        _state.update { it.copy(destination = RootDestination.CHECKING) }
        val status = if (authRepository.hasLocalSession()) {
            authRepository.checkSession()
        } else {
            SessionStatus.ABSENT
        }
        _state.update { current ->
            when (status) {
                SessionStatus.VALID, SessionStatus.UNKNOWN ->
                    current.copy(destination = RootDestination.SCHEDULE)

                SessionStatus.INVALID -> current.copy(
                    destination = RootDestination.LOGIN,
                    sessionNotice = UiText.res(R.string.login_session_expired),
                )

                SessionStatus.ABSENT -> current.copy(destination = RootDestination.LOGIN)
            }
        }
    }

    fun onLoginSuccess() {
        _state.update {
            it.copy(
                destination = RootDestination.SCHEDULE,
                savedAccount = authRepository.savedAccount().orEmpty(),
                sessionNotice = null,
                sessionKey = it.sessionKey + 1,
            )
        }
    }

    fun onSessionExpired() {
        if (_state.value.destination != RootDestination.SCHEDULE) return
        viewModelScope.launch {
            authRepository.invalidateSession()
            _state.update {
                it.copy(
                    destination = RootDestination.LOGIN,
                    sessionNotice = UiText.res(R.string.login_session_expired),
                    sessionKey = it.sessionKey + 1,
                )
            }
        }
    }

    fun onOpenSettings() {
        _state.update { it.copy(destination = RootDestination.SETTINGS) }
    }

    fun onBackFromSettings() {
        _state.update { it.copy(destination = RootDestination.SCHEDULE) }
    }

    fun onLoggedOut() {
        _state.update {
            it.copy(
                destination = RootDestination.LOGIN,
                savedAccount = "",
                sessionNotice = null,
                sessionKey = it.sessionKey + 1,
            )
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        settingsRepository.setThemeMode(mode)
    }

    fun cycleThemeMode() {
        val next = when (_state.value.themeMode) {
            ThemeMode.AUTO -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.AUTO
        }
        settingsRepository.setThemeMode(next)
    }

    companion object {
        private const val AUTO_THEME_TICK_MS = 60_000L

        fun factory(container: com.xingclass.app.di.AppContainer): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    RootViewModel(
                        authRepository = container.authRepository,
                        settingsRepository = container.settingsRepository,
                        clock = container.clock,
                    )
                }
            }
    }
}
