package io.github.rajumark.hoverfly.moji

import io.github.rajumark.hoverfly.moji.internal.Featurizer
import io.github.rajumark.hoverfly.moji.internal.Network
import io.github.rajumark.hoverfly.moji.internal.SentencePiece
import io.github.rajumark.hoverfly.moji.internal.readModelFile
import kotlin.jvm.JvmOverloads

/**
 * On-device emoji suggestions for a piece of text, in 22+ languages.
 *
 * ```
 * Moji().use { moji ->
 *     moji.suggestions("Pay my bills")   // [💰, 🧾, 💳, ...]
 * }
 * ```
 *
 * Everything runs locally on Android, iOS, macOS, the JVM and the web: the ~5 MB model ships
 * inside the library, there is no network, no permission and no dependency. Creating an
 * instance reads the model (tens of ms), so create it off the main thread where the platform
 * has one and keep it around; [suggestions] takes well under a millisecond and is safe to
 * call from several threads.
 */
public class Moji internal constructor(open: (String) -> ByteArray) : AutoCloseable {

    /** Loads the model bundled in the library. */
    public constructor() : this(::readModelFile)

    private class Entry(val emoji: String, val name: String, val skin: Boolean)

    private var network: Network? = Network(open("moji.bin"))
    private val featurizer = Featurizer(SentencePiece(open("spm_pieces.tsv").decodeToString()))
    private val vocab: List<Entry> = open("vocab.tsv").decodeToString().lineSequence()
        .filter { it.isNotEmpty() }.map { it.split('\t') }.map { Entry(it[0], it[1], it[2] == "1") }.toList()

    init {
        check(vocab.size == requireNotNull(network).nEmoji) { "vocab.tsv does not match moji.bin" }
        // The first calls run interpreted; pay that here (off the UI thread) instead of on the first keystroke.
        repeat(WARM_UP) { probabilities("warm up the model $it") }
    }

    /**
     * The emoji that best fit [text], best first.
     *
     * @param limit how many suggestions to return (1..1000).
     * @param skinTone applied to the emoji that support one; `null` keeps the default yellow.
     */
    @JvmOverloads
    public fun suggestions(text: String, limit: Int = 8, skinTone: SkinTone? = null): List<MojiSuggestion> {
        require(limit > 0) { "limit must be positive" }
        val p = probabilities(text)
        return topK(p, minOf(limit, p.size)).map { i ->
            val e = vocab[i]
            MojiSuggestion(
                emoji = if (e.skin && skinTone != null) skinTone.applyTo(e.emoji) else e.emoji,
                name = e.name,
                confidence = p[i],
                supportsSkinTone = e.skin,
            )
        }
    }

    /** Releases the model (about 12 MB of heap). The instance cannot be used afterwards. */
    override fun close() {
        network = null
    }

    internal fun probabilities(text: String): FloatArray {
        val net = checkNotNull(network) { "Moji is closed" }
        val f = featurizer.featurize(text)
        return net.probs(f.tokIds, f.gramIds)
    }

    internal companion object {
        const val WARM_UP = 20

        /** Indices of the k largest values, best first (insertion into a small array; k << n). */
        fun topK(p: FloatArray, k: Int): List<Int> {
            val idx = IntArray(k) { -1 }
            val v = FloatArray(k) { Float.NEGATIVE_INFINITY }
            for (i in p.indices) {
                val x = p[i]
                if (x <= v[k - 1]) continue
                var j = k - 1
                while (j > 0 && v[j - 1] < x) { v[j] = v[j - 1]; idx[j] = idx[j - 1]; j-- }
                v[j] = x; idx[j] = i
            }
            return idx.filter { it >= 0 }
        }
    }
}
