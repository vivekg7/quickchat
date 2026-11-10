package com.crylo.quickchat.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.crylo.quickchat.R
import com.crylo.quickchat.core.ChatApp

/** Material You colours from the wallpaper; minSdk 31 guarantees dynamic colour exists. */
@Composable
fun QuickChatTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val scheme = if (isSystemInDarkTheme()) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    MaterialTheme(colorScheme = scheme, content = content)
}

@StringRes
fun appName(app: ChatApp): Int = when (app) {
    ChatApp.WHATSAPP -> R.string.app_whatsapp
    ChatApp.WHATSAPP_BUSINESS -> R.string.app_whatsapp_business
    ChatApp.TELEGRAM -> R.string.app_telegram
    ChatApp.SIGNAL -> R.string.app_signal
}
