package io.github.halilozel1903.imagecompare.core

import kotlin.math.abs

/**
 * A zoom applied to both images of a comparison: a [scale] around the center of the viewport, then a translation of
 * [offsetX], [offsetY] pixels. A point `p` of the unzoomed content (relative to the center) is drawn at
 * `center + offset + scale * p`.
 */
public data class ZoomTransform(
    public val scale: Float = 1f,
    public val offsetX: Float = 0f,
    public val offsetY: Float = 0f,
) {
    /** `true` when the content is zoomed in. */
    public val isZoomed: Boolean get() = scale > 1f + EPSILON

    public companion object {
        /** No zoom. */
        public val Identity: ZoomTransform = ZoomTransform()

        internal const val EPSILON: Float = 1e-3f
    }
}

/**
 * Zoom and pan math that keeps the zoomed content covering the viewport, so no empty space shows at the edges and
 * both images always line up.
 */
public object ZoomMath {
    /** Clamps [scale] to `minScale..maxScale`. */
    public fun clampScale(scale: Float, minScale: Float, maxScale: Float): Float {
        require(minScale > 0f && maxScale >= minScale) { "Invalid zoom range $minScale..$maxScale" }
        if (scale.isNaN()) return minScale
        return scale.coerceIn(minScale, maxScale)
    }

    /** How far content scaled by [scale] can move along an axis of [length] pixels before its edge shows. */
    public fun maxOffset(scale: Float, length: Float): Float =
        if (scale <= 1f || length <= 0f) 0f else (scale - 1f) * length / 2f

    /** [transform] with its scale clamped and its offset kept within the content's edges. */
    public fun clamp(
        transform: ZoomTransform,
        width: Float,
        height: Float,
        minScale: Float = 1f,
        maxScale: Float = 5f,
    ): ZoomTransform {
        val scale = clampScale(transform.scale, minScale, maxScale)
        val maxX = maxOffset(scale, width)
        val maxY = maxOffset(scale, height)
        return ZoomTransform(
            scale = scale,
            // "+ 0f" turns -0f into 0f, so a transform at rest equals Identity.
            offsetX = transform.offsetX.coerceIn(-maxX, maxX) + 0f,
            offsetY = transform.offsetY.coerceIn(-maxY, maxY) + 0f,
        )
    }

    /**
     * Applies one step of a pinch: the content is scaled by [zoomChange] around the focus point ([focusX], [focusY],
     * in pixels from the top left of the viewport), which stays under the fingers, and moved by [panX], [panY].
     */
    public fun transform(
        current: ZoomTransform,
        zoomChange: Float,
        panX: Float,
        panY: Float,
        focusX: Float,
        focusY: Float,
        width: Float,
        height: Float,
        minScale: Float = 1f,
        maxScale: Float = 5f,
    ): ZoomTransform {
        val target = clampScale(current.scale * zoomChange, minScale, maxScale)
        val zoomed = zoomAround(current, target, focusX, focusY, width, height)
        return clamp(
            ZoomTransform(zoomed.scale, zoomed.offsetX + panX, zoomed.offsetY + panY),
            width,
            height,
            minScale,
            maxScale,
        )
    }

    /**
     * [current] scaled to [scale] so that the content under the focus point ([focusX], [focusY]) stays there. The
     * result is not clamped; use [clamp] when it should stay within the content's edges.
     */
    public fun zoomAround(
        current: ZoomTransform,
        scale: Float,
        focusX: Float,
        focusY: Float,
        width: Float,
        height: Float,
    ): ZoomTransform {
        if (current.scale <= 0f) return ZoomTransform(scale, 0f, 0f)
        val ratio = scale / current.scale
        // Focus relative to the viewport center.
        val fx = focusX - width / 2f
        val fy = focusY - height / 2f
        return ZoomTransform(
            scale = scale,
            offsetX = fx * (1f - ratio) + current.offsetX * ratio,
            offsetY = fy * (1f - ratio) + current.offsetY * ratio,
        )
    }

    /** Where a point of the viewport ([x], [y]) is in the unzoomed content, in pixels from its top left. */
    public fun toContent(transform: ZoomTransform, x: Float, y: Float, width: Float, height: Float): Pair<Float, Float> {
        val cx = width / 2f
        val cy = height / 2f
        return Pair(
            cx + (x - cx - transform.offsetX) / transform.scale,
            cy + (y - cy - transform.offsetY) / transform.scale,
        )
    }

    /**
     * The scale a double tap zooms to: [zoomedScale] from no zoom, back to [minScale] when already zoomed.
     */
    public fun doubleTapTarget(current: Float, minScale: Float, zoomedScale: Float): Float =
        if (abs(current - minScale) < ZoomTransform.EPSILON) zoomedScale else minScale
}
