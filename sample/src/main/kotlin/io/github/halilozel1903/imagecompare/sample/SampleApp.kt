package io.github.halilozel1903.imagecompare.sample

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** The gallery on wide screens (and in the tablet scene), the editor on phones. */
@Composable
fun SampleApp(scene: Scene?) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 840.dp
        when {
            scene == Scene.Tablet || (scene == null && wide) -> GalleryScreen(scene)
            else -> CompareScreen(scene)
        }
    }
}
