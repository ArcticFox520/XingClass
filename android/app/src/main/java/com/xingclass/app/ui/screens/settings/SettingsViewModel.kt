package com.xingclass.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.xingclass.app.R
import com.xingclass.app.core.AppResult
import com.xingclass.app.domain.model.ExportDocument
import com.xingclass.app.domain.model.ThemeMode
import com.xingclass.app.domain.repository.AuthRepository
import com.xingclass.app.domain.repository.CourseRepository
import com.xingclass.app.domain.repository.DiagnosticsRepository
import com.xingclass.app.domain.repository.SettingsRepository
import com.xingclass.app.ui.util.UiText
import com.xingclass.app.ui.util.maskAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.AUTO,
    val maskedAccount: String? = null,
    val lastSyncedAtMillis: Long? = null,
    val versionName: String = "",
    val message: UiText? = null,
    val loggingOut: Boolean = false,
    val exportDocument: ExportDocument? = null,
)

class SettingsViewModel(
    private val authRepository: AuthRepository,
    private val courseRepository: CourseRepository,
    private val settingsRepository: SettingsRepository,
    private val diagnosticsRepository: DiagnosticsRepository,
    versionName: String,
) : ViewModel() {

    private val _state = MutableStateFlow(
        SettingsUiState(
            themeMode = settingsRepository.themeMode.value,
            maskedAccount = authRepository.savedAccount()?.let(::maskAccount),
            versionName = versionName,
        ),
    )
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.themeMode.collect { mode ->
                _state.update { it.copy(themeMode = mode) }
            }
        }
        viewModelScope.launch {
            val cached = runCatching { courseRepository.getCachedCourses() }.getOrNull()
            if (cached != null) {
                _state.update { it.copy(lastSyncedAtMillis = cached.savedAtMillis) }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        settingsRepository.setThemeMode(mode)
    }

    fun clearCache() {
        viewModelScope.launch {
            courseRepository.clearCourses()
            _state.update {
                it.copy(
                    lastSyncedAtMillis = null,
                    message = UiText.res(R.string.schedule_cache_cleared),
                )
            }
        }
    }

    fun dismissMessage() {
        _state.update { it.copy(message = null) }
    }

    fun exportLogs() {
        viewModelScope.launch {
            when (val result = diagnosticsRepository.exportLogs()) {
                is AppResult.Success -> _state.update { it.copy(exportDocument = result.value) }
                is AppResult.Failure -> _state.update {
                    it.copy(message = UiText.res(R.string.settings_export_failed))
                }
            }
        }
    }

    fun exportCourses() {
        viewModelScope.launch {
            when (val result = diagnosticsRepository.exportCourses()) {
                is AppResult.Success -> _state.update { it.copy(exportDocument = result.value) }
                is AppResult.Failure -> _state.update {
                    it.copy(
                        message = if (result.error == com.xingclass.app.core.AppError.NoData) {
                            UiText.res(R.string.settings_export_no_data)
                        } else {
                            UiText.res(R.string.settings_export_failed)
                        },
                    )
                }
            }
        }
    }

    fun consumeExport() {
        _state.update { it.copy(exportDocument = null) }
    }

    fun logout(onLoggedOut: () -> Unit) {
        if (_state.value.loggingOut) return
        _state.update { it.copy(loggingOut = true) }
        viewModelScope.launch {
            authRepository.logout()
            runCatching { courseRepository.clearCourses() }
            _state.update { it.copy(loggingOut = false) }
            onLoggedOut()
        }
    }

    companion object {
        fun factory(
            authRepository: AuthRepository,
            courseRepository: CourseRepository,
            settingsRepository: SettingsRepository,
            diagnosticsRepository: DiagnosticsRepository,
            versionName: String,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SettingsViewModel(
                    authRepository = authRepository,
                    courseRepository = courseRepository,
                    settingsRepository = settingsRepository,
                    diagnosticsRepository = diagnosticsRepository,
                    versionName = versionName,
                )
            }
        }
    }
}
