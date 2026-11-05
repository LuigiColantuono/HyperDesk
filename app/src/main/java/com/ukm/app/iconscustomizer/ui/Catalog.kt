package com.ukm.app.iconscustomizer.ui

import com.ukm.app.iconscustomizer.Prefs
import com.ukm.app.iconscustomizer.R
import com.ukm.app.iconscustomizer.ui.Dependency.Companion.on
import com.ukm.app.iconscustomizer.ui.Dependency.Companion.oneOf

/** Declarative description of every screen; the renderer builds the UI from it. */
object Catalog {

    private val materialChoices = listOf(
        Choice("default", R.string.material_default),
        Choice("glass", R.string.material_glass),
        Choice("frosted", R.string.material_frosted),
        Choice("liquid", R.string.material_liquid),
        Choice("solid", R.string.material_solid),
        Choice("monet", R.string.material_monet),
        Choice("gradient", R.string.material_gradient),
        Choice("amoled", R.string.material_amoled),
        Choice("outline", R.string.material_outline),
    )

    private val tintable = arrayOf("glass", "frosted", "liquid", "solid", "monet", "gradient", "outline")

    val icons = Category(
        "icons", R.string.cat_icons, R.string.cat_icons_summary, R.drawable.ic_apps,
        0xFF4F7CFF.toInt(), Group.LAUNCHER,
        listOf(
            Section(
                R.string.sec_icon_pack, listOf(
                    ActionItem("pick_icon_pack", R.string.pref_choose_pack, R.string.pref_choose_pack_summary, R.drawable.ic_apps),
                    SwitchItem(Prefs.THEMED_ICONS, R.string.pref_icon_packs, R.string.pref_icon_packs_summary, R.drawable.ic_palette, Scope.LAUNCHER),
                    ActionItem("pick_fallback_packs", R.string.pref_fallback_packs, R.string.icon_pack_none, R.drawable.ic_widgets, listOf(on(Prefs.THEMED_ICONS))),
                    SwitchItem(Prefs.HOMESCREEN_ONLY, R.string.pref_homescreen_only, R.string.pref_homescreen_only_summary, R.drawable.ic_home, Scope.LAUNCHER, listOf(on(Prefs.THEMED_ICONS))),
                    ActionItem("per_app_icons", R.string.pref_apply_custom, R.string.pref_apply_custom_summary, R.drawable.ic_smartphone, listOf(on(Prefs.THEMED_ICONS))),
                )
            ),
            Section(
                R.string.sec_icon_colors, listOf(
                    SwitchItem(Prefs.MONET_ICONS, R.string.pref_override_colors, R.string.pref_override_colors_summary, R.drawable.ic_palette, Scope.LAUNCHER, listOf(on(Prefs.THEMED_ICONS))),
                    ColorItem(Prefs.MONET_FG, "selected_fg_color_name", R.string.pref_fg_color, R.drawable.ic_opacity, Scope.LAUNCHER, listOf(on(Prefs.THEMED_ICONS), on(Prefs.MONET_ICONS))),
                    ColorItem(Prefs.MONET_BG, "selected_bg_color_name", R.string.pref_bg_color, R.drawable.ic_folder, Scope.LAUNCHER, listOf(on(Prefs.THEMED_ICONS), on(Prefs.MONET_ICONS))),
                )
            ),
            Section(
                R.string.sec_icon_look, listOf(
                    PreviewItem(PreviewKind.ICONS),
                    SliderItem(Prefs.ICON_SCALE, R.string.pref_icon_scale, R.drawable.ic_resize, 70, 130, Scope.LAUNCHER, unit = "%", summary = R.string.pref_icon_scale_summary),
                    SwitchItem(Prefs.ICON_NORMALIZE, R.string.pref_icon_normalize, R.string.pref_icon_normalize_summary, R.drawable.ic_tune, Scope.LAUNCHER, listOf(on(Prefs.THEMED_ICONS))),
                    SliderItem(Prefs.ICON_NORMALIZE_SIZE, R.string.pref_icon_normalize_size, R.drawable.ic_width, 70, 100, Scope.LAUNCHER, unit = "%", summary = R.string.pref_icon_normalize_size_summary, dependsOn = listOf(on(Prefs.THEMED_ICONS), on(Prefs.ICON_NORMALIZE))),
                    SwitchItem(Prefs.HIDE_SHADOWS, R.string.pref_hide_icon_shadows, R.string.pref_hide_icon_shadows_summary, R.drawable.ic_shadow, Scope.LAUNCHER, beta = true),
                    SwitchItem(Prefs.BADGE_BLUR, R.string.pref_badge_blur, R.string.pref_badge_blur_summary, R.drawable.ic_blur, Scope.LAUNCHER, beta = true),
                    SliderItem(Prefs.BADGE_BLUR_LEVEL, R.string.pref_badge_blur_level, R.drawable.ic_blur, 1, 100, Scope.LAUNCHER, unit = "%", dependsOn = listOf(on(Prefs.BADGE_BLUR))),
                )
            ),
        )
    )

    val labels = Category(
        "labels", R.string.cat_labels, R.string.cat_labels_summary, R.drawable.ic_title,
        0xFF9C5BFF.toInt(), Group.LAUNCHER,
        listOf(
            Section(
                R.string.sec_labels, listOf(
                    PreviewItem(PreviewKind.ICONS),
                    SwitchItem(Prefs.HIDE_LABELS, R.string.pref_hide_icon_labels, R.string.pref_hide_icon_labels_summary, R.drawable.ic_label_off, Scope.LAUNCHER, beta = true),
                    SwitchItem(Prefs.HIDE_DOCK_LABELS, R.string.pref_hide_dock_labels, R.string.pref_hide_dock_labels_summary, R.drawable.ic_dock, Scope.LAUNCHER, beta = true),
                    SliderItem(Prefs.LABEL_SIZE, R.string.pref_label_size, R.drawable.ic_text_size, 0, 24, Scope.LAUNCHER, zeroLabel = R.string.label_system, beta = true),
                )
            ),
        )
    )

    val dock = Category(
        "dock", R.string.cat_dock, R.string.cat_dock_summary, R.drawable.ic_dock,
        0xFF00A99D.toInt(), Group.LAUNCHER,
        listOf(
            Section(
                R.string.sec_dock, listOf(
                    PreviewItem(PreviewKind.DOCK),
                    SwitchItem(Prefs.DOCK, R.string.pref_dock, R.string.pref_dock_summary, R.drawable.ic_dock, Scope.LAUNCHER),
                )
            ),
            Section(
                R.string.sec_material, listOf(
                    ChoiceItem(Prefs.DOCK_STYLE, R.string.pref_material, R.drawable.ic_blur, materialChoices, Scope.LAUNCHER, listOf(on(Prefs.DOCK))),
                    ColorItem(Prefs.DOCK_COLOR, "selected_dock_color", R.string.pref_material_color, R.drawable.ic_palette, Scope.LAUNCHER, listOf(on(Prefs.DOCK), oneOf(Prefs.DOCK_STYLE, *tintable))),
                    SliderItem(Prefs.DOCK_ALPHA, R.string.pref_opacity, R.drawable.ic_opacity, 0, 100, Scope.LAUNCHER, unit = "%", dependsOn = listOf(on(Prefs.DOCK))),
                    SliderItem(Prefs.DOCK_BLUR, R.string.pref_blur_radius, R.drawable.ic_blur, 0, 150, Scope.LAUNCHER, unit = " dp", summary = R.string.pref_blur_radius_summary, dependsOn = listOf(on(Prefs.DOCK), oneOf(Prefs.DOCK_STYLE, "frosted"))),
                    SliderItem(Prefs.DOCK_STROKE, R.string.pref_stroke, R.drawable.ic_stroke, 0, 4, Scope.LAUNCHER, unit = " dp", dependsOn = listOf(on(Prefs.DOCK))),
                )
            ),
            Section(
                R.string.sec_shape, listOf(
                    SliderItem(Prefs.DOCK_RADIUS, R.string.pref_corner_radius, R.drawable.ic_rounded_corner, 0, 48, Scope.LAUNCHER, unit = " dp", dependsOn = listOf(on(Prefs.DOCK))),
                    SliderItem(Prefs.DOCK_WIDTH, R.string.pref_dock_width, R.drawable.ic_width, 50, 100, Scope.LAUNCHER, unit = "%", dependsOn = listOf(on(Prefs.DOCK))),
                    SliderItem(Prefs.DOCK_PADDING, R.string.pref_dock_padding, R.drawable.ic_height, 0, 40, Scope.LAUNCHER, unit = " dp", dependsOn = listOf(on(Prefs.DOCK))),
                )
            ),
            Section(
                R.string.sec_more, listOf(
                    SwitchItem(Prefs.DOCK_UNLIMITED, R.string.pref_dock_unlimited, R.string.pref_dock_unlimited_summary, R.drawable.ic_apps, Scope.LAUNCHER),
                )
            ),
        )
    )

    val folders = Category(
        "folders", R.string.cat_folders, R.string.cat_folders_summary, R.drawable.ic_folder,
        0xFFFF8A3D.toInt(), Group.LAUNCHER,
        listOf(
            Section(
                R.string.sec_folders, listOf(
                    PreviewItem(PreviewKind.FOLDER),
                    SwitchItem(Prefs.FOLDER, R.string.pref_folder_style, R.string.pref_folder_style_summary, R.drawable.ic_folder, Scope.LAUNCHER),
                )
            ),
            Section(
                R.string.sec_material, listOf(
                    ChoiceItem(Prefs.FOLDER_STYLE, R.string.pref_material, R.drawable.ic_blur, materialChoices.filter { it.value != "frosted" }, Scope.LAUNCHER, listOf(on(Prefs.FOLDER))),
                    ColorItem(Prefs.FOLDER_COLOR, "selected_folder_color", R.string.pref_material_color, R.drawable.ic_palette, Scope.LAUNCHER, listOf(on(Prefs.FOLDER), oneOf(Prefs.FOLDER_STYLE, *tintable))),
                    SliderItem(Prefs.FOLDER_ALPHA, R.string.pref_opacity, R.drawable.ic_opacity, 0, 100, Scope.LAUNCHER, unit = "%", dependsOn = listOf(on(Prefs.FOLDER))),
                    SliderItem(Prefs.FOLDER_STROKE, R.string.pref_stroke, R.drawable.ic_stroke, 0, 4, Scope.LAUNCHER, unit = " dp", dependsOn = listOf(on(Prefs.FOLDER))),
                    SliderItem(Prefs.FOLDER_RADIUS, R.string.pref_corner_radius, R.drawable.ic_rounded_corner, 0, 40, Scope.LAUNCHER, unit = " dp", dependsOn = listOf(on(Prefs.FOLDER))),
                    SwitchItem(Prefs.FOLDER_BLUR, R.string.pref_folder_blur, R.string.pref_folder_blur_summary, R.drawable.ic_blur, Scope.LAUNCHER, beta = true),
                    SliderItem(Prefs.FOLDER_BLUR_RADIUS, R.string.pref_blur_radius, R.drawable.ic_blur, 10, 150, Scope.LAUNCHER, unit = " dp", dependsOn = listOf(on(Prefs.FOLDER_BLUR))),
                )
            ),
            Section(
                R.string.sec_layout, listOf(
                    SliderItem(Prefs.FOLDER_WIDTH, R.string.pref_folder_width, R.drawable.ic_width, 70, 100, Scope.LAUNCHER, unit = "%", summary = R.string.pref_folder_width_summary),
                    SliderItem(Prefs.FOLDER_COLUMNS, R.string.pref_folder_columns, R.drawable.ic_columns, 0, 6, Scope.LAUNCHER, zeroLabel = R.string.label_system, beta = true),
                )
            ),
        )
    )

    val widgets = Category(
        "widgets", R.string.cat_widgets, R.string.cat_widgets_summary, R.drawable.ic_widgets,
        0xFFE5508C.toInt(), Group.LAUNCHER,
        listOf(
            Section(
                R.string.sec_clock, listOf(
                    SwitchItem(Prefs.CLOCK, R.string.pref_clock, R.string.pref_clock_summary, R.drawable.ic_schedule, Scope.LAUNCHER),
                    ColorItem(Prefs.CLOCK_COLOR, "selected_monet_clock_color", R.string.pref_clock_color, R.drawable.ic_palette, Scope.LAUNCHER, listOf(on(Prefs.CLOCK))),
                )
            ),
        )
    )

    val recents = Category(
        "recents", R.string.cat_recents, R.string.cat_recents_summary, R.drawable.ic_recents,
        0xFF5C6BC0.toInt(), Group.LAUNCHER,
        listOf(
            Section(
                R.string.sec_recents, listOf(
                    SliderItem(Prefs.RECENTS_RADIUS, R.string.pref_recents_radius, R.drawable.ic_rounded_corner, 0, 60, Scope.LAUNCHER, unit = " dp", zeroLabel = R.string.label_system),
                    SwitchItem(Prefs.RECENTS_BLUR, R.string.pref_recents_blur, R.string.pref_recents_blur_summary, R.drawable.ic_blur, Scope.LAUNCHER),
                    SwitchItem(Prefs.RECENTS_HIDE_STATUSBAR, R.string.pref_recents_statusbar, R.string.pref_recents_statusbar_summary, R.drawable.ic_smartphone, Scope.LAUNCHER),
                )
            ),
            Section(
                R.string.sec_blur, listOf(
                    SliderItem(Prefs.BLUR_SCALE, R.string.pref_blur_scale, R.drawable.ic_blur, 0, 200, Scope.LAUNCHER, unit = "%", summary = R.string.pref_blur_scale_summary),
                )
            ),
        )
    )

    val animations = Category(
        "animations", R.string.cat_animations, R.string.cat_animations_summary, R.drawable.ic_motion,
        0xFFEF5350.toInt(), Group.SYSTEM,
        listOf(
            Section(
                R.string.sec_anim_presets, listOf(
                    ChoiceItem(
                        Prefs.ANIM_PRESET, R.string.pref_anim_preset, R.drawable.ic_motion,
                        listOf(
                            Choice("system", R.string.preset_system),
                            Choice("snappy", R.string.preset_snappy),
                            Choice("balanced", R.string.preset_balanced),
                            Choice("smooth", R.string.preset_smooth),
                            Choice("cinematic", R.string.preset_cinematic),
                            Choice("custom", R.string.preset_custom),
                        ),
                        Scope.APP
                    ),
                )
            ),
            Section(
                R.string.sec_anim_apps, listOf(
                    SliderItem(Prefs.APP_ANIM, R.string.pref_app_anim, R.drawable.ic_launch, 25, 250, Scope.LAUNCHER, unit = "%", summary = R.string.pref_app_anim_summary, step = 5),
                    SliderItem(Prefs.RECENTS_ANIM, R.string.pref_recents_anim, R.drawable.ic_recents, 25, 250, Scope.LAUNCHER, unit = "%", summary = R.string.pref_recents_anim_summary, step = 5),
                    SwitchItem(Prefs.FULL_ANIM, R.string.pref_full_anim, R.string.pref_full_anim_summary, R.drawable.ic_motion, Scope.LAUNCHER),
                    SwitchItem(Prefs.HIGH_END_ANIM, R.string.pref_high_end, R.string.pref_high_end_summary, R.drawable.ic_speed, Scope.LAUNCHER),
                )
            ),
            Section(
                R.string.sec_anim_system, listOf(
                    InfoItem(R.string.info_root_scales, R.drawable.ic_info),
                    SliderItem(Prefs.WINDOW_SCALE, R.string.pref_window_scale, R.drawable.ic_window, 0, 300, Scope.SYSTEM_SETTINGS, unit = "x", step = 5, divisor = 100),
                    SliderItem(Prefs.TRANSITION_SCALE, R.string.pref_transition_scale, R.drawable.ic_launch, 0, 300, Scope.SYSTEM_SETTINGS, unit = "x", step = 5, divisor = 100),
                    SliderItem(Prefs.ANIMATOR_SCALE, R.string.pref_animator_scale, R.drawable.ic_speed, 0, 300, Scope.SYSTEM_SETTINGS, unit = "x", step = 5, divisor = 100),
                )
            ),
        )
    )

    val controlCenter = Category(
        "control_center", R.string.cat_control_center, R.string.cat_control_center_summary, R.drawable.ic_control,
        0xFF00ACC1.toInt(), Group.SYSTEM,
        listOf(
            Section(
                R.string.sec_corners, listOf(
                    PreviewItem(PreviewKind.CONTROL_CENTER),
                    SliderItem(Prefs.CC_TILE_RADIUS, R.string.pref_cc_tile_radius, R.drawable.ic_apps, 0, 40, Scope.SYSTEMUI, unit = " dp", zeroLabel = R.string.label_system, beta = true),
                    SliderItem(Prefs.CC_CARD_RADIUS, R.string.pref_cc_card_radius, R.drawable.ic_widgets, 0, 40, Scope.SYSTEMUI, unit = " dp", zeroLabel = R.string.label_system, beta = true),
                    SliderItem(Prefs.CC_BACKGROUND, R.string.pref_cc_background, R.drawable.ic_opacity, 10, 100, Scope.SYSTEMUI, unit = "%", summary = R.string.pref_cc_background_summary, beta = true),
                    SliderItem(Prefs.CC_SLIDER_RADIUS, R.string.pref_cc_slider_radius, R.drawable.ic_tune, 0, 40, Scope.SYSTEMUI, unit = " dp", zeroLabel = R.string.label_system, beta = true),
                )
            ),
        )
    )

    val volume = Category(
        "volume", R.string.cat_volume, R.string.cat_volume_summary, R.drawable.ic_volume,
        0xFFFFB300.toInt(), Group.SYSTEM,
        listOf(
            Section(
                R.string.sec_volume, listOf(
                    PreviewItem(PreviewKind.VOLUME),
                    SliderItem(Prefs.VOL_PANEL_RADIUS, R.string.pref_volume_panel_radius, R.drawable.ic_rounded_corner, 0, 40, Scope.SYSTEMUI, unit = " dp", zeroLabel = R.string.label_system, beta = true),
                    SliderItem(Prefs.VOL_SLIDER_RADIUS, R.string.pref_volume_slider_radius, R.drawable.ic_volume, 0, 40, Scope.SYSTEMUI, unit = " dp", zeroLabel = R.string.label_system, beta = true),
                    SwitchItem(Prefs.VOL_BLUR, R.string.pref_volume_blur, R.string.pref_volume_blur_summary, R.drawable.ic_blur, Scope.SYSTEMUI, beta = true),
                )
            ),
        )
    )

    val categories = listOf(icons, labels, dock, folders, widgets, recents, animations, controlCenter, volume)

    fun byId(id: String) = categories.first { it.id == id }

    val tools = listOf(
        Section(
            R.string.sec_apply, listOf(
                SwitchItem(Prefs.AUTO_APPLY, R.string.pref_auto_apply, R.string.pref_auto_apply_summary, R.drawable.ic_bolt, Scope.APP),
                ActionItem("restart_launcher", R.string.action_restart_launcher, R.string.action_restart_launcher_summary, R.drawable.ic_home),
                ActionItem("restart_systemui", R.string.action_restart_systemui, R.string.action_restart_systemui_summary, R.drawable.ic_refresh),
            )
        ),
        Section(
            R.string.sec_backup, listOf(
                ActionItem("export", R.string.action_export, R.string.action_export_summary, R.drawable.ic_backup),
                ActionItem("import", R.string.action_import, R.string.action_import_summary, R.drawable.ic_restore),
            )
        ),
        Section(
            R.string.sec_reset, listOf(
                ActionItem("reset", R.string.action_reset, R.string.action_reset_summary, R.drawable.ic_reset, danger = true),
            )
        ),
        Section(
            R.string.sec_about, listOf(
                ActionItem("about", R.string.action_about, R.string.action_about_summary, R.drawable.ic_info),
            )
        ),
    )

    /** Keys that a backup/reset covers (everything except app-only state). */
    val settingKeys: List<String> get() = Prefs.DEFAULTS.keys.toList()
}
