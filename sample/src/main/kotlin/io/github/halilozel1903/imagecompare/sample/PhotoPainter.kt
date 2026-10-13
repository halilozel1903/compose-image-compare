package io.github.halilozel1903.imagecompare.sample

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource

/** The bundled JPEG of this demo picture. */
@Composable
fun DemoPhoto.painter(): Painter = painterResource(
    when (this) {
        DemoPhoto.MirrorLake -> R.drawable.mirror_lake
        DemoPhoto.QuietFjord -> R.drawable.quiet_fjord
        DemoPhoto.AutumnHills -> R.drawable.autumn_hills
    },
)
