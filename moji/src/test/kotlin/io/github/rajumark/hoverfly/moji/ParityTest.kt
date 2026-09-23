package io.github.rajumark.hoverfly.moji

import io.github.rajumark.hoverfly.moji.internal.Featurizer
import io.github.rajumark.hoverfly.moji.internal.SentencePiece
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs

/**
 * Checks the Kotlin port against Python on testvectors.tsv (written by
 * moji/scripts/export_kotlin.py): identical token and n-gram ids, and the same top-5
 * emoji with the same probabilities as the reference model in PyTorch.
 */
class ParityTest {
    private class Vector(val text: String, val tok: List<Int>, val grams: List<Int>, val top5: List<Int>, val p5: List<Float>)

    private fun ints(s: String) = if (s.isEmpty()) emptyList() else s.split(',').map { it.toInt() }

    private val vectors = javaClass.getResourceAsStream("/testvectors.tsv")!!.bufferedReader().readLines()
        .map { it.split('\t') }
        .map { Vector(it[0], ints(it[1]), ints(it[2]), ints(it[3]), it[4].split(',').map(String::toFloat)) }

    @Test
    fun featurizerMatchesPython() {
        val f = Featurizer(File("$ASSETS/spm_pieces.tsv").inputStream().use { SentencePiece(it) })
        var bad = 0
        for (v in vectors) {
            val got = f.featurize(v.text)
            if (got.tokIds.toList() != v.tok || got.gramIds.toList() != v.grams) {
                bad++
                println("MISMATCH: ${v.text}")
            }
        }
        println("featurizer: ${vectors.size - bad}/${vectors.size} identical")
        assertEquals(0, bad)
    }

    @Test
    fun modelMatchesPython() {
        val moji = testMoji()
        var top1 = 0
        var top5 = 0
        var maxDiff = 0f
        for (v in vectors) {
            val p = moji.probabilities(v.text)
            val got = Moji.topK(p, 5)
            if (got[0] == v.top5[0]) top1++
            if (got == v.top5) top5++
            for ((i, e) in v.top5.withIndex()) maxDiff = maxOf(maxDiff, abs(p[e] - v.p5[i]))
        }
        val n = vectors.size
        println("model: top-1 $top1/$n, identical top-5 $top5/$n, max |dp| on reference top-5 = $maxDiff")
        assertEquals(n, top1)
        assertTrue("top-5 order differs too often: $top5/$n", top5 >= n - 3) // near-ties may swap
        assertTrue("probabilities drift: $maxDiff", maxDiff < 1e-4f)
    }

    companion object {
        const val ASSETS = "src/main/assets/moji"
        fun testMoji() = Moji { name -> File("$ASSETS/$name").inputStream() }
    }
}
