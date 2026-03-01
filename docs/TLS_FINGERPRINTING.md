# TLS fingerprinting (JA3) — simple guide

## What’s going on?

Some websites check **how** your app connects (not just the URL or headers). That check is called **TLS fingerprinting** (or JA3).

- The **Go and Node** tls-clients can **pretend to be a real browser** (Chrome, Firefox, etc.) so the site sees a normal browser fingerprint.
- This **Kotlin library**, when you use it **without** the extra native setup, uses the normal Android/Java way to connect. So the site will see “an app”, not “Chrome” or “Firefox”.

So: **same API**, but **real “browser” fingerprint only if you add the native part** below.

---

## Option 1: Use the real browser fingerprint on your phone/app (native)

You get the **same fingerprint** as the Go/Node clients by **using their code** inside your app.

**In short:**

1. Turn the **Go tls-client** into a small library file (`.so` on Android).
2. Use the **JNI wrapper** we give you so Kotlin can call that library.
3. Put both files in your app and load them when the app starts.
4. Create the client with `TlsClient(NativeTlsEngine())` and use it as usual.

Then your requests use the Go code and look like a real browser (real JA3).

---

## Option 2: Use a server instead of the phone (no native code)

If you **don’t** want to put Go/native code on the device:

- Run the **Go or Node** tls-client on **your server**.
- Your app only talks to **your server** (send URL, headers, body; get back the response).
- The server does the HTTPS request with the real browser fingerprint and sends the result back.

So the **fingerprint is on the server**; the phone just talks to you. No native libraries on the device.

---

## Option 3: Only Kotlin/Java, no Go (no real JA3 today)

To do real JA3 **only** in Kotlin/Java you’d need a special TLS stack that lets you control every detail of the handshake. Nothing like that is ready to use today. So if you need a real browser fingerprint, use **Option 1** or **Option 2**.

---

## Quick summary

| What you want | What to do |
|---------------|------------|
| Same API as Go/Node | Use this Kotlin library (with or without native). |
| Real browser fingerprint **on the device** (e.g. Android) | Option 1: add the Go library + JNI wrapper (see jni/README.md). |
| Real browser fingerprint **without** native on device | Option 2: run Go/Node on a server; app calls your server. |

If the fingerprint **has to** match the Go/Node clients, use **Option 1** (native) or **Option 2** (server).
