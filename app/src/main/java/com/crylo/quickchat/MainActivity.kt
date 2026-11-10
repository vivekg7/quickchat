package com.crylo.quickchat

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.crylo.quickchat.core.ChatApp
import com.crylo.quickchat.core.PhoneNumbers
import com.crylo.quickchat.ui.MainScreen
import com.crylo.quickchat.ui.QuickChatTheme
import com.crylo.quickchat.ui.appName
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    /** What to do with the clipboard once the window has focus (Android only allows reads then). */
    private enum class ClipAction { NONE, SUGGEST, FILL }

    private var clipAction = ClipAction.NONE

    /** Opened from another app (selection, share, tel: link): go back there once the chat opens. */
    private var fromOtherApp = false

    private val dialAlias by lazy { ComponentName(this, "$packageName.DialAlias") }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            handleIntent(intent)
        } else {
            fromOtherApp = savedInstanceState.getBoolean(KEY_FROM_OTHER_APP)
        }
        vm.dialHandlerEnabled = packageManager.getComponentEnabledSetting(dialAlias) !=
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        setContent {
            QuickChatTheme {
                MainScreen(
                    vm = vm,
                    onOpen = ::openChat,
                    onPaste = ::pasteFromClipboard,
                    onDialHandlerChange = ::setDialHandler,
                )
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_FROM_OTHER_APP, fromOtherApp)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        vm.refreshInstalled()
    }

    private fun handleIntent(intent: Intent) {
        fromOtherApp = false
        clipAction = ClipAction.NONE
        when (intent.action) {
            Intent.ACTION_PROCESS_TEXT -> {
                fromOtherApp = true
                vm.receiveText(intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString(), isShare = false)
            }
            Intent.ACTION_SEND -> {
                fromOtherApp = true
                vm.receiveText(intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString(), isShare = true)
            }
            Intent.ACTION_DIAL, Intent.ACTION_VIEW -> {
                fromOtherApp = true
                // schemeSpecificPart is already URL-decoded ("%2B91..." -> "+91...").
                vm.receiveText(intent.data?.schemeSpecificPart, isShare = false)
            }
            ACTION_FROM_CLIPBOARD -> clipAction = ClipAction.FILL
            else -> clipAction = ClipAction.SUGGEST
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus || clipAction == ClipAction.NONE) return
        val action = clipAction
        clipAction = ClipAction.NONE
        when (action) {
            ClipAction.FILL -> {
                val found = clipboardNumber()
                if (found != null) {
                    vm.number = found
                    vm.clipboardSuggestion = null
                } else {
                    vm.post(R.string.msg_clipboard_no_number)
                }
            }
            ClipAction.SUGGEST -> lifecycleScope.launch {
                // Checked first so that a user who turned suggestions off never gets the
                // system's "pasted from your clipboard" notice.
                if (vm.number.isBlank() && vm.clipboardSuggestEnabled()) {
                    vm.clipboardSuggestion = clipboardNumber()
                }
            }
            ClipAction.NONE -> Unit
        }
    }

    /** First phone number in the clipboard's text, skipping anything marked sensitive. */
    private fun clipboardNumber(): String? {
        val cm = getSystemService(ClipboardManager::class.java) ?: return null
        val description = cm.primaryClipDescription ?: return null
        if (!description.hasMimeType("text/*")) return null
        if (Build.VERSION.SDK_INT >= 33 &&
            description.extras?.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE) == true
        ) {
            return null
        }
        val text = cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text ?: return null
        return PhoneNumbers.find(text.toString())
    }

    private fun pasteFromClipboard() {
        val found = clipboardNumber()
        if (found != null) {
            vm.number = found
            vm.clipboardSuggestion = null
        } else {
            vm.post(R.string.msg_clipboard_no_number)
        }
    }

    private fun openChat(app: ChatApp, digits: String) {
        val message = vm.message.trim()
        val result = ChatLauncher.open(this, app, digits, if (app.supportsMessage) message else "")
        when (result) {
            ChatLauncher.Result.OPENED -> {
                if (!app.supportsMessage && message.isNotEmpty()) {
                    // Telegram and Signal links can't carry text, so put it where a
                    // long-press "Paste" in the chat will find it.
                    getSystemService(ClipboardManager::class.java)
                        ?.setPrimaryClip(ClipData.newPlainText(getString(R.string.message_label), message))
                }
                vm.recordChat(digits, app)
                if (fromOtherApp) finish()
            }
            ChatLauncher.Result.NOT_INSTALLED -> vm.post(R.string.msg_not_installed, appName(app))
            ChatLauncher.Result.FAILED -> vm.post(R.string.msg_open_failed, appName(app))
        }
    }

    /** The tel: handler is an activity-alias so it can be switched off without touching the rest. */
    private fun setDialHandler(enabled: Boolean) {
        packageManager.setComponentEnabledSetting(
            dialAlias,
            if (enabled) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            },
            PackageManager.DONT_KILL_APP,
        )
        vm.dialHandlerEnabled = enabled
    }

    companion object {
        const val ACTION_FROM_CLIPBOARD = "com.crylo.quickchat.action.FROM_CLIPBOARD"
        private const val KEY_FROM_OTHER_APP = "from_other_app"
    }
}
