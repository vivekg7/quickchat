package com.crylo.quickchat.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CountriesTest {
    @Test fun tableHasUniqueRegions() {
        assertTrue(Countries.all.size > 230)
        assertEquals(Countries.all.size, Countries.all.map { it.iso }.toSet().size)
    }

    @Test fun defaultPrefersSimThenNetworkThenLocaleThenIndia() {
        assertEquals("GB", Countries.resolveDefault("gb", "in", "US").iso)
        assertEquals("IN", Countries.resolveDefault("", "in", "US").iso)
        assertEquals("US", Countries.resolveDefault(null, null, "US").iso)
        assertEquals("IN", Countries.resolveDefault(null, "", "").iso)
        assertEquals("IN", Countries.resolveDefault("zz", null, null).iso)
    }

    @Test fun callingCodeIsLongestMatch() {
        assertEquals("91", Countries.callingCodeOf("919876543210"))
        assertEquals("1", Countries.callingCodeOf("12025550123"))
        assertEquals("971", Countries.callingCodeOf("971501234567"))
        assertEquals("7", Countries.callingCodeOf("79161234567"))
    }

    @Test fun formatting() {
        assertEquals("+91 98765 43210", Countries.format("919876543210"))
        assertEquals("+1 202-555-0123", Countries.format("12025550123"))
        assertEquals("+44 7700900123", Countries.format("447700900123"))
    }

    @Test fun flagEmoji() = assertEquals("🇮🇳", Countries.byIso("in")!!.flag)
}

class ChatAppTest {
    @Test fun whatsappLinkCarriesEncodedMessage() {
        assertEquals("https://api.whatsapp.com/send?phone=919876543210", ChatApp.WHATSAPP.link("919876543210"))
        assertEquals(
            "https://api.whatsapp.com/send?phone=919876543210&text=Hi%20there%21%201%2B1%20%26%20%E2%82%B9",
            ChatApp.WHATSAPP.link("919876543210", "Hi there! 1+1 & ₹"),
        )
        assertEquals(
            "https://api.whatsapp.com/send?phone=1&text=a%0Ab",
            ChatApp.WHATSAPP_BUSINESS.link("1", "a\nb"),
        )
    }

    @Test fun telegramAndSignalUsePhoneLinks() {
        assertEquals("https://t.me/+919876543210", ChatApp.TELEGRAM.link("919876543210", "ignored"))
        assertEquals("https://signal.me/#p/+919876543210", ChatApp.SIGNAL.link("919876543210"))
    }
}

class HistoryTest {
    private val a = RecentNumber("919876543210", 1, ChatApp.WHATSAPP, "Plumber")
    private val b = RecentNumber("12025550123", 2, ChatApp.SIGNAL)

    @Test fun recordMovesToTopAndKeepsLabel() {
        val list = History.record(listOf(b, a), "919876543210", ChatApp.TELEGRAM, now = 9)
        assertEquals(listOf("919876543210", "12025550123"), list.map { it.digits })
        assertEquals(RecentNumber("919876543210", 9, ChatApp.TELEGRAM, "Plumber"), list[0])
    }

    @Test fun recordCapsLength() {
        var list = emptyList<RecentNumber>()
        repeat(History.MAX + 5) { list = History.record(list, "9100000000$it", ChatApp.WHATSAPP, it.toLong()) }
        assertEquals(History.MAX, list.size)
    }

    @Test fun relabelAndRemove() {
        assertEquals("Office", History.relabel(listOf(a, b), b.digits, "  Office ")[1].label)
        assertEquals(listOf(b), History.remove(listOf(a, b), a.digits))
    }

    @Test fun roundTripsAwkwardLabels() {
        val list = listOf(a.copy(label = "tab\there\nnew\\line"), b)
        assertEquals(list, History.decode(History.encode(list)))
    }

    @Test fun decodeSkipsGarbage() {
        val text = History.encode(listOf(a)) + "\nnot\ta row\n\n12\tx\tWHATSAPP\n34\t5\tNOPE"
        assertEquals(listOf(a), History.decode(text))
        assertEquals(emptyList<RecentNumber>(), History.decode(null))
    }
}
