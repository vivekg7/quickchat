package com.crylo.quickchat.core

import java.net.URLEncoder

/**
 * The messengers QuickChat can open, with the package names to look for (first
 * installed one wins) and their official click-to-chat links.
 */
enum class ChatApp(val packages: List<String>, val supportsMessage: Boolean) {
    WHATSAPP(listOf("com.whatsapp"), supportsMessage = true),
    WHATSAPP_BUSINESS(listOf("com.whatsapp.w4b"), supportsMessage = true),

    // Official app from Play, the telegram.org direct download, and Telegram X.
    TELEGRAM(
        listOf("org.telegram.messenger", "org.telegram.messenger.web", "org.thunderdog.challegram"),
        supportsMessage = false,
    ),

    // Signal, and Molly (the hardened Signal fork many F-Droid users run).
    SIGNAL(listOf("org.thoughtcrime.securesms", "im.molly.app"), supportsMessage = false),
    ;

    /**
     * The deep link for [digits] (international, no `+`). Telegram and Signal links
     * cannot carry a message, so [message] is ignored for them.
     */
    fun link(digits: String, message: String = ""): String = when (this) {
        WHATSAPP, WHATSAPP_BUSINESS -> buildString {
            append("https://api.whatsapp.com/send?phone=").append(digits)
            if (message.isNotBlank()) append("&text=").append(encode(message))
        }
        TELEGRAM -> "https://t.me/+$digits"
        SIGNAL -> "https://signal.me/#p/+$digits"
    }

    companion object {
        fun fromName(name: String?): ChatApp? = entries.firstOrNull { it.name == name }

        // The String-charset overload, because the Charset one needs API 33.
        // URLEncoder does form encoding (space -> '+'); WhatsApp wants %20 so a literal
        // plus sign in the message isn't turned into a space.
        private fun encode(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")
    }
}
