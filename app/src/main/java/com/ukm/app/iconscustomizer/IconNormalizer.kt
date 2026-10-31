package com.ukm.app.iconscustomizer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Packs leave different amounts of transparent margin around their artwork, so mixed icons
 * look mismatched even at the same bounds. This measures the visible artwork and redraws it
 * centred at a fixed share of the canvas, whichever pack it came from.
 */
object IconNormalizer {

    private const val CANVAS_PX = 192

    /** Faint pixels are soft drop shadows, which must not count as part of the artwork. */
    private const val MIN_ALPHA = 40

    /** Keeps tiny glyphs from being blown up into a blurry blob. */
    private const val MAX_UPSCALE = 3f

    /** Pixel bounds of the artwork; right and bottom are exclusive. */
    data class Box(val left: Int, val top: Int, val right: Int, val bottom: Int)

    data class Fit(val scale: Float, val dx: Float, val dy: Float)

    fun visibleBox(pixels: IntArray, width: Int, height: Int): Box? {
        var left = width
        var top = height
        var right = 0
        var bottom = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                if ((pixels[y * width + x] ushr 24) < MIN_ALPHA) continue
                left = min(left, x)
                top = min(top, y)
                right = max(right, x + 1)
                bottom = max(bottom, y + 1)
            }
        }
        return if (right > left && bottom > top) Box(left, top, right, bottom) else null
    }

    /** Scales the longest side of [box] to [percent] of the canvas, keeping it centred. */
    fun fit(box: Box, canvas: Int, percent: Int): Fit {
        val longest = max(box.right - box.left, box.bottom - box.top)
        val scale = min(canvas * percent / 100f / longest, MAX_UPSCALE)
        return Fit(
            scale,
            (canvas - (box.left + box.right) * scale) / 2f,
            (canvas - (box.top + box.bottom) * scale) / 2f,
        )
    }

    fun normalize(context: Context, icon: Drawable, percent: Int): Drawable {
        placeOnCanvas(icon)
        val bitmap = createBitmap(CANVAS_PX, CANVAS_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        icon.draw(canvas)
        val pixels = IntArray(CANVAS_PX * CANVAS_PX)
        bitmap.getPixels(pixels, 0, CANVAS_PX, 0, 0, CANVAS_PX, CANVAS_PX)
        val box = visibleBox(pixels, CANVAS_PX, CANVAS_PX) ?: return icon

        // Redraw the source rather than resampling the bitmap, so vectors stay sharp.
        val fit = fit(box, CANVAS_PX, percent)
        bitmap.eraseColor(0)
        canvas.translate(fit.dx, fit.dy)
        canvas.scale(fit.scale, fit.scale)
        icon.draw(canvas)
        return bitmap.toDrawable(context.resources)
    }

    /** Sets bounds that fit the canvas without distorting non-square artwork. */
    private fun placeOnCanvas(icon: Drawable) {
        val intrinsicWidth = icon.intrinsicWidth.takeIf { it > 0 } ?: CANVAS_PX
        val intrinsicHeight = icon.intrinsicHeight.takeIf { it > 0 } ?: CANVAS_PX
        val ratio = CANVAS_PX.toFloat() / max(intrinsicWidth, intrinsicHeight)
        val width = (intrinsicWidth * ratio).roundToInt()
        val height = (intrinsicHeight * ratio).roundToInt()
        val left = (CANVAS_PX - width) / 2
        val top = (CANVAS_PX - height) / 2
        icon.setBounds(left, top, left + width, top + height)
    }
}
