package com.crylo.quickchat

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.net.toUri
import com.crylo.quickchat.core.ChatApp

/** Finds the installed messenger and hands it the click-to-chat link. */
object ChatLauncher {
    enum class Result { OPENED, NOT_INSTALLED, FAILED }

    /** Visible only because each package is declared under `<queries>` in the manifest. */
    fun installedPackage(context: Context, app: ChatApp): String? =
        app.packages.firstOrNull { isInstalled(context.packageManager, it) }

    private fun isInstalled(pm: PackageManager, pkg: String): Boolean = try {
        val info = if (Build.VERSION.SDK_INT >= 33) {
            pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getApplicationInfo(pkg, 0)
        }
        info.enabled
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    fun open(context: Context, app: ChatApp, digits: String, message: String): Result {
        val pkg = installedPackage(context, app) ?: return Result.NOT_INSTALLED
        // Pinning the package keeps the https link out of the browser and skips the
        // "open with" chooser; the link is the app's own documented click-to-chat URL.
        val links = listOfNotNull(app.link(digits, message), app.fallbackLink(digits))
        for (link in links) {
            val intent = Intent(Intent.ACTION_VIEW, link.toUri())
                .setPackage(pkg)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
                return Result.OPENED
            } catch (_: ActivityNotFoundException) {
                // Try the next form of the link.
            }
        }
        return Result.FAILED
    }
}
