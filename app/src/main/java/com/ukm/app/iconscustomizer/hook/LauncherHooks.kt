package com.ukm.app.iconscustomizer.hook

import android.app.Application
import android.app.Instrumentation
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Process
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.GridView
import androidx.core.graphics.createBitmap
import com.ukm.app.iconscustomizer.BuildConfig
import com.ukm.app.iconscustomizer.Constants
import com.ukm.app.iconscustomizer.IconNormalizer
import com.ukm.app.iconscustomizer.IconPackHelper
import com.ukm.app.iconscustomizer.Materials
import com.ukm.app.iconscustomizer.Prefs
import com.ukm.app.iconscustomizer.XposedHelpers
import io.github.libxposed.api.XposedModule
import org.w3c.dom.Element
import java.util.WeakHashMap

/** Hooks for the HyperOS System Launcher (com.miui.home) and the POCO Launcher. */
class LauncherHooks(module: XposedModule, prefs: SharedPreferences?, pkg: String) :
    HookModule(module, prefs, pkg) {

    private var context: Context? = null
    private var deepShortcutClass: Class<*>? = null

    private val iconPack = string(Prefs.ICON_PACK)
    private val packChain = IconPackHelper.packChain(iconPack, string(Prefs.ICON_PACK_FALLBACKS))
    private val iconPackActive = bool(Prefs.THEMED_ICONS) && iconPack != "none"

    @Volatile private var launcherColorCallbackSeen = false
    @Volatile private var dockColorCallbackSeen = false

    private val deviceConfigs = listOf(
        "com.miui.home.common.device.DeviceConfigs",
        "com.miui.home.launcher.DeviceConfig"
    )
    private val deviceLevelUtils = listOf(
        "com.miui.home.launcher.common.DeviceLevelUtils",
        "com.miui.home.common.device.DeviceLevelUtils"
    )
    private val blurUtils = listOf("com.miui.home.launcher.common.BlurUtils")

    fun install(cl: ClassLoader) {
        safeHook("launcherContext") { contextHook() }
        deepShortcutClass = findClassOrNull(cl, "com.miui.home.launcher.DeepShortcutInfo")

        // Icons
        if (int(Prefs.ICON_SCALE) != 100) safeHook("iconScale") { iconScaleHook(cl) }
        if (bool(Prefs.HIDE_SHADOWS)) safeHook("hideShadows") {
            returnConstant(cl, listOf("com.miui.home.launcher.ShortcutIcon"), "getIsHideShadow", true)
        }
        if (iconPackActive) {
            safeHook("iconPack") { iconPackHook(cl) }
            safeHook("defaultIconFlag") { defaultIconFlagHook(cl) }
            safeHook("shortcutIcons") { shortcutIconHook(cl) }
        }
        if (bool(Prefs.BADGE_BLUR)) safeHook("badgeBlur") { badgeBlurHooks(cl) }

        // Labels
        safeHook("appLabels") { appLabelsHook(cl) }
        if (bool(Prefs.HIDE_LABELS)) safeHook("hideLabels") {
            returnConstant(cl, listOf("com.miui.home.launcher.ShortcutIcon"), "getIsHideTitle", true)
            returnConstant(cl, deviceConfigs, "isHideAppTitle", true)
            // HyperOS 2: ShortcutIcon reads its mIsHideTitle field directly, so the getter alone
            // does nothing. Use the launcher's native "no word" (hide app names) mode instead.
            returnConstant(cl, listOf("com.miui.home.launcher.common.Utilities"), "isNoWordModel", true)
        }
        if (bool(Prefs.HIDE_DOCK_LABELS)) safeHook("hideDockLabels") {
            returnConstant(cl, deviceConfigs, "isHotseatsAppTitleHided", true)
        }
        if (int(Prefs.LABEL_SIZE) > 0) safeHook("labelSize") {
            returnConstant(cl, deviceConfigs, "getIconTitleTextSize", int(Prefs.LABEL_SIZE))
        }

        // Dock & folders
        if (bool(Prefs.DOCK)) dockHooks(cl)
        if (bool(Prefs.DOCK_UNLIMITED)) safeHook("dockUnlimited") {
            returnConstant(cl, deviceConfigs, "getHotseatMaxCount", 99)
        }
        if (bool(Prefs.FOLDER)) safeHook("folderStyle") { folderStyleHook(cl) }
        if (int(Prefs.FOLDER_COLUMNS) > 0) folderColumnsHooks(cl)

        // Widgets
        if (bool(Prefs.CLOCK)) safeHook("clock") { clockHook(cl) }

        // Recents & blur
        if (int(Prefs.RECENTS_RADIUS) > 0) safeHook("recentsRadius") { recentsRadiusHook(cl) }
        if (bool(Prefs.RECENTS_BLUR)) safeHook("recentsBlur") {
            returnConstant(cl, blurUtils, "isUseCompleteRecentsBlurAnimation", true)
            returnConstant(cl, blurUtils, "isUseNoRecentsBlurAnimation", false)
        }
        if (bool(Prefs.RECENTS_HIDE_STATUSBAR)) safeHook("recentsStatusBar") {
            returnConstant(
                cl,
                listOf("com.miui.home.launcher.common.StatusBarUtils") + deviceLevelUtils,
                "isHideStatusBarWhenEnterRecents",
                true
            )
            returnConstant(cl, deviceConfigs, "keepStatusBarShowingForBetterPerformance", false)
        }
        if (int(Prefs.BLUR_SCALE) != 100) safeHook("blurScale") { blurScaleHook(cl) }

        // Animations
        if (int(Prefs.APP_ANIM) != 100) safeHook("appAnim") { appAnimHook(cl) }
        if (int(Prefs.RECENTS_ANIM) != 100) safeHook("recentsAnim") {
            returnConstant(cl, deviceLevelUtils, "getDeviceLevelTransitionAnimRatio", int(Prefs.RECENTS_ANIM) / 100f)
        }
        if (bool(Prefs.FULL_ANIM)) safeHook("fullAnim") {
            returnConstant(cl, deviceConfigs, "isSupportCompleteAnimation", true)
            returnConstant(cl, deviceConfigs, "isSupportFolderAnimation", true)
            returnConstant(cl, deviceConfigs, "isSupportRecentsAnimation", true)
            returnConstant(cl, deviceLevelUtils, "isUseSimpleAnim", false)
        }
        if (bool(Prefs.HIGH_END_ANIM)) safeHook("highEnd") {
            returnConstant(cl, deviceLevelUtils, "supportSetAnimationRate", true)
            returnConstant(cl, deviceLevelUtils, "isHighLevelDevice", true)
            returnConstant(cl, deviceLevelUtils, "isLowLevelDevice", false)
            // HyperOS 2 names
            returnConstant(cl, deviceLevelUtils, "isHighLevelDeviceFromFolme", true)
            returnConstant(cl, deviceLevelUtils, "isLowLevelDeviceFromFolme", false)
        }

        safeHook("wallpaperColor") { wallpaperColorChangedHook(cl) }
    }

    private fun ctx(view: View? = null): Context? = context ?: view?.context

    private fun dp(value: Int, c: Context) = value * c.resources.displayMetrics.density

    private companion object {
        const val BADGE_FILL_ALPHA = 150
        const val BADGE_BLUR_FACTOR = 6
    }

    // ---------------------------------------------------------------- context & reload

    /** Framework entry point, independent of the launcher's own Application class name. */
    private fun contextHook() {
        val method = Instrumentation::class.java.getDeclaredMethod("callApplicationOnCreate", Application::class.java)
        after(method) { chain, result ->
            val app = chain.args[0] as? Application
            if (context == null && app != null && app.packageName == target) {
                context = app
                try {
                    app.registerReceiver(object : BroadcastReceiver() {
                        override fun onReceive(c: Context, intent: Intent) {
                            when (intent.action) {
                                Constants.ACTION_RELOAD -> {
                                    Log.i(Constants.TAG, "Reload broadcast received, restarting $target")
                                    Process.killProcess(Process.myPid())
                                }
                                Constants.ACTION_LIST_SHORTCUTS -> sendPinnedShortcuts(c)
                            }
                        }
                    }, IntentFilter().apply {
                        addAction(Constants.ACTION_RELOAD)
                        addAction(Constants.ACTION_LIST_SHORTCUTS)
                    }, Context.RECEIVER_EXPORTED)
                } catch (e: Exception) {
                    Log.e(Constants.TAG, "registerReceiver", e)
                }
            }
            result
        }
    }

    /** Answers to this module only, whoever asked, so other apps can't read the list. */
    private fun sendPinnedShortcuts(c: Context) {
        try {
            val query = LauncherApps.ShortcutQuery().setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
            val pinned = c.getSystemService(LauncherApps::class.java)
                .getShortcuts(query, Process.myUserHandle()).orEmpty()
            val entries = ArrayList(pinned.map { "${it.`package`}\t${it.id}\t${it.shortLabel}" })
            c.sendBroadcast(
                Intent(Constants.ACTION_SHORTCUTS_RESULT)
                    .setPackage(BuildConfig.APPLICATION_ID)
                    .putStringArrayListExtra(Constants.EXTRA_SHORTCUTS, entries)
            )
        } catch (t: Throwable) {
            warn("sendPinnedShortcuts: $t")
        }
    }

    // ---------------------------------------------------------------- icons

    /**
     * Scales the launcher's own icon size instead of forcing an absolute pixel value, so the
     * grid, label and icon stay centred relative to each other on every screen density.
     */
    private fun iconScaleHook(cl: ClassLoader) {
        val scale = int(Prefs.ICON_SCALE) / 100f
        val cls = findClassAny(
            cl,
            "com.miui.home.common.gridconfig.GridConfig\$IconConfig",
            "com.miui.home.launcher.gridconfig.GridConfig\$IconConfig",
            "com.miui.home.launcher.GridConfig\$IconConfig" // HyperOS 2
        )
        after(XposedHelpers.findMethodExact(cls, "getIconSize")) { _, result ->
            (result as? Int)?.let { (it * scale).toInt() } ?: result
        }
    }

    private fun defaultIconFlagHook(cl: ClassLoader) {
        val cls = findClassAny(cl, *deviceConfigs.toTypedArray())
        after(findMethodLoose(cls, "updateIsDefaultIcon", Context::class.java)) { _, result ->
            XposedHelpers.setStaticBooleanField(cls, "sIsDefaultIcon", false)
            result
        }
    }

    /**
     * Pinned shortcuts skip DrawableInfo#applyTo: their icon comes straight from the launcher
     * database or from the owning app, and every path ends in this setter.
     */
    private fun shortcutIconHook(cl: ClassLoader) {
        val deep = deepShortcutClass ?: return
        val itemInfo = findClassAny(cl, "com.miui.home.launcher.ItemInfoWithIconAndMessage")
        hook(findMethodLoose(itemInfo, "setIconDrawable", Drawable::class.java)) { chain ->
            val self = chain.thisObject
            val original = chain.args[0] as? Drawable
            if (self == null || original == null || !deep.isInstance(self)) return@hook chain.proceed()
            val custom = try {
                shortcutIcon(self, original)
            } catch (e: Exception) {
                Log.e(Constants.TAG, "shortcutIcon", e)
                null
            }
            if (custom == null) chain.proceed() else chain.proceed(arrayOf<Any?>(custom))
        }
    }

    private fun shortcutIcon(item: Any, original: Drawable): Drawable? {
        val c = context ?: return null
        val key = itemKey(item) ?: return null
        val manual = prefs?.getString("custom_icon_${iconPack}_$key", null) ?: return null
        val refs = IconPackHelper.candidates(c, packChain, "", "", manual, overrideOnly = true)
        val drawable = IconPackHelper.loadFirst(c, refs) ?: return null
        return normalized(recolor(drawable)).apply { bounds = original.bounds }
    }

    private fun iconPackHook(cl: ClassLoader) {
        // HyperOS 3: model.core / model.api — HyperOS 2: launcher.graphics / launcher
        val drawableInfo = findClassAny(
            cl,
            "com.miui.home.model.core.DrawableInfo",
            "com.miui.home.launcher.graphics.DrawableInfo"
        )
        val itemInfoClass = findClassOrNull(
            cl,
            "com.miui.home.model.api.ItemInfoWithIconAndMessage",
            "com.miui.home.launcher.ItemInfoWithIconAndMessage"
        )
        val applyTo = findMethodLoose(drawableInfo, "applyTo", itemInfoClass ?: Any::class.java)
        info("iconPack: hooking ${applyTo.declaringClass.name}#${applyTo.name}(${applyTo.parameterTypes.joinToString { it.simpleName }})")
        val homeOnly = bool(Prefs.HOMESCREEN_ONLY)

        hook(applyTo) { chain ->
            try {
                val itemInfo = chain.args[0] ?: return@hook chain.proceed()
                val iconType = iconTypeOf(itemInfo)
                val isHomeItem = itemInfo.javaClass.name == "com.miui.home.launcher.ShortcutInfo"
                // 8 = "big icon" (large app icon tile) on both HyperOS 2 and 3
                if ((homeOnly && !isHomeItem) || iconType == 8) return@hook chain.proceed()

                val original = XposedHelpers.getObjectField(chain.thisObject, "icon") as? Drawable
                    ?: return@hook chain.proceed()
                val component = componentOf(itemInfo) ?: return@hook chain.proceed()
                val custom = customIcon(component, original)
                val mask = XposedHelpers.getObjectField(chain.thisObject, "enableIconMask")
                try {
                    XposedHelpers.findMethodExact(itemInfo.javaClass, "setIconDrawable", Drawable::class.java)
                        .invoke(itemInfo, custom)
                    findMethodLoose(itemInfo.javaClass, "setEnableIconMask", Int::class.java).invoke(itemInfo, mask)
                } catch (_: Exception) {
                    return@hook chain.proceed()
                }
                null
            } catch (e: Exception) {
                Log.e(Constants.TAG, "iconPackHook", e)
                chain.proceed()
            }
        }
    }

    /** HyperOS 3 exposes getIconType(); HyperOS 2 only has the public field mIconType. */
    private fun iconTypeOf(itemInfo: Any): Int? {
        try {
            findMethodByName(itemInfo.javaClass, "getIconType", 0).invoke(itemInfo)?.let { return it as? Int }
        } catch (_: Throwable) {
        }
        return try {
            XposedHelpers.findField(itemInfo.javaClass, "mIconType").getInt(itemInfo)
        } catch (_: Throwable) {
            null
        }
    }

    /** HyperOS 3: getComponentInfo(); HyperOS 2: getTargetComponent() / getComponentName(). */
    private fun componentOf(itemInfo: Any): ComponentName? {
        for (name in arrayOf("getComponentInfo", "getTargetComponent", "getComponentName")) {
            try {
                val value = findMethodByName(itemInfo.javaClass, name, 0).invoke(itemInfo)
                if (value is ComponentName) return value
            } catch (_: Throwable) {
            }
        }
        return null
    }

    private fun customIcon(component: ComponentName, original: Drawable): Drawable {
        val c = context ?: return original
        val exact = "ComponentInfo{${component.packageName}/${component.className}}"
        val manual = try { prefs?.getString("custom_icon_${iconPack}_$exact", null) } catch (_: Exception) { null }
        val refs = IconPackHelper.candidates(c, packChain, component.packageName, component.className, manual)
        val drawable = IconPackHelper.loadFirst(c, refs) ?: return original
        // Bounds go last: recoloring and normalizing both return a new drawable.
        return normalized(recolor(drawable)).apply { bounds = original.bounds }
    }

    private fun normalized(icon: Drawable): Drawable {
        val c = context
        if (!bool(Prefs.ICON_NORMALIZE) || c == null) return icon
        return try {
            IconNormalizer.normalize(c, icon, int(Prefs.ICON_NORMALIZE_SIZE))
        } catch (e: Exception) {
            Log.e(Constants.TAG, "normalize", e)
            icon
        }
    }

    private fun recolor(icon: Drawable): Drawable {
        val c = context
        if (!bool(Prefs.MONET_ICONS) || c == null) return icon
        return try {
            val bg = int(Prefs.MONET_BG).takeIf { it != 0 }?.let { Constants.resolveColor(c, it) }
            val fg = int(Prefs.MONET_FG).takeIf { it != 0 }?.let { Constants.resolveColor(c, it) }
            IconPackHelper.putColorIntoDrawable(c, icon, bg, fg) ?: icon
        } catch (e: Exception) {
            Log.e(Constants.TAG, "recolor", e)
            icon
        }
    }

    // ---------------------------------------------------------------- labels

    /**
     * Renames at the view rather than on ShortcutInfo: its title getter also feeds the
     * launcher database, so a rename there would persist after the module is disabled.
     * Folders are a different view class, so they are never affected.
     */
    private fun appLabelsHook(cl: ClassLoader) {
        val labels = (prefs?.all ?: emptyMap<String, Any?>())
            .filterKeys { it.startsWith(Prefs.CUSTOM_LABEL_PREFIX) }
        if (labels.isEmpty()) return

        fun labelFor(icon: Any?): String? {
            val info = (icon as? View)?.tag ?: return null
            val key = itemKey(info) ?: return null
            return labels[Prefs.labelKey(key)] as? String
        }

        val shortcutIcon = findClassAny(cl, "com.miui.home.launcher.ShortcutIcon")
        val setTitle = findMethodLoose(shortcutIcon, "setTitle", CharSequence::class.java)
        hook(setTitle) { chain ->
            val label = labelFor(chain.thisObject) ?: return@hook chain.proceed()
            chain.proceed(arrayOf<Any?>(label))
        }
        // The title can arrive before the item it belongs to, so apply it again once the tag is set.
        after(findMethodByName(shortcutIcon, "setTag", 1)) { chain, result ->
            labelFor(chain.thisObject)?.let { setTitle.invoke(chain.thisObject, it) }
            result
        }
    }

    /** The key an item's custom icon and name are stored under, or null if it can't have them. */
    private fun itemKey(itemInfo: Any): String? {
        val component = componentOf(itemInfo) ?: return null
        if (deepShortcutClass?.isInstance(itemInfo) == true) {
            val id = findMethodByName(itemInfo.javaClass, "getDeepShortcutId", 0).invoke(itemInfo) as? String
            return id?.let { Prefs.shortcutKey(component.packageName, it) }
        }
        if (!isAppItem(itemInfo)) return null
        return "ComponentInfo{${component.packageName}/${component.className}}"
    }

    /** itemType 0 is an app, as opposed to widgets and other home screen items. */
    private fun isAppItem(itemInfo: Any): Boolean = try {
        XposedHelpers.findField(itemInfo.javaClass, "itemType").getInt(itemInfo) == 0
    } catch (_: Throwable) {
        false
    }

    // ---------------------------------------------------------------- badges

    private var badgeHost: View? = null
    private var capturingIcon = false
    private val frostCache = WeakHashMap<Bitmap, Bitmap?>()
    private val frostPaint = Paint(Paint.FILTER_BITMAP_FLAG)

    /**
     * Frosted glass for notification badges. Window blur can't do this: it only reaches what
     * is behind the launcher window, so it would blur the wallpaper and never the icon under
     * the badge. Instead the icon is captured, softened by downscaling, and drawn under a
     * translucent badge. Whatever fails, the badge is still drawn.
     */
    private fun badgeBlurHooks(cl: ClassLoader) {
        val render = findClassAny(cl, "com.miui.home.launcher.IconMessageRender")
        val params = findClassAny(cl, "com.miui.home.launcher.IconMessageRender\$DrawParams")

        hook(findMethodByName(params, "createMessageBitmap", 1)) { chain ->
            (XposedHelpers.getObjectField(chain.thisObject, "mCirclePaint") as? Paint)?.alpha = BADGE_FILL_ALPHA
            chain.proceed()
        }

        // The renderer doesn't know its view, and the frost needs it, so remember it for the call.
        for (name in arrayOf("com.miui.home.launcher.ShortcutIcon", "com.miui.home.launcher.ItemIcon")) {
            safeHook("badgeHost") {
                val icon = findClassAny(cl, name)
                hook(findMethodByName(icon, "drawMessageIfNecessary", 1)) { chain ->
                    if (capturingIcon) return@hook null
                    badgeHost = chain.thisObject as? View
                    try {
                        chain.proceed()
                    } finally {
                        badgeHost = null
                    }
                }
            }
        }

        hook(findMethodByName(render, "drawIconMessageBitmap", 4)) { chain ->
            val host = badgeHost
            if (host != null) {
                try {
                    val canvas = chain.args[0] as Canvas
                    drawFrost(canvas, host, chain.args[1]!!, chain.args[2] as Float, chain.args[3] as Float)
                } catch (t: Throwable) {
                    warn("drawFrost: $t")
                }
            }
            chain.proceed()
        }
    }

    private fun drawFrost(canvas: Canvas, host: View, params: Any, x: Float, y: Float) {
        val message = XposedHelpers.getObjectField(params, "messageBitmap") as? Bitmap ?: return
        if (!frostCache.containsKey(message)) frostCache[message] = buildFrost(host, params, message, x, y)
        val frost = frostCache[message] ?: return
        val scale = XposedHelpers.findField(params.javaClass, "scale").getFloat(params)
        frostPaint.alpha = (XposedHelpers.findField(params.javaClass, "alpha").getFloat(params) * 255).toInt()
        canvas.save()
        canvas.translate(x, y)
        canvas.scale(scale, scale)
        canvas.drawBitmap(frost, -frost.width / 2f, -frost.height / 2f, frostPaint)
        canvas.restore()
    }

    /** Null means "can't be captured", which is cached too so a failure isn't retried every frame. */
    private fun buildFrost(host: View, params: Any, message: Bitmap, x: Float, y: Float): Bitmap? = try {
        val width = message.width
        val height = message.height
        val small = createBitmap((width / BADGE_BLUR_FACTOR).coerceAtLeast(1), (height / BADGE_BLUR_FACTOR).coerceAtLeast(1))
        val capture = Canvas(small)
        capture.scale(small.width.toFloat() / width, small.height.toFloat() / height)
        capture.translate(width / 2f - x, height / 2f - y)
        capturingIcon = true
        try {
            host.draw(capture)
        } finally {
            capturingIcon = false
        }
        val frost = createBitmap(width, height)
        val canvas = Canvas(frost)
        (findMethodByName(params.javaClass, "createCirclePatch", 0).invoke(params) as? Path)?.let(canvas::clipPath)
        canvas.drawBitmap(small, null, RectF(0f, 0f, width.toFloat(), height.toFloat()), Paint(Paint.FILTER_BITMAP_FLAG))
        frost
    } catch (t: Throwable) {
        warn("buildFrost: $t")
        null
    }

    // ---------------------------------------------------------------- dock

    private fun dockSpec(c: Context) = Materials.Spec(
        style = string(Prefs.DOCK_STYLE),
        color = Constants.resolveColor(c, int(Prefs.DOCK_COLOR)),
        alphaPercent = int(Prefs.DOCK_ALPHA),
        cornerPx = dp(int(Prefs.DOCK_RADIUS), c),
        strokePx = dp(int(Prefs.DOCK_STROKE), c).toInt(),
        blurRadius = dp(int(Prefs.DOCK_BLUR), c).toInt(),
    )

    private fun dockHooks(cl: ClassLoader) {
        val widthPercent = int(Prefs.DOCK_WIDTH)
        val paddingDp = int(Prefs.DOCK_PADDING)

        safeHook("dockMeasure") {
            val hotSeats = findClassAny(cl, "com.miui.home.launcher.hotseats.HotSeats", "com.miui.home.launcher.HotSeats")
            after(XposedHelpers.findMethodExact(hotSeats, "onMeasure", Int::class.java, Int::class.java)) { chain, result ->
                val view = chain.thisObject as? View ?: return@after result
                val metrics = view.resources.displayMetrics
                // Symmetric margins keep the dock centred; only change them when needed to avoid relayout loops.
                val side = maxOf((8 * metrics.density).toInt(), (metrics.widthPixels * (100 - widthPercent) / 200f).toInt())
                (view.layoutParams as? ViewGroup.MarginLayoutParams)?.let { lp ->
                    if (lp.leftMargin != side || lp.rightMargin != side) {
                        lp.leftMargin = side
                        lp.rightMargin = side
                        view.layoutParams = lp
                    }
                }
                val pad = (paddingDp * metrics.density).toInt()
                if (view.paddingTop != pad || view.paddingBottom != pad) {
                    view.setPadding(view.paddingLeft, pad, view.paddingRight, pad)
                }
                result
            }
        }

        val screen = findClassOrNull(
            cl,
            "com.miui.home.launcher.hotseats.HotSeatsScreenViewContent",
            "com.miui.home.launcher.HotSeatsScreenViewContent"
        ) ?: return warn("Dock background unavailable on $target")

        fun style(view: View?) {
            view ?: return
            val c = ctx(view) ?: return
            Materials.apply(view, c, dockSpec(c))
        }

        safeHook("dockInflate") {
            after(XposedHelpers.findMethodExact(screen, "onFinishInflate")) { chain, result ->
                style(chain.thisObject as? View)
                result
            }
        }
        safeHook("dockWallpaperColor") {
            after(XposedHelpers.findMethodExact(screen, "onWallpaperColorChanged")) { chain, result ->
                if (dockColorCallbackSeen) style(chain.thisObject as? View)
                dockColorCallbackSeen = true
                result
            }
        }
    }

    // ---------------------------------------------------------------- folders

    /**
     * Folder backgrounds are returned as plain drawables (not fixed-size bitmaps) so they scale
     * to whatever bounds the launcher uses and corners never stretch or skew.
     */
    private fun folderStyleHook(cl: ClassLoader) {
        val cls = findClassAny(cl, "com.miui.home.model.core.IconCache", "com.miui.home.launcher.IconCache")
        val ids = setOf(4097, 4098, 4099, 4100, 4104)
        hook(XposedHelpers.findMethodExact(cls, "getDrawable", Int::class.java)) { chain ->
            val id = chain.args[0] as? Int
            val c = context
            if (c == null || id !in ids) return@hook chain.proceed()
            Materials.build(
                c,
                Materials.Spec(
                    style = string(Prefs.FOLDER_STYLE).let { if (it == "frosted") "glass" else it },
                    color = Constants.resolveColor(c, int(Prefs.FOLDER_COLOR)),
                    alphaPercent = int(Prefs.FOLDER_ALPHA),
                    cornerPx = dp(int(Prefs.FOLDER_RADIUS), c),
                    strokePx = dp(int(Prefs.FOLDER_STROKE), c).toInt(),
                )
            )
        }
    }

    private fun folderColumnsHooks(cl: ClassLoader) {
        val columns = int(Prefs.FOLDER_COLUMNS)
        safeHook("folderBind") {
            val folder = findClassAny(cl, "com.miui.home.folder.FolderView", "com.miui.home.launcher.Folder")
            after(findMethodByName(folder, "bind")) { chain, result ->
                (XposedHelpers.getObjectField(chain.thisObject, "mContent") as? GridView)?.numColumns = columns
                result
            }
        }
        safeHook("folderAnim") {
            val anim = findClassAny(
                cl,
                "com.miui.home.folder.FolderAnimController",
                "com.miui.home.launcher.folder.FolderAnimController" // HyperOS 2
            )
            after(findMethodByName(anim, "setupView")) { chain, result ->
                val self = chain.thisObject ?: return@after result
                val grid = XposedHelpers.getObjectField(self, "mAnimaFolderGridView") ?: return@after result
                val rows = (XposedHelpers.callMethod(grid, "getMaxRow") as? Int) ?: 3
                setFieldNumber(self, "DISPLAY_COUNT_MAX", rows * columns)
                setFieldNumber(self, "mFolderColumnCount", columns)
                result
            }
        }
    }

    // ---------------------------------------------------------------- recents, blur, animations

    private fun recentsRadiusHook(cl: ClassLoader) {
        val cls = findClassAny(cl, "com.miui.home.recents.util.WindowCornerRadiusUtil")
        val m = XposedHelpers.findMethodExact(cls, "getTaskViewCornerRadius")
        hook(m) { chain ->
            val c = context ?: return@hook chain.proceed()
            coerce(dp(int(Prefs.RECENTS_RADIUS), c), m.returnType) ?: chain.proceed()
        }
    }

    private fun blurScaleHook(cl: ClassLoader) {
        val scale = int(Prefs.BLUR_SCALE) / 100.0
        val cls = findClassAny(cl, *blurUtils.toTypedArray())
        val m = findMethodByName(cls, "fastBlur", 4)
        replaceFirstArg(m) { v ->
            when (v) {
                is Float -> (v * scale).toFloat()
                is Double -> v * scale
                is Int -> (v * scale).toInt()
                else -> v
            }
        }
    }

    /** Scales the spring response of the app open/close (icon <-> window) animation. */
    private fun appAnimHook(cl: ClassLoader) {
        val scale = int(Prefs.APP_ANIM) / 100f
        val cls = findClassAny(cl, "com.miui.home.recents.util.RectFSpringAnim")
        replaceFirstArg(findMethodLoose(cls, "getModifyResponse", Float::class.java)) { v ->
            (v as? Float)?.times(scale) ?: v
        }
    }

    // ---------------------------------------------------------------- clock & wallpaper

    private fun clockHook(cl: ClassLoader) {
        val factory = XposedHelpers.findClass("com.miui.maml.elements.ScreenElementFactory", cl)
        val root = XposedHelpers.findClass("com.miui.maml.ScreenElementRoot", cl)
        val colorRes = int(Prefs.CLOCK_COLOR)
        hook(XposedHelpers.findMethodExact(factory, "createInstance", Element::class.java, root)) { chain ->
            val el = chain.args[0] as? Element
            val c = context
            if (el != null && c != null && el.tagName in setOf("DateTime", "Time", "Text")) {
                try {
                    val color = if (colorRes != 0) Constants.resolveColor(c, colorRes)
                    else c.getColor(android.R.color.system_accent1_200)
                    el.removeAttribute("colorExp")
                    el.removeAttribute("colorLight")
                    el.removeAttribute("colorDark")
                    el.setAttribute("color", String.format("#%08X", color))
                } catch (e: Exception) {
                    Log.e(Constants.TAG, "clockHook", e)
                }
            }
            chain.proceed()
        }
    }

    private fun wallpaperColorChangedHook(cl: ClassLoader) {
        val cls = findClassAny(cl, "com.miui.home.launcher.BaseLauncher", "com.miui.home.launcher.Launcher")
        after(XposedHelpers.findMethodExact(cls, "onWallpaperColorChanged")) { chain, result ->
            if (!launcherColorCallbackSeen) {
                launcherColorCallbackSeen = true
                return@after result
            }
            val launcher = chain.thisObject ?: return@after result
            if (iconPackActive) {
                try { XposedHelpers.callMethod(launcher, "refreshAllAppsIcon") } catch (t: Throwable) { warn("refreshAllAppsIcon: $t") }
            }
            if (bool(Prefs.CLOCK)) {
                refreshMaml(launcher)
                refreshGadgets(launcher)
            }
            result
        }
    }

    private fun refreshGadgets(launcher: Any) {
        try {
            (XposedHelpers.getObjectField(launcher, "mGadgets") as? ArrayList<*>)?.forEach { gadget ->
                val info = gadget?.let { XposedHelpers.callMethod(it, "getTag") } ?: return@forEach
                val id = XposedHelpers.getObjectField(info, "mGadgetId") as? Int ?: return@forEach
                try {
                    XposedHelpers.callMethod(launcher, "reloadGadget", arrayOf(Int::class.java), id)
                } catch (e: Exception) {
                    Log.e(Constants.TAG, "reloadGadget", e)
                }
            }
        } catch (t: Throwable) {
            warn("refreshGadgets: $t")
        }
    }

    private fun refreshMaml(launcher: Any) {
        try {
            try {
                val utils = XposedHelpers.findClass("com.miui.launcher.utils.MamlUtils", launcher.javaClass.classLoader)
                XposedHelpers.callStaticMethod(utils, "clearMamlCache")
            } catch (_: Throwable) {
            }
            (XposedHelpers.getObjectField(launcher, "mMaMlViews") as? ArrayList<*>)?.forEach { widget ->
                widget ?: return@forEach
                try {
                    XposedHelpers.setBooleanField(widget, "mThemeApplied", false)
                    XposedHelpers.callMethod(widget, "onUpgrade", arrayOf(Int::class.java, Int::class.java), 0, 0)
                } catch (e: Exception) {
                    Log.e(Constants.TAG, "MAML rebuild", e)
                }
            }
        } catch (t: Throwable) {
            warn("refreshMaml: $t")
        }
    }
}
