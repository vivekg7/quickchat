package com.crylo.quickchat

import android.app.Application
import android.telephony.TelephonyManager
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.crylo.quickchat.core.ChatApp
import com.crylo.quickchat.core.Countries
import com.crylo.quickchat.core.Country
import com.crylo.quickchat.core.History
import com.crylo.quickchat.core.ParseResult
import com.crylo.quickchat.core.PhoneNumbers
import com.crylo.quickchat.core.RecentNumber
import com.crylo.quickchat.data.Settings
import com.crylo.quickchat.data.Store
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

/** A one-off snackbar message; [arg] is an optional string resource formatted into it. */
data class UiMessage(@param:StringRes val text: Int, @param:StringRes val arg: Int? = null)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val store = Store(app)

    var number by mutableStateOf("")
    var message by mutableStateOf("")

    /** A number found on the clipboard at launch, offered but never filled in unasked. */
    var clipboardSuggestion by mutableStateOf<String?>(null)

    var installed by mutableStateOf<Set<ChatApp>>(emptySet())
        private set

    var dialHandlerEnabled by mutableStateOf(true)

    val settings: StateFlow<Settings> = store.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, Settings())
    val history: StateFlow<List<RecentNumber>> = store.history
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    /** Reading the SIM/network country needs no permission. */
    val defaultCountry: Country = run {
        val tm = app.getSystemService(TelephonyManager::class.java)
        Countries.resolveDefault(tm?.simCountryIso, tm?.networkCountryIso, Locale.getDefault().country)
    }

    fun country(settings: Settings): Country = Countries.byIso(settings.countryIso) ?: defaultCountry

    private fun callingCode() = country(settings.value).callingCode

    fun post(@StringRes text: Int, @StringRes arg: Int? = null) {
        _messages.trySend(UiMessage(text, arg))
    }

    fun refreshInstalled() {
        installed = ChatApp.entries.filter { ChatLauncher.installedPackage(getApplication(), it) != null }.toSet()
    }

    /**
     * Text arriving from another app (selection, share, tel: link). Pulls out the
     * number; a share with no number is kept as the message, since "share this text
     * to someone on WhatsApp" is the other reason people share to this app.
     */
    fun receiveText(text: String?, isShare: Boolean) {
        val t = text?.trim().orEmpty()
        clipboardSuggestion = null
        val found = PhoneNumbers.find(t)
            ?: t.takeIf { PhoneNumbers.parse(it, callingCode()) is ParseResult.Valid }
        when {
            found != null -> number = found
            isShare && t.isNotEmpty() -> {
                message = t
                post(R.string.msg_share_no_number)
            }
            else -> {
                number = t.take(40)
                post(R.string.msg_no_number_found)
            }
        }
    }

    /** True when the user still wants clipboard suggestions; read fresh so a just-loaded setting counts. */
    suspend fun clipboardSuggestEnabled() = store.settings.first().clipboardSuggest

    fun recordChat(digits: String, app: ChatApp) {
        if (!settings.value.historyEnabled) return
        viewModelScope.launch { store.updateHistory { History.record(it, digits, app, System.currentTimeMillis()) } }
    }

    fun relabel(entry: RecentNumber, label: String) =
        viewModelScope.launch { store.updateHistory { History.relabel(it, entry.digits, label) } }

    fun remove(entry: RecentNumber) = viewModelScope.launch { store.updateHistory { History.remove(it, entry.digits) } }

    fun clearHistory() = viewModelScope.launch {
        store.updateHistory { emptyList() }
        post(R.string.msg_history_cleared)
    }

    fun setHistoryEnabled(on: Boolean) = viewModelScope.launch { store.setHistoryEnabled(on) }

    fun setClipboardSuggest(on: Boolean) = viewModelScope.launch {
        store.setClipboardSuggest(on)
        if (!on) clipboardSuggestion = null
    }

    fun setCountry(country: Country) = viewModelScope.launch { store.setCountry(country.iso) }
}
