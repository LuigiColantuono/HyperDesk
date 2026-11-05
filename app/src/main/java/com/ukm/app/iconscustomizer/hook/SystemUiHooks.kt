package com.ukm.app.iconscustomizer.hook

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import com.ukm.app.iconscustomizer.Prefs
import com.ukm.app.iconscustomizer.XposedHelpers
import io.github.libxposed.api.XposedModule
import java.util.concurrent.ConcurrentHashMap

/**
 * Control center and volume panel tweaks. On HyperOS both live in the `miui.systemui.plugin`
 * APK, which SystemUI loads with its own ClassLoader, so plugin hooks are installed lazily once
 * that plugin context is created.
 */
class SystemUiHooks(module: XposedModule, prefs: SharedPreferences?) :
    HookModule(module, prefs, SYSTEMUI) {

    companion object {
        const val SYSTEMUI = "com.android.systemui"
        private const val PLUGIN = "miui.systemui.plugin"
    }

    private val ccTile = int(Prefs.CC_TILE_RADIUS)
    private val ccCard = int(Prefs.CC_CARD_RADIUS)
    private val ccSlider = int(Prefs.CC_SLIDER_RADIUS)
    private val volPanel = int(Prefs.VOL_PANEL_RADIUS)
    private val volSlider = int(Prefs.VOL_SLIDER_RADIUS)
    private val volBlur = bool(Prefs.VOL_BLUR)
    private val ccBackground = int(Prefs.CC_BACKGROUND)

    /** Resource id -> replacement size in px, filled when the plugin loads. */
    private val dimenOverrides = ConcurrentHashMap<Int, Float>()
    @Volatile private var pluginHooked = false

    fun install(cl: ClassLoader) {
        val anything = ccTile + ccCard + ccSlider + volPanel + volSlider > 0 || volBlur || ccBackground < 100
        if (!anything) return
        safeHook("dimenOverrides") { resourceHooks() }
        safeHook("pluginLoader") {
            val factory = findClassAny(cl, "com.android.systemui.shared.plugins.PluginInstance\$PluginFactory")
            after(findMethodByName(factory, "createPluginContext")) { _, result ->
                val ctx = result as? Context
                if (ctx != null && ctx.packageName == PLUGIN && !pluginHooked) {
                    pluginHooked = true
                    installPluginHooks(ctx)
                }
                result
            }
        }
    }

    private fun px(ctx: Context, dp: Int) = dp * ctx.resources.displayMetrics.density

    /** Global, cheap id lookup: only ids registered in [dimenOverrides] are changed. */
    private fun resourceHooks() {
        val res = Resources::class.java
        for (name in listOf("getDimensionPixelSize", "getDimensionPixelOffset")) {
            after(res.getDeclaredMethod(name, Int::class.javaPrimitiveType)) { chain, result ->
                if (dimenOverrides.isEmpty()) return@after result
                dimenOverrides[chain.args[0] as Int]?.toInt() ?: result
            }
        }
        after(res.getDeclaredMethod("getDimension", Int::class.javaPrimitiveType)) { chain, result ->
            if (dimenOverrides.isEmpty()) return@after result
            dimenOverrides[chain.args[0] as Int] ?: result
        }
    }

    private fun overrideDimen(ctx: Context, name: String, dp: Int) {
        val id = ctx.resources.getIdentifier(name, "dimen", PLUGIN)
        if (id != 0) dimenOverrides[id] = px(ctx, dp) else warn("dimen $name not found")
    }

    private fun installPluginHooks(ctx: Context) {
        val cl = ctx.classLoader
        info("SystemUI plugin loaded, installing control center / volume hooks")

        if (volPanel > 0) {
            listOf("miui_volume_bg_radius", "miui_volume_bg_radius_expanded", "miui_volume_blur_bg_radius")
                .forEach { overrideDimen(ctx, it, volPanel) }
        }
        if (ccCard > 0) overrideDimen(ctx, "control_center_universal_corner_radius", ccCard)

        if (volSlider > 0) safeHook("volumeSlider") {
            val cls = findClassAny(cl, "com.android.systemui.miui.volume.MiuiVolumeSeekBarProgressView")
            val radius = px(ctx, volSlider)
            cls.declaredConstructors.forEach { ctor ->
                after(ctor) { chain, result ->
                    chain.thisObject?.let { setFieldNumber(it, "mProgressRadius", radius) }
                    result
                }
            }
        }
        if (volBlur) safeHook("volumeBlur") {
            returnConstant(cl, listOf("com.android.systemui.miui.volume.Util"), "isSupportBlurS", true)
        }

        if (ccTile > 0) safeHook("ccTile") {
            val cls = findClassAny(cl, "miui.systemui.controlcenter.qs.tileview.QSTileItemIconView")
            val radius = px(ctx, ccTile)
            val getter = XposedHelpers.findMethodExact(cls, "getCornerRadius")
            hook(getter) { chain -> coerce(radius, getter.returnType) ?: chain.proceed() }
            for (setter in listOf("setEnabledBg", "setDisabledBg")) {
                XposedHelpers.findMethodExactIfExists(cls, setter, Drawable::class.java)?.let {
                    replaceFirstArg(it) { d -> (d as? GradientDrawable)?.mutate()?.also { g -> (g as GradientDrawable).cornerRadius = radius } ?: d }
                }
            }
        }
        if (ccCard > 0) safeHook("ccCard") {
            val cls = findClassAny(cl, "miui.systemui.controlcenter.qs.tileview.QSCardItemView")
            val radius = px(ctx, ccCard)
            replaceFirstArg(findMethodLoose(cls, "setCornerRadius", Float::class.java)) { radius }
        }
        // The blur ratio drives both the blur and the dimming behind the control center, so
        // scaling it makes the background see-through. 0 stays 0, so closing is unaffected.
        if (ccBackground < 100) safeHook("ccBackground") {
            val cls = findClassAny(cl, "miui.systemui.controlcenter.windowview.ControlCenterWindowViewController")
            val factor = ccBackground / 100f
            replaceFirstArg(findMethodLoose(cls, "setBlurRatio", Float::class.java)) { (it as? Float)?.times(factor) ?: it }
        }
        if (ccSlider > 0) safeHook("ccSlider") {
            val cls = findClassAny(cl, "miui.systemui.controlcenter.panel.main.recyclerview.ToggleSliderViewHolder")
            val radius = px(ctx, ccSlider)
            // The outline is the slider's outer shape. progressRadius alone only rounds the
            // level fill inside it, which is what users saw changing.
            val setters = listOf("setOutlineRadius", "setProgressRadius")
                .mapNotNull { XposedHelpers.findMethodExactIfExists(cls, it, Float::class.java) }
            if (setters.isEmpty()) throw NoSuchMethodException("${cls.name}#setOutlineRadius")
            after(findMethodByName(cls, "updateSize")) { chain, result ->
                chain.thisObject?.let { holder -> setters.forEach { it.invoke(holder, radius) } }
                result
            }
        }
    }
}
