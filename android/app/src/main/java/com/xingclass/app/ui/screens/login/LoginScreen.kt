package com.xingclass.app.ui.screens.login

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.xingclass.app.R
import com.xingclass.app.ui.components.BrandMark
import com.xingclass.app.ui.components.CryptoKeyEditor
import com.xingclass.app.ui.components.neumorphic.NeumorphicBadge
import com.xingclass.app.ui.components.neumorphic.NeumorphicButton
import com.xingclass.app.ui.components.neumorphic.NeumorphicLoadingDots
import com.xingclass.app.ui.components.neumorphic.NeumorphicStyle
import com.xingclass.app.ui.components.neumorphic.NeumorphicSurface
import com.xingclass.app.ui.components.neumorphic.NeumorphicTextField
import com.xingclass.app.ui.theme.LocalNeumorphicColors
import com.xingclass.app.ui.theme.XingClassType
import com.xingclass.app.ui.theme.XingControlSize
import com.xingclass.app.ui.theme.XingRadii
import com.xingclass.app.ui.theme.XingSpacing
import com.xingclass.app.ui.util.UiText
import com.xingclass.app.ui.util.asString

@Composable
fun LoginScreen(
    state: LoginUiState,
    sessionNotice: UiText?,
    onAccountChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisible: () -> Unit,
    onSubmit: () -> Unit,
    onLoggedIn: () -> Unit,
    currentCryptoKey: () -> String,
    onSaveCryptoKey: (String) -> Boolean,
    onResetCryptoKey: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalNeumorphicColors.current
    val focusManager = LocalFocusManager.current
    var showKeyDialog by remember { mutableStateOf(false) }
    var keySaved by remember { mutableStateOf("") }
    var keyDraft by remember { mutableStateOf("") }

    LaunchedEffect(state.loggedIn) {
        if (state.loggedIn) onLoggedIn()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        BrandMark(size = 72.dp, fontSize = 30.sp)

        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = XingClassType.wordmark,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.login_subtitle),
            style = XingClassType.caption,
            color = colors.textSecondary,
        )

        if (sessionNotice != null) {
            Spacer(Modifier.height(20.dp))
            NeumorphicBadge(
                text = sessionNotice.asString(),
                contentColor = colors.danger,
                inset = true,
            )
        }

        Spacer(Modifier.height(36.dp))

        NeumorphicTextField(
            value = state.account,
            onValueChange = onAccountChange,
            label = stringResource(R.string.login_account_label),
            placeholder = stringResource(R.string.login_account_placeholder),
            enabled = !state.loading,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(XingSpacing.lg))

        NeumorphicTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = stringResource(R.string.login_password_label),
            placeholder = stringResource(R.string.login_password_placeholder),
            enabled = !state.loading,
            isPassword = !state.passwordVisible,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    onSubmit()
                },
            ),
            modifier = Modifier.fillMaxWidth(),
            trailing = {
                Text(
                    text = stringResource(
                        if (state.passwordVisible) R.string.login_hide_password else R.string.login_show_password,
                    ),
                    style = XingClassType.caption,
                    color = colors.accent,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                            onClick = onTogglePasswordVisible,
                        )
                        .padding(4.dp),
                )
            },
        )

        if (state.error != null) {
            Spacer(Modifier.height(XingSpacing.md))
            Text(
                text = state.error.asString(),
                style = XingClassType.caption,
                color = colors.danger,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(XingSpacing.xl))

        NeumorphicButton(
            onClick = {
                focusManager.clearFocus()
                onSubmit()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.loading,
            accent = true,
            contentPadding = PaddingValues(
                horizontal = 22.dp,
                vertical = 15.dp,
            ),
        ) {
            if (state.loading) {
                NeumorphicLoadingDots(color = colors.onAccent, dotSize = 5.dp)
                Spacer(Modifier.size(10.dp))
                Text(
                    text = stringResource(R.string.login_button_loading),
                    style = XingClassType.bodyStrong,
                    color = colors.onAccent,
                )
            } else {
                Text(
                    text = stringResource(R.string.login_button),
                    style = XingClassType.bodyStrong,
                    color = colors.onAccent,
                )
            }
        }

        Spacer(Modifier.height(XingSpacing.md))
        Text(
            text = stringResource(R.string.login_key_entry),
            style = XingClassType.caption,
            color = colors.accent,
            maxLines = 1,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                ) {
                    keySaved = currentCryptoKey()
                    keyDraft = keySaved
                    showKeyDialog = true
                }
                .padding(6.dp),
        )

        Spacer(Modifier.height(XingSpacing.lg))
        Box(Modifier.height(XingSpacing.sm))
    }

    if (showKeyDialog) {
        LoginKeyDialog(
            savedKey = keySaved,
            draft = keyDraft,
            onDraftChange = { keyDraft = it },
            onSave = {
                if (onSaveCryptoKey(keyDraft)) showKeyDialog = false
            },
            onReset = {
                onResetCryptoKey()
                keySaved = currentCryptoKey()
                keyDraft = keySaved
            },
            onDismiss = { showKeyDialog = false },
        )
    }
}

/**
 * Pre-login recovery for the Chaoxing AES key.
 *
 * The key editor also exists in Settings, but Settings is only reachable after login. If the
 * built-in key is stale (or the user mistyped a custom one), this dialog is the only way back in.
 */
@Composable
private fun LoginKeyDialog(
    savedKey: String,
    draft: String,
    onDraftChange: (String) -> Unit,
    onSave: () -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalNeumorphicColors.current

    Dialog(onDismissRequest = onDismiss) {
        NeumorphicSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(XingRadii.large),
            // No outer shadow: a large halo on the dialog scrim reads as spilled light.
            style = NeumorphicStyle.FLAT,
            contentPadding = PaddingValues(XingSpacing.xl),
            contentAlignment = Alignment.CenterStart,
        ) {
            Column {
                Text(
                    text = stringResource(R.string.settings_section_login_key),
                    style = XingClassType.title,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(XingSpacing.md))
                CryptoKeyEditor(
                    draft = draft,
                    savedKey = savedKey,
                    onDraftChange = onDraftChange,
                    onSave = onSave,
                    onReset = onReset,
                    actionVerticalPadding = XingControlSize.compactActionVerticalPadding,
                    actionsTopSpacing = XingSpacing.xl,
                )
            }
        }
    }
}
