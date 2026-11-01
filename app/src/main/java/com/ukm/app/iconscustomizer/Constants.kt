package com.ukm.app.iconscustomizer

object Constants {
    const val TAG = "UKMTAG"
    const val ACTION_RELOAD = "com.ukm.app.RELOAD_ICONS"

    /** App to launcher: list the pinned shortcuts. Only the launcher is allowed to read them. */
    const val ACTION_LIST_SHORTCUTS = "com.ukm.app.LIST_SHORTCUTS"

    /** Launcher to app, sent to this module only. */
    const val ACTION_SHORTCUTS_RESULT = "com.ukm.app.SHORTCUTS_RESULT"
    const val EXTRA_SHORTCUTS = "shortcuts"

    /** HyperOS system launcher (CN + most global ROMs) and the POCO launcher. */
    val LAUNCHER_PACKAGES = listOf("com.miui.home", "com.mi.android.globallauncher")

    /**
     * Resolves a color that can be either an Android resource ID (0x01... or 0x7f...)
     * or a direct ARGB color int (0xFF...).
     */
    fun resolveColor(context: android.content.Context, colorOrResId: Int): Int {
        if (colorOrResId == 0) return 0
        val packageId = (colorOrResId ushr 24) and 0xFF
        if (packageId == 0x01 || packageId == 0x7f) {
            try {
                return androidx.core.content.ContextCompat.getColor(context, colorOrResId)
            } catch (_: Throwable) {
            }
        }
        return colorOrResId
    }
}

