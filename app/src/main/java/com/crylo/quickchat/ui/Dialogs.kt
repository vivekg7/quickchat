package com.crylo.quickchat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.crylo.quickchat.R
import com.crylo.quickchat.core.Countries
import com.crylo.quickchat.core.Country
import java.util.Locale

private fun Country.displayName(): String =
    Locale.Builder().setRegion(iso).build().displayCountry.ifBlank { iso }

/** Searchable list of every region, by localized name, ISO code or calling code. */
@Composable
fun CountryPickerDialog(selected: Country, onPick: (Country) -> Unit, onDismiss: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val all = remember { Countries.all.map { it to it.displayName() }.sortedBy { it.second } }
    val q = query.trim().removePrefix("+")
    val shown = if (q.isEmpty()) {
        all
    } else {
        all.filter { (c, name) ->
            name.contains(q, ignoreCase = true) || c.iso.equals(q, ignoreCase = true) || c.callingCode.startsWith(q)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.country_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.country_search)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(Modifier.heightIn(max = 400.dp).padding(top = 8.dp)) {
                    items(shown, key = { it.first.iso }) { (c, name) ->
                        val isSelected = c.iso == selected.iso
                        ListItem(
                            headlineContent = {
                                Text(
                                    name,
                                    fontWeight = if (isSelected) FontWeight.Bold else null,
                                )
                            },
                            leadingContent = { Text(c.flag, style = MaterialTheme.typography.titleLarge) },
                            trailingContent = { Text("+${c.callingCode}") },
                            colors = ListItemDefaults.colors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                            ),
                            modifier = Modifier.clickable { onPick(c) },
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
fun LabelDialog(initial: String, number: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.label_title)) },
        text = {
            Column {
                Text(number, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(60) },
                    label = { Text(stringResource(R.string.label_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    historyEnabled: Boolean,
    clipboardSuggest: Boolean,
    dialHandler: Boolean,
    hasHistory: Boolean,
    onHistoryEnabled: (Boolean) -> Unit,
    onClipboardSuggest: (Boolean) -> Unit,
    onDialHandler: (Boolean) -> Unit,
    onClearHistory: () -> Unit,
    onDismiss: () -> Unit,
) {
    var confirmClear by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
            Text(
                stringResource(R.string.settings),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            SwitchRow(R.string.setting_history, R.string.setting_history_desc, historyEnabled, onHistoryEnabled)
            SwitchRow(R.string.setting_clipboard, R.string.setting_clipboard_desc, clipboardSuggest, onClipboardSuggest)
            SwitchRow(R.string.setting_dial, R.string.setting_dial_desc, dialHandler, onDialHandler)
            TextButton(
                onClick = { confirmClear = true },
                enabled = hasHistory,
                modifier = Modifier.padding(horizontal = 12.dp),
            ) { Text(stringResource(R.string.clear_history)) }
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.clear_history_title)) },
            text = { Text(stringResource(R.string.clear_history_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onClearHistory()
                }) { Text(stringResource(R.string.clear)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun SwitchRow(title: Int, desc: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(title)) },
        supportingContent = { Text(stringResource(desc)) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
    )
}
