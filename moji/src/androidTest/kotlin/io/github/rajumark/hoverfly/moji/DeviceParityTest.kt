package io.github.rajumark.hoverfly.moji

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

/**
 * Same check as the JVM ParityTest, on a real device: Android's ICU-backed Unicode tables
 * (NFKC, lowercase, character types) must give Python's features and top-5 on every vector.
 */
@RunWith(AndroidJUnit4::class)
class DeviceParityTest {
    @Test
    fun matchesPythonOnDevice() {
        val inst = InstrumentationRegistry.getInstrumentation()
        val lines = inst.context.assets.open("testvectors.tsv").bufferedReader().readLines()
        val t0 = System.nanoTime()
        val moji = Moji(inst.targetContext)
        val loadMs = (System.nanoTime() - t0) / 1e6
        var same = 0
        var maxDiff = 0f
        for (line in lines) {
            val c = line.split('\t')
            val want = c[3].split(',').map { it.toInt() }
            val p5 = c[4].split(',').map { it.toFloat() }
            val p = moji.probabilities(c[0])
            if (Moji.topK(p, 5) == want) same++ else println("DIFF: ${c[0]}")
            for ((i, e) in want.withIndex()) maxDiff = maxOf(maxDiff, abs(p[e] - p5[i]))
        }
        val texts = lines.map { it.substringBefore('\t') }
        repeat(300) { moji.suggestions(texts[it % texts.size]) }
        val n = 3000
        val s0 = System.nanoTime()
        repeat(n) { moji.suggestions(texts[it % texts.size]) }
        val ms = (System.nanoTime() - s0) / 1e6 / n
        val f = io.github.rajumark.hoverfly.moji.internal.Featurizer(inst.targetContext.assets.open("moji/spm_pieces.tsv").use { io.github.rajumark.hoverfly.moji.internal.SentencePiece(it) })
        val net = inst.targetContext.assets.open("moji/moji.bin").use { io.github.rajumark.hoverfly.moji.internal.Network(it) }
        val feats = texts.map { f.featurize(it) }
        repeat(1000) { f.featurize(texts[it % texts.size]) }
        val f0 = System.nanoTime()
        repeat(n) { f.featurize(texts[it % texts.size]) }
        val fMs = (System.nanoTime() - f0) / 1e6 / n
        repeat(1000) { val x = feats[it % feats.size]; net.probs(x.tokIds, x.gramIds) }
        val n0 = System.nanoTime()
        repeat(n) { val x = feats[it % feats.size]; net.probs(x.tokIds, x.gramIds) }
        val nMs = (System.nanoTime() - n0) / 1e6 / n
        println("MOJI_BREAKDOWN featurize=${"%.3f".format(fMs)}ms network=${"%.3f".format(nMs)}ms")
        println("MOJI_DEVICE top5 $same/${lines.size} maxDiff=$maxDiff load=${"%.0f".format(loadMs)}ms latency=${"%.3f".format(ms)}ms")
        assertEquals(lines.size, same)
    }
}
