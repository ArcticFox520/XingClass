package com.xingclass.app.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.xingclass.app.R
import com.xingclass.app.core.AppResult
import com.xingclass.app.domain.repository.AuthRepository
import com.xingclass.app.ui.util.UiText
import com.xingclass.app.ui.util.toUiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val account: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val loading: Boolean = false,
    val error: UiText? = null,
    val loggedIn: Boolean = false,
)

class LoginViewModel(
    private val authRepository: AuthRepository,
    initialAccount: String,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState(account = initialAccount))
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onAccountChange(value: String) {
        _state.update { it.copy(account = value, error = null) }
    }

    fun onPasswordChange(value: String) {
        _state.update { it.copy(password = value, error = null) }
    }

    fun togglePasswordVisibility() {
        _state.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun submit() {
        val current = _state.value
        if (current.loading) return

        if (current.account.isBlank()) {
            _state.update { it.copy(error = UiText.res(R.string.login_error_account_required)) }
            return
        }
        if (current.password.isEmpty()) {
            _state.update { it.copy(error = UiText.res(R.string.login_error_password_required)) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = authRepository.login(current.account, current.password)) {
                is AppResult.Success -> _state.update {
                    // Clear the password from memory as soon as the session exists.
                    it.copy(loading = false, loggedIn = true, password = "")
                }

                is AppResult.Failure -> _state.update {
                    it.copy(loading = false, error = result.error.toUiText())
                }
            }
        }
    }

    companion object {
        fun factory(
            authRepository: AuthRepository,
            initialAccount: String,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { LoginViewModel(authRepository, initialAccount) }
        }
    }
}
