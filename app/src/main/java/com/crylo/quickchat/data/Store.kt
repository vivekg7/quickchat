package com.crylo.quickchat.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.crylo.quickchat.core.History
import com.crylo.quickchat.core.RecentNumber
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class Settings(
    val historyEnabled: Boolean = true,
    val clipboardSuggest: Boolean = true,
    /** Region the user picked by hand; null means "use the SIM/network/locale default". */
    val countryIso: String? = null,
)

private val Context.dataStore by preferencesDataStore(name = "quickchat")

/** Everything QuickChat remembers, in one on-device DataStore file. */
class Store(context: Context) {
    private val ds = context.applicationContext.dataStore

    private object Keys {
        val historyEnabled = booleanPreferencesKey("history_enabled")
        val clipboardSuggest = booleanPreferencesKey("clipboard_suggest")
        val country = stringPreferencesKey("country")
        val history = stringPreferencesKey("history")
    }

    val settings: Flow<Settings> = ds.data.map {
        Settings(
            historyEnabled = it[Keys.historyEnabled] ?: true,
            clipboardSuggest = it[Keys.clipboardSuggest] ?: true,
            countryIso = it[Keys.country],
        )
    }

    val history: Flow<List<RecentNumber>> = ds.data.map { History.decode(it[Keys.history]) }

    suspend fun setHistoryEnabled(on: Boolean) = ds.edit { it[Keys.historyEnabled] = on }

    suspend fun setClipboardSuggest(on: Boolean) = ds.edit { it[Keys.clipboardSuggest] = on }

    suspend fun setCountry(iso: String) = ds.edit { it[Keys.country] = iso }

    suspend fun updateHistory(transform: (List<RecentNumber>) -> List<RecentNumber>) = ds.edit {
        val updated = transform(History.decode(it[Keys.history]))
        if (updated.isEmpty()) it.remove(Keys.history) else it[Keys.history] = History.encode(updated)
    }
}
