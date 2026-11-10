package com.crylo.quickchat.core

/** One number the user chatted with recently. [label] is optional, set by the user. */
data class RecentNumber(
    val digits: String,
    val lastUsed: Long,
    val app: ChatApp,
    val label: String = "",
)

/**
 * The recent-numbers list, kept as one small text value in DataStore: one entry per
 * line, tab-separated. A list of at most [MAX] short rows doesn't justify a database,
 * and a plain-text format is trivially testable and migratable.
 */
object History {
    const val MAX = 50

    /** Moves [digits] to the top (keeping its label), or adds it, and trims to [MAX]. */
    fun record(list: List<RecentNumber>, digits: String, app: ChatApp, now: Long): List<RecentNumber> {
        val existing = list.firstOrNull { it.digits == digits }
        val entry = RecentNumber(digits, now, app, existing?.label.orEmpty())
        return (listOf(entry) + list.filter { it.digits != digits }).take(MAX)
    }

    fun relabel(list: List<RecentNumber>, digits: String, label: String): List<RecentNumber> =
        list.map { if (it.digits == digits) it.copy(label = label.trim()) else it }

    fun remove(list: List<RecentNumber>, digits: String): List<RecentNumber> =
        list.filter { it.digits != digits }

    fun encode(list: List<RecentNumber>): String = list.joinToString("\n") {
        listOf(it.digits, it.lastUsed.toString(), it.app.name, escape(it.label)).joinToString("\t")
    }

    /** Skips malformed lines rather than failing, so one bad row never loses the rest. */
    fun decode(text: String?): List<RecentNumber> {
        if (text.isNullOrEmpty()) return emptyList()
        return text.split('\n').mapNotNull { line ->
            val parts = line.split('\t')
            if (parts.size < 3) return@mapNotNull null
            val digits = parts[0].takeIf { d -> d.isNotEmpty() && d.all { it in '0'..'9' } }
            val time = parts[1].toLongOrNull()
            val app = ChatApp.fromName(parts[2])
            if (digits == null || time == null || app == null) return@mapNotNull null
            RecentNumber(digits, time, app, unescape(parts.getOrElse(3) { "" }))
        }
    }

    private fun escape(s: String) = buildString {
        for (c in s) {
            when (c) {
                '\\' -> append("\\\\")
                '\t' -> append("\\t")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                else -> append(c)
            }
        }
    }

    private fun unescape(s: String) = buildString {
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                append(
                    when (s[i + 1]) {
                        't' -> '\t'
                        'n' -> '\n'
                        'r' -> '\r'
                        else -> s[i + 1]
                    },
                )
                i += 2
            } else {
                append(c)
                i++
            }
        }
    }
}
