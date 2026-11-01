package com.ukm.app.iconscustomizer.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ukm.app.iconscustomizer.AllAppsFragment
import com.ukm.app.iconscustomizer.BuildConfig
import com.ukm.app.iconscustomizer.ColorPickerActivity
import com.ukm.app.iconscustomizer.IconPackHelper
import com.ukm.app.iconscustomizer.MainActivity
import com.ukm.app.iconscustomizer.Prefs
import com.ukm.app.iconscustomizer.R
import com.ukm.app.iconscustomizer.UIHelpers
import org.json.JSONObject

/** Implemented by pages that must re-read settings, e.g. after LSPosed connects. */
interface Refreshable {
    fun refreshPage()
}

/** Common behaviour for every settings page: actions, colour picking, side effects. */
abstract class SettingsPageFragment : Fragment(), SettingsRenderer.Host, Refreshable {

    protected var renderer: SettingsRenderer? = null
    private var pendingColorScope: Scope = Scope.LAUNCHER

    private val colorPicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            onSettingChanged(pendingColorScope)
            renderer?.refresh()
        }
    }

    private val exporter = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { exportTo(it) }
    }

    private val importer = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { importFrom(it) }
    }

    protected val main get() = activity as? MainActivity

    override fun refreshPage() {
        renderer?.refresh()
    }

    protected fun toast(res: Int) {
        context?.let { Toast.makeText(it, res, Toast.LENGTH_SHORT).show() }
    }

    protected fun background(work: () -> Boolean, onDone: (Boolean) -> Unit) {
        Thread {
            val ok = work()
            activity?.runOnUiThread { if (isAdded) onDone(ok) }
        }.start()
    }

    override fun onColorClick(item: ColorItem) {
        pendingColorScope = item.scope
        colorPicker.launch(Intent(requireContext(), ColorPickerActivity::class.java).apply {
            putExtra("EXTRA_TITLE", getString(item.title))
            putExtra("EXTRA_TARGET_KEY", item.key)
            putExtra("EXTRA_TYPE_KEY", item.typeKey)
        })
    }

    override fun onChanged(key: String, value: Any?, scope: Scope) {
        when (key) {
            Prefs.ANIM_PRESET -> applyPreset(value as? String ?: return)
            Prefs.APP_ANIM, Prefs.RECENTS_ANIM, Prefs.WINDOW_SCALE, Prefs.TRANSITION_SCALE, Prefs.ANIMATOR_SCALE ->
                PrefStore.put(Prefs.ANIM_PRESET, "custom")
            Prefs.THEMED_ICONS -> if (value == true && PrefStore.string(Prefs.ICON_PACK) == "none") showIconPackPicker()
        }
        if (scope == Scope.SYSTEM_SETTINGS) applySystemScales()
        onSettingChanged(scope)
    }

    private fun onSettingChanged(scope: Scope) {
        when (scope) {
            Scope.LAUNCHER -> if (PrefStore.bool(Prefs.AUTO_APPLY)) UIHelpers.restartLauncher(requireContext()) else PendingChanges.add(scope)
            Scope.SYSTEMUI -> PendingChanges.add(scope)
            else -> {}
        }
    }

    private fun applyPreset(id: String) {
        val preset = Prefs.ANIM_PRESETS.firstOrNull { it.id == id } ?: return
        PrefStore.put(Prefs.APP_ANIM, preset.app)
        PrefStore.put(Prefs.RECENTS_ANIM, preset.recents)
        PrefStore.put(Prefs.WINDOW_SCALE, preset.scale)
        PrefStore.put(Prefs.TRANSITION_SCALE, preset.scale)
        PrefStore.put(Prefs.ANIMATOR_SCALE, preset.scale)
        applySystemScales()
        onSettingChanged(Scope.LAUNCHER)
    }

    private fun applySystemScales() {
        val values = listOf(
            Settings.Global.WINDOW_ANIMATION_SCALE to PrefStore.int(Prefs.WINDOW_SCALE),
            Settings.Global.TRANSITION_ANIMATION_SCALE to PrefStore.int(Prefs.TRANSITION_SCALE),
            Settings.Global.ANIMATOR_DURATION_SCALE to PrefStore.int(Prefs.ANIMATOR_SCALE),
        )
        background({ UIHelpers.setAnimationScales(values) }) { ok -> if (!ok) toast(R.string.toast_root_denied) }
    }

    override fun onAction(item: ActionItem) {
        when (item.id) {
            "pick_icon_pack" -> showIconPackPicker()
            "pick_fallback_packs" -> showFallbackPackPicker()
            "per_app_icons" -> {
                val pack = PrefStore.string(Prefs.ICON_PACK)
                if (pack == "none") showIconPackPicker(openAppsAfter = true) else openAllApps(pack)
            }
            "restart_launcher" -> restartLauncher()
            "restart_systemui" -> confirmRestartSystemUi()
            "export" -> exporter.launch("HyperDesk-backup.json")
            "import" -> importer.launch(arrayOf("application/json", "text/plain", "*/*"))
            "reset" -> confirmReset()
            "about" -> showAbout()
        }
    }

    // ------------------------------------------------------------------ icon packs

    private fun showIconPackPicker(openAppsAfter: Boolean = false) {
        val ctx = requireContext()
        val packs = IconPacks.installed(ctx)
        if (packs.isEmpty()) {
            MaterialAlertDialogBuilder(ctx)
                .setIcon(R.drawable.ic_info)
                .setTitle(R.string.dialog_no_packs_title)
                .setMessage(R.string.dialog_no_packs_msg)
                .setPositiveButton(R.string.btn_search_google_play) { _, _ ->
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=icon+pack")))
                    } catch (_: Exception) {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=icon+pack&c=apps")))
                    }
                }
                .setNegativeButton(R.string.action_cancel, null)
                .show()
            return
        }
        val names = arrayOf(getString(R.string.icon_pack_none)) + packs.map { it.name }
        val ids = arrayOf("none") + packs.map { it.packageName }
        val current = ids.indexOf(PrefStore.string(Prefs.ICON_PACK)).coerceAtLeast(0)
        MaterialAlertDialogBuilder(ctx)
            .setIcon(R.drawable.ic_apps)
            .setTitle(R.string.pref_choose_pack)
            .setSingleChoiceItems(names, current) { dialog, which ->
                val pack = ids[which]
                PrefStore.put(Prefs.ICON_PACK, pack)
                PrefStore.put(Prefs.THEMED_ICONS, pack != "none")
                onSettingChanged(Scope.LAUNCHER)
                renderer?.refresh()
                dialog.dismiss()
                if (openAppsAfter && pack != "none") openAllApps(pack)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    /** Packs are tried in the order they were ticked, which the row's summary shows. */
    private fun showFallbackPackPicker() {
        val ctx = requireContext()
        val main = PrefStore.string(Prefs.ICON_PACK)
        val packs = IconPacks.installed(ctx).filter { it.packageName != main }
        if (packs.isEmpty()) return toast(R.string.toast_no_other_packs)
        val ids = packs.map { it.packageName }
        val selected = IconPackHelper.parsePackList(PrefStore.string(Prefs.ICON_PACK_FALLBACKS))
            .filter { it in ids }
            .toMutableList()
        MaterialAlertDialogBuilder(ctx)
            .setIcon(R.drawable.ic_widgets)
            .setTitle(R.string.pref_fallback_packs)
            .setMultiChoiceItems(
                packs.map { it.name }.toTypedArray(),
                BooleanArray(ids.size) { ids[it] in selected }
            ) { _, which, checked ->
                if (checked) selected += ids[which] else selected -= ids[which]
            }
            .setPositiveButton(R.string.action_save) { _, _ ->
                PrefStore.put(Prefs.ICON_PACK_FALLBACKS, selected.joinToString(","))
                onSettingChanged(Scope.LAUNCHER)
                renderer?.refresh()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun openAllApps(pack: String) {
        main?.openPage(AllAppsFragment().apply {
            arguments = Bundle().apply { putString("EXTRA_ICON_PACK", pack) }
        })
    }

    // ------------------------------------------------------------------ restarts

    protected fun restartLauncher() {
        val app = requireContext().applicationContext
        background({ UIHelpers.forceRestartLauncher(app) }) { ok ->
            if (ok) PendingChanges.remove(Scope.LAUNCHER)
            toast(if (ok) R.string.toast_launcher_restarted else R.string.toast_root_denied)
        }
    }

    protected fun confirmRestartSystemUi() {
        MaterialAlertDialogBuilder(requireContext())
            .setIcon(R.drawable.ic_refresh)
            .setTitle(R.string.action_restart_systemui)
            .setMessage(R.string.action_restart_systemui_confirm)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_restart) { _, _ ->
                background({ UIHelpers.restartSystemUi() }) { ok ->
                    if (ok) PendingChanges.remove(Scope.SYSTEMUI)
                    toast(if (ok) R.string.toast_systemui_restarted else R.string.toast_root_denied)
                }
            }
            .show()
    }

    // ------------------------------------------------------------------ backup

    private fun exportTo(uri: Uri) {
        try {
            val settings = JSONObject()
            for ((k, v) in PrefStore.all()) {
                if (k == Prefs.AUTO_APPLY || v == null) continue
                if (k in Prefs.DEFAULTS || k.startsWith("custom_icon_") || k.startsWith(Prefs.CUSTOM_LABEL_PREFIX) || k.startsWith("selected_")) settings.put(k, v)
            }
            val json = JSONObject()
                .put("app", "HyperDesk")
                .put("version", BuildConfig.VERSION_CODE)
                .put("settings", settings)
            requireContext().contentResolver.openOutputStream(uri)?.use { it.write(json.toString(2).toByteArray()) }
            toast(R.string.toast_export_done)
        } catch (_: Exception) {
            toast(R.string.toast_backup_failed)
        }
    }

    private fun importFrom(uri: Uri) {
        try {
            val text = requireContext().contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: return
            val settings = JSONObject(text).getJSONObject("settings")
            for (k in settings.keys()) {
                val expected = Prefs.DEFAULTS[k]
                val raw = settings.get(k)
                val value: Any? = when {
                    expected is Boolean || raw is Boolean -> raw as? Boolean
                    expected is Int || raw is Number -> (raw as? Number)?.toInt()
                    else -> raw as? String
                }
                if (value != null) PrefStore.put(k, value)
            }
            PendingChanges.add(Scope.LAUNCHER)
            PendingChanges.add(Scope.SYSTEMUI)
            main?.refreshAll()
            toast(R.string.toast_import_done)
        } catch (_: Exception) {
            toast(R.string.toast_backup_failed)
        }
    }

    private fun confirmReset() {
        MaterialAlertDialogBuilder(requireContext())
            .setIcon(R.drawable.ic_reset)
            .setTitle(R.string.action_reset)
            .setMessage(R.string.action_reset_confirm)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_reset_short) { _, _ ->
                for ((k, v) in Prefs.DEFAULTS) if (k != Prefs.AUTO_APPLY) PrefStore.put(k, v)
                applySystemScales()
                PendingChanges.add(Scope.LAUNCHER)
                PendingChanges.add(Scope.SYSTEMUI)
                main?.refreshAll()
            }
            .show()
    }

    private fun showAbout() {
        MaterialAlertDialogBuilder(requireContext())
            .setIcon(R.drawable.ic_sparkle)
            .setTitle(getString(R.string.about_title, BuildConfig.VERSION_NAME))
            .setMessage(R.string.about_message)
            .setPositiveButton(R.string.about_source) { _, _ ->
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/egorovsankin988-create/IconsCustomizer")))
            }
            .setNegativeButton(R.string.action_close, null)
            .show()
    }

    // ------------------------------------------------------------------ layout helpers

    protected fun pageView(inflater: LayoutInflater, container: ViewGroup?): View =
        inflater.inflate(R.layout.fragment_page, container, false)

    protected fun View.content(): LinearLayout = findViewById(R.id.page_content)
}
