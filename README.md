# wantd. — tap a tag in store, decide later, let AI compare

Kotlin Multiplatform hackathon project. Shoppers tap an NFC tag or scan a QR code on any piece in a
physical store, and the piece lands in their wishlist with price, material, brand and the exact store
location. They keep browsing across stores, then let a Koog-powered AI assistant compare two saved
pieces using colour, style/fit, occasion and shopping priority before deciding what to buy. Merchants get a live
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
- An OpenAI API key for AI answers, stored as `OPENAI_API_KEY` in the repo-root `.env`
  or server environment. Missing keys and API failures return an error; there are no canned answers.

For hosting, phone scanning and public QR links, see [the demo deployment guide](DEMO_DEPLOYMENT.md).

## Run (demo setup, one process)

```powershell
# 1. Build both web apps (first build downloads the Wasm toolchain; ~3 min)
.\gradlew.bat :buyerApp:wasmJsBrowserDistribution :merchantApp:wasmJsBrowserDistribution

# 2. Start the server. It serves the API, both web apps and the QR sheet on :8080
# Set OPENAI_API_KEY in the repo-root .env first (the key stays on the server).
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
| `OPENAI_API_KEY`  | –                                | Required for AI answers. Absent → HTTP 503                      |
| `OPENAI_MODEL`    | `gpt-5.4-mini` (then fallbacks)  | Vision-capable model id, e.g. `gpt-4o`, `gpt-4.1`, `gpt-5-mini` |
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
3. **Wishlist** – tap **Compare with AI**, select exactly two pieces, then tap the compare pill.
4. **Compare** – answer colour, style/fit, occasion and what matters most (or choose **No preference**),
   then **Get my recommendation**. The assistant names one pick and gives one or two short sentences
   of reasoning using those answers and the product facts, including price. Changing an answer clears
   the previous result. Product photos are sent automatically; no shopper photo is needed.
5. **Assistant** – "Ask a follow-up" opens the chat. The Koog agent has tools that read the real
   cart, wishlist and full catalogue, can recommend unsaved items to go with the cart, and can add to cart ("add the jacket to my cart").
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
- `POST /api/ai/compare` `{ uid, articleIds: [id1, id2], preferences: { color, fit, occasion, priority }, language }` → `CompareResult`
- `POST /api/ai/chat` `{ uid, messages[], language }` → `{ reply }`
- `GET /api/images/{file}` · `GET /qr-sheet` · `/` buyer app · `/merchant/` merchant app

The four preference fields are required, with stable values independent of the display language:
`color`: `ANY | NEUTRAL | DARK | LIGHT | BOLD`; `fit`: `ANY | RELAXED | REGULAR | TAILORED | OVERSIZED`;
`occasion`: `ANY | EVERYDAY | WORK | EVENING | TRAVEL`; `priority`: `ANY | PRICE | QUALITY | VERSATILITY | COMFORT`.
Compare rejects duplicate IDs or a selection other than two. Results contain `summary`, `winnerArticleId`,
`recommendation` and `mock`; there is no score breakdown. Rebuild both the buyer app and server together.
Every successful AI response comes from OpenAI, including the suggestion headline. Missing credentials,
failed model calls or invalid responses return HTTP 503. The legacy `mock` flag is always `false`.

## NFC vs QR

A tag carries `{PUBLIC_BASE_URL}/?a=<articleId>` (the app also accepts `#/a/<id>`). For the demo the
carrier is a QR code; writing the same URL into an NFC NDEF URI record works unchanged — Android and
iOS open URLs from NFC tags natively, no app install required.

## Tech

Kotlin 2.4 · Compose Multiplatform 1.12 (Wasm + Desktop) · Ktor 3.6 (server + client) · Koog 1.1
(`AIAgent`, prompt DSL with binary image attachments, tool set) · kotlinx.serialization · ZXing.
