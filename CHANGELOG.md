# Changelog

## 2.0.0

- Kotlin Multiplatform: Android, JVM desktop, iOS (arm64 device + simulator), macOS arm64,
  JavaScript and WebAssembly, from the same coordinates `io.github.rajumark:moji`.
- New constructor `Moji()`: the model ships inside the library on every platform, so no
  `Context` is needed. `Moji(context)` still compiles on Android (deprecated).
- Same model and same results as 1.1.0; parity with the Python reference is tested on every target.
- The sample is now a Compose Multiplatform app (Android, desktop, iOS) plus a web page (JS and Wasm).

## 1.1.0

- License changed to the Hoverfly Community License: free for products with up to 10,000 monthly
  active devices per platform, commercial license above that. No code or model changes.
- 1.0.0 and earlier stay under Apache-2.0.

## 1.0.0 (unreleased)

- First version: `Moji(context).suggestions(text, limit, skinTone)`.
- Model v3 (dev hit@5 0.79), 1000 emoji, 22+ languages, int8 weights (5 MB).
- Pure Kotlin inference with no dependencies. minSdk 21.
