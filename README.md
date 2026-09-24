# Moji 🙂

By Hoverfly. On-device emoji suggestions for Android. You give it a task or a message and get back the emoji that fit, in 22+ languages.

```kotlin
import io.github.rajumark.hoverfly.moji.Moji

Moji(context).use { moji ->
    moji.suggestions("Pay my bills")   // 💰 💸 🧾 💵 💳 …
}
```

- **No dependencies.** Inference is plain Kotlin. There is no ONNX Runtime, TFLite or native code, so the library adds about 5 MB to an APK.
- **Private and offline.** The model ships inside the AAR. There is no network, no permission and no telemetry.
- **Fast.** About 0.5 ms per suggestion once warm (emulator on Apple silicon) and about 2 ms on a mid-range phone (Moto G57 Power).
- **minSdk 21.** Works from Kotlin and Java.

## Install

Available via [JitPack](https://jitpack.io/#rajumark/moji):

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

// build.gradle.kts
dependencies {
    implementation("com.github.rajumark:moji:v1.1.0")
}
```

## Screenshots

Same model, same device, three languages — suggestions run entirely on-device, no network round trip.

| Hindi | Spanish | French |
|---|---|---|
| ![Hindi example](docs/screenshots/moji-hindi.png) | ![Spanish example](docs/screenshots/moji-spanish.png) | ![French example](docs/screenshots/moji-french.png) |
| "जिम जाना है" | "Partido de fútbol esta noche" | "Réserver un vol pour Paris" |

## Use

```kotlin
import io.github.rajumark.hoverfly.moji.Moji
import io.github.rajumark.hoverfly.moji.SkinTone

val moji = Moji(context)               // loads the model: ~50–200 ms, do it off the main thread, keep one instance

val top = moji.suggestions("Pay my bills")
top.first().emoji                      // "💰"
top.first().name                       // "money bag"
top.first().confidence                 // 0.36

moji.suggestions("Great job thumbs up", limit = 5, skinTone = SkinTone.MEDIUM)   // 👍🏽 👏🏽 …

moji.close()                           // frees the model's heap memory
```

`suggestions()` is thread-safe and fast enough to call on every keystroke.

With coroutines:

```kotlin
val moji = withContext(Dispatchers.Default) { Moji(context) }
```

From Java:

```java
try (Moji moji = new Moji(context)) {
    List<MojiSuggestion> s = moji.suggestions("Pay my bills");
}
```

### API

| | |
|---|---|
| `Moji(context)` | Loads the bundled model. `Closeable`. |
| `suggestions(text, limit = 8, skinTone = null)` | The best emoji first. Returns `List<MojiSuggestion>`. |
| `MojiSuggestion(emoji, name, confidence, supportsSkinTone)` | One result. |
| `SkinTone.LIGHT … DARK` | Applied to the people and hand emoji that support it. `applyTo(emoji)` is also public. |

## Sample app

`sample/` is a Jetpack Compose (Material 3) demo: live suggestions as you type, tap-to-insert, skin tones and a confidence view.

```bash
./gradlew :sample:installDebug
```

## Project layout

```
moji/                 the library (AAR)
  src/main/assets/moji/   moji.bin (int8 weights) · spm_pieces.tsv (tokenizer) · vocab.tsv (1000 emoji)
  src/main/kotlin/io/github/rajumark/hoverfly/moji/          public API: Moji, MojiSuggestion, SkinTone
  src/main/kotlin/io/github/rajumark/hoverfly/moji/internal/ Featurizer, SentencePiece, Network (the model in plain Kotlin)
  src/test/           JVM tests: parity with Python on 443 vectors, API, latency
  src/androidTest/    the same parity check on a real device (Android ICU)
sample/               demo app
```

## Tests

```bash
./gradlew :moji:testDebugUnitTest                        # JVM: parity + API
./gradlew :moji:connectedDebugAndroidTest                # on a connected device/emulator
./gradlew :moji:connectedReleaseAndroidTest -PtestBuildType=release   # realistic device latency
```

The parity tests require identical featurizer ids and an identical top-5 to the Python reference on all 443 vectors (tasks, 22 languages and edge cases). Probabilities match to within 1e-4; the current maximum difference is 3e-6.

## Publishing

See [PUBLISHING.md](PUBLISHING.md).

## Pricing & license

**Free for up to 10,000 monthly active devices.** You don't need an API key, an account or a license file: add the dependency and ship. It works in commercial apps too, with no limit on how often each device runs it.

| | Community | Commercial | Custom models |
|---|---|---|---|
| **Price** | Free | Contact us | Contact us |
| **For** | Products with up to 10,000 monthly active devices per platform | Products above 10,000 monthly active devices on any platform | A model trained for your own language, domain or task |
| **Includes** | Commercial use, unlimited calls, no key or sign-up | One license per product per model, direct support, early access to updates | Designed and trained by Hoverfly, shipped as a plain Kotlin library |

**How devices are counted.** A monthly active device is a device that runs Moji at least once in a calendar month. The limit applies separately to each product, each platform (Android, iOS, web…) and each Hoverfly model. Once a product passes it, you have 30 days to get a commercial license. The library keeps working and never checks in with a server.

**Not allowed** under any tier (unless agreed in writing):

- selling or redistributing Moji or its model on its own, or inside another SDK or library
- extracting, modifying, fine-tuning or retraining the model weights
- using the model or its outputs to train or distill another model
- reverse engineering the model or its file format
- offering it as a hosted API for others

**Custom models.** Hoverfly also designs and trains small, fast on-device models for your needs: moderation, classification, language detection, smart replies and more.

**Contact** for a commercial license or a custom model: [raju348636@gmail.com](mailto:raju348636@gmail.com) or **+91 63533 21951** (call or WhatsApp).

Full terms: [Hoverfly Community License](LICENSE). Versions 1.0.0 and earlier were released under Apache-2.0.
