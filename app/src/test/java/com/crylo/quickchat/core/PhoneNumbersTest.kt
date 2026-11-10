package com.crylo.quickchat.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneNumbersTest {
    private fun ok(expected: String, raw: String, cc: String = "91") =
        assertEquals(raw, ParseResult.Valid(expected), PhoneNumbers.parse(raw, cc))

    @Test fun indianTenDigitMobileGetsCountryCode() = ok("919876543210", "9876543210")

    @Test fun spacesDashesAndBracketsAreIgnored() {
        ok("919876543210", "98765 43210")
        ok("919876543210", "98765-43210")
        ok("919876543210", "(98765) 43210")
        ok("919876543210", "+91 (987) 654-3210")
        ok("919876543210", "+91 98765 43210")
    }

    @Test fun leadingTrunkZeroIsDropped() {
        ok("919876543210", "09876543210")
        ok("918023456789", "080 2345 6789")
        ok("447700900123", "07700 900123", cc = "44")
    }

    @Test fun plusMeansInternationalWhateverTheDefault() {
        ok("919876543210", "+919876543210", cc = "44")
        ok("12025550123", "+1 202-555-0123")
        ok("447700900123", "＋44 7700 900123")
    }

    @Test fun doubleZeroPrefixIsInternational() {
        ok("919876543210", "0091 98765 43210", cc = "44")
        ok("447700900123", "00447700900123")
    }

    @Test fun northAmericanExitCode() = ok("447700900123", "011 44 7700 900123", cc = "1")

    @Test fun indianNumberWithCodeButNoPlus() {
        ok("919876543210", "919876543210")
        ok("919876543210", "91 98765 43210")
        ok("919876543210", "091 98765 43210")
    }

    @Test fun tenDigitIndianNumberStartingWith91IsNational() = ok("919198765432", "9198765432")

    @Test fun northAmericanNumbers() {
        ok("12025550123", "(202) 555-0123", cc = "1")
        ok("12025550123", "1 202 555 0123", cc = "1")
    }

    @Test fun nationalZeroInBracketsIsDropped() = ok("442079460958", "+44 (0) 20 7946 0958")

    @Test fun italyKeepsLeadingZero() = ok("390612345678", "06 1234 5678", cc = "39")

    @Test fun russianTrunkEight() = ok("79161234567", "8 916 123-45-67", cc = "7")

    @Test fun telUriAndEncodedPlus() {
        ok("919876543210", "tel:+919876543210")
        ok("919876543210", "tel:%2B91-98765-43210")
    }

    @Test fun extensionIsCutOff() {
        ok("12025550123", "+1 202 555 0123 ext. 45")
        ok("12025550123", "+1 202 555 0123 x45")
        ok("12025550123", "tel:+12025550123;ext=45")
    }

    @Test fun otherScriptDigits() = ok("919876543210", "९८७६५४३२१०")

    @Test fun ukNumberWithCodeButNoPlus() = ok("442079460958", "442079460958", cc = "44")

    @Test fun emptyShortAndLong() {
        assertEquals(ParseResult.Empty, PhoneNumbers.parse("", "91"))
        assertEquals(ParseResult.Empty, PhoneNumbers.parse("  call me  ", "91"))
        assertEquals(ParseResult.TooShort, PhoneNumbers.parse("+12345", "91"))
        assertEquals(ParseResult.TooLong, PhoneNumbers.parse("+1234567890123456", "91"))
    }

    @Test fun findsNumberInsideText() {
        assertEquals("+91 98765 43210", PhoneNumbers.find("Call me on +91 98765 43210 after 5"))
        assertEquals("98765-43210", PhoneNumbers.find("ph: 98765-43210."))
        assertEquals("919876543210", PhoneNumbers.find("https://wa.me/919876543210?text=hi"))
        assertEquals("(202) 555-0123", PhoneNumbers.find("Office (202) 555-0123"))
    }

    @Test fun ignoresThingsThatAreNotPhoneNumbers() {
        assertNull(PhoneNumbers.find("Your OTP is 123456"))
        assertNull(PhoneNumbers.find("Meeting on 2026-11-10"))
        assertNull(PhoneNumbers.find("Total: 1,299.00"))
        assertNull(PhoneNumbers.find("no digits here"))
    }

    @Test fun findSkipsShortRunsToReachTheNumber() =
        assertEquals("9876543210", PhoneNumbers.find("Flat 12, call 9876543210"))
}
