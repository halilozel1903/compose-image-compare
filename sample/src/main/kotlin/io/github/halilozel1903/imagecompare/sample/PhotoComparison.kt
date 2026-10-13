package io.github.halilozel1903.imagecompare.sample

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import io.github.halilozel1903.imagecompare.CompareOrientation
import io.github.halilozel1903.imagecompare.ImageCompare
import io.github.halilozel1903.imagecompare.ImageCompareDefaults
import io.github.halilozel1903.imagecompare.ImageCompareState
import io.github.halilozel1903.imagecompare.LabelPlacement

/** A demo picture compared with [filter] applied to the same picture. */
@Composable
fun PhotoComparison(
    photo: DemoPhoto,
    filter: PhotoFilter,
    state: ImageCompareState,
    orientation: CompareOrientation,
    modifier: Modifier = Modifier,
    labelPlacement: LabelPlacement = LabelPlacement.Start,
) {
    val painter = photo.painter()
    val description = "${photo.title}: original and ${filter.label}"
    if (filter == PhotoFilter.Vignette) {
        // The composable overload: the after side is the picture with a gradient drawn over it.
        ImageCompare(
            before = { Image(painter, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) },
            after = {
                Box(Modifier.fillMaxSize()) {
                    Image(painter, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    Box(
                        Modifier
                            .fillMaxSize()
                            .drawWithCache {
                                val brush = Brush.radialGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f)),
                                    radius = size.maxDimension * 0.62f,
                                )
                                onDrawBehind { drawRect(brush) }
                            },
                    )
                }
            },
            modifier = modifier,
            state = state,
            orientation = orientation,
            beforeLabel = { ImageCompareDefaults.Label("Original") },
            afterLabel = { ImageCompareDefaults.Label(filter.label) },
            labelPlacement = labelPlacement,
            contentDescription = description,
        )
    } else {
        // The painter overload: the same painter twice, with a color matrix on the after side.
        ImageCompare(
            before = painter,
            after = painter,
            modifier = modifier,
            state = state,
            orientation = orientation,
            beforeLabel = "Original",
            afterLabel = filter.label,
            labelPlacement = labelPlacement,
            afterColorFilter = filter.colorFilter,
            contentDescription = description,
        )
    }
}

/** "2.5" for a zoom of 2.5, with one decimal and no locale formatting. */
fun formatZoom(scale: Float): String {
    val tenths = Math.round(scale * 10f)
    return "${tenths / 10}.${tenths % 10}"
}
