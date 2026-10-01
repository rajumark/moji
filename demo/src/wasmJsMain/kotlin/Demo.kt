@file:OptIn(ExperimentalWasmJsInterop::class)

import io.github.rajumark.hoverfly.moji.Moji
import io.github.rajumark.hoverfly.moji.SkinTone
import kotlin.js.ExperimentalWasmJsInterop

private var moji: Moji? = null

private fun json(s: String) = buildString {
    append('"')
    for (c in s) when (c) {
        '"' -> append("\\\""); '\\' -> append("\\\\")
        else -> if (c < ' ') append("\\u").append(c.code.toString(16).padStart(4, '0')) else append(c)
    }
    append('"')
}

/** Loads the bundled model and warms it up. Call once, from the worker. */
@JsExport
fun load() {
    if (moji == null) moji = Moji()
}

/** Suggestions as a JSON array of {emoji, name, confidence}. [tone] is a SkinTone name or "". */
@JsExport
fun suggest(text: String, limit: Int, tone: String): String {
    val m = moji ?: Moji().also { moji = it }
    val skin = SkinTone.entries.firstOrNull { it.name == tone }
    return m.suggestions(text, limit, skin).joinToString(",", "[", "]") {
        "{\"emoji\":${json(it.emoji)},\"name\":${json(it.name)},\"confidence\":${it.confidence}}"
    }
}

fun main() {}
