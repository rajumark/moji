// Runs the Moji Kotlin/Wasm build off the main thread, so the page never waits on the model.
// Messages in: {id, text, limit, tone}. Messages out: {type: "ready"|"error"|"result", ...}.
let moji;

try {
  moji = await import("./wasm/moji-demo.mjs");
  moji.load();
  postMessage({ type: "ready" });
} catch (e) {
  moji = null;
  postMessage({ type: "error", message: String(e && e.message || e) });
}

onmessage = (event) => {
  if (!moji) return;
  const { id, text, limit, tone } = event.data;
  const t = performance.now();
  const suggestions = JSON.parse(moji.suggest(text, limit, tone || ""));
  postMessage({ type: "result", id, suggestions, ms: performance.now() - t });
};
