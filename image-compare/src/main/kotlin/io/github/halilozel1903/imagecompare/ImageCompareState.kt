package io.github.halilozel1903.imagecompare

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.unit.IntSize
import io.github.halilozel1903.imagecompare.core.CompareMath
import io.github.halilozel1903.imagecompare.core.ZoomMath
import io.github.halilozel1903.imagecompare.core.ZoomTransform

/**
 * The state of an [ImageCompare]: where the divider is, the [mode], and the zoom shared by both images.
 *
 * Read [position], [scale] and [offset] in composition or in a draw or layout lambda; they are snapshot state. Move
 * the divider with [snapTo] and [animateTo], and the zoom with [animateZoomTo], [snapZoomTo] and [resetZoom]. A drag
 * by the user cancels an animation that is running, and an animation cancels the previous one.
 *
 * Create it with [rememberImageCompareState], which saves it across configuration changes and process death.
 *
 * @param initialPosition where the divider starts, from `0` (only the after image) to `1` (only the before image).
 * @param initialMode split or fade.
 * @param minZoom the smallest zoom, at least `1` so the images always fill the comparison.
 * @param maxZoom the largest zoom.
 */
@Stable
public class ImageCompareState(
    initialPosition: Float = CompareMath.CENTER,
    initialMode: CompareMode = CompareMode.Split,
    public val minZoom: Float = 1f,
    public val maxZoom: Float = 5f,
) {
    init {
        require(minZoom >= 1f) { "minZoom must be at least 1, was $minZoom" }
        require(maxZoom >= minZoom) { "maxZoom ($maxZoom) must not be smaller than minZoom ($minZoom)" }
    }

    private var positionState by mutableFloatStateOf(CompareMath.clampPosition(initialPosition))
    private var transformState by mutableStateOf(ZoomTransform(scale = minZoom))
    private var draggingState by mutableStateOf(false)
    private var animatingState by mutableStateOf(false)
    private val positionMutex = MutatorMutex()
    private val zoomMutex = MutatorMutex()

    /** The size of the comparison in pixels, once it has been laid out. Zoom offsets are clamped to it. */
    internal var viewportSize: IntSize by mutableStateOf(IntSize.Zero)

    /**
     * Where the divider is, from `0` to `1`, measured from the start of the axis: the before image fills
     * `0..position`. In [CompareMode.Fade] the after image is drawn with an opacity of `1 - position`.
     */
    public val position: Float get() = positionState

    /** Split or fade. Changing it keeps the position. */
    public var mode: CompareMode by mutableStateOf(initialMode)

    /** The zoom applied to both images; [minZoom] when not zoomed. */
    public val scale: Float get() = transformState.scale

    /** How far the zoomed images are moved from the center, in pixels. */
    public val offset: Offset get() = Offset(transformState.offsetX, transformState.offsetY)

    /** The zoom as one value. */
    public val transform: ZoomTransform get() = transformState

    /** `true` when the images are zoomed in past [minZoom]. */
    public val isZoomed: Boolean get() = transformState.scale > minZoom + 1e-3f

    /** `true` while the user drags the divider. */
    public val isDragging: Boolean get() = draggingState

    /** `true` while [animateTo] moves the divider. */
    public val isAnimating: Boolean get() = animatingState

    /** Moves the divider to [position] at once, stopping an animation that is running. */
    public suspend fun snapTo(position: Float) {
        positionMutex.mutate(MutatePriority.PreventUserInput) {
            positionState = CompareMath.clampPosition(position)
        }
    }

    /**
     * Animates the divider to [position]. Suspends until the animation ends; it is cancelled by a drag, by [snapTo] or
     * by another [animateTo].
     */
    public suspend fun animateTo(
        position: Float,
        animationSpec: AnimationSpec<Float> = ImageCompareDefaults.PositionAnimationSpec,
    ) {
        val target = CompareMath.clampPosition(position)
        positionMutex.mutate(MutatePriority.Default) {
            animatingState = true
            try {
                animate(positionState, target, animationSpec = animationSpec) { value, _ ->
                    positionState = CompareMath.clampPosition(value)
                }
            } finally {
                animatingState = false
            }
        }
    }

    /**
     * Sweeps the divider from one end to the other and back to where it was, a quick way to show what changed.
     */
    public suspend fun sweep(
        animationSpec: AnimationSpec<Float> = ImageCompareDefaults.SweepAnimationSpec,
    ) {
        val start = positionState
        animateTo(1f, animationSpec)
        animateTo(0f, animationSpec)
        animateTo(start, animationSpec)
    }

    /**
     * Zooms both images to [scale] with an animation, keeping the point at [focus] (pixels from the top left of the
     * comparison) in place. Without a focus it zooms around the center.
     */
    public suspend fun animateZoomTo(
        scale: Float,
        focus: Offset = Offset.Unspecified,
        animationSpec: AnimationSpec<Float> = ImageCompareDefaults.ZoomAnimationSpec,
    ) {
        val target = ZoomMath.clampScale(scale, minZoom, maxZoom)
        zoomMutex.mutate(MutatePriority.Default) {
            val start = transformState
            val (fx, fy) = focusOrCenter(focus)
            animate(start.scale, target, animationSpec = animationSpec) { value, _ ->
                val zoomed = ZoomMath.zoomAround(start, value, fx, fy, width, height)
                transformState = ZoomMath.clamp(zoomed, width, height, minZoom, maxZoom)
            }
        }
    }

    /** Zooms both images to [scale] at once, keeping the point at [focus] in place (the center by default). */
    public suspend fun snapZoomTo(scale: Float, focus: Offset = Offset.Unspecified) {
        val target = ZoomMath.clampScale(scale, minZoom, maxZoom)
        zoomMutex.mutate(MutatePriority.PreventUserInput) {
            val (fx, fy) = focusOrCenter(focus)
            val zoomed = ZoomMath.zoomAround(transformState, target, fx, fy, width, height)
            transformState = ZoomMath.clamp(zoomed, width, height, minZoom, maxZoom)
        }
    }

    /** Zooms back out to [minZoom], with an animation when [animated]. */
    public suspend fun resetZoom(animated: Boolean = true) {
        if (animated) {
            animateZoomTo(minZoom)
        } else {
            zoomMutex.mutate(MutatePriority.PreventUserInput) { transformState = ZoomTransform(scale = minZoom) }
        }
    }

    // Gesture entry points. They run on the main thread from pointer input and stop a running animation first.

    internal suspend fun stopPositionAnimation() {
        positionMutex.mutate(MutatePriority.UserInput) { }
    }

    internal suspend fun stopZoomAnimation() {
        zoomMutex.mutate(MutatePriority.UserInput) { }
    }

    internal fun dispatchPosition(position: Float) {
        positionState = CompareMath.clampPosition(position)
    }

    internal fun dispatchDragging(dragging: Boolean) {
        draggingState = dragging
    }

    internal fun dispatchPinch(zoomChange: Float, pan: Offset, centroid: Offset) {
        transformState = ZoomMath.transform(
            current = transformState,
            zoomChange = zoomChange,
            panX = pan.x,
            panY = pan.y,
            focusX = centroid.x,
            focusY = centroid.y,
            width = width,
            height = height,
            minScale = minZoom,
            maxScale = maxZoom,
        )
    }

    /** Keeps the zoom valid after the comparison changed size. */
    internal fun onViewportChanged(size: IntSize) {
        if (size == viewportSize) return
        viewportSize = size
        transformState = ZoomMath.clamp(transformState, width, height, minZoom, maxZoom)
    }

    private val width: Float get() = viewportSize.width.toFloat()
    private val height: Float get() = viewportSize.height.toFloat()

    private fun focusOrCenter(focus: Offset): Pair<Float, Float> =
        if (focus.isSpecified) Pair(focus.x, focus.y) else Pair(width / 2f, height / 2f)

    public companion object {
        /**
         * Saves the position, the mode and the zoom. The zoom range is not saved; it comes from the arguments of
         * [rememberImageCompareState].
         */
        public fun Saver(minZoom: Float = 1f, maxZoom: Float = 5f): Saver<ImageCompareState, Any> =
            listSaver<ImageCompareState, Float>(
                save = { state ->
                    listOf(
                        state.position,
                        state.mode.ordinal.toFloat(),
                        state.scale,
                        state.offset.x,
                        state.offset.y,
                    )
                },
                restore = { saved ->
                    ImageCompareState(
                        initialPosition = saved[0],
                        initialMode = CompareMode.entries[saved[1].toInt().coerceIn(0, CompareMode.entries.size - 1)],
                        minZoom = minZoom,
                        maxZoom = maxZoom,
                    ).apply {
                        // Clamped to the viewport once it is laid out again.
                        transformState = ZoomTransform(
                            scale = ZoomMath.clampScale(saved[2], minZoom, maxZoom),
                            offsetX = saved[3],
                            offsetY = saved[4],
                        )
                    }
                },
            )
    }
}

/**
 * Creates an [ImageCompareState] that survives configuration changes and process death.
 *
 * @param initialPosition where the divider starts, from `0` (only the after image) to `1` (only the before image).
 * @param initialMode split or fade.
 * @param minZoom the smallest zoom, at least `1`.
 * @param maxZoom the largest zoom.
 */
@Composable
public fun rememberImageCompareState(
    initialPosition: Float = CompareMath.CENTER,
    initialMode: CompareMode = CompareMode.Split,
    minZoom: Float = 1f,
    maxZoom: Float = 5f,
): ImageCompareState = rememberSaveable(minZoom, maxZoom, saver = ImageCompareState.Saver(minZoom, maxZoom)) {
    ImageCompareState(initialPosition, initialMode, minZoom, maxZoom)
}
