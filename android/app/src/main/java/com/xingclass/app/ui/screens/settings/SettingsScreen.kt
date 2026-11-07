package com.xingclass.app.ui.screens.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.xingclass.app.R
import com.xingclass.app.domain.model.ThemeMode
import com.xingclass.app.ui.components.neumorphic.NeumorphicMessageBar
import com.xingclass.app.ui.theme.XingSpacing
import com.xingclass.app.ui.util.shareExportDocument
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.time.LocalDate

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    todayDate: LocalDate,
    onBack: () -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onCryptoKeyChange: (String) -> Unit,
    onSaveCryptoKey: () -> Unit,
    onResetCryptoKey: () -> Unit,
    onClearCache: () -> Unit,
    onLogout: () -> Unit,
    onDismissMessage: () -> Unit,
    onExportLogs: () -> Unit,
    onExportCourses: () -> Unit,
    onExportConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmLogout by remember { mutableStateOf(false) }
    var visibleMessage by remember { mutableStateOf(state.message) }
    val context = LocalContext.current

    BackHandler(onBack = onBack)

    LaunchedEffect(state.message) {
        val message = state.message
        if (message != null) {
            visibleMessage = message
            delay(2400)
            onDismissMessage()
        } else {
            delay(320)
            visibleMessage = null
        }
    }

    LaunchedEffect(state.exportDocument) {
        val document = state.exportDocument ?: return@LaunchedEffect
        val shared = try {
            shareExportDocument(context, document)
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
        onExportConsumed()
        if (!shared) {
            Toast.makeText(context, R.string.settings_export_failed, Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = XingSpacing.lg),
        ) {
            SettingsHeader(onBack = onBack)
            SettingsBody(
                modifier = Modifier.weight(1f),
                state = state,
                todayDate = todayDate,
                onThemeChange = onThemeChange,
                onCryptoKeyChange = onCryptoKeyChange,
                onSaveCryptoKey = onSaveCryptoKey,
                onResetCryptoKey = onResetCryptoKey,
                onClearCache = onClearCache,
                onLogout = { confirmLogout = true },
                onExportLogs = onExportLogs,
                onExportCourses = onExportCourses,
            )
        }

        NeumorphicMessageBar(
            visible = state.message != null,
            message = visibleMessage,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
        )
    }

    if (confirmLogout) {
        LogoutDialog(
            loggingOut = state.loggingOut,
            onDismiss = { confirmLogout = false },
            onConfirm = {
                confirmLogout = false
                onLogout()
            },
        )
    }
}
