package io.github.halilozel1903.imagecompare.sample

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.imagecompare.CompareMode
import io.github.halilozel1903.imagecompare.CompareOrientation
import io.github.halilozel1903.imagecompare.ImageCompareState
import io.github.halilozel1903.imagecompare.LabelPlacement
import io.github.halilozel1903.imagecompare.core.CompareMath
import io.github.halilozel1903.imagecompare.rememberImageCompareState
import kotlinx.coroutines.launch

/** How a phone scene starts. */
private data class EditorSetup(
    val photo: DemoPhoto,
    val filter: PhotoFilter,
    val orientation: CompareOrientation,
    val position: Float,
)

private fun setupFor(scene: Scene?): EditorSetup = when (scene) {
    Scene.Vertical -> EditorSetup(DemoPhoto.AutumnHills, PhotoFilter.Mono, CompareOrientation.Vertical, 0.46f)
    Scene.Zoomed -> EditorSetup(DemoPhoto.QuietFjord, PhotoFilter.Mono, CompareOrientation.Horizontal, 0.5f)
    else -> EditorSetup(DemoPhoto.MirrorLake, PhotoFilter.Vivid, CompareOrientation.Horizontal, 0.42f)
}

/** The phone screen: one comparison with controls for every option. */
@Composable
fun CompareScreen(scene: Scene?) {
    val setup = remember(scene) { setupFor(scene) }
    var photo by rememberSaveable { mutableStateOf(setup.photo) }
    var filter by rememberSaveable { mutableStateOf(setup.filter) }
    var orientation by rememberSaveable { mutableStateOf(setup.orientation) }
    val state = rememberImageCompareState(initialPosition = setup.position)
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    val scope = rememberCoroutineScope()

    if (scene == Scene.Zoomed) {
        // Zoom in on the boat in the middle of the fjord, once the comparison has its size.
        LaunchedEffect(viewport) {
            if (viewport == IntSize.Zero) return@LaunchedEffect
            state.snapZoomTo(2.5f, Offset(viewport.width * 0.5f, viewport.height * 0.68f))
        }
    }
    if (scene != null) {
        SceneReady(scene, ready = viewport != IntSize.Zero && (scene != Scene.Zoomed || state.isZoomed))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Text("Image Compare", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Demo pictures drawn for this sample, filters applied in the app",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(14.dp))
        PhotoComparison(
            photo = photo,
            filter = filter,
            state = state,
            orientation = orientation,
            labelPlacement = LabelPlacement.Start,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 5f)
                .clip(RoundedCornerShape(24.dp))
                .onSizeChanged { viewport = it },
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Readout(state, Modifier.weight(1f))
            OutlinedButton(onClick = { scope.launch { state.resetZoom() } }) { Text("Reset") }
            Spacer(Modifier.padding(4.dp))
            FilledTonalButton(onClick = { scope.launch { state.sweep() } }) { Text("Sweep") }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SingleChoiceSegmentedButtonRow(Modifier.weight(1.25f)) {
                val options = listOf(CompareOrientation.Horizontal to "Side by side", CompareOrientation.Vertical to "Stacked")
                options.forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        selected = orientation == value,
                        onClick = { orientation = value },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        icon = {},
                    ) { Text(label, maxLines = 1) }
                }
            }
            SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
                val options = listOf(CompareMode.Split to "Split", CompareMode.Fade to "Fade")
                options.forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        selected = state.mode == value,
                        onClick = { state.mode = value },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        icon = {},
                    ) { Text(label, maxLines = 1) }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        ChipRow {
            DemoPhoto.entries.forEach { p ->
                FilterChip(selected = photo == p, onClick = { photo = p }, label = { Text(p.title) })
            }
        }
        ChipRow {
            PhotoFilter.entries.forEach { f ->
                FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f.label) })
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

/** The position and zoom, in a scope of their own so a drag only recomposes this text. */
@Composable
private fun Readout(state: ImageCompareState, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            "${CompareMath.percent(state.position)}% original",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "Zoom ${formatZoom(state.scale)}x",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}
