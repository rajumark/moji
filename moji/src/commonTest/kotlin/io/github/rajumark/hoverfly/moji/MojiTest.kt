package io.github.rajumark.hoverfly.moji

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.TimeSource

class MojiTest {
    private val moji = ParityTest.testMoji()

    @Test
    fun suggestsSensibleEmoji() {
        val cases = mapOf(
            "Pay my bills" to "💰",        // money bag
            "Happy birthday!" to "🎂",     // birthday cake
            "Walk the dog" to "🐕",        // dog
            "犬の散歩" to "🐕",
        )
        for ((text, expected) in cases) {
            val got = moji.suggestions(text).map { it.emoji }
            println("$text -> ${got.joinToString(" ")}")
            assertTrue(expected in got, "$expected not in suggestions for '$text': $got")
        }
    }

    @Test
    fun limitAndOrdering() {
        val s = moji.suggestions("Dentist appointment", limit = 5)
        assertEquals(5, s.size)
        assertTrue(s.zipWithNext().all { (a, b) -> a.confidence >= b.confidence })
        assertEquals(1000, moji.suggestions("x", limit = 5000).size)
    }

    @Test
    fun emptyAndOddInputsDoNotCrash() {
        for (t in listOf("", "   ", "😀😀", "x".repeat(10_000), " \uD800")) {
            val s = moji.suggestions(t)
            assertEquals(8, s.size)
            assertFalse(s.any { it.confidence.isNaN() })
        }
    }

    @Test
    fun skinTone() {
        assertEquals("👍🏽", SkinTone.MEDIUM.applyTo("👍"))  // thumbs up
        assertEquals("✌🏿", SkinTone.DARK.applyTo("✌️"))            // victory hand, VS16 dropped
        val s = moji.suggestions("Thumbs up, great job", limit = 20, skinTone = SkinTone.DARK)
        val skinnable = s.filter { it.supportsSkinTone }
        assertTrue(skinnable.isNotEmpty(), "expected a skin-tone emoji in $s")
        assertTrue(skinnable.all { it.emoji.contains(SkinTone.DARK.modifier) })
        assertTrue(s.filterNot { it.supportsSkinTone }.none { it.emoji.contains(SkinTone.DARK.modifier) })
    }

    @Test
    fun closedInstanceThrows() {
        val m = Moji()
        m.close()
        assertFailsWith<IllegalStateException> { m.suggestions("hello") }
    }

    @Test
    fun latency() {
        val texts = listOf("Pay my bills", "Dentist appointment tomorrow at 5", "犬の散歩に行く", "Buy groceries and milk")
        repeat(200) { moji.suggestions(texts[it % texts.size]) }
        val n = 1000
        val t0 = TimeSource.Monotonic.markNow()
        repeat(n) { moji.suggestions(texts[it % texts.size]) }
        println("latency: ${(t0.elapsedNow().inWholeMicroseconds / n) / 1000.0} ms / suggestion")
        val l0 = TimeSource.Monotonic.markNow()
        Moji().close()
        println("load: ${l0.elapsedNow().inWholeMilliseconds} ms")
    }
}
