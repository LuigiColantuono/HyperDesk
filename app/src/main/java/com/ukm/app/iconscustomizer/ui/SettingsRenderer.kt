package com.ukm.app.iconscustomizer.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.color.MaterialColors
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.slider.Slider
import com.ukm.app.iconscustomizer.Constants
import com.ukm.app.iconscustomizer.R
import com.google.android.material.R as MR

/** Category accent colours, harmonized with the current (dynamic) colour scheme. */
class Accent(val container: Int, val onContainer: Int, val accent: Int) {
    companion object {
        fun of(ctx: Context, hue: Int?): Accent {
            if (hue == null) {
                return Accent(
                    MaterialColors.getColor(ctx, MR.attr.colorSecondaryContainer, Color.LTGRAY),
                    MaterialColors.getColor(ctx, MR.attr.colorOnSecondaryContainer, Color.BLACK),
                    MaterialColors.getColor(ctx, MR.attr.colorSecondary, Color.DKGRAY),
                )
            }
            val roles = MaterialColors.getColorRoles(ctx, MaterialColors.harmonizeWithPrimary(ctx, hue))
            return Accent(roles.accentContainer, roles.onAccentContainer, roles.accent)
        }
    }
}

/** Builds Material 3 "segmented list" UI for a list of [Section]s and keeps it in sync. */
class SettingsRenderer(
    private val context: Context,
    private val accent: Accent,
    private val host: Host,
) {
    interface Host {
        fun onAction(item: ActionItem)
        fun onColorClick(item: ColorItem)
        fun onChanged(key: String, value: Any?, scope: Scope)
    }

    private class Binding(val item: Item, val view: View, val update: () -> Unit)
    private class SectionView(val header: View, val container: LinearLayout, val bindings: List<Binding>)

    private val inflater = LayoutInflater.from(context)
    private val sections = mutableListOf<SectionView>()
    private val previews = mutableListOf<PreviewView>()

    /** Slider values while dragging, so previews update live before the value is saved. */
    private val live = mutableMapOf<String, Any?>()

    fun valueOf(key: String): Any? = if (live.containsKey(key)) live[key] else PrefStore.get(key)

    private val density = context.resources.displayMetrics.density
    private fun dp(v: Int) = (v * density).toInt()

    fun render(root: LinearLayout, list: List<Section>) {
        for (section in list) {
            val header = inflater.inflate(R.layout.item_section_header, root, false) as TextView
            header.setText(section.title)
            root.addView(header)
            val container = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            root.addView(container, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            val bindings = section.items.map { item -> bind(item, container).also { container.addView(it.view) } }
            sections += SectionView(header, container, bindings)
        }
        refresh()
    }

    fun refresh() {
        for (section in sections) {
            var anyVisible = false
            for (b in section.bindings) {
                val visible = b.item.dependsOn.all { it.test(valueOf(it.key)) }
                b.view.isVisible = visible
                if (visible) {
                    anyVisible = true
                    b.update()
                }
            }
            section.header.isVisible = anyVisible
            applySegments(section.container)
        }
        previews.forEach { it.invalidate() }
    }

    // ------------------------------------------------------------------ segments

    private fun applySegments(container: LinearLayout) {
        val visible = (0 until container.childCount).map { container.getChildAt(it) }.filter { it.isVisible }
        val outer = context.resources.getDimension(R.dimen.segment_corner_outer)
        val inner = context.resources.getDimension(R.dimen.segment_corner_inner)
        val gap = context.resources.getDimensionPixelSize(R.dimen.segment_gap)
        val fill = MaterialColors.getColor(context, MR.attr.colorSurfaceContainer, Color.WHITE)
        val ripple = MaterialColors.getColor(context, androidx.appcompat.R.attr.colorControlHighlight, 0x22000000)
        visible.forEachIndexed { i, v ->
            val top = if (i == 0) outer else inner
            val bottom = if (i == visible.lastIndex) outer else inner
            val shape = ShapeAppearanceModel.builder()
                .setTopLeftCornerSize(top).setTopRightCornerSize(top)
                .setBottomLeftCornerSize(bottom).setBottomRightCornerSize(bottom)
                .build()
            val content = MaterialShapeDrawable(shape).apply { fillColor = ColorStateList.valueOf(fill) }
            val mask = MaterialShapeDrawable(shape).apply { fillColor = ColorStateList.valueOf(Color.WHITE) }
            v.background = RippleDrawable(ColorStateList.valueOf(ripple), content, mask)
            v.clipToOutline = true
            v.updateLayoutParams<LinearLayout.LayoutParams> { topMargin = if (i == 0) 0 else gap }
        }
    }

    // ------------------------------------------------------------------ binding

    private fun bind(item: Item, parent: ViewGroup): Binding = when (item) {
        is SwitchItem -> bindSwitch(item, parent)
        is SliderItem -> bindSlider(item, parent)
        is ChoiceItem -> bindChoice(item, parent)
        is ColorItem -> bindColor(item, parent)
        is ActionItem -> bindAction(item, parent)
        is InfoItem -> bindInfo(item, parent)
        is PreviewItem -> bindPreview(item)
    }

    private fun setupRow(row: View, icon: Int, title: CharSequence, summary: CharSequence?, beta: Boolean = false) {
        row.findViewById<View>(R.id.icon_container).background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(accent.container)
        }
        row.findViewById<ImageView>(R.id.icon).apply {
            setImageResource(icon)
            imageTintList = ColorStateList.valueOf(accent.onContainer)
        }
        row.findViewById<TextView>(R.id.title).text = title
        row.findViewById<TextView>(R.id.summary).apply {
            text = summary
            isVisible = !summary.isNullOrEmpty()
        }
        row.findViewById<View>(R.id.badge).isVisible = beta
    }

    private fun changed(key: String, value: Any?, scope: Scope) {
        PrefStore.put(key, value)
        live.remove(key)
        host.onChanged(key, value, scope)
        refresh()
    }

    private fun bindSwitch(item: SwitchItem, parent: ViewGroup): Binding {
        val row = inflater.inflate(R.layout.item_pref_row, parent, false)
        setupRow(row, item.icon, context.getString(item.title), context.getString(item.summary), item.beta)
        val sw = row.findViewById<MaterialSwitch>(R.id.switch_widget).apply { isVisible = true }
        row.setOnClickListener {
            sw.isChecked = !sw.isChecked
            changed(item.key, sw.isChecked, item.scope)
        }
        return Binding(item, row) { sw.isChecked = valueOf(item.key) == true }
    }

    private fun format(item: SliderItem, value: Int): String {
        if (item.zeroLabel != 0 && value == item.zeroValue) return context.getString(item.zeroLabel)
        if (item.divisor > 1) {
            val v = value.toFloat() / item.divisor
            return String.format("%.2f", v).trimEnd('0').trimEnd('.', ',') + item.unit
        }
        return "$value${item.unit}"
    }

    private fun bindSlider(item: SliderItem, parent: ViewGroup): Binding {
        val view = inflater.inflate(R.layout.item_pref_slider, parent, false)
        val row = view.findViewById<View>(R.id.row)
        setupRow(row, item.icon, context.getString(item.title), item.summary.takeIf { it != 0 }?.let { context.getString(it) }, item.beta)
        row.minimumHeight = 0
        row.setPadding(row.paddingLeft, dp(12), row.paddingRight, dp(2))

        val valueView = TextView(context).apply {
            setTextAppearance(MR.style.TextAppearance_Material3_LabelLarge)
            setTextColor(accent.onContainer)
            setPadding(dp(12), dp(6), dp(12), dp(6))
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(accent.container)
            }
        }
        row.findViewById<ViewGroup>(R.id.trailing).addView(valueView)

        val slider = view.findViewById<Slider>(R.id.slider).apply {
            valueFrom = item.min.toFloat()
            valueTo = item.max.toFloat()
            stepSize = item.step.toFloat()
            trackActiveTintList = ColorStateList.valueOf(accent.accent)
            thumbTintList = ColorStateList.valueOf(accent.accent)
            setLabelFormatter { format(item, it.toInt()) }
        }
        var tracking = false
        slider.addOnChangeListener { _, value, fromUser ->
            valueView.text = format(item, value.toInt())
            if (!fromUser) return@addOnChangeListener
            if (tracking) {
                live[item.key] = value.toInt()
                previews.forEach { it.invalidate() }
            } else {
                changed(item.key, value.toInt(), item.scope)
            }
        }
        slider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(s: Slider) {
                tracking = true
            }

            override fun onStopTrackingTouch(s: Slider) {
                tracking = false
                changed(item.key, s.value.toInt(), item.scope)
            }
        })
        return Binding(item, view) {
            if (tracking) return@Binding
            val v = ((valueOf(item.key) as? Int) ?: item.min).coerceIn(item.min, item.max)
            // Snap to the step grid so Slider never throws on an off-grid stored value.
            val snapped = item.min + ((v - item.min) / item.step) * item.step
            slider.value = snapped.toFloat()
            valueView.text = format(item, snapped)
        }
    }

    private fun bindChoice(item: ChoiceItem, parent: ViewGroup): Binding {
        val view = inflater.inflate(R.layout.item_pref_choice, parent, false)
        val row = view.findViewById<View>(R.id.row)
        setupRow(row, item.icon, context.getString(item.title), null)
        val group = view.findViewById<ChipGroup>(R.id.chips)
        val chips = item.choices.map { choice ->
            (inflater.inflate(R.layout.item_choice_chip, group, false) as Chip).apply {
                id = View.generateViewId()
                text = context.getString(choice.label)
                tag = choice.value
                group.addView(this)
            }
        }
        var updating = false
        group.setOnCheckedStateChangeListener { g, ids ->
            if (updating) return@setOnCheckedStateChangeListener
            val value = ids.firstOrNull()?.let { g.findViewById<Chip>(it).tag as String } ?: return@setOnCheckedStateChangeListener
            if (value != valueOf(item.key)) changed(item.key, value, item.scope)
        }
        return Binding(item, view) {
            updating = true
            val current = valueOf(item.key)
            chips.firstOrNull { it.tag == current }?.let { group.check(it.id) }
            updating = false
        }
    }

    private fun bindColor(item: ColorItem, parent: ViewGroup): Binding {
        val row = inflater.inflate(R.layout.item_pref_row, parent, false)
        setupRow(row, item.icon, context.getString(item.title), null)
        val swatch = row.findViewById<ImageView>(R.id.swatch).apply { isVisible = true }
        val summary = row.findViewById<TextView>(R.id.summary)
        val outline = MaterialColors.getColor(context, MR.attr.colorOutline, Color.GRAY)
        row.setOnClickListener { host.onColorClick(item) }
        return Binding(item, row) {
            val raw = valueOf(item.key) as? Int ?: 0
            val color = if (raw == 0) 0 else Constants.resolveColor(context, raw)
            summary.isVisible = true
            summary.text = if (raw == 0) context.getString(R.string.color_automatic)
            else String.format("#%06X", color and 0xFFFFFF)
            swatch.setImageDrawable(GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                if (raw == 0) {
                    colors = intArrayOf(accent.accent, accent.container)
                    gradientType = GradientDrawable.SWEEP_GRADIENT
                } else {
                    setColor(color)
                }
                setStroke(dp(2), outline)
            })
        }
    }

    private fun bindAction(item: ActionItem, parent: ViewGroup): Binding {
        val row = inflater.inflate(R.layout.item_pref_row, parent, false)
        setupRow(row, item.icon, context.getString(item.title), context.getString(item.summary))
        row.findViewById<View>(R.id.chevron).isVisible = true
        if (item.danger) {
            val error = MaterialColors.getColor(context, androidx.appcompat.R.attr.colorError, Color.RED)
            row.findViewById<TextView>(R.id.title).setTextColor(error)
            row.findViewById<View>(R.id.icon_container).background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(MaterialColors.getColor(context, MR.attr.colorErrorContainer, Color.LTGRAY))
            }
            row.findViewById<ImageView>(R.id.icon).imageTintList =
                ColorStateList.valueOf(MaterialColors.getColor(context, MR.attr.colorOnErrorContainer, Color.BLACK))
        }
        row.setOnClickListener { host.onAction(item) }
        val summary = row.findViewById<TextView>(R.id.summary)
        return Binding(item, row) {
            if (item.id == "pick_icon_pack") {
                summary.text = IconPacks.label(context, PrefStore.string(com.ukm.app.iconscustomizer.Prefs.ICON_PACK))
            }
            if (item.id == "pick_fallback_packs") {
                val main = PrefStore.string(com.ukm.app.iconscustomizer.Prefs.ICON_PACK)
                val extras = com.ukm.app.iconscustomizer.IconPackHelper.packChain(
                    main,
                    PrefStore.string(com.ukm.app.iconscustomizer.Prefs.ICON_PACK_FALLBACKS)
                ).filter { it != main }
                summary.text = if (extras.isEmpty()) context.getString(R.string.pref_fallback_packs_summary)
                else extras.joinToString(" → ") { IconPacks.label(context, it) }
            }
        }
    }

    private fun bindInfo(item: InfoItem, parent: ViewGroup): Binding {
        val view = inflater.inflate(R.layout.item_pref_info, parent, false)
        view.findViewById<ImageView>(R.id.icon).setImageResource(item.icon)
        view.findViewById<TextView>(R.id.text).setText(item.text)
        return Binding(item, view) {}
    }

    private fun bindPreview(item: PreviewItem): Binding {
        val preview = PreviewView(context, item.kind, accent) { valueOf(it) }
        preview.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(196))
        previews += preview
        return Binding(item, preview) {}
    }
}
