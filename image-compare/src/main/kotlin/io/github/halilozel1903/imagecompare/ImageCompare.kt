package io.github.halilozel1903.imagecompare

import androidx.compose.foundation.Image
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import io.github.halilozel1903.imagecompare.core.CompareMath
import io.github.halilozel1903.imagecompare.core.DragIntent
import io.github.halilozel1903.imagecompare.core.DragIntentDetector
import io.github.halilozel1903.imagecompare.core.ZoomMath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Compares two images, a [before] and an [after] painter, with a draggable divider.
 *
 * Give it a size with [modifier] (for example `fillMaxWidth().aspectRatio(4f / 3f)`); without one it takes the
 * intrinsic size of [after]. Both painters are drawn with the same [contentScale] and [alignment] so they line up;
 * use two images of the same aspect ratio, or the same image with a different [afterColorFilter].
 *
 * See the other overload for the parameters they share.
 *
 * @param beforeLabel shown at the start of the axis on the before side, with [ImageCompareDefaults.Label].
 * @param afterLabel shown at the end of the axis on the after side.
 * @param beforeColorFilter applied to the before painter.
 * @param afterColorFilter applied to the after painter, for example a `ColorFilter.colorMatrix` to compare a filter
 *   with the original.
 */
@Composable
public fun ImageCompare(
    before: Painter,
    after: Painter,
    modifier: Modifier = Modifier,
    state: ImageCompareState = rememberImageCompareState(),
    orientation: CompareOrientation = CompareOrientation.Horizontal,
    beforeLabel: String? = null,
    afterLabel: String? = null,
    labelPlacement: LabelPlacement = LabelPlacement.Start,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
    beforeColorFilter: ColorFilter? = null,
    afterColorFilter: ColorFilter? = null,
    handle: @Composable () -> Unit = { ImageCompareDefaults.Handle(orientation) },
    dividerColor: Color = ImageCompareDefaults.DividerColor,
    dividerWidth: Dp = ImageCompareDefaults.DividerWidth,
    zoomEnabled: Boolean = true,
    tapToJump: Boolean = true,
    doubleTapToZoom: Boolean = zoomEnabled,
    dragAnywhere: Boolean = true,
    keyboardStep: Float = ImageCompareDefaults.KeyboardStep,
    contentDescription: String? = null,
) {
    ImageCompare(
        before = {
            Image(
                painter = before,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                alignment = alignment,
                contentScale = contentScale,
                colorFilter = beforeColorFilter,
            )
        },
        after = {
            Image(
                painter = after,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                alignment = alignment,
                contentScale = contentScale,
                colorFilter = afterColorFilter,
            )
        },
        modifier = modifier,
        state = state,
        orientation = orientation,
        beforeLabel = beforeLabel?.let { text -> { ImageCompareDefaults.Label(text) } },
        afterLabel = afterLabel?.let { text -> { ImageCompareDefaults.Label(text) } },
        labelPlacement = labelPlacement,
        handle = handle,
        dividerColor = dividerColor,
        dividerWidth = dividerWidth,
        zoomEnabled = zoomEnabled,
        tapToJump = tapToJump,
        doubleTapToZoom = doubleTapToZoom,
        dragAnywhere = dragAnywhere,
        keyboardStep = keyboardStep,
        contentDescription = contentDescription,
    )
}

/**
 * Compares two composables, [before] and [after], with a draggable divider. Both get the full size of the comparison
 * and are clipped to their side of the divider, so any content works: images loaded by an image library, a map
 * before and after an edit, two renderings of a chart.
 *
 * Give it a size with [modifier]; without one it takes the size of [after].
 *
 * **Gestures**
 * - Drag the divider (or anywhere, with [dragAnywhere], when the drag goes along the axis) to move it. A drag that
 *   goes across the axis is left to a parent, so a list around the comparison still scrolls.
 * - Tap to move the divider to the tap ([tapToJump]).
 * - Pinch to zoom and pan both images in sync ([zoomEnabled]); once zoomed, drag away from the divider to pan.
 * - Double tap to zoom in to [ImageCompareDefaults.DoubleTapZoom] at the tap, and again to zoom out ([doubleTapToZoom]).
 *
 * **Keyboard and D-pad**: when focused, the arrow keys along the axis move the divider by [keyboardStep], page up and
 * page down by [ImageCompareDefaults.PageStep], home and end to the ends; `+`, `-` and `0` zoom in, out and reset.
 *
 * **Accessibility**: the comparison is an adjustable slider for TalkBack, with its position as a percentage of the
 * before image and actions to show the before image, the after image or both halves.
 *
 * @param before the before content, on the start (or top) side of the divider.
 * @param after the after content, on the end (or bottom) side.
 * @param state the divider position, mode and zoom; see [rememberImageCompareState].
 * @param orientation a side by side or a top and bottom split.
 * @param beforeLabel drawn over the before side at the start of the axis; it fades out as the divider reaches it.
 * @param afterLabel drawn over the after side at the end of the axis.
 * @param labelPlacement where the labels sit across the axis.
 * @param handle drawn centered on the divider. It is decoration: the whole divider is draggable.
 * @param dividerColor the divider line, in [CompareMode.Split].
 * @param dividerWidth the divider line's width; `0.dp` for none.
 * @param contentDescription what the comparison shows, for accessibility.
 */
@Composable
public fun ImageCompare(
    before: @Composable () -> Unit,
    after: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    state: ImageCompareState = rememberImageCompareState(),
    orientation: CompareOrientation = CompareOrientation.Horizontal,
    beforeLabel: (@Composable () -> Unit)? = null,
    afterLabel: (@Composable () -> Unit)? = null,
    labelPlacement: LabelPlacement = LabelPlacement.Start,
    handle: @Composable () -> Unit = { ImageCompareDefaults.Handle(orientation) },
    dividerColor: Color = ImageCompareDefaults.DividerColor,
    dividerWidth: Dp = ImageCompareDefaults.DividerWidth,
    zoomEnabled: Boolean = true,
    tapToJump: Boolean = true,
    doubleTapToZoom: Boolean = zoomEnabled,
    dragAnywhere: Boolean = true,
    keyboardStep: Float = ImageCompareDefaults.KeyboardStep,
    contentDescription: String? = null,
) {
    require(keyboardStep > 0f && keyboardStep <= 1f) { "keyboardStep must be in (0, 1], was $keyboardStep" }
    val scope = rememberCoroutineScope()
    val layoutDirection = LocalLayoutDirection.current
    val reversed = orientation == CompareOrientation.Horizontal && layoutDirection == LayoutDirection.Rtl
    val axis = remember(orientation, reversed) { Axis(orientation, reversed) }
    var focused by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val touchRadius = with(density) { ImageCompareDefaults.TouchRadius.toPx() }
    val currentZoomEnabled by rememberUpdatedState(zoomEnabled)
    val currentDragAnywhere by rememberUpdatedState(dragAnywhere)

    Box(
        modifier = modifier
            .onSizeChanged { state.onViewportChanged(it) }
            .clipToBounds()
            .pointerInput(state, axis, tapToJump, doubleTapToZoom) {
                detectTapGestures(
                    onDoubleTap = if (doubleTapToZoom) {
                        { offset ->
                            val target = ZoomMath.doubleTapTarget(
                                state.scale,
                                state.minZoom,
                                ImageCompareDefaults.DoubleTapZoom.coerceAtMost(state.maxZoom),
                            )
                            scope.launch { state.animateZoomTo(target, offset) }
                        }
                    } else {
                        null
                    },
                    onTap = if (tapToJump) {
                        { offset ->
                            val target = CompareMath.positionAt(axis.along(offset), axis.length(size), axis.reversed)
                            scope.launch { state.animateTo(target) }
                        }
                    } else {
                        null
                    },
                )
            }
            .pointerInput(state, axis) {
                compareGestures(
                    state = state,
                    axis = axis,
                    scope = scope,
                    touchRadius = touchRadius,
                    zoomEnabled = { currentZoomEnabled },
                    dragAnywhere = { currentDragAnywhere },
                )
            },
        propagateMinConstraints = true,
    ) {
        // The after layer sets the size when the modifier doesn't.
        CompareLayer(state, axis, isBefore = false, modifier = Modifier, content = after)
        CompareLayer(state, axis, isBefore = true, modifier = Modifier.matchParentSize(), content = before)
        CompareOverlay(
            state = state,
            axis = axis,
            handle = handle,
            beforeLabel = beforeLabel,
            afterLabel = afterLabel,
            labelPlacement = labelPlacement,
            dividerColor = dividerColor,
            dividerWidth = dividerWidth,
            focused = focused,
            modifier = Modifier.matchParentSize(),
        )
        CompareSemantics(
            state = state,
            axis = axis,
            scope = scope,
            keyboardStep = keyboardStep,
            zoomEnabled = zoomEnabled,
            contentDescription = contentDescription,
            onFocusChanged = { focused = it },
            modifier = Modifier.matchParentSize(),
        )
    }
}

/** The comparison axis: x for a side by side split, y for a top and bottom split. */
internal class Axis(val orientation: CompareOrientation, val reversed: Boolean) {
    val horizontal: Boolean get() = orientation == CompareOrientation.Horizontal

    fun along(offset: Offset): Float = if (horizontal) offset.x else offset.y

    fun across(offset: Offset): Float = if (horizontal) offset.y else offset.x

    fun length(size: IntSize): Float =
        (if (horizontal) size.width else size.height).toFloat()

    override fun equals(other: Any?): Boolean =
        other is Axis && other.orientation == orientation && other.reversed == reversed

    override fun hashCode(): Int = orientation.hashCode() * 31 + reversed.hashCode()
}

/** One image, clipped to its side of the divider (or faded in), with the shared zoom. */
@Composable
private fun CompareLayer(
    state: ImageCompareState,
    axis: Axis,
    isBefore: Boolean,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .graphicsLayer {
                // The before layer is drawn over the after layer. Drawing it with an opacity of `position` looks the
                // same as the after image over the before image with an opacity of `1 - position`.
                alpha = if (isBefore && state.mode == CompareMode.Fade) 1f - CompareMath.fadeAlpha(state.position) else 1f
            }
            .drawWithContent {
                if (state.mode == CompareMode.Fade) {
                    drawContent()
                    return@drawWithContent
                }
                val length = if (axis.horizontal) size.width else size.height
                val divider = CompareMath.dividerOffset(state.position, length, axis.reversed)
                // The before image fills the low side of the divider unless the axis is reversed.
                val lowSide = isBefore != axis.reversed
                val start = if (lowSide) 0f else divider
                val end = if (lowSide) divider else length
                if (end <= start) return@drawWithContent
                if (axis.horizontal) {
                    clipRect(left = start, top = 0f, right = end, bottom = size.height) {
                        this@drawWithContent.drawContent()
                    }
                } else {
                    clipRect(left = 0f, top = start, right = size.width, bottom = end) {
                        this@drawWithContent.drawContent()
                    }
                }
            },
        propagateMinConstraints = true,
    ) {
        Box(
            modifier = Modifier.graphicsLayer {
                val transform = state.transform
                scaleX = transform.scale
                scaleY = transform.scale
                translationX = transform.offsetX
                translationY = transform.offsetY
            },
            propagateMinConstraints = true,
        ) {
            content()
        }
    }
}

private const val HandleId = "handle"
private const val BeforeLabelId = "before"
private const val AfterLabelId = "after"

/** The divider line, the handle and the labels. Placement reads the position, so a drag never recomposes. */
@Composable
private fun CompareOverlay(
    state: ImageCompareState,
    axis: Axis,
    handle: @Composable () -> Unit,
    beforeLabel: (@Composable () -> Unit)?,
    afterLabel: (@Composable () -> Unit)?,
    labelPlacement: LabelPlacement,
    dividerColor: Color,
    dividerWidth: Dp,
    focused: Boolean,
    modifier: Modifier,
) {
    val density = LocalDensity.current
    val padding = with(density) { ImageCompareDefaults.LabelPadding.roundToPx() }
    val fadeDistance = with(density) { ImageCompareDefaults.LabelFadeDistance.toPx() }
    val ringWidth = with(density) { ImageCompareDefaults.DividerWidth.toPx() * 1.5f }
    Layout(
        modifier = modifier.drawBehind {
            if (state.mode != CompareMode.Split || dividerWidth.value <= 0f) return@drawBehind
            val length = if (axis.horizontal) size.width else size.height
            val divider = CompareMath.dividerOffset(state.position, length, axis.reversed)
            val stroke = dividerWidth.toPx()
            val shadow = Color.Black.copy(alpha = 0.25f)
            if (axis.horizontal) {
                drawLine(shadow, Offset(divider + stroke / 2f, 0f), Offset(divider + stroke / 2f, size.height), stroke)
                drawLine(dividerColor, Offset(divider, 0f), Offset(divider, size.height), stroke)
            } else {
                drawLine(shadow, Offset(0f, divider + stroke / 2f), Offset(size.width, divider + stroke / 2f), stroke)
                drawLine(dividerColor, Offset(0f, divider), Offset(size.width, divider), stroke)
            }
        },
        content = {
            Box(
                modifier = Modifier
                    .layoutId(HandleId)
                    .drawBehind {
                        if (focused) {
                            drawCircle(
                                color = ImageCompareDefaults.FocusRingColor,
                                radius = size.minDimension / 2f + ringWidth * 2f,
                                style = Stroke(width = ringWidth * 1.5f),
                            )
                        }
                    },
            ) { handle() }
            if (beforeLabel != null) Box(Modifier.layoutId(BeforeLabelId)) { beforeLabel() }
            if (afterLabel != null) Box(Modifier.layoutId(AfterLabelId)) { afterLabel() }
        },
    ) { measurables, constraints ->
        val loose = Constraints(maxWidth = constraints.maxWidth, maxHeight = constraints.maxHeight)
        val handlePlaceable = measurables.first { it.layoutId == HandleId }.measure(loose)
        val beforePlaceable = measurables.firstOrNull { it.layoutId == BeforeLabelId }?.measure(loose)
        val afterPlaceable = measurables.firstOrNull { it.layoutId == AfterLabelId }?.measure(loose)
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        layout(width, height) {
            val mainLength = if (axis.horizontal) width else height
            val crossLength = if (axis.horizontal) height else width
            val divider = CompareMath.dividerOffset(state.position, mainLength.toFloat(), axis.reversed)
            val handleMain = if (axis.horizontal) handlePlaceable.width else handlePlaceable.height
            val handleCross = if (axis.horizontal) handlePlaceable.height else handlePlaceable.width
            val hMain = (divider - handleMain / 2f).roundToInt()
            val hCross = (crossLength - handleCross) / 2
            fun placeLabel(placeable: Placeable, isBefore: Boolean) {
                val pMain = if (axis.horizontal) placeable.width else placeable.height
                val pCross = if (axis.horizontal) placeable.height else placeable.width
                // The before label sits on the low side unless the axis is reversed.
                val lowSide = isBefore != axis.reversed
                val main = if (lowSide) padding else mainLength - padding - pMain
                // Across the axis, LabelPlacement.Start is the top, or the start edge of a vertical comparison.
                val crossStartIsLow = axis.horizontal || layoutDirection == LayoutDirection.Ltr
                val cross = when (labelPlacement) {
                    LabelPlacement.Center -> (crossLength - pCross) / 2
                    LabelPlacement.Start -> if (crossStartIsLow) padding else crossLength - padding - pCross
                    LabelPlacement.End -> if (crossStartIsLow) crossLength - padding - pCross else padding
                }
                val x = if (axis.horizontal) main else cross
                val y = if (axis.horizontal) cross else main
                // Physical coordinates (place, not placeRelative): mirroring is handled above. The opacity follows
                // the divider without a relayout, because only the layer block reads the position.
                placeable.placeWithLayer(x, y) {
                    alpha = if (state.mode == CompareMode.Fade) {
                        CompareMath.fadeLabelAlpha(state.position, isBefore)
                    } else {
                        val d = CompareMath.dividerOffset(state.position, mainLength.toFloat(), axis.reversed)
                        CompareMath.labelAlpha(
                            dividerOffset = d,
                            labelLow = main.toFloat(),
                            labelHigh = (main + pMain).toFloat(),
                            labelOnLowSide = lowSide,
                            fadeDistance = fadeDistance,
                        )
                    }
                }
            }
            beforePlaceable?.let { placeLabel(it, isBefore = true) }
            afterPlaceable?.let { placeLabel(it, isBefore = false) }
            if (axis.horizontal) handlePlaceable.place(hMain, hCross) else handlePlaceable.place(hCross, hMain)
        }
    }
}

/** Focus, keys and the accessibility slider. Reads the position in its own scope, so only this recomposes. */
@Composable
private fun CompareSemantics(
    state: ImageCompareState,
    axis: Axis,
    scope: CoroutineScope,
    keyboardStep: Float,
    zoomEnabled: Boolean,
    contentDescription: String?,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier,
) {
    val position = state.position
    val description = contentDescription ?: "Before and after comparison"
    Box(
        modifier = modifier
            .semantics {
                this.contentDescription = description
                stateDescription = "${CompareMath.percent(position)}% before"
                progressBarRangeInfo = ProgressBarRangeInfo(current = position, range = 0f..1f)
                setProgress { target ->
                    scope.launch { state.snapTo(target) }
                    true
                }
                customActions = listOf(
                    CustomAccessibilityAction("Show before") {
                        scope.launch { state.animateTo(1f) }
                        true
                    },
                    CustomAccessibilityAction("Show after") {
                        scope.launch { state.animateTo(0f) }
                        true
                    },
                    CustomAccessibilityAction("Show both halves") {
                        scope.launch { state.animateTo(CompareMath.CENTER) }
                        true
                    },
                )
            }
            .onFocusChanged { onFocusChanged(it.isFocused) }
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                handleKey(event.key, state, axis, scope, keyboardStep, zoomEnabled)
            }
            .focusable(),
    )
}

private fun handleKey(
    key: Key,
    state: ImageCompareState,
    axis: Axis,
    scope: CoroutineScope,
    step: Float,
    zoomEnabled: Boolean,
): Boolean {
    // Which physical direction grows the position: right (or down), unless the axis is reversed.
    val growKey = if (axis.horizontal) Key.DirectionRight else Key.DirectionDown
    val shrinkKey = if (axis.horizontal) Key.DirectionLeft else Key.DirectionUp
    fun stepBy(forward: Boolean, size: Float) {
        val target = CompareMath.step(state.position, forward, size)
        scope.launch(start = CoroutineStart.UNDISPATCHED) { state.snapTo(target) }
    }
    when (key) {
        growKey -> stepBy(forward = !axis.reversed, size = step)
        shrinkKey -> stepBy(forward = axis.reversed, size = step)
        Key.PageDown -> stepBy(forward = true, size = ImageCompareDefaults.PageStep)
        Key.PageUp -> stepBy(forward = false, size = ImageCompareDefaults.PageStep)
        Key.MoveHome -> scope.launch { state.animateTo(0f) }
        Key.MoveEnd -> scope.launch { state.animateTo(1f) }
        Key.Plus, Key.Equals, Key.NumPadAdd -> {
            if (!zoomEnabled) return false
            scope.launch { state.animateZoomTo(state.scale * ImageCompareDefaults.KeyboardZoomFactor) }
        }
        Key.Minus, Key.NumPadSubtract -> {
            if (!zoomEnabled) return false
            scope.launch { state.animateZoomTo(state.scale / ImageCompareDefaults.KeyboardZoomFactor) }
        }
        Key.Zero, Key.NumPad0 -> {
            if (!zoomEnabled) return false
            scope.launch { state.resetZoom() }
        }
        else -> return false
    }
    return true
}

private enum class GestureMode { Undecided, Divider, Pan, Transform }

/** Divider drags, pans and pinches. Taps are handled by a separate detector. */
private suspend fun PointerInputScope.compareGestures(
    state: ImageCompareState,
    axis: Axis,
    scope: CoroutineScope,
    touchRadius: Float,
    zoomEnabled: () -> Boolean,
    dragAnywhere: () -> Boolean,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val length = axis.length(size)
        val divider = CompareMath.dividerOffset(state.position, length, axis.reversed)
        var mode = GestureMode.Undecided
        if (CompareMath.isOnDivider(axis.along(down.position), divider, touchRadius)) {
            mode = GestureMode.Divider
            down.consume()
            scope.launch(start = CoroutineStart.UNDISPATCHED) { state.stopPositionAnimation() }
            state.dispatchDragging(true)
        }
        var totalAlong = 0f
        var totalAcross = 0f
        val slop = viewConfiguration.touchSlop
        try {
            while (true) {
                val event = awaitPointerEvent()
                val pressed = event.changes.filter { it.pressed }
                if (pressed.isEmpty()) break

                if (pressed.size >= 2 && zoomEnabled()) {
                    if (mode == GestureMode.Divider) state.dispatchDragging(false)
                    if (mode != GestureMode.Transform) {
                        mode = GestureMode.Transform
                        scope.launch(start = CoroutineStart.UNDISPATCHED) { state.stopZoomAnimation() }
                    }
                    val zoom = event.calculateZoom()
                    val pan = event.calculatePan()
                    val centroid = event.calculateCentroid(useCurrent = true)
                    if (centroid.isSpecified) state.dispatchPinch(zoom, pan, centroid)
                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                    continue
                }

                val change = event.changes.firstOrNull { it.id == down.id && it.pressed } ?: pressed.first()
                val delta = change.positionChange()
                when (mode) {
                    GestureMode.Divider -> {
                        state.dispatchPosition(
                            CompareMath.dragBy(state.position, axis.along(delta), axis.length(size), axis.reversed),
                        )
                        change.consume()
                    }
                    GestureMode.Pan, GestureMode.Transform -> {
                        // After a pinch, the finger that stays down keeps panning.
                        if (state.isZoomed) {
                            state.dispatchPinch(1f, delta, change.position)
                            change.consume()
                        }
                    }
                    GestureMode.Undecided -> {
                        if (change.isConsumed) break // a child or another detector took it
                        totalAlong += axis.along(delta)
                        totalAcross += axis.across(delta)
                        if (state.isZoomed) {
                            if (abs(totalAlong) > slop || abs(totalAcross) > slop) {
                                mode = GestureMode.Pan
                                scope.launch(start = CoroutineStart.UNDISPATCHED) { state.stopZoomAnimation() }
                                state.dispatchPinch(1f, delta, change.position)
                                change.consume()
                            }
                        } else if (dragAnywhere()) {
                            when (DragIntentDetector.intent(totalAlong, totalAcross, slop)) {
                                DragIntent.Undecided -> Unit
                                DragIntent.Ignore -> break // across the axis: leave it to a scrolling parent
                                DragIntent.MoveDivider -> {
                                    mode = GestureMode.Divider
                                    scope.launch(start = CoroutineStart.UNDISPATCHED) { state.stopPositionAnimation() }
                                    state.dispatchDragging(true)
                                    state.dispatchPosition(
                                        CompareMath.dragBy(state.position, axis.along(delta), length, axis.reversed),
                                    )
                                    change.consume()
                                }
                            }
                        } else if (abs(totalAlong) > slop || abs(totalAcross) > slop) {
                            break
                        }
                    }
                }
            }
        } finally {
            if (mode == GestureMode.Divider) state.dispatchDragging(false)
        }
    }
}
