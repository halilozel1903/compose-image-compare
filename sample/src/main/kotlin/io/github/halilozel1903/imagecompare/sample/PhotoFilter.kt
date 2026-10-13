package io.github.halilozel1903.imagecompare.sample

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

/**
 * The edits the sample compares with the original picture. All but [Vignette] are color matrices applied while
 * drawing (`afterColorFilter`); [Vignette] is a composable overlay and shows the composable overload of ImageCompare.
 */
enum class PhotoFilter(val label: String, val matrix: ColorMatrix?) {
    Vivid("Vivid", contrast(saturation(1.5f), 1.12f)),
    Mono("Mono", contrast(saturation(0f), 1.15f)),
    Sepia(
        "Sepia",
        ColorMatrix(
            floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            ),
        ),
    ),
    Cool(
        "Cool",
        ColorMatrix(
            floatArrayOf(
                0.86f, 0f, 0f, 0f, -6f,
                0f, 0.98f, 0f, 0f, 4f,
                0f, 0f, 1.12f, 0f, 18f,
                0f, 0f, 0f, 1f, 0f,
            ),
        ),
    ),
    Faded("Faded", lift(contrast(saturation(0.7f), 0.78f), 18f)),
    Vignette("Vignette", null),
    ;

    val colorFilter: ColorFilter? get() = matrix?.let { ColorFilter.colorMatrix(it) }
}

/** A saturation matrix with the luminance weights of sRGB. */
private fun saturation(s: Float): ColorMatrix {
    val r = 0.213f * (1 - s)
    val g = 0.715f * (1 - s)
    val b = 0.072f * (1 - s)
    return ColorMatrix(
        floatArrayOf(
            r + s, g, b, 0f, 0f,
            r, g + s, b, 0f, 0f,
            r, g, b + s, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        ),
    )
}

/** [m] followed by a contrast change [c] around mid gray. */
private fun contrast(m: ColorMatrix, c: Float): ColorMatrix {
    val v = m.values.copyOf()
    for (row in 0 until 3) {
        for (col in 0 until 4) v[row * 5 + col] *= c
        v[row * 5 + 4] = v[row * 5 + 4] * c + (1 - c) * 128f
    }
    return ColorMatrix(v)
}

/** [m] with its color channels raised by [amount] (0 to 255). */
private fun lift(m: ColorMatrix, amount: Float): ColorMatrix {
    val v = m.values.copyOf()
    for (row in 0 until 3) v[row * 5 + 4] += amount
    return ColorMatrix(v)
}
