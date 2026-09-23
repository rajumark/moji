package io.github.rajumark.hoverfly.moji

/**
 * One suggested emoji.
 *
 * @property emoji the emoji to insert, with the requested [SkinTone] already applied when it supports one.
 * @property name the Unicode (CLDR) name, e.g. "money bag". Handy for accessibility labels.
 * @property confidence the model probability, 0..1. All suggestions for one text sum to at most 1.
 * @property supportsSkinTone true for people and hand emoji that accept a skin-tone modifier.
 */
public data class MojiSuggestion(
    val emoji: String,
    val name: String,
    val confidence: Float,
    val supportsSkinTone: Boolean,
)
