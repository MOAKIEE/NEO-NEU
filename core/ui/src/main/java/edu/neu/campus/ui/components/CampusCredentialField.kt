package edu.neu.campus.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import top.yukonga.miuix.kmp.basic.TextField

/** Miuix account/password input; credentials must stay in memory, never rememberSaveable. */
@Composable
fun CampusCredentialField(value: String, onValueChange: (String) -> Unit, label: String,
    password: Boolean = false, enabled: Boolean = true, modifier: Modifier = Modifier) {
    val interactions = remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()
    TextField(value = value, onValueChange = onValueChange,
        label = if (focused && value.isEmpty()) "" else label,
        useLabelAsPlaceholder = true, singleLine = true, enabled = enabled,
        interactionSource = interactions,
        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false,
            keyboardType = if (password) KeyboardType.Password else KeyboardType.Ascii),
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = modifier.fillMaxWidth())
}
