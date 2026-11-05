package com.ukm.app.iconscustomizer

import android.content.SharedPreferences

/**
 * Single source of truth for preference keys and defaults, shared by the app UI and the
 * hooks so both sides always agree on what "not configured" means.
 */
object Prefs {
    // Icons
    const val ICON_PACK = "icon_pack"
    /** Comma-separated pack packages, tried in order for apps the main pack doesn't cover. */
    const val ICON_PACK_FALLBACKS = "icon_pack_fallbacks"
    const val THEMED_ICONS = "enable_themed_icons"
    const val HOMESCREEN_ONLY = "themed_icons_homescreen_only"
    const val MONET_ICONS = "enable_monet_colors"
    const val MONET_FG = "monet_fg_color"
    const val MONET_BG = "monet_bg_color"
    const val ICON_SCALE = "icon_scale"
    const val ICON_NORMALIZE = "icon_normalize"
    const val ICON_NORMALIZE_SIZE = "icon_normalize_size"
    const val HIDE_SHADOWS = "hide_icon_shadows"
    const val BADGE_BLUR = "badge_blur"
    const val BADGE_BLUR_LEVEL = "badge_blur_level"

    // Labels
    const val HIDE_LABELS = "hide_icon_labels"
    const val HIDE_DOCK_LABELS = "hide_dock_labels"
    const val LABEL_SIZE = "icon_title_text_size"

    // Dock
    const val DOCK = "enable_dock"
    const val DOCK_STYLE = "dock_style"
    const val DOCK_COLOR = "dock_color"
    const val DOCK_WIDTH = "dock_width"
    const val DOCK_PADDING = "dock_height"
    const val DOCK_RADIUS = "dock_radius_dp"
    const val DOCK_ALPHA = "dock_alpha"
    const val DOCK_STROKE = "dock_stroke_dp"
    const val DOCK_BLUR = "dock_blur_radius"
    const val DOCK_UNLIMITED = "dock_unlimited_icons"

    // Folders
    const val FOLDER = "folder_custom"
    const val FOLDER_STYLE = "folder_style"
    const val FOLDER_COLOR = "folder_color"
    const val FOLDER_RADIUS = "folder_radius_dp"
    const val FOLDER_ALPHA = "folder_alpha"
    const val FOLDER_STROKE = "folder_stroke_dp"
    const val FOLDER_COLUMNS = "folder_columns"
    const val FOLDER_BLUR = "folder_blur"
    const val FOLDER_BLUR_RADIUS = "folder_blur_radius"
    const val FOLDER_WIDTH = "folder_width"

    // Widgets
    const val CLOCK = "enable_themed_clock"
    const val CLOCK_COLOR = "monet_clock_color"

    // Recents & blur
    const val RECENTS_RADIUS = "recents_radius_dp"
    const val RECENTS_BLUR = "recents_full_blur"
    const val RECENTS_HIDE_STATUSBAR = "recents_hide_statusbar"
    const val BLUR_SCALE = "launcher_blur_scale"

    // Animations
    const val ANIM_PRESET = "anim_preset"
    const val APP_ANIM = "app_anim_duration"
    const val RECENTS_ANIM = "recents_anim_duration"
    const val FULL_ANIM = "unlock_animations"
    const val HIGH_END_ANIM = "unlock_anim_rate"
    const val WINDOW_SCALE = "sys_window_scale"
    const val TRANSITION_SCALE = "sys_transition_scale"
    const val ANIMATOR_SCALE = "sys_animator_scale"

    // Control center
    const val CC_TILE_RADIUS = "cc_tile_radius_dp"
    const val CC_CARD_RADIUS = "cc_card_radius_dp"
    const val CC_SLIDER_RADIUS = "cc_slider_radius_dp"
    const val CC_BACKGROUND = "cc_background_opacity"

    // Volume panel
    const val VOL_PANEL_RADIUS = "volume_panel_radius_dp"
    const val VOL_SLIDER_RADIUS = "volume_slider_radius_dp"
    const val VOL_BLUR = "volume_blur"

    // App only
    const val AUTO_APPLY = "auto_apply"

    /** Per-app names, stored per activity and independent of the icon pack. */
    const val CUSTOM_LABEL_PREFIX = "custom_label_"

    fun labelKey(component: String) = CUSTOM_LABEL_PREFIX + component

    private const val SHORTCUT_PREFIX = "ShortcutInfo{"

    /** Pinned shortcuts share their app's component, so they are identified by id instead. */
    fun shortcutKey(packageName: String, id: String) = "$SHORTCUT_PREFIX$packageName/$id}"

    fun isShortcutKey(key: String) = key.startsWith(SHORTCUT_PREFIX)

    val DEFAULTS: Map<String, Any> = mapOf(
        ICON_PACK to "none",
        ICON_PACK_FALLBACKS to "",
        THEMED_ICONS to false,
        HOMESCREEN_ONLY to false,
        MONET_ICONS to false,
        MONET_FG to 0,
        MONET_BG to 0,
        ICON_SCALE to 100,
        ICON_NORMALIZE to false,
        ICON_NORMALIZE_SIZE to 90,
        HIDE_SHADOWS to false,
        BADGE_BLUR to false,
        BADGE_BLUR_LEVEL to 60,
        HIDE_LABELS to false,
        HIDE_DOCK_LABELS to false,
        LABEL_SIZE to 0,
        DOCK to false,
        DOCK_STYLE to "glass",
        DOCK_COLOR to 0,
        DOCK_WIDTH to 92,
        DOCK_PADDING to 12,
        DOCK_RADIUS to 28,
        DOCK_ALPHA to 70,
        DOCK_STROKE to 1,
        DOCK_BLUR to 60,
        DOCK_UNLIMITED to false,
        FOLDER to false,
        FOLDER_STYLE to "glass",
        FOLDER_COLOR to 0,
        FOLDER_RADIUS to 22,
        FOLDER_ALPHA to 75,
        FOLDER_STROKE to 1,
        FOLDER_COLUMNS to 0,
        FOLDER_BLUR to false,
        FOLDER_BLUR_RADIUS to 60,
        FOLDER_WIDTH to 100,
        CLOCK to false,
        CLOCK_COLOR to 0,
        RECENTS_RADIUS to 0,
        RECENTS_BLUR to false,
        RECENTS_HIDE_STATUSBAR to false,
        BLUR_SCALE to 100,
        ANIM_PRESET to "system",
        APP_ANIM to 100,
        RECENTS_ANIM to 100,
        FULL_ANIM to false,
        HIGH_END_ANIM to false,
        WINDOW_SCALE to 100,
        TRANSITION_SCALE to 100,
        ANIMATOR_SCALE to 100,
        CC_TILE_RADIUS to 0,
        CC_CARD_RADIUS to 0,
        CC_SLIDER_RADIUS to 0,
        CC_BACKGROUND to 100,
        VOL_PANEL_RADIUS to 0,
        VOL_SLIDER_RADIUS to 0,
        VOL_BLUR to false,
        AUTO_APPLY to false,
    )

    /** Animation presets: app open/close duration %, recents duration %, system scale %. */
    data class AnimPreset(val id: String, val app: Int, val recents: Int, val scale: Int)

    val ANIM_PRESETS = listOf(
        AnimPreset("system", 100, 100, 100),
        AnimPreset("snappy", 60, 60, 50),
        AnimPreset("balanced", 85, 85, 75),
        AnimPreset("smooth", 120, 115, 100),
        AnimPreset("cinematic", 160, 150, 150),
    )

    // Typed reads that never throw: a key written with an older type falls back to the default.
    fun bool(p: SharedPreferences?, key: String): Boolean {
        val def = DEFAULTS[key] as? Boolean ?: false
        return try { p?.getBoolean(key, def) ?: def } catch (_: ClassCastException) { def }
    }

    fun int(p: SharedPreferences?, key: String): Int {
        val def = DEFAULTS[key] as? Int ?: 0
        return try { p?.getInt(key, def) ?: def } catch (_: ClassCastException) { def }
    }

    fun string(p: SharedPreferences?, key: String): String {
        val def = DEFAULTS[key] as? String ?: ""
        return try { p?.getString(key, def) ?: def } catch (_: ClassCastException) { def }
    }
}
