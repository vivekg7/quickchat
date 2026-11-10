package com.crylo.quickchat.ui

import android.text.format.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crylo.quickchat.MainViewModel
import com.crylo.quickchat.R
import com.crylo.quickchat.core.ChatApp
import com.crylo.quickchat.core.Countries
import com.crylo.quickchat.core.ParseResult
import com.crylo.quickchat.core.PhoneNumbers
import com.crylo.quickchat.core.RecentNumber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    vm: MainViewModel,
    onOpen: (ChatApp, String) -> Unit,
    onPaste: () -> Unit,
    onDialHandlerChange: (Boolean) -> Unit,
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val country = vm.country(settings)
    val parsed = remember(vm.number, country) { PhoneNumbers.parse(vm.number, country.callingCode) }
    val digits = (parsed as? ParseResult.Valid)?.digits

    val resources = LocalResources.current
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        vm.messages.collect { m ->
            val text = if (m.arg != null) resources.getString(m.text, resources.getString(m.arg)) else resources.getString(m.text)
            snackbar.showSnackbar(text)
        }
    }

    var showCountries by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var labelTarget by remember { mutableStateOf<RecentNumber?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(painterResource(R.drawable.ic_settings), stringResource(R.string.settings))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            vm.clipboardSuggestion?.let { raw ->
                item(key = "clip") {
                    ClipboardCard(
                        number = (PhoneNumbers.parse(raw, country.callingCode) as? ParseResult.Valid)
                            ?.let { Countries.format(it.digits) } ?: raw,
                        onUse = {
                            vm.number = raw
                            vm.clipboardSuggestion = null
                        },
                        onDismiss = { vm.clipboardSuggestion = null },
                    )
                }
            }
            item(key = "number") {
                Row(verticalAlignment = Alignment.Top) {
                    OutlinedButton(
                        onClick = { showCountries = true },
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        shape = MaterialTheme.shapes.extraSmall,
                        modifier = Modifier.padding(top = 8.dp).heightIn(min = 56.dp),
                    ) {
                        Text("${country.flag} +${country.callingCode}")
                        Icon(painterResource(R.drawable.ic_arrow_drop_down), stringResource(R.string.country_title))
                    }
                    Spacer(Modifier.width(8.dp))
                    NumberField(
                        value = vm.number,
                        onChange = { vm.number = it },
                        parsed = parsed,
                        onPaste = onPaste,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item(key = "message") {
                OutlinedTextField(
                    value = vm.message,
                    onValueChange = { vm.message = it },
                    label = { Text(stringResource(R.string.message_label)) },
                    supportingText = { Text(stringResource(R.string.message_help)) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    minLines = 2,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item(key = "buttons") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val apps = ChatApp.entries.filter { it != ChatApp.WHATSAPP_BUSINESS || it in vm.installed }
                    apps.forEachIndexed { i, app ->
                        val label = stringResource(R.string.open_in, stringResource(appName(app)))
                        val modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        val onClick = { digits?.let { onOpen(app, it) } ?: Unit }
                        if (i == 0) {
                            Button(onClick = onClick, enabled = digits != null, modifier = modifier) { Text(label) }
                        } else {
                            FilledTonalButton(onClick = onClick, enabled = digits != null, modifier = modifier) {
                                Text(label)
                            }
                        }
                    }
                }
            }
            if (history.isNotEmpty()) {
                item(key = "recent-header") {
                    Column(Modifier.padding(top = 8.dp)) {
                        Text(stringResource(R.string.recent), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.recent_help),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(history, key = { it.digits }) { entry ->
                    RecentRow(
                        entry = entry,
                        onOpen = { onOpen(entry.app, entry.digits) },
                        onFill = { vm.number = "+" + entry.digits },
                        onLabel = { labelTarget = entry },
                        onDelete = { vm.remove(entry) },
                    )
                }
            }
            if (!settings.historyEnabled) {
                item(key = "history-off") {
                    Text(
                        stringResource(R.string.history_off),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (showCountries) {
        CountryPickerDialog(
            selected = country,
            onPick = {
                vm.setCountry(it)
                showCountries = false
            },
            onDismiss = { showCountries = false },
        )
    }
    if (showSettings) {
        SettingsSheet(
            historyEnabled = settings.historyEnabled,
            clipboardSuggest = settings.clipboardSuggest,
            dialHandler = vm.dialHandlerEnabled,
            hasHistory = history.isNotEmpty(),
            onHistoryEnabled = { vm.setHistoryEnabled(it) },
            onClipboardSuggest = { vm.setClipboardSuggest(it) },
            onDialHandler = onDialHandlerChange,
            onClearHistory = { vm.clearHistory() },
            onDismiss = { showSettings = false },
        )
    }
    labelTarget?.let { entry ->
        LabelDialog(
            initial = entry.label,
            number = Countries.format(entry.digits),
            onSave = {
                vm.relabel(entry, it)
                labelTarget = null
            },
            onDismiss = { labelTarget = null },
        )
    }
}

@Composable
private fun NumberField(
    value: String,
    onChange: (String) -> Unit,
    parsed: ParseResult,
    onPaste: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val blank = value.isBlank()
    val error = !blank && parsed !is ParseResult.Valid
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(R.string.number_label)) },
        singleLine = true,
        isError = error,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
        supportingText = {
            val text = when (parsed) {
                is ParseResult.Valid -> stringResource(R.string.number_preview, Countries.format(parsed.digits))
                ParseResult.TooShort -> stringResource(R.string.number_too_short)
                ParseResult.TooLong -> stringResource(R.string.number_too_long)
                ParseResult.Empty -> if (blank) stringResource(R.string.number_help) else stringResource(R.string.number_no_digits)
            }
            Text(text, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        trailingIcon = {
            Row {
                if (!blank) {
                    IconButton(onClick = { onChange("") }) {
                        Icon(painterResource(R.drawable.ic_close), stringResource(R.string.clear_number))
                    }
                }
                IconButton(onClick = onPaste) {
                    Icon(painterResource(R.drawable.ic_paste), stringResource(R.string.paste))
                }
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun ClipboardCard(number: String, onUse: () -> Unit, onDismiss: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(vertical = 12.dp)) {
                Text(stringResource(R.string.clipboard_found), style = MaterialTheme.typography.labelLarge)
                Text(number, style = MaterialTheme.typography.titleMedium)
            }
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dismiss)) }
            TextButton(onClick = onUse) { Text(stringResource(R.string.use)) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentRow(
    entry: RecentNumber,
    onOpen: () -> Unit,
    onFill: () -> Unit,
    onLabel: () -> Unit,
    onDelete: () -> Unit,
) {
    val formatted = Countries.format(entry.digits)
    val app = stringResource(appName(entry.app))
    val ago = DateUtils.getRelativeTimeSpanString(
        entry.lastUsed,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE,
    )
    ListItem(
        headlineContent = { Text(entry.label.ifEmpty { formatted }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            val parts = listOfNotNull(formatted.takeIf { entry.label.isNotEmpty() }, app, ago.toString())
            Text(parts.joinToString(" · "), maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        trailingContent = {
            Row {
                IconButton(onClick = onLabel) {
                    Icon(painterResource(R.drawable.ic_edit), stringResource(R.string.label_title))
                }
                IconButton(onClick = onDelete) {
                    Icon(painterResource(R.drawable.ic_delete), stringResource(R.string.remove))
                }
            }
        },
        modifier = Modifier.combinedClickable(
            onClick = onOpen,
            onLongClick = onFill,
            onClickLabel = stringResource(R.string.open_in, app),
            onLongClickLabel = stringResource(R.string.use_number),
        ),
    )
}
