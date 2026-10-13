package io.github.halilozel1903.imagecompare.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ZoomMathTest {
    private val w = 1000f
    private val h = 800f

    @Test
    fun scaleIsClampedToTheRange() {
        assertEquals(1f, ZoomMath.clampScale(0.4f, 1f, 5f))
        assertEquals(5f, ZoomMath.clampScale(9f, 1f, 5f))
        assertEquals(1f, ZoomMath.clampScale(Float.NaN, 1f, 5f))
        assertFailsWith<IllegalArgumentException> { ZoomMath.clampScale(2f, 3f, 2f) }
    }

    @Test
    fun offsetStaysWithinTheContentEdges() {
        assertEquals(0f, ZoomMath.maxOffset(1f, w))
        assertEquals(500f, ZoomMath.maxOffset(2f, w))
        val clamped = ZoomMath.clamp(ZoomTransform(2f, 900f, -900f), w, h)
        assertEquals(ZoomTransform(2f, 500f, -400f), clamped)
        assertEquals(ZoomTransform.Identity, ZoomMath.clamp(ZoomTransform(0.5f, 40f, 40f), w, h))
    }

    @Test
    fun pinchKeepsTheFocusPointInPlace() {
        val focusX = 700f
        val focusY = 300f
        val before = ZoomMath.toContent(ZoomTransform.Identity, focusX, focusY, w, h)
        val zoomed = ZoomMath.transform(ZoomTransform.Identity, 2f, 0f, 0f, focusX, focusY, w, h)
        val after = ZoomMath.toContent(zoomed, focusX, focusY, w, h)
        assertEquals(2f, zoomed.scale)
        assertEquals(before.first, after.first, absoluteTolerance = 0.01f)
        assertEquals(before.second, after.second, absoluteTolerance = 0.01f)
    }

    @Test
    fun pinchAddsThePanAndClamps() {
        val zoomed = ZoomMath.transform(ZoomTransform(2f, 0f, 0f), 1f, 120f, -60f, 500f, 400f, w, h)
        assertEquals(ZoomTransform(2f, 120f, -60f), zoomed)
        val far = ZoomMath.transform(ZoomTransform(2f, 0f, 0f), 1f, 5000f, 0f, 500f, 400f, w, h)
        assertEquals(500f, far.offsetX)
        val capped = ZoomMath.transform(ZoomTransform(4f, 0f, 0f), 3f, 0f, 0f, 500f, 400f, w, h, maxScale = 5f)
        assertEquals(5f, capped.scale)
    }

    @Test
    fun zoomingOutRecentersTheContent() {
        val zoomed = ZoomTransform(3f, 800f, 300f)
        val out = ZoomMath.transform(zoomed, 0.2f, 0f, 0f, 200f, 200f, w, h)
        assertEquals(ZoomTransform.Identity, out)
        assertFalse(out.isZoomed)
        assertTrue(zoomed.isZoomed)
    }

    @Test
    fun doubleTapTogglesBetweenTheMinimumAndTheZoomedScale() {
        assertEquals(2.5f, ZoomMath.doubleTapTarget(1f, 1f, 2.5f))
        assertEquals(1f, ZoomMath.doubleTapTarget(2.5f, 1f, 2.5f))
        assertEquals(1f, ZoomMath.doubleTapTarget(1.6f, 1f, 2.5f))
    }
}
