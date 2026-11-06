package com.xingclass.app.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.xingclass.app.R
import com.xingclass.app.domain.model.ThemeMode
import com.xingclass.app.ui.components.neumorphic.NeumorphicButton
import com.xingclass.app.ui.components.neumorphic.NeumorphicElevation
import com.xingclass.app.ui.components.neumorphic.NeumorphicIconButton
import com.xingclass.app.ui.components.neumorphic.NeumorphicStyle
import com.xingclass.app.ui.components.neumorphic.NeumorphicSurface
import com.xingclass.app.ui.components.neumorphic.NeumorphicTab
import com.xingclass.app.ui.theme.LocalNeumorphicColors
import com.xingclass.app.ui.theme.XingAnimation
import com.xingclass.app.ui.theme.XingClassType
import com.xingclass.app.ui.theme.XingRadii
import com.xingclass.app.ui.theme.XingSpacing
import com.xingclass.app.ui.util.asString
import com.xingclass.app.ui.util.shareExportDocument
import com.xingclass.app.ui.util.syncTimeLabel
import com.xingclass.app.ui.util.themeModeLabel
import kotlinx.coroutines.delay
import android.widget.Toast
import java.time.LocalDate

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    todayDate: LocalDate,
    onBack: () -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onClearCache: () -> Unit,
    onLogout: () -> Unit,
    onDismissMessage: () -> Unit,
    onExportLogs: () -> Unit,
    onExportCourses: () -> Unit,
    onExportConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalNeumorphicColors.current
    var confirmLogout by remember { mutableStateOf(false) }
    var visibleMessage by remember { mutableStateOf(state.message) }

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

    val context = LocalContext.current
    LaunchedEffect(state.exportDocument) {
        val document = state.exportDocument ?: return@LaunchedEffect
        val shared = runCatching { shareExportDocument(context, document) }.isSuccess
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
                    size = 38.dp,
                    iconSize = 17.dp,
                )
                Spacer(Modifier.width(XingSpacing.md))
                Text(
                    text = stringResource(R.string.settings_title),
                    style = XingClassType.title,
                    color = colors.textPrimary,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = XingSpacing.xxl),
                verticalArrangement = Arrangement.spacedBy(XingSpacing.xl),
            ) {
                SettingsSection(title = stringResource(R.string.settings_section_appearance)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(XingSpacing.sm),
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            NeumorphicTab(
                                selected = state.themeMode == mode,
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

                SettingsSection(title = stringResource(R.string.settings_section_account)) {
                    NeumorphicSurface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(XingRadii.medium),
                        elevation = NeumorphicElevation.SMALL,
                        contentPadding = PaddingValues(XingSpacing.lg),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Column {
                            Text(
                                text = state.maskedAccount
                                    ?: stringResource(R.string.settings_account_anonymous),
                                style = XingClassType.bodyStrong,
                                color = colors.textPrimary,
                            )
                        }
                    }
                    Spacer(Modifier.height(XingSpacing.sm))
                    NeumorphicButton(
                        onClick = { confirmLogout = true },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 13.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.settings_logout),
                            style = XingClassType.bodyStrong,
                            color = colors.danger,
                        )
                    }
                }

                SettingsSection(title = stringResource(R.string.settings_section_data)) {
                    NeumorphicSurface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(XingRadii.medium),
                        elevation = NeumorphicElevation.SMALL,
                        contentPadding = PaddingValues(XingSpacing.lg),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Column {
                            Text(
                                text = state.lastSyncedAtMillis?.let {
                                    stringResource(
                                        R.string.settings_last_sync,
                                        syncTimeLabel(it, todayDate),
                                    )
                                } ?: stringResource(R.string.settings_never_synced),
                                style = XingClassType.bodyStrong,
                                color = colors.textPrimary,
                            )
                        }
                    }
                    Spacer(Modifier.height(XingSpacing.sm))
                    NeumorphicButton(
                        onClick = onClearCache,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 13.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.settings_clear_cache),
                            style = XingClassType.bodyStrong,
                        )
                    }
                }

                SettingsSection(title = stringResource(R.string.settings_section_diagnostics)) {
                    NeumorphicButton(
                        onClick = onExportLogs,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 13.dp),
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
                        contentPadding = PaddingValues(vertical = 13.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.settings_export_courses),
                            style = XingClassType.bodyStrong,
                        )
                    }
                }

                SettingsSection(title = stringResource(R.string.settings_section_about)) {
                    NeumorphicSurface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(XingRadii.medium),
                        elevation = NeumorphicElevation.SMALL,
                        contentPadding = PaddingValues(XingSpacing.lg),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.settings_version, state.versionName),
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
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = state.message != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            enter = fadeIn(tween(XingAnimation.FAST_MS)) +
                slideInVertically(tween(XingAnimation.NORMAL_MS, easing = EaseInOut)) { it / 2 },
            exit = fadeOut(tween(XingAnimation.NORMAL_MS)) +
                slideOutVertically(tween(XingAnimation.NORMAL_MS, easing = EaseInOut)) { it / 2 },
        ) {
            NeumorphicSurface(
                style = NeumorphicStyle.INSET,
                shape = RoundedCornerShape(XingRadii.pill),
                elevation = NeumorphicElevation.SMALL,
                contentColor = colors.textSecondary,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 9.dp),
            ) {
                Text(
                    text = visibleMessage?.asString().orEmpty(),
                    style = XingClassType.caption,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    if (confirmLogout) {
        Dialog(
            onDismissRequest = { if (!state.loggingOut) confirmLogout = false },
        ) {
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
                            onClick = { confirmLogout = false },
                            modifier = Modifier.weight(1f),
                            enabled = !state.loggingOut,
                            contentPadding = PaddingValues(vertical = 12.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.settings_cancel),
                                style = XingClassType.bodyStrong,
                            )
                        }
                        NeumorphicButton(
                            onClick = {
                                confirmLogout = false
                                onLogout()
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !state.loggingOut,
                            contentPadding = PaddingValues(vertical = 12.dp),
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
