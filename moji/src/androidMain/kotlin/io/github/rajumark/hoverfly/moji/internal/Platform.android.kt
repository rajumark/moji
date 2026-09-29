package io.github.rajumark.hoverfly.moji.internal

import io.github.rajumark.hoverfly.moji.Moji
import java.text.Normalizer

internal actual fun nfkc(s: String): String = Normalizer.normalize(s, Normalizer.Form.NFKC)

// The model ships as Java resources in the jar/AAR (src/modelData), so no Context or copy is needed.
internal actual fun readModelFile(name: String): ByteArray =
    Moji::class.java.getResourceAsStream("/io/github/rajumark/hoverfly/moji/model/$name")?.use { it.readBytes() }
        ?: error("Moji model file $name is missing from the library jar")
