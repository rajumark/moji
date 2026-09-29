package io.github.rajumark.hoverfly.moji.sample

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.rajumark.hoverfly.moji.Moji
import io.github.rajumark.hoverfly.moji.SkinTone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import kotlin.time.TimeSource

private val examples = listOf(
    "Pay my bills", "Dentist appointment tomorrow", "Happy birthday!", "Walk the dog",
    "犬の散歩", "Feliz cumpleaños", "Gym at 6am", "Buy groceries and milk",
)

/** The whole demo: type text, see Moji's suggestions. [platform] is shown so screenshots say where they ran. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun App(platform: String) {
    MaterialTheme(colorScheme = lightColorScheme()) {
        Surface(Modifier.fillMaxSize()) {
            // Loading reads the ~5 MB model: do it off the main thread, once.
            val moji by produceState<Moji?>(null) { value = withContext(Dispatchers.Default) { Moji() } }
            var text by remember { mutableStateOf(examples[0]) }
            var tone by remember { mutableStateOf<SkinTone?>(null) }

            Column(
                Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Moji", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "Kotlin Multiplatform · $platform · io.github.rajumark:moji:$MOJI_VERSION",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Type a task or a message") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (e in examples) SuggestionChip(onClick = { text = e }, label = { Text(e) })
                }

                val m = moji
                if (m == null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator()
                        Text("Loading the model…")
                    }
                    return@Column
                }

                val mark = TimeSource.Monotonic.markNow()
                val suggestions = remember(text, tone, m) { m.suggestions(text, limit = 8, skinTone = tone) }
                val ms = remember(text, tone, m) { mark.elapsedNow().inWholeMicroseconds / 1000.0 }

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Suggestions", style = MaterialTheme.typography.titleMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (s in suggestions) {
                                Text(s.emoji, fontSize = 34.sp, modifier = Modifier.clickable { text = text.trimEnd() + " " + s.emoji })
                            }
                        }
                        Text(
                            "$ms ms · on device · no network · tap an emoji to add it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Text("Skin tone", style = MaterialTheme.typography.titleMedium)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = tone == null, onClick = { tone = null }, label = { Text("👋") })
                    for (t in SkinTone.entries) {
                        FilterChip(selected = tone == t, onClick = { tone = t }, label = { Text(t.applyTo("👋")) })
                    }
                }

                Text("Why these", style = MaterialTheme.typography.titleMedium)
                val top = suggestions.firstOrNull()?.confidence ?: 1f
                for (s in suggestions) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(s.emoji, fontSize = 24.sp)
                        Column(Modifier.weight(1f)) {
                            Text(s.name.replaceFirstChar { it.uppercase() })
                            LinearProgressIndicator(progress = { s.confidence / top }, modifier = Modifier.fillMaxWidth())
                        }
                        Text("${(s.confidence * 1000).roundToInt() / 10.0}%", modifier = Modifier.width(56.dp))
                    }
                }
            }
        }
    }
}

const val MOJI_VERSION = "2.0.0"
