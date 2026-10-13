package io.github.halilozel1903.imagecompare.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.imagecompare.CompareMode
import io.github.halilozel1903.imagecompare.CompareOrientation
import io.github.halilozel1903.imagecompare.LabelPlacement
import io.github.halilozel1903.imagecompare.rememberImageCompareState

private data class Tile(
    val photo: DemoPhoto,
    val filter: PhotoFilter,
    val orientation: CompareOrientation,
    val position: Float,
    val mode: CompareMode = CompareMode.Split,
)

private val tiles = listOf(
    Tile(DemoPhoto.MirrorLake, PhotoFilter.Vivid, CompareOrientation.Horizontal, 0.45f),
    Tile(DemoPhoto.QuietFjord, PhotoFilter.Mono, CompareOrientation.Horizontal, 0.56f),
    Tile(DemoPhoto.AutumnHills, PhotoFilter.Sepia, CompareOrientation.Vertical, 0.5f),
    Tile(DemoPhoto.MirrorLake, PhotoFilter.Cool, CompareOrientation.Vertical, 0.42f),
    Tile(DemoPhoto.QuietFjord, PhotoFilter.Vignette, CompareOrientation.Horizontal, 0.5f),
    Tile(DemoPhoto.AutumnHills, PhotoFilter.Faded, CompareOrientation.Horizontal, 0.5f, CompareMode.Fade),
)

/** Six comparisons side by side, for tablets and other wide windows. */
@Composable
fun GalleryScreen(scene: Scene?) {
    if (scene != null) SceneReady(scene, ready = true)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Text("Gallery", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Bundled demo pictures with filters applied in the app. Drag, tap, pinch or use the arrow keys.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        tiles.chunked(3).forEachIndexed { rowIndex, row ->
            if (rowIndex > 0) Spacer(Modifier.height(16.dp))
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { tile -> GalleryTile(tile, Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun GalleryTile(tile: Tile, modifier: Modifier) {
    val state = rememberImageCompareState(initialPosition = tile.position, initialMode = tile.mode)
    Surface(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(8.dp)) {
            PhotoComparison(
                photo = tile.photo,
                filter = tile.filter,
                state = state,
                orientation = tile.orientation,
                labelPlacement = if (tile.orientation == CompareOrientation.Vertical) LabelPlacement.Start else LabelPlacement.End,
                modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(14.dp)),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                tile.photo.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
            val layout = if (tile.orientation == CompareOrientation.Horizontal) "side by side" else "stacked"
            val mode = if (tile.mode == CompareMode.Fade) "fade" else layout
            Text(
                "Original vs ${tile.filter.label}, $mode",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 4.dp),
            )
        }
    }
}
