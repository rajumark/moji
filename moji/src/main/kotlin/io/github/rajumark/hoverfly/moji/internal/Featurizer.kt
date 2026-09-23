package io.github.rajumark.hoverfly.moji.internal

import java.text.Normalizer
import java.util.Locale

/**
 * Kotlin port of moji/features.py. Must produce the exact same ids as Python;
 * FeaturizerParityTest checks it against vectors exported by scripts/export_android.py.
 *
 * Python-semantics notes: strings are walked by code point (not UTF-16 unit), `\w` means
 * a letter or number of any script or "_" (combining marks are NOT word chars), and
 * whitespace follows Python's str.isspace().
 */
internal class Featurizer(private val sp: SentencePiece) {

    /** Model inputs without padding: at most [MAX_TOKENS] token ids and [MAX_GRAMS] n-gram ids. */
    class Features(val tokIds: IntArray, val gramIds: IntArray)

    fun featurize(text: String): Features {
        val norm = normalize(text)
        val toks = sp.encode(norm)
        return Features(if (toks.size > MAX_TOKENS) toks.copyOf(MAX_TOKENS) else toks, gramIds(norm))
    }

    companion object {
        const val MAX_TOKENS = 32
        const val MAX_GRAMS = 128
        const val N_BUCKETS = 1 shl 16

        /** String.codePoints() needs API 24; this works on every API level. */
        fun codePoints(s: String): IntArray {
            val out = IntArray(s.codePointCount(0, s.length))
            var i = 0
            var k = 0
            while (i < s.length) {
                val cp = s.codePointAt(i)
                out[k++] = cp
                i += Character.charCount(cp)
            }
            return out
        }

        fun isPySpace(cp: Int): Boolean =
            Character.isWhitespace(cp) || Character.isSpaceChar(cp) || cp == 0x85

        fun isWordChar(cp: Int): Boolean {
            if (cp == '_'.code) return true
            return when (Character.getType(cp).toByte()) {
                Character.UPPERCASE_LETTER, Character.LOWERCASE_LETTER, Character.TITLECASE_LETTER,
                Character.MODIFIER_LETTER, Character.OTHER_LETTER,
                Character.DECIMAL_DIGIT_NUMBER, Character.LETTER_NUMBER, Character.OTHER_NUMBER -> true
                else -> false
            }
        }

        private fun isSpaceless(cp: Int) =
            cp in 0x0E00..0x0E7F || cp in 0x3040..0x30FF || cp in 0x3400..0x9FFF || cp in 0xAC00..0xD7AF

        /** NFKC, lowercase, drop URLs and @mentions, collapse whitespace. */
        fun normalize(text: String): String {
            var t = Normalizer.normalize(text, Normalizer.Form.NFKC).lowercase(Locale.ROOT)
            t = stripUrls(t)
            t = stripMentions(t)
            val sb = StringBuilder(t.length)
            var pendingSpace = false
            var i = 0
            while (i < t.length) {
                val cp = t.codePointAt(i)
                if (isPySpace(cp)) pendingSpace = true
                else {
                    if (pendingSpace && sb.isNotEmpty()) sb.append(' ')
                    pendingSpace = false
                    sb.appendCodePoint(cp)
                }
                i += Character.charCount(cp)
            }
            return sb.toString()
        }

        /** Same as Python re.sub(r"https?://\S+|www\.\S+", " ", t), without platform regex quirks. */
        fun stripUrls(t: String): String {
            val sb = StringBuilder(t.length)
            var i = 0
            while (i < t.length) {
                val prefix = when {
                    t.startsWith("https://", i) -> 8
                    t.startsWith("http://", i) -> 7
                    t.startsWith("www.", i) -> 4
                    else -> 0
                }
                val end = if (prefix > 0) nonSpaceRunEnd(t, i + prefix) else i + prefix
                if (prefix > 0 && end > i + prefix) {
                    sb.append(' ')
                    i = end
                } else {
                    val cp = t.codePointAt(i)
                    sb.appendCodePoint(cp)
                    i += Character.charCount(cp)
                }
            }
            return sb.toString()
        }

        /** Same as Python re.sub(r"@\w+", " ", t). */
        fun stripMentions(t: String): String {
            val sb = StringBuilder(t.length)
            var i = 0
            while (i < t.length) {
                if (t[i] == '@') {
                    var j = i + 1
                    while (j < t.length) {
                        val cp = t.codePointAt(j)
                        if (!isWordChar(cp)) break
                        j += Character.charCount(cp)
                    }
                    if (j > i + 1) { sb.append(' '); i = j; continue }
                }
                val cp = t.codePointAt(i)
                sb.appendCodePoint(cp)
                i += Character.charCount(cp)
            }
            return sb.toString()
        }

        private fun nonSpaceRunEnd(t: String, from: Int): Int {
            var j = from
            while (j < t.length) {
                val cp = t.codePointAt(j)
                if (isPySpace(cp)) break
                j += Character.charCount(cp)
            }
            return j
        }

        fun fnv1a(s: String): Long {
            var h = 0x811C9DC5L
            for (b in s.toByteArray(Charsets.UTF_8)) {
                h = h xor (b.toLong() and 0xFF)
                h = (h * 0x01000193L) and 0xFFFFFFFFL
            }
            return h
        }

        private fun cpString(cps: IntArray, from: Int, to: Int): String {
            val sb = StringBuilder()
            for (k in from until to) sb.appendCodePoint(cps[k])
            return sb.toString()
        }

        fun lexicalFeatures(norm: String): List<String> {
            val feats = ArrayList<String>()
            val cps = codePoints(norm)
            val words = ArrayList<String>()
            var start = -1
            for (k in 0..cps.size) {
                val w = k < cps.size && isWordChar(cps[k])
                if (w && start < 0) start = k
                if (!w && start >= 0) { words.add(cpString(cps, start, k)); start = -1 }
            }
            for (w in words) feats.add("w:$w")
            for (k in 0 until words.size - 1) feats.add("b:${words[k]} ${words[k + 1]}")
            val padded = intArrayOf(' '.code) + cps + intArrayOf(' '.code)
            for (n in 2..4) {
                for (k in 0..padded.size - n) {
                    var allSpace = true
                    for (j in k until k + n) if (!isPySpace(padded[j])) { allSpace = false; break }
                    if (!allSpace) feats.add(cpString(padded, k, k + n))
                }
            }
            for (cp in cps) if (isSpaceless(cp)) feats.add("c:" + String(Character.toChars(cp)))
            return feats
        }

        fun gramIds(norm: String): IntArray {
            val ids = LinkedHashSet<Int>()
            for (f in lexicalFeatures(norm)) {
                ids.add((fnv1a(f) % (N_BUCKETS - 1) + 1).toInt())
                if (ids.size >= MAX_GRAMS) break
            }
            return ids.toIntArray()
        }
    }
}
