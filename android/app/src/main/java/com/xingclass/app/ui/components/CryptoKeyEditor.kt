package com.xingclass.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import com.xingclass.app.R
import com.xingclass.app.domain.model.ChaoxingKey
import com.xingclass.app.ui.components.neumorphic.NeumorphicButton
import com.xingclass.app.ui.components.neumorphic.NeumorphicTextField
import com.xingclass.app.ui.theme.LocalNeumorphicColors
import com.xingclass.app.ui.theme.XingClassType
import com.xingclass.app.ui.theme.XingControlSize
import com.xingclass.app.ui.theme.XingSpacing

/**
 * Shared editor for the configurable Chaoxing login key.
 *
 * Used by the Settings section and by the pre-login recovery dialog. The save button is only
 * active when the draft is valid and differs from the stored key; unavailable actions render
 * as an inset slot.
 */
@Composable
fun CryptoKeyEditor(
    draft: String,
    savedKey: String,
    onDraftChange: (String) -> Unit,
    onSave: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    actionVerticalPadding: Dp = XingControlSize.actionVerticalPadding,
    actionsTopSpacing: Dp = XingSpacing.md,
) {
    val colors = LocalNeumorphicColors.current
    val focusManager = LocalFocusManager.current
    val valid = ChaoxingKey.isValid(draft)
    val dirty = ChaoxingKey.normalize(draft) != savedKey
    val resetEnabled = savedKey != ChaoxingKey.DEFAULT || draft != ChaoxingKey.DEFAULT

    Column(modifier = modifier) {
        NeumorphicTextField(
            value = draft,
            onValueChange = onDraftChange,
            placeholder = stringResource(R.string.settings_key_placeholder),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() },
            ),
        )
        Spacer(Modifier.height(XingSpacing.sm))
        Text(
            text = stringResource(
                if (valid) R.string.settings_key_hint else R.string.settings_key_invalid,
            ),
            style = XingClassType.caption,
            color = if (valid) colors.textSecondary else colors.danger,
        )
        Spacer(Modifier.height(actionsTopSpacing))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(XingSpacing.sm),
        ) {
            NeumorphicButton(
                onClick = onReset,
                modifier = Modifier.weight(1f),
                enabled = resetEnabled,
                insetWhenDisabled = true,
                contentPadding = PaddingValues(vertical = actionVerticalPadding),
            ) {
                Text(
                    text = stringResource(R.string.settings_key_restore),
                    style = XingClassType.bodyStrong,
                )
            }
            NeumorphicButton(
                onClick = onSave,
                modifier = Modifier.weight(1f),
                enabled = valid && dirty,
                insetWhenDisabled = true,
                contentPadding = PaddingValues(vertical = actionVerticalPadding),
            ) {
                Text(
                    text = stringResource(R.string.settings_key_save),
                    style = XingClassType.bodyStrong,
                )
            }
        }
    }
}
