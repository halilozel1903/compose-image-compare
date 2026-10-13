package io.github.halilozel1903.imagecompare

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Default values and building blocks for [ImageCompare]. */
public object ImageCompareDefaults {
    /** The divider color: white, which reads on photos. */
    public val DividerColor: Color = Color.White

    /** The divider width. */
    public val DividerWidth: Dp = 2.dp

    /** How close to the divider a touch has to start to grab it, on each side. */
    public val TouchRadius: Dp = 28.dp

    /** How far before the divider reaches a label it starts fading out. */
    public val LabelFadeDistance: Dp = 24.dp

    /** The space between a label and the edges of the comparison. */
    public val LabelPadding: Dp = 12.dp

    /** The ring drawn around the handle while the comparison has keyboard focus. */
    public val FocusRingColor: Color = Color(0xFF4C8DFF)

    /** One arrow key or D-pad press moves the divider by this share of the axis. */
    public const val KeyboardStep: Float = 0.05f

    /** Page up and page down move the divider by this share of the axis. */
    public const val PageStep: Float = 0.25f

    /** The zoom a double tap goes to. */
    public const val DoubleTapZoom: Float = 2.5f

    /** The `+` and `-` keys multiply or divide the zoom by this factor. */
    public const val KeyboardZoomFactor: Float = 1.5f

    /** Used by [ImageCompareState.animateTo] and tap to jump. */
    public val PositionAnimationSpec: AnimationSpec<Float> =
        spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)

    /** Used by each leg of [ImageCompareState.sweep]. */
    public val SweepAnimationSpec: AnimationSpec<Float> = tween(durationMillis = 650, easing = FastOutSlowInEasing)

    /** Used by [ImageCompareState.animateZoomTo] and double tap. */
    public val ZoomAnimationSpec: AnimationSpec<Float> = tween(durationMillis = 280, easing = FastOutSlowInEasing)

    /**
     * The default handle: a white circle with two arrows pointing along the axis of [orientation].
     *
     * @param orientation which way the arrows point.
     * @param containerColor the circle.
     * @param arrowColor the arrows.
     * @param size the diameter.
     */
    @Composable
    public fun Handle(
        orientation: CompareOrientation,
        modifier: Modifier = Modifier,
        containerColor: Color = Color.White,
        arrowColor: Color = Color(0xFF1F2430),
        size: Dp = 40.dp,
    ) {
        Box(
            modifier = modifier
                .size(size)
                .shadow(elevation = 6.dp, shape = CircleShape)
                .background(containerColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(size * 0.6f)) {
                val stroke = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                val arm = this.size.minDimension * 0.18f
                val gap = this.size.minDimension * 0.14f
                val c = center
                rotate(if (orientation == CompareOrientation.Vertical) 90f else 0f, c) {
                    // Left arrow, then right arrow (up and down when rotated).
                    val left = Path().apply {
                        moveTo(c.x - gap, c.y - arm)
                        lineTo(c.x - gap - arm, c.y)
                        lineTo(c.x - gap, c.y + arm)
                    }
                    val right = Path().apply {
                        moveTo(c.x + gap, c.y - arm)
                        lineTo(c.x + gap + arm, c.y)
                        lineTo(c.x + gap, c.y + arm)
                    }
                    drawPath(left, arrowColor, style = stroke)
                    drawPath(right, arrowColor, style = stroke)
                    drawCircle(arrowColor, radius = 1.4.dp.toPx(), center = Offset(c.x, c.y))
                }
            }
        }
    }

    /**
     * The default label: [text] in a translucent dark pill, readable on light and dark photos.
     */
    @Composable
    public fun Label(
        text: String,
        modifier: Modifier = Modifier,
        containerColor: Color = Color.Black.copy(alpha = 0.55f),
        textColor: Color = Color.White,
    ) {
        BasicText(
            text = text,
            modifier = modifier
                .background(containerColor, RoundedCornerShape(percent = 50))
                .padding(horizontal = 12.dp, vertical = 5.dp),
            style = TextStyle(
                color = textColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.4.sp,
            ),
            maxLines = 1,
        )
    }
}
