package io.github.halilozel1903.imagecompare.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier

/**
 * The compose-image-compare sample: bundled demo pictures (procedural landscapes, see `scripts/demo-photos.py`)
 * compared with filters applied in the app. `scripts/screenshots.sh` starts it with `--es scene <scene>`:
 *
 * - `horizontal`: Mirror lake, original and Vivid side by side
 * - `vertical`: Autumn hills, original on top and Mono below
 * - `zoomed`: Quiet fjord zoomed in on the boat, original and Mono
 * - `tablet`: the gallery of six comparisons (tablets in landscape)
 *
 * Without a scene it opens the editor on phones and the gallery on wide screens.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val scene = Scene.from(intent.getStringExtra(EXTRA_SCENE))
        setContent {
            SampleTheme(dark = isSystemInDarkTheme()) {
                // The Surface makes text default to onBackground, so it stays readable in dark mode.
                Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                    SampleApp(scene = scene)
                }
            }
        }
    }

    companion object {
        const val EXTRA_SCENE = "scene"
    }
}
