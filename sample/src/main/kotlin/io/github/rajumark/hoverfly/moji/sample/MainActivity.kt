package io.github.rajumark.hoverfly.moji.sample

import io.github.rajumark.hoverfly.moji.Moji
import io.github.rajumark.hoverfly.moji.MojiSuggestion
import io.github.rajumark.hoverfly.moji.SkinTone
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { MojiTheme { MojiScreen() } }
    }
}

private val EXAMPLES = listOf(
    "Pay my bills", "Dentist appointment tomorrow", "Happy birthday! 🎉", "Walk the dog",
    "犬の散歩", "Réserver un vol pour Paris", "जिम जाना है", "Partido de fútbol esta noche",
)

/** Result of one inference, with its wall-clock time. */
private class Result(val suggestions: List<MojiSuggestion>, val micros: Long)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MojiScreen() {
    val context = LocalContext.current.applicationContext

    // Loading reads ~5 MB: do it once, off the main thread.
    val moji by produceState<Moji?>(null) {
        value = withContext(Dispatchers.Default) { Moji(context) }
        awaitDispose { value?.close() }
    }
    var field by remember { mutableStateOf(TextFieldValue(EXAMPLES[0], TextRange(EXAMPLES[0].length))) }
    var skinTone by remember { mutableStateOf<SkinTone?>(null) }

    val result by produceState<Result?>(null, moji, field.text, skinTone) {
        val m = moji ?: return@produceState
        value = withContext(Dispatchers.Default) {
            val t0 = System.nanoTime()
            val s = m.suggestions(field.text, limit = 8, skinTone = skinTone)
            Result(s, (System.nanoTime() - t0) / 1000)
        }
    }

    fun insert(emoji: String) {
        val t = field.text
        val sep = if (t.isEmpty() || t.endsWith(" ")) "" else " "
        val next = "$t$sep$emoji"
        field = TextFieldValue(next, TextRange(next.length))
    }

    val scroll = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Moji")
                        Text(
                            "On-device emoji suggestions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().imePadding(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                OutlinedTextField(
                    value = field,
                    onValueChange = { field = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Type a task or a message") },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    minLines = 2,
                    trailingIcon = {
                        if (field.text.isNotEmpty()) {
                            IconButton(onClick = { field = TextFieldValue("") }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(EXAMPLES) { ex ->
                        SuggestionChip(
                            onClick = { field = TextFieldValue(ex, TextRange(ex.length)) },
                            label = { Text(ex) },
                        )
                    }
                }
            }
            item {
                SuggestionsCard(moji == null, result, onPick = ::insert)
            }
            item {
                SkinTones(skinTone) { skinTone = it }
            }
            result?.let { r ->
                if (r.suggestions.isNotEmpty()) {
                    item { ConfidenceCard(r.suggestions) }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuggestionsCard(loading: Boolean, result: Result?, onPick: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Suggestions",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.height(8.dp))
            if (loading || result == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Loading model…", style = MaterialTheme.typography.bodyMedium)
                }
                return@Column
            }
            AnimatedContent(
                targetState = result.suggestions.map { it.emoji },
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "emoji",
            ) { emoji ->
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    emoji.forEach { e ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Transparent,
                            modifier = Modifier.clickable { onPick(e) },
                        ) {
                            Text(e, fontSize = 34.sp, modifier = Modifier.padding(6.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                String.format(Locale.US, "%.2f ms · on device · no network · tap an emoji to add it", result.micros / 1000.0),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
            )
        }
    }
}

@Composable
private fun SkinTones(selected: SkinTone?, onSelect: (SkinTone?) -> Unit) {
    Column {
        Text("Skin tone", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text("👋") })
            }
            items(SkinTone.entries) { tone ->
                FilterChip(
                    selected = selected == tone,
                    onClick = { onSelect(tone) },
                    label = { Text(tone.applyTo("👋")) },
                )
            }
        }
    }
}

@Composable
private fun ConfidenceCard(suggestions: List<MojiSuggestion>) {
    val max = suggestions.first().confidence.coerceAtLeast(1e-6f)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Text(
                "Why these",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            suggestions.forEachIndexed { i, s ->
                if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                ConfidenceRow(s, s.confidence / max)
            }
        }
    }
}

@Composable
private fun ConfidenceRow(s: MojiSuggestion, fraction: Float) {
    val bar by animateFloatAsState(fraction, label = "bar")
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(s.emoji, fontSize = 24.sp, modifier = Modifier.width(44.dp))
        Column(Modifier.weight(1f)) {
            Text(
                s.name.replaceFirstChar { it.titlecase(Locale.getDefault()) },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { bar },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                drawStopIndicator = {},
            )
        }
        Box(Modifier.width(64.dp), contentAlignment = Alignment.CenterEnd) {
            Text(
                String.format(Locale.US, "%.1f%%", s.confidence * 100),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun MojiTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= 31 -> if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = colors, content = content)
}

@Preview(showBackground = true)
@Composable
private fun PreviewConfidence() {
    MojiTheme {
        ConfidenceCard(
            listOf(
                MojiSuggestion("💰", "money bag", 0.35f, false),
                MojiSuggestion("💸", "money with wings", 0.18f, false),
            ),
        )
    }
}
