package com.xingclass.app.ui.util

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.xingclass.app.R
import com.xingclass.app.core.AppError

/**
 * Localized UI copy that can be produced by ViewModels without holding a Context.
 */
sealed interface UiText {
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText

    data class Raw(val value: String) : UiText

    companion object {
        fun res(@StringRes id: Int, vararg args: Any): UiText = Res(id, args.toList())
    }
}

@Composable
fun UiText.asString(): String = when (this) {
    is UiText.Res -> if (args.isEmpty()) {
        stringResource(id)
    } else {
        stringResource(id, *args.toTypedArray())
    }

    is UiText.Raw -> value
}

fun AppError.toUiText(): UiText = when (this) {
    AppError.Network -> UiText.res(R.string.error_network)
    AppError.InvalidCredentials -> UiText.res(R.string.error_invalid_credentials)
    AppError.LoginFailed -> UiText.res(R.string.error_login_failed)
    AppError.SessionExpired -> UiText.res(R.string.error_session_expired)
    AppError.CourseFetch -> UiText.res(R.string.error_course_fetch)
    AppError.Server -> UiText.res(R.string.error_server)
    AppError.NoData -> UiText.res(R.string.settings_export_no_data)
    AppError.Unknown -> UiText.res(R.string.error_unknown)
}
