package io.github.halilozel1903.imagecompare.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CompareMathTest {
    @Test
    fun clampKeepsPositionsInRangeAndReplacesNaN() {
        assertEquals(0f, CompareMath.clampPosition(-0.2f))
        assertEquals(1f, CompareMath.clampPosition(1.7f))
        assertEquals(0.3f, CompareMath.clampPosition(0.3f))
        assertEquals(CompareMath.CENTER, CompareMath.clampPosition(Float.NaN))
    }

    @Test
    fun dividerOffsetMeasuresFromTheStartOfTheAxis() {
        assertEquals(300f, CompareMath.dividerOffset(0.25f, 1200f))
        assertEquals(900f, CompareMath.dividerOffset(0.25f, 1200f, reversed = true))
        assertEquals(1200f, CompareMath.dividerOffset(2f, 1200f))
        assertEquals(0f, CompareMath.dividerOffset(0.5f, 0f))
    }

    @Test
    fun positionAtInvertsDividerOffset() {
        for (reversed in listOf(false, true)) {
            for (p in listOf(0f, 0.1f, 0.42f, 0.5f, 0.9f, 1f)) {
                val offset = CompareMath.dividerOffset(p, 1080f, reversed)
                assertEquals(p, CompareMath.positionAt(offset, 1080f, reversed), absoluteTolerance = 1e-5f)
            }
        }
        assertEquals(1f, CompareMath.positionAt(5000f, 1080f))
        assertEquals(0f, CompareMath.positionAt(5000f, 1080f, reversed = true))
        assertEquals(CompareMath.CENTER, CompareMath.positionAt(10f, 0f))
    }

    @Test
    fun dragFollowsTheFingerAndMirrorsOnAReversedAxis() {
        assertEquals(0.6f, CompareMath.dragBy(0.5f, 100f, 1000f), absoluteTolerance = 1e-5f)
        assertEquals(0.4f, CompareMath.dragBy(0.5f, 100f, 1000f, reversed = true), absoluteTolerance = 1e-5f)
        assertEquals(1f, CompareMath.dragBy(0.95f, 400f, 1000f))
        assertEquals(0f, CompareMath.dragBy(0.05f, -400f, 1000f))
    }

    @Test
    fun stepsLandOnTheGridAndStopAtTheEnds() {
        var p = 0f
        repeat(10) { p = CompareMath.step(p, forward = true, step = 0.1f) }
        assertEquals(1f, p)
        assertEquals(1f, CompareMath.step(1f, forward = true, step = 0.05f))
        assertEquals(0f, CompareMath.step(0.02f, forward = false, step = 0.05f))
        assertEquals(0.45f, CompareMath.step(0.5f, forward = false, step = 0.05f), absoluteTolerance = 1e-5f)
        // Off the grid: a plain step.
        assertEquals(0.47f, CompareMath.step(0.42f, forward = true, step = 0.05f), absoluteTolerance = 1e-5f)
        assertFailsWith<IllegalArgumentException> { CompareMath.step(0.5f, forward = true, step = 0f) }
    }

    @Test
    fun hitTestUsesTheTouchRadius() {
        assertTrue(CompareMath.isOnDivider(520f, 500f, touchRadius = 24f))
        assertTrue(CompareMath.isOnDivider(476f, 500f, touchRadius = 24f))
        assertFalse(CompareMath.isOnDivider(530f, 500f, touchRadius = 24f))
    }

    @Test
    fun percentAndFadeAlpha() {
        assertEquals(42, CompareMath.percent(0.42f))
        assertEquals(100, CompareMath.percent(3f))
        assertEquals(1f, CompareMath.fadeAlpha(0f))
        assertEquals(0f, CompareMath.fadeAlpha(1f))
        assertEquals(0.25f, CompareMath.fadeAlpha(0.75f))
    }

    @Test
    fun labelsFadeAsTheDividerReachesThem() {
        // Before label on the left at 16..96 px, fading over 32 px.
        assertEquals(1f, CompareMath.labelAlpha(500f, 16f, 96f, labelOnLowSide = true, fadeDistance = 32f))
        assertEquals(0.5f, CompareMath.labelAlpha(112f, 16f, 96f, labelOnLowSide = true, fadeDistance = 32f))
        assertEquals(0f, CompareMath.labelAlpha(60f, 16f, 96f, labelOnLowSide = true, fadeDistance = 32f))
        // After label on the right at 900..1000 px.
        assertEquals(1f, CompareMath.labelAlpha(500f, 900f, 1000f, labelOnLowSide = false, fadeDistance = 32f))
        assertEquals(0.25f, CompareMath.labelAlpha(892f, 900f, 1000f, labelOnLowSide = false, fadeDistance = 32f))
        assertEquals(0f, CompareMath.labelAlpha(950f, 900f, 1000f, labelOnLowSide = false, fadeDistance = 32f))
        // No fade distance: on or off.
        assertEquals(1f, CompareMath.labelAlpha(96f, 16f, 96f, labelOnLowSide = true, fadeDistance = 0f))
        assertEquals(0f, CompareMath.labelAlpha(95f, 16f, 96f, labelOnLowSide = true, fadeDistance = 0f))
    }

    @Test
    fun fadeModeLabelsCrossFade() {
        assertEquals(1f, CompareMath.fadeLabelAlpha(0.5f, isBefore = true))
        assertEquals(1f, CompareMath.fadeLabelAlpha(0.5f, isBefore = false))
        assertEquals(0f, CompareMath.fadeLabelAlpha(0f, isBefore = true))
        assertEquals(1f, CompareMath.fadeLabelAlpha(0f, isBefore = false))
        assertEquals(0.5f, CompareMath.fadeLabelAlpha(0.75f, isBefore = false), absoluteTolerance = 1e-5f)
    }

    @Test
    fun dragIntentWaitsForTheSlopThenPicksTheDominantAxis() {
        assertEquals(DragIntent.Undecided, DragIntentDetector.intent(4f, 3f, touchSlop = 8f))
        assertEquals(DragIntent.MoveDivider, DragIntentDetector.intent(-12f, 5f, touchSlop = 8f))
        assertEquals(DragIntent.Ignore, DragIntentDetector.intent(3f, 14f, touchSlop = 8f))
    }
}
