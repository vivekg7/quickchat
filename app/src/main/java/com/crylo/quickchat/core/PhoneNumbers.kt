package com.crylo.quickchat.core

/** Outcome of turning something a person typed or copied into an international number. */
sealed interface ParseResult {
    /** [digits] is the full international number without `+`, e.g. "919876543210". */
    data class Valid(val digits: String) : ParseResult

    data object Empty : ParseResult

    data object TooShort : ParseResult

    data object TooLong : ParseResult
}

/**
 * Normalises phone numbers the way people actually write them — "+91 98765-43210",
 * "(0) 98765 43210", "0091 98765 43210", "098765 43210", "9876543210",
 * "tel:%2B919876543210" — into international digits for click-to-chat links.
 *
 * This is deliberately not a full libphonenumber: chat apps validate the number
 * themselves, so all we need is the right digits in the right order. The rules:
 *  1. Strip a `tel:` prefix, URL-encoded `+`, and anything after an extension marker.
 *  2. A leading `+` (or `00`, or `011` in North America) means the number is already
 *     international.
 *  3. Otherwise it is national: drop the trunk prefix (`0`; `8` in Russia/Kazakhstan),
 *     except where the leading 0 is part of the number (Italy, San Marino, Côte
 *     d'Ivoire), then prepend the default calling code — unless the digits already
 *     start with that code and are too long to be a national number (people paste
 *     "919876543210" without the `+`).
 */
object PhoneNumbers {
    /** E.164 caps numbers at 15 digits; the shortest real ones (Niue, etc.) are 7. */
    const val MIN_DIGITS = 7
    const val MAX_DIGITS = 15

    /** Regions whose national numbers keep their leading 0 in international form. */
    private val keepsLeadingZero = setOf("39", "378", "225")

    private val extensionMarker = Regex("""(?i)(;|,|#|\bext\.?|\bx(?=\s*\d)).*$""")
    private val nationalZero = Regex("""\(\s*0\s*\)""")

    fun parse(raw: String, defaultCallingCode: String): ParseResult {
        val cleaned = clean(raw)
        val international = cleaned.trimStart().let { it.startsWith('+') || it.startsWith('＋') }
        val digits = toAsciiDigits(cleaned)
        if (digits.isEmpty()) return ParseResult.Empty

        val full = when {
            international -> digits
            digits.startsWith("00") -> digits.drop(2)
            defaultCallingCode == "1" && digits.startsWith("011") -> digits.drop(3)
            else -> national(digits, defaultCallingCode)
        }
        return when {
            full.length < MIN_DIGITS -> ParseResult.TooShort
            full.length > MAX_DIGITS -> ParseResult.TooLong
            else -> ParseResult.Valid(full)
        }
    }

    private fun national(digits: String, cc: String): String {
        // An Indian mobile written without + but with its code: 91 + 10 digits starting 6–9.
        if (cc == "91" && digits.length == 12 && digits.startsWith("91") && digits[2] in '6'..'9') {
            return digits
        }
        // North American numbers with the country code but no +: "1 202 555 0123".
        if (cc == "1" && digits.length == 11 && digits.startsWith("1")) return digits

        var national = digits
        if (cc == "7" && national.length == 11 && national.startsWith("8")) {
            national = national.drop(1)
        } else if (cc !in keepsLeadingZero && national.startsWith("0")) {
            national = national.drop(1)
        }
        // Already carries the default code without a +, e.g. "4420 7946 0958" in the UK.
        // National numbers are at most 10 digits almost everywhere, so 11+ that start
        // with the code are safest read as international.
        if (national.startsWith(cc) && national.length >= 11 && national.length - cc.length >= MIN_DIGITS - 1) {
            return national
        }
        return cc + national
    }

    private fun clean(raw: String): String {
        var s = raw.trim()
        if (s.startsWith("tel:", ignoreCase = true)) s = s.substring(4)
        s = s.replace("%2B", "+", ignoreCase = true).replace("%20", " ")
        s = s.replace(extensionMarker, "")
        // "+44 (0) 20 7946 0958": the (0) is only dialled from inside the country.
        s = s.replace(nationalZero, " ")
        return s
    }

    /** Keeps digits only, mapping other scripts' digits (Devanagari, Arabic-Indic, …) to ASCII. */
    private fun toAsciiDigits(s: String): String = buildString {
        for (ch in s) {
            val d = Character.digit(ch, 10)
            if (d >= 0) append(('0' + d))
        }
    }

    // A run that starts with + or a digit, may contain spaces, dashes, dots-free
    // brackets, and ends in a digit. Dots and slashes are excluded so that prices,
    // versions and most dates don't match.
    private val candidate = Regex("""[+＋]?[\p{Nd}(][\p{Nd}\s ()\-]{5,}\p{Nd}""")
    private val isoDate = Regex("""^\d{4}-\d{1,2}-\d{1,2}$""")

    /**
     * Finds the first plausible phone number inside free text — a selection, a shared
     * message, the clipboard, or a wa.me link. Returns the matched text as written, so
     * it can be shown and re-parsed with the user's chosen country.
     */
    fun find(text: String): String? = candidate.findAll(text)
        .map { it.value.trim() }
        .firstOrNull { match ->
            val count = match.count { Character.digit(it, 10) >= 0 }
            count in MIN_DIGITS..MAX_DIGITS && !isoDate.matches(match)
        }
}
