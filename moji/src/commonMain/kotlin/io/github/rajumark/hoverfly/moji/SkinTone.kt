package io.github.rajumark.hoverfly.moji

import io.github.rajumark.hoverfly.moji.internal.cpAt
import io.github.rajumark.hoverfly.moji.internal.cpLen
import io.github.rajumark.hoverfly.moji.internal.cpToString

/** Fitzpatrick skin-tone modifiers (U+1F3FB..U+1F3FF). */
public enum class SkinTone(public val modifier: String) {
    LIGHT("🏻"),
    MEDIUM_LIGHT("🏼"),
    MEDIUM("🏽"),
    MEDIUM_DARK("🏾"),
    DARK("🏿");

    /** [emoji] with this modifier inserted after its first code point (dropping a VS16 there). */
    public fun applyTo(emoji: String): String {
        val first = emoji.cpAt(0)
        var rest = emoji.substring(cpLen(first))
        if (rest.startsWith("️")) rest = rest.substring(1)
        return cpToString(first) + modifier + rest
    }
}
