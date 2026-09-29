package io.github.rajumark.hoverfly.moji.sample

import io.github.rajumark.hoverfly.moji.Moji
import io.github.rajumark.hoverfly.moji.SkinTone
import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import kotlin.math.roundToInt
import kotlin.time.TimeSource

/** "Kotlin/JS" or "Kotlin/Wasm". */
expect val runtime: String

private val examples = listOf(
    "Pay my bills", "Dentist appointment tomorrow", "Happy birthday!", "Walk the dog",
    "犬の散歩", "Feliz cumpleaños", "Gym at 6am", "Buy groceries and milk",
)

private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

fun main() {
    fun el(id: String) = document.getElementById(id) as HTMLElement
    val input = document.getElementById("text") as HTMLInputElement
    el("platform").textContent = "Kotlin Multiplatform · $runtime · io.github.rajumark:moji:2.0.0"

    val t0 = TimeSource.Monotonic.markNow()
    val moji = Moji()
    el("load").textContent = "Model loaded in ${t0.elapsedNow().inWholeMilliseconds} ms"
    var tone: SkinTone? = null

    fun render() {
        val mark = TimeSource.Monotonic.markNow()
        val s = moji.suggestions(input.value, limit = 8, skinTone = tone)
        val ms = mark.elapsedNow().inWholeMicroseconds / 1000.0
        el("emoji").innerHTML = s.joinToString("") { "<button class=\"emoji\" data-e=\"${esc(it.emoji)}\">${esc(it.emoji)}</button>" }
        el("timing").textContent = "$ms ms · on device · no network · tap an emoji to add it"
        val top = s.firstOrNull()?.confidence ?: 1f
        el("why").innerHTML = s.joinToString("") {
            val pct = (it.confidence * 1000).roundToInt() / 10.0
            "<div class=\"why\"><span class=\"e\">${esc(it.emoji)}</span><div class=\"bar\"><div>${esc(it.name)}</div>" +
                "<div class=\"track\"><div class=\"fill\" style=\"width:${(it.confidence / top * 100).roundToInt()}%\"></div></div></div>" +
                "<span class=\"pct\">$pct%</span></div>"
        }
        el("emoji").querySelectorAll("button").let { list ->
            for (i in 0 until list.length) {
                val b = list.item(i) as HTMLElement
                b.onclick = { input.value = input.value.trimEnd() + " " + (b.getAttribute("data-e") ?: ""); render(); null }
            }
        }
    }

    el("examples").innerHTML = examples.joinToString("") { "<button class=\"chip\">${esc(it)}</button>" }
    el("examples").querySelectorAll("button").let { list ->
        for (i in 0 until list.length) {
            val b = list.item(i) as HTMLElement
            b.onclick = { input.value = b.textContent ?: ""; render(); null }
        }
    }
    val tones = listOf<SkinTone?>(null) + SkinTone.entries
    el("tones").innerHTML = tones.joinToString("") { t ->
        "<button class=\"chip${if (t == null) " on" else ""}\">${t?.applyTo("👋") ?: "👋"}</button>"
    }
    el("tones").querySelectorAll("button").let { list ->
        for (i in 0 until list.length) {
            val b = list.item(i) as HTMLElement
            b.onclick = {
                tone = tones[i]
                for (j in 0 until list.length) (list.item(j) as HTMLElement).className = if (j == i) "chip on" else "chip"
                render()
                null
            }
        }
    }
    input.oninput = { render(); null }
    input.value = examples[0]
    render()
}
