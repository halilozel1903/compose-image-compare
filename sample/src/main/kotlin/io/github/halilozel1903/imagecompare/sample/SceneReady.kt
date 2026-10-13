package io.github.halilozel1903.imagecompare.sample

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.delay

/**
 * Logs `SCENE_READY <scene>` once [ready] is true and a few frames have been drawn, so `scripts/screenshots.sh`
 * captures a settled screen. The demo pictures are bundled, so the only thing to wait for is layout (and the zoom of
 * the zoomed scene).
 */
@Composable
fun SceneReady(scene: Scene, ready: Boolean) {
    LaunchedEffect(scene, ready) {
        if (!ready) return@LaunchedEffect
        repeat(3) { withFrameNanos { } }
        delay(1_500)
        logScene("SCENE_READY ${scene.key}")
    }
}
