package io.github.halilozel1903.imagecompare

/** How the two images share the space. */
public enum class CompareOrientation {
    /**
     * Side by side: the before image on the start side, the after image on the end side, and a vertical divider that
     * moves horizontally. In a right-to-left layout the before image is on the right.
     */
    Horizontal,

    /** Top and bottom: the before image on top, the after image below, and a horizontal divider that moves vertically. */
    Vertical,
}

/** How the after image is revealed. */
public enum class CompareMode {
    /** A divider splits the two images; the position is where the divider is. */
    Split,

    /**
     * The after image is drawn over the whole before image with an opacity of `1 - position`, and the handle works as
     * a fader along the same axis.
     */
    Fade,
}

/**
 * Where the before and after labels sit across the comparison axis. For a [CompareOrientation.Horizontal] comparison
 * [Start] is the top edge and [End] the bottom edge; for a [CompareOrientation.Vertical] one [Start] is the start
 * edge and [End] the end edge. Along the axis, the before label is always at the start and the after label at the
 * end.
 */
public enum class LabelPlacement {
    Start,
    Center,
    End,
}
