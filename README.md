# TapShop — tap a tag in store, decide later, let AI compare

Kotlin Multiplatform hackathon project. Shoppers tap an NFC tag or scan a QR code on any piece in a
physical store, and the piece lands in their wishlist with price, material, brand and the exact store
location. They keep browsing across stores, then let a Koog-powered AI assistant compare their saved
pieces (optionally against a photo of themselves) before deciding what to buy. Merchants get a live
dashboard of scans, saves, cart adds and reservations per article.

Everything is Kotlin: Compose Multiplatform (Wasm + Desktop) on the front, Ktor + Koog on the back.

## Modules

| Module        | Targets          | What it is                                                                 |
|---------------|------------------|----------------------------------------------------------------------------|
| `shared`      | jvm, wasmJs      | Serializable models, `ApiClient` (Ktor), typed i18n strings (EN/FR/ES/JA/AR) |
| `uiCore`      | jvm, wasmJs      | Apple-style theme (light/dark), components, settings persistence, navigator, platform bridges |
| `buyerApp`    | wasmJs           | Phone web app opened by QR/NFC: Home, Article, Wishlist, Compare (AI), Cart, Assistant, Settings |
| `merchantApp` | wasmJs, jvm      | Dashboard: Overview KPIs + charts, Articles + QR codes, Reservations, Settings (web + desktop) |
| `server`      | jvm              | Ktor 3: in-memory store seeded with 4 pieces, REST API, analytics, ZXing QR, Koog AI, static hosting |

No login anywhere: shoppers are identified by an anonymous device id kept in `localStorage`; the
merchant dashboard opens straight on the Overview.

## Prerequisites

- JDK 21+ (the repo is configured for IntelliJ's bundled JBR 25 in `gradle.properties`
  → `org.gradle.java.home`; change or remove that line if your path differs).
- A modern browser with WasmGC (Chrome 119+, Safari 18.2+, Firefox 120+). The buyer app is meant for a phone.
- Optional: an OpenAI API key for real AI answers. Without it the server runs in **demo mode** with
  realistic canned answers so the presentation never depends on network or quota.

## Run (demo setup, one process)

```powershell
# 1. Build both web apps (first build downloads the Wasm toolchain; ~3 min)
.\gradlew.bat :buyerApp:wasmJsBrowserDistribution :merchantApp:wasmJsBrowserDistribution

# 2. Start the server. It serves the API, both web apps and the QR sheet on :8080
$env:OPENAI_API_KEY = "sk-..."      # optional; omit for demo mode
.\gradlew.bat :server:run
```

The server prints the URLs it detected on your LAN, e.g.

```
Buyer app      : http://192.168.1.20:8080/
Merchant app   : http://192.168.1.20:8080/merchant/
QR sheet       : http://192.168.1.20:8080/qr-sheet
```

Phone and laptop must be on the same Wi-Fi / hotspot. Open the **QR sheet** on the laptop, scan a
code with the phone camera → the buyer app opens directly on that piece.

Desktop merchant app (native window instead of the browser):

```powershell
.\gradlew.bat :merchantApp:run
```

### Environment variables

| Variable          | Default                          | Purpose                                                        |
|-------------------|----------------------------------|----------------------------------------------------------------|
| `PORT`            | `8080`                           | Server port                                                    |
| `OPENAI_API_KEY`  | –                                | Enables Koog + OpenAI. Absent → demo mode                      |
| `OPENAI_MODEL`    | `gpt-5.4-mini` (then fallbacks)  | Vision-capable model id, e.g. `gpt-4o`, `gpt-4.1`, `gpt-5-mini` |
| `AI_MOCK`         | `false`                          | Force demo answers even with a key                             |
| `PUBLIC_BASE_URL` | auto-detected LAN IPv4           | URL baked into QR codes; set it when using a tunnel            |

If the venue Wi-Fi blocks device-to-device traffic, expose the server with a quick tunnel and point
the QR codes at it **before** opening the QR sheet:

```powershell
cloudflared tunnel --url http://localhost:8080      # prints https://xxxx.trycloudflare.com
$env:PUBLIC_BASE_URL = "https://xxxx.trycloudflare.com"; .\gradlew.bat :server:run
```

## Dev loop (hot reload)

```powershell
.\gradlew.bat :server:run                                   # API on :8080 (CORS enabled)
.\gradlew.bat :buyerApp:wasmJsBrowserDevelopmentRun          # buyer on http://localhost:3000
.\gradlew.bat :merchantApp:wasmJsBrowserDevelopmentRun       # merchant on http://localhost:3001
```

Dev servers call the API at `http://<host>:8080` automatically; the served production build uses the
same origin. Deep links work in dev too: `http://localhost:3000/?a=linen-overshirt`.

## Demo script (≈4 minutes)

1. **Merchant** – open `/merchant/` on the laptop. Overview shows 7 days of seeded engagement.
   Articles → tap the share icon on a piece → the QR code + tag URL (this is what an NFC tag would carry).
   "Open printable sheet" shows all four codes.
2. **Scan** – on the phone, scan piece 1. The article page opens full-bleed with the "Scanned at
   Maison Noor" chip, size / colour / quantity, composition and store location. Tap **♥ Save**.
   Repeat for pieces 2–4 (different brands, different malls).
3. **Wishlist** – pieces are grouped by store with their location. Tap **Compare with AI**
   (or **Select** to pick 2–3). The verdict card names a best pick, followed by score bars for
   fit, colour & versatility, material quality, price-to-quality, occasion, with notes.
4. **Photo** – "Add a photo" of yourself / your outfit, ask "which works for a summer wedding?",
   **Compare again**. With a key, OpenAI sees the product photos and yours; in demo mode the
   canned answer still updates.
5. **Assistant** – "Ask a follow-up" opens the chat. The Koog agent has tools that read the real
   wishlist / cart and can add to cart ("add the jacket to my cart").
6. **Cart → Reserve at store** – the reservation code appears on the phone…
7. …and on the **merchant Reservations** tab a few seconds later (dashboard polls every 4 s). The
   Overview KPIs and per-article chart already include the live scans / saves from the demo.
8. **Settings** – flip Dark/Light and switch to العربية to show full RTL, or 日本語 / Français / Español.

## API (Ktor, JSON)

- `GET /api/info` · `GET /api/stores`
- `GET|POST /api/articles`, `GET|PUT|DELETE /api/articles/{id}`, `GET /api/articles/{id}/qr.png?size=`
- `GET|POST|DELETE /api/users/{uid}/wishlist[/{articleId}]`
- `GET|POST /api/users/{uid}/cart`, `PUT|DELETE /api/users/{uid}/cart/{articleId}?quantity=`, `POST /api/users/{uid}/reserve`
- `GET /api/reservations`, `PUT /api/reservations/{id}?status=PICKED_UP`
- `POST /api/events` (`SCAN VIEW WISHLIST_ADD CART_ADD SHARE COMPARE RESERVE`) · `GET /api/analytics/summary`
- `POST /api/ai/compare` `{ uid, articleIds[], userImagesBase64[], question?, language }` → `CompareResult`
- `POST /api/ai/chat` `{ uid, messages[], language }` → `{ reply }`
- `GET /api/images/{file}` · `GET /qr-sheet` · `/` buyer app · `/merchant/` merchant app

## NFC vs QR

A tag carries `{PUBLIC_BASE_URL}/?a=<articleId>` (the app also accepts `#/a/<id>`). For the demo the
carrier is a QR code; writing the same URL into an NFC NDEF URI record works unchanged — Android and
iOS open URLs from NFC tags natively, no app install required.

## Tech

Kotlin 2.4 · Compose Multiplatform 1.12 (Wasm + Desktop) · Ktor 3.6 (server + client) · Koog 1.1
(`AIAgent`, prompt DSL with binary image attachments, tool set) · kotlinx.serialization · ZXing.
