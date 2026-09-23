package io.github.rajumark.hoverfly.moji

/** Fitzpatrick skin-tone modifiers (U+1F3FB..U+1F3FF). */
public enum class SkinTone(public val modifier: String) {
    LIGHT("🏻"),
    MEDIUM_LIGHT("🏼"),
    MEDIUM("🏽"),
    MEDIUM_DARK("🏾"),
    DARK("🏿");

    /** [emoji] with this modifier inserted after its first code point (dropping a VS16 there). */
    public fun applyTo(emoji: String): String {
        val first = emoji.codePointAt(0)
        var rest = emoji.substring(Character.charCount(first))
        if (rest.startsWith("️")) rest = rest.substring(1)
        return String(Character.toChars(first)) + modifier + rest
    }
}
