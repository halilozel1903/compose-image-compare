package io.github.halilozel1903.imagecompare.core

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Divider math for a before and after comparison.
 *
 * A comparison has one axis: the x axis for a side by side split (the divider is a vertical line) and the y axis for
 * a top and bottom split. The **position** is a fraction from `0` to `1` measured from the start of that axis: the
 * before image fills `0..position` and the after image fills `position..1`. Position `1` shows only the before image
 * and position `0` only the after image, in the split and in the fade mode alike.
 *
 * Offsets are physical pixels from the left (or top) edge. `reversed` is `true` when the start of the axis is on the
 * right, which is the case for a side by side split in a right-to-left layout.
 */
public object CompareMath {
    /** The default position: half before, half after. */
    public const val CENTER: Float = 0.5f

    /** Clamps [position] to `0..1`; `NaN` becomes [CENTER]. */
    public fun clampPosition(position: Float): Float =
        if (position.isNaN()) CENTER else position.coerceIn(0f, 1f)

    /** Where the divider is, in pixels from the left (or top) edge of an axis [length] pixels long. */
    public fun dividerOffset(position: Float, length: Float, reversed: Boolean = false): Float {
        if (length <= 0f) return 0f
        val p = clampPosition(position)
        return if (reversed) length * (1f - p) else length * p
    }

    /** The position that puts the divider at [offset] pixels from the left (or top) edge, clamped to `0..1`. */
    public fun positionAt(offset: Float, length: Float, reversed: Boolean = false): Float {
        if (length <= 0f) return CENTER
        val p = (offset / length).coerceIn(0f, 1f)
        return if (reversed) 1f - p else p
    }

    /**
     * The position after a drag of [delta] pixels along the axis, starting at [position]. Dragging towards the
     * physical end of the axis grows the position, except on a [reversed] axis.
     */
    public fun dragBy(position: Float, delta: Float, length: Float, reversed: Boolean = false): Float {
        if (length <= 0f) return clampPosition(position)
        val change = delta / length
        return clampPosition(position + if (reversed) -change else change)
    }

    /**
     * One keyboard or accessibility step of [step] (a fraction of the axis) from [position], towards the end of the
     * axis when [forward]. The result is rounded to the step grid when [position] is already on it, so ten steps of
     * `0.1` from `0` land on exactly `1`.
     */
    public fun step(position: Float, forward: Boolean, step: Float): Float {
        require(step > 0f) { "step must be positive, was $step" }
        val p = clampPosition(position)
        val next = if (forward) p + step else p - step
        val snapped = (next / step).roundToInt() * step
        val result = if (abs(snapped - next) < step * 1e-3f) snapped else next
        return clampPosition(result)
    }

    /** `true` when a pointer at [pointerOffset] is within [touchRadius] pixels of the divider at [dividerOffset]. */
    public fun isOnDivider(pointerOffset: Float, dividerOffset: Float, touchRadius: Float): Boolean =
        abs(pointerOffset - dividerOffset) <= touchRadius

    /** The position as a whole percentage of the before image, for accessibility and display. */
    public fun percent(position: Float): Int = (clampPosition(position) * 100f).roundToInt()

    /** The opacity of the after image drawn over the before image in the fade mode. */
    public fun fadeAlpha(position: Float): Float = 1f - clampPosition(position)

    /**
     * The opacity of a label in the split mode. A label is fully visible while the divider is at least
     * [fadeDistance] pixels away from it on the side of the image it names, and fades out as the divider reaches it,
     * so a label never sits on top of the other image.
     *
     * [labelLow] and [labelHigh] are the label's physical bounds along the axis. [labelOnLowSide] is `true` for a
     * label whose image fills the low (left or top) side of the divider.
     */
    public fun labelAlpha(
        dividerOffset: Float,
        labelLow: Float,
        labelHigh: Float,
        labelOnLowSide: Boolean,
        fadeDistance: Float,
    ): Float {
        // Room between the divider and the label's far edge, on the label's own side.
        val room = if (labelOnLowSide) dividerOffset - labelHigh else labelLow - dividerOffset
        if (fadeDistance <= 0f) return if (room >= 0f) 1f else 0f
        return (room / fadeDistance).coerceIn(0f, 1f)
    }

    /**
     * The opacity of a label in the fade mode. Both labels are fully visible at the center; the before label fades out
     * towards position `0` and the after label towards position `1`.
     */
    public fun fadeLabelAlpha(position: Float, isBefore: Boolean): Float {
        val p = clampPosition(position)
        val share = if (isBefore) p else 1f - p
        return (share * 2f).coerceIn(0f, 1f)
    }
}

/** What a drag that started away from the divider should do, once it moved. */
public enum class DragIntent {
    /** It hasn't moved past the touch slop yet. */
    Undecided,

    /** It moves mostly along the comparison axis: move the divider. */
    MoveDivider,

    /** It moves mostly across the axis: leave it to a parent, such as a scrolling list. */
    Ignore,
}

/** Decides whether a drag that started away from the divider moves it, so a list around a comparison still scrolls. */
public object DragIntentDetector {
    /**
     * The intent of a drag that moved [alongAxis] pixels along the comparison axis and [acrossAxis] pixels across it.
     * It stays [DragIntent.Undecided] until the drag is longer than [touchSlop].
     */
    public fun intent(alongAxis: Float, acrossAxis: Float, touchSlop: Float): DragIntent {
        val along = abs(alongAxis)
        val across = abs(acrossAxis)
        return when {
            along <= touchSlop && across <= touchSlop -> DragIntent.Undecided
            along >= across -> DragIntent.MoveDivider
            else -> DragIntent.Ignore
        }
    }
}
