package com.crylo.quickchat.core

/** A region and its international calling code (digits only, no `+`). */
data class Country(val iso: String, val callingCode: String) {
    /** Regional-indicator flag emoji built from the ISO code, e.g. "IN" -> 🇮🇳. */
    val flag: String
        get() = iso.uppercase().map { Character.toChars(0x1F1E6 + (it - 'A')) }
            .joinToString("") { String(it) }
}

/**
 * ITU-T E.164 calling codes for every region a phone could report. Kept as a compact
 * table instead of pulling in libphonenumber (several MB of metadata) because the app
 * only needs "which code do I prepend" and "where does the code end when formatting".
 * Display names come from the platform's `Locale`, so they follow the user's language.
 */
object Countries {
    private const val TABLE =
        "AF93 AX358 AL355 DZ213 AS1 AD376 AO244 AI1 AG1 AR54 AM374 AW297 AU61 AT43 AZ994 " +
            "BS1 BH973 BD880 BB1 BY375 BE32 BZ501 BJ229 BM1 BT975 BO591 BQ599 BA387 BW267 " +
            "BR55 IO246 VG1 BN673 BG359 BF226 BI257 KH855 CM237 CA1 CV238 KY1 CF236 TD235 " +
            "CL56 CN86 CX61 CC61 CO57 KM269 CG242 CD243 CK682 CR506 CI225 HR385 CU53 CW599 " +
            "CY357 CZ420 DK45 DJ253 DM1 DO1 EC593 EG20 SV503 GQ240 ER291 EE372 SZ268 ET251 " +
            "FK500 FO298 FJ679 FI358 FR33 GF594 PF689 GA241 GM220 GE995 DE49 GH233 GI350 " +
            "GR30 GL299 GD1 GP590 GU1 GT502 GG44 GN224 GW245 GY592 HT509 HN504 HK852 HU36 " +
            "IS354 IN91 ID62 IR98 IQ964 IE353 IM44 IL972 IT39 JM1 JP81 JE44 JO962 KZ7 KE254 " +
            "KI686 XK383 KW965 KG996 LA856 LV371 LB961 LS266 LR231 LY218 LI423 LT370 LU352 " +
            "MO853 MG261 MW265 MY60 MV960 ML223 MT356 MH692 MQ596 MR222 MU230 YT262 MX52 " +
            "FM691 MD373 MC377 MN976 ME382 MS1 MA212 MZ258 MM95 NA264 NR674 NP977 NL31 NC687 " +
            "NZ64 NI505 NE227 NG234 NU683 NF672 KP850 MK389 MP1 NO47 OM968 PK92 PW680 PS970 " +
            "PA507 PG675 PY595 PE51 PH63 PL48 PT351 PR1 QA974 RE262 RO40 RU7 RW250 BL590 " +
            "SH290 KN1 LC1 MF590 PM508 VC1 WS685 SM378 ST239 SA966 SN221 RS381 SC248 SL232 " +
            "SG65 SX1 SK421 SI386 SB677 SO252 ZA27 KR82 SS211 ES34 LK94 SD249 SR597 SJ47 " +
            "SE46 CH41 SY963 TW886 TJ992 TZ255 TH66 TL670 TG228 TK690 TO676 TT1 TN216 TR90 " +
            "TM993 TC1 TV688 VI1 UG256 UA380 AE971 GB44 US1 UY598 UZ998 VU678 VA39 VE58 " +
            "VN84 WF681 EH212 YE967 ZM260 ZW263"

    val all: List<Country> = TABLE.split(' ').map { entry ->
        Country(entry.substring(0, 2), entry.substring(2))
    }

    private val byIso = all.associateBy { it.iso }
    private val codes = all.map { it.callingCode }.toSet()

    const val FALLBACK_ISO = "IN"

    fun byIso(iso: String?): Country? = iso?.uppercase()?.let(byIso::get)

    /**
     * Picks the default region: SIM country first (where the number was issued), then
     * the network the phone is on, then the locale's region, then India — the region
     * most of this app's users are in.
     */
    fun resolveDefault(simIso: String?, networkIso: String?, localeIso: String?): Country =
        listOf(simIso, networkIso, localeIso).firstNotNullOfOrNull { byIso(it?.trim()) }
            ?: byIso.getValue(FALLBACK_ISO)

    /** The calling code at the start of [e164Digits], longest match first (codes are 1–3 digits). */
    fun callingCodeOf(e164Digits: String): String? =
        (3 downTo 1).asSequence()
            .filter { it < e164Digits.length }
            .map { e164Digits.substring(0, it) }
            .firstOrNull { it in codes }

    /** Human-readable international form, e.g. "+91 98765 43210" or "+1 202-555-0123". */
    fun format(e164Digits: String): String {
        val cc = callingCodeOf(e164Digits) ?: return "+$e164Digits"
        val national = e164Digits.substring(cc.length)
        val grouped = when {
            cc == "91" && national.length == 10 -> "${national.take(5)} ${national.drop(5)}"
            cc == "1" && national.length == 10 ->
                "${national.take(3)}-${national.substring(3, 6)}-${national.drop(6)}"
            else -> national
        }
        return "+$cc $grouped"
    }
}
