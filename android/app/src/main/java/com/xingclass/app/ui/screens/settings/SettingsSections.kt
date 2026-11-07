package com.xingclass.app.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.xingclass.app.R
import com.xingclass.app.domain.model.ThemeMode
import com.xingclass.app.ui.components.CryptoKeyEditor
import com.xingclass.app.ui.components.neumorphic.NeumorphicButton
import com.xingclass.app.ui.components.neumorphic.NeumorphicElevation
import com.xingclass.app.ui.components.neumorphic.NeumorphicIconButton
import com.xingclass.app.ui.components.neumorphic.NeumorphicSurface
import com.xingclass.app.ui.components.neumorphic.NeumorphicTab
import com.xingclass.app.ui.theme.LocalNeumorphicColors
import com.xingclass.app.ui.theme.XingClassType
import com.xingclass.app.ui.theme.XingControlSize
import com.xingclass.app.ui.theme.XingRadii
import com.xingclass.app.ui.theme.XingSpacing
import com.xingclass.app.ui.util.syncTimeLabel
import com.xingclass.app.ui.util.themeModeLabel
import java.time.LocalDate

/** Public profile of the author; opened through the system browser, never inside a WebView. */
private const val AUTHOR_PROFILE_URL = "https://github.com/arcticfox520"

@Composable
internal fun SettingsHeader(onBack: () -> Unit) {
    val colors = LocalNeumorphicColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = XingSpacing.sm, bottom = XingSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NeumorphicIconButton(
            onClick = onBack,
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.settings_back),
            size = XingControlSize.iconButton,
            iconSize = XingControlSize.iconGlyph,
        )
        Spacer(Modifier.width(XingSpacing.md))
        Text(
            text = stringResource(R.string.settings_title),
            style = XingClassType.title,
            color = colors.textPrimary,
        )
    }
}

@Composable
internal fun SettingsBody(
    state: SettingsUiState,
    todayDate: LocalDate,
    onThemeChange: (ThemeMode) -> Unit,
    onCryptoKeyChange: (String) -> Unit,
    onSaveCryptoKey: () -> Unit,
    onResetCryptoKey: () -> Unit,
    onClearCache: () -> Unit,
    onLogout: () -> Unit,
    onExportLogs: () -> Unit,
    onExportCourses: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = XingSpacing.xxl),
        verticalArrangement = Arrangement.spacedBy(XingSpacing.xl),
    ) {
        AppearanceSection(state.themeMode, onThemeChange)
        AccountSection(state.maskedAccount, onLogout)
        LoginKeySection(
            draft = state.cryptoKeyDraft,
            savedKey = state.cryptoKey,
            onDraftChange = onCryptoKeyChange,
            onSave = onSaveCryptoKey,
            onReset = onResetCryptoKey,
        )
        DataSection(state.lastSyncedAtMillis, todayDate, onClearCache)
        DiagnosticsSection(onExportLogs, onExportCourses)
        AboutSection(state.versionName)
    }
}

@Composable
private fun AppearanceSection(
    selected: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
) {
    SettingsSection(title = stringResource(R.string.settings_section_appearance)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(XingSpacing.sm),
        ) {
            ThemeMode.entries.forEach { mode ->
                NeumorphicTab(
                    selected = selected == mode,
                    onClick = { onThemeChange(mode) },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text(
                        text = themeModeLabel(mode),
                        style = XingClassType.label,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountSection(
    maskedAccount: String?,
    onLogout: () -> Unit,
) {
    val colors = LocalNeumorphicColors.current
    SettingsSection(title = stringResource(R.string.settings_section_account)) {
        SettingsInfoCard {
            Text(
                text = maskedAccount ?: stringResource(R.string.settings_account_anonymous),
                style = XingClassType.bodyStrong,
                color = colors.textPrimary,
            )
        }
        Spacer(Modifier.height(XingSpacing.sm))
        NeumorphicButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = XingControlSize.actionVerticalPadding),
        ) {
            Text(
                text = stringResource(R.string.settings_logout),
                style = XingClassType.bodyStrong,
                color = colors.danger,
            )
        }
    }
}

@Composable
private fun LoginKeySection(
    draft: String,
    savedKey: String,
    onDraftChange: (String) -> Unit,
    onSave: () -> Unit,
    onReset: () -> Unit,
) {
    SettingsSection(title = stringResource(R.string.settings_section_login_key)) {
        CryptoKeyEditor(
            draft = draft,
            savedKey = savedKey,
            onDraftChange = onDraftChange,
            onSave = onSave,
            onReset = onReset,
        )
    }
}

@Composable
private fun DataSection(
    lastSyncedAtMillis: Long?,
    todayDate: LocalDate,
    onClearCache: () -> Unit,
) {
    val colors = LocalNeumorphicColors.current
    SettingsSection(title = stringResource(R.string.settings_section_data)) {
        SettingsInfoCard {
            Text(
                text = lastSyncedAtMillis?.let {
                    stringResource(R.string.settings_last_sync, syncTimeLabel(it, todayDate))
                } ?: stringResource(R.string.settings_never_synced),
                style = XingClassType.bodyStrong,
                color = colors.textPrimary,
            )
        }
        Spacer(Modifier.height(XingSpacing.sm))
        NeumorphicButton(
            onClick = onClearCache,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = XingControlSize.actionVerticalPadding),
        ) {
            Text(
                text = stringResource(R.string.settings_clear_cache),
                style = XingClassType.bodyStrong,
            )
        }
    }
}

@Composable
private fun DiagnosticsSection(
    onExportLogs: () -> Unit,
    onExportCourses: () -> Unit,
) {
    SettingsSection(title = stringResource(R.string.settings_section_diagnostics)) {
        NeumorphicButton(
            onClick = onExportLogs,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = XingControlSize.actionVerticalPadding),
        ) {
            Text(
                text = stringResource(R.string.settings_export_logs),
                style = XingClassType.bodyStrong,
            )
        }
        Spacer(Modifier.height(XingSpacing.sm))
        NeumorphicButton(
            onClick = onExportCourses,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = XingControlSize.actionVerticalPadding),
        ) {
            Text(
                text = stringResource(R.string.settings_export_courses),
                style = XingClassType.bodyStrong,
            )
        }
    }
}

@Composable
private fun AboutSection(versionName: String) {
    val colors = LocalNeumorphicColors.current
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    SettingsSection(title = stringResource(R.string.settings_section_about)) {
        SettingsInfoCard {
            Text(
                text = stringResource(R.string.settings_version, versionName),
                style = XingClassType.bodyStrong,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.settings_arch_note),
                style = XingClassType.caption,
                color = colors.textSecondary,
            )
        }
        Spacer(Modifier.height(XingSpacing.sm))
        NeumorphicButton(
            onClick = {
                runCatching { uriHandler.openUri(AUTHOR_PROFILE_URL) }
                    .onFailure {
                        Toast.makeText(
                            context,
                            R.string.settings_link_failed,
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
            },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                horizontal = XingSpacing.lg,
                vertical = XingControlSize.actionVerticalPadding,
            ),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.settings_author_link),
                    style = XingClassType.bodyStrong,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = AUTHOR_PROFILE_URL,
                    style = XingClassType.caption,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
internal fun LogoutDialog(
    loggingOut: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = LocalNeumorphicColors.current
    Dialog(onDismissRequest = { if (!loggingOut) onDismiss() }) {
        NeumorphicSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(XingRadii.large),
            elevation = NeumorphicElevation.LARGE,
            contentPadding = PaddingValues(XingSpacing.xl),
            contentAlignment = Alignment.CenterStart,
        ) {
            Column {
                Text(
                    text = stringResource(R.string.settings_logout_title),
                    style = XingClassType.title,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(XingSpacing.sm))
                Text(
                    text = stringResource(R.string.settings_logout_message),
                    style = XingClassType.body,
                    color = colors.textSecondary,
                )
                Spacer(Modifier.height(XingSpacing.xl))
                Row(horizontalArrangement = Arrangement.spacedBy(XingSpacing.sm)) {
                    NeumorphicButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !loggingOut,
                        contentPadding = PaddingValues(
                            vertical = XingControlSize.compactActionVerticalPadding,
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.settings_cancel),
                            style = XingClassType.bodyStrong,
                        )
                    }
                    NeumorphicButton(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        enabled = !loggingOut,
                        contentPadding = PaddingValues(
                            vertical = XingControlSize.compactActionVerticalPadding,
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.settings_logout_confirm),
                            style = XingClassType.bodyStrong,
                            color = colors.danger,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsInfoCard(content: @Composable ColumnScope.() -> Unit) {
    NeumorphicSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(XingRadii.medium),
        elevation = NeumorphicElevation.SMALL,
        contentPadding = PaddingValues(XingSpacing.lg),
        contentAlignment = Alignment.CenterStart,
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalNeumorphicColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = XingClassType.sectionLabel,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(XingSpacing.sm))
        content()
    }
}
