package com.ukm.app.iconscustomizer

import com.ukm.app.iconscustomizer.IconNormalizer.Box
import com.ukm.app.iconscustomizer.IconPackHelper.IconRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IconMixingTest {

    private val opaque = 0xFF000000.toInt()

    @Test
    fun chainPutsMainPackFirstAndDropsDuplicates() {
        assertEquals(listOf("a", "b", "c"), IconPackHelper.packChain("a", "b, a,,c,b"))
        assertEquals(listOf("b"), IconPackHelper.packChain("none", "b"))
        assertEquals(emptyList<String>(), IconPackHelper.packChain("none", ""))
    }

    @Test
    fun overrideFromMainPackKeepsOldFormat() {
        assertEquals("icon", IconPackHelper.encodeOverride("a", IconRef("a", "icon")))
        assertEquals(IconRef("a", "icon"), IconPackHelper.decodeOverride("a", "icon"))
    }

    @Test
    fun overrideFromOtherPackRoundTrips() {
        val ref = IconRef("b", "icon")
        assertEquals(ref, IconPackHelper.decodeOverride("a", IconPackHelper.encodeOverride("a", ref)))
    }

    @Test
    fun exactActivityBeatsSamePackageEntry() {
        val map = mapOf(
            "ComponentInfo{p/.Other}" to "other",
            "ComponentInfo{p/.Main}" to "main",
        )
        assertEquals("main", IconPackHelper.matchComponent(map, "p", ".Main"))
        assertEquals("other", IconPackHelper.matchComponent(map, "p", ".Missing"))
        assertNull(IconPackHelper.matchComponent(map, "pp", ".Main"))
    }

    @Test
    fun visibleBoxIgnoresShadowPixels() {
        val pixels = IntArray(16)
        pixels[0] = 0x10000000 // faint shadow
        pixels[1 * 4 + 1] = opaque
        pixels[2 * 4 + 2] = opaque
        assertEquals(Box(1, 1, 3, 3), IconNormalizer.visibleBox(pixels, 4, 4))
    }

    @Test
    fun emptyIconHasNoBox() {
        assertNull(IconNormalizer.visibleBox(IntArray(16), 4, 4))
    }

    @Test
    fun differentMarginsEndUpAtSameSize() {
        for (box in listOf(Box(0, 0, 100, 100), Box(25, 25, 75, 75), Box(40, 10, 100, 70))) {
            val fit = IconNormalizer.fit(box, 100, 80)
            assertEquals(80f, (box.right - box.left) * fit.scale, 0.01f)
            assertEquals(10f, box.left * fit.scale + fit.dx, 0.01f)
            assertEquals(10f, box.top * fit.scale + fit.dy, 0.01f)
        }
    }

    @Test
    fun nonSquareArtworkStaysCentred() {
        val box = Box(10, 40, 90, 60)
        val fit = IconNormalizer.fit(box, 100, 80)
        assertEquals(1f, fit.scale, 0.01f)
        assertEquals(50f, (box.top + box.bottom) / 2f * fit.scale + fit.dy, 0.01f)
    }

    @Test
    fun shortcutKeysAreTellableFromAppKeys() {
        val key = Prefs.shortcutKey("com.app", "pkg:string/clean")
        assertEquals("ShortcutInfo{com.app/pkg:string/clean}", key)
        assertEquals(true, Prefs.isShortcutKey(key))
        assertEquals(false, Prefs.isShortcutKey("ComponentInfo{com.app/.Main}"))
    }

    @Test
    fun tinyGlyphUpscaleIsCapped() {
        val fit = IconNormalizer.fit(Box(49, 49, 51, 51), 100, 100)
        assertEquals(3f, fit.scale, 0.01f)
    }
}
