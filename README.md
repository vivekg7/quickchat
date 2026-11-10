# QuickChat

Open a WhatsApp, WhatsApp Business, Telegram or Signal chat with any phone number
without saving it as a contact. Works from the app, from text selection, from the
share menu, from phone-number links and from a Quick Settings tile.

The Play Store versions of this idea are full of ads and trackers for what is a
one-line deep link. QuickChat is free, open source (MIT), with **no ads, no tracking,
no analytics, no account — and no internet permission at all**.

## Features

- **Number field that accepts what people actually copy**: spaces, dashes, brackets,
  a leading trunk `0`, `00`/`011` international prefixes, `+44 (0) 20…`, `tel:` URIs,
  wa.me links, non-Latin digits, and bare 10-digit Indian mobiles. A live preview shows
  the international number that will be opened.
- **Country-code picker**, defaulting to the SIM's country, then the network's, then
  the phone's locale region, then India. A number typed with `+` or `00` ignores the
  picker. Your pick is remembered.
- **Optional pre-filled message.** WhatsApp receives it in the link. Telegram's and
  Signal's phone links cannot carry text, so the message is copied to the clipboard
  instead, ready to paste.
- **Paste button**, and a suggestion card when the app opens with a phone number on
  the clipboard (can be turned off).
- **Four messengers**: WhatsApp, WhatsApp Business (button shown only if installed),
  Telegram (also Telegram X and the telegram.org build), Signal (also Molly). A clear
  message if the chosen app isn't installed.
- **Entry points**:
  - Select text in any app → **Chat via QuickChat** (`ACTION_PROCESS_TEXT`).
  - Share text to QuickChat (`ACTION_SEND`). If the text has no number it becomes the
    message instead.
  - Tap a `tel:` link → QuickChat appears next to the dialer in the chooser. Can be
    switched off in Settings.
  - **Quick Settings tile**: copy a number anywhere, pull down the shade, tap the tile.
  - When opened from another app, QuickChat closes itself after the chat opens, so
    Back returns you where you were.
- **Recent numbers** (on by default): tap to reopen in the same app, long-press to
  load the number into the field, add a label, remove one, or clear all. Turning the
  setting off stops saving new numbers; existing ones stay until you clear them.

## Permissions

None. The manifest declares no `<uses-permission>` at all:

- No `INTERNET` — the app never talks to a server; it only hands a link to the
  messenger you pick.
- No contacts or phone-state permission — the SIM/network country comes from
  `TelephonyManager`, which needs none.
- Package visibility uses a `<queries>` list of exactly the seven messenger package
  names, not `QUERY_ALL_PACKAGES`.

Backup and device transfer are disabled, because recent numbers are other people's
phone numbers. See [PRIVACY.md](PRIVACY.md).

## Building

Requires JDK 17+. This machine has no system JDK, so use Android Studio's bundled JBR:

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug testDebugUnitTest
./gradlew assembleRelease
```

Release signing is optional: put a `keystore.properties` (`storeFile`, `storePassword`,
`keyAlias`, `keyPassword`) in the project root to sign; without it the release APK is
built unsigned. Never commit it.

Toolchain: Gradle 9.9, AGP 9.5 (built-in Kotlin), Kotlin 2.4, Compose BOM 2026.09,
Material 3, minSdk 31 (Android 12), targetSdk 37. Only AndroidX dependencies
(Compose, DataStore); nothing proprietary, so it's F-Droid-compatible.

## Design decisions

- **Own parser instead of libphonenumber.** libphonenumber would add several MB of
  metadata to an app whose job is to put the right digits in a link; the messenger
  validates the number itself. The rules in `core/PhoneNumbers.kt` cover how numbers
  are really written and are pinned down by JVM unit tests (`app/src/test`). Known
  limits: no per-country length validation (any 7–15 digit result is accepted), and a
  national number of 11+ digits that happens to start with your country code is read
  as already international — this is what makes `919876543210` work, and real
  national numbers that long are rare.
- **Region-specific trunk rules** are kept to the cases that matter: `0` is dropped
  except in Italy, San Marino and Côte d'Ivoire (where it is part of the number), and
  Russia/Kazakhstan's `8` trunk prefix is handled.
- **Official links, package pinned.** WhatsApp gets `https://api.whatsapp.com/send?phone=…&text=…`,
  Telegram `https://t.me/+<number>`, Signal `https://signal.me/#p/+<number>`. The
  intent is pinned to the installed package so the link never detours through a
  browser. If a build doesn't claim the https link, the app's own scheme
  (`tg://resolve?phone=`, `sgnl://signal.me/…`) is tried before reporting an error.
- **History in DataStore as plain text**, not a database: at most 50 short rows, and a
  tab-separated format is trivial to test and migrate.
- **Clipboard read on window focus.** Android 10+ only lets the focused app read the
  clipboard, so the launch suggestion and the Quick Settings tile both read it in
  `onWindowFocusChanged`. The setting is checked _before_ reading, so turning
  suggestions off also stops the system's "pasted from clipboard" notice.
- **`tel:` handler is an activity-alias** so it can be enabled or disabled at runtime
  without affecting the other entry points.

## License

MIT — see [LICENSE](LICENSE).
