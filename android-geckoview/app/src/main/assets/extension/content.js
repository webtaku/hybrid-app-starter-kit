const NATIVE_APP = "browser";
const port = browser.runtime.connectNative(NATIVE_APP);

port.onMessage.addListener((msg) => {
  window.postMessage({ __from: "android", ...msg }, "*");
});

window.addEventListener("message", (event) => {
  const req = event.data;
  if (!req || req.__to !== "android") return;
  port.postMessage(req);
});
