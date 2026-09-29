package io.github.rajumark.hoverfly.moji.internal

internal actual fun nfkc(s: String): String = s.asDynamic().normalize("NFKC") as String
