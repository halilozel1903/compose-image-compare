package io.github.halilozel1903.imagecompare.sample

/** Screenshot scenes, picked with the `scene` intent extra. */
enum class Scene(val key: String) {
    /** Mirror lake, original and Vivid side by side. */
    Horizontal("horizontal"),

    /** Autumn hills, original on top and Mono below. */
    Vertical("vertical"),

    /** Quiet fjord zoomed in 2.5x on the boat, original and Mono side by side. */
    Zoomed("zoomed"),

    /** The gallery of all photos and filters (tablets in landscape). */
    Tablet("tablet"),
    ;

    companion object {
        fun from(key: String?): Scene? = entries.firstOrNull { it.key == key }
    }
}
