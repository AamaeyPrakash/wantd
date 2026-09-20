# wantd. phone demo

The same Ktor server serves the buyer website (`/`), merchant dashboard (`/merchant/`),
API (`/api/...`) and printable product QR codes (`/qr-sheet`). Deploy these together
as one service. All AI answers require the server's OpenAI key; missing credentials
or failed API calls show an error, never a canned recommendation.

## Recommended: Railway

1. Commit the app, `Dockerfile`, `.dockerignore` and `railway.toml` to your GitHub repo.
   Keep `.env` untracked. The Docker build context also excludes environment files.
2. In Railway, create a project and deploy from that GitHub repository. Use the
   repository root as the service root; Railway detects the Dockerfile there.
3. Add these service variables:

   | Variable | Value |
   | --- | --- |
   | `OPENAI_API_KEY` | Your key, entered privately in Railway Variables |
   | `OPENAI_MODEL` | `gpt-5.4-mini` (optional; this is already the default) |
   | `PORT` | `8080` |

   The key is used at runtime only and is never needed to build the browser apps.
   `AI_MOCK` is no longer used and cannot enable canned answers.
4. Deploy. The first Docker build compiles both Compose/Wasm apps and may take several
   minutes. The builder uses up to a 3 GB Java heap; give the runtime at least 1 GB
   of memory to leave room around its 512 MB heap. Use one replica because the store
   currently lives in memory. Leave serverless/sleeping disabled for the live demo.
5. In **Settings > Networking > Public Networking**, choose **Generate Domain** and
   target port **8080**. You will get an HTTPS address such as
   `https://your-service.up.railway.app`.
6. Set `PUBLIC_BASE_URL` to that exact HTTPS address, with no trailing slash, then
   apply the variable change/redeploy. Do this before printing QR codes.
7. Open `/api/info` on that domain. `publicBaseUrl` should match your HTTPS address,
   `aiEnabled` should be `true`, and `aiMock` must be `false`. This verifies configuration;
   make an actual comparison and a chat request to verify key access/billing too.
8. Test on your phone's mobile data before presenting. You should be able to browse,
   save two pieces, answer the four questions, request a recommendation, add to cart,
   ask what goes with the cart, and reserve items. Confirm the reservation appears
   at `/merchant/` on your laptop.

Railway's Hobby plan currently costs $5/month including $5 of usage; excess usage
is billed separately. OpenAI API usage is separate from hosting. Check current
[pricing](https://docs.railway.com/pricing/plans) before subscribing.
Railway documents [Dockerfile builds](https://docs.railway.com/builds/dockerfiles)
and [public HTTPS domains](https://docs.railway.com/networking/public-networking).

## Generate the QR codes

No external QR generator is needed. Once `PUBLIC_BASE_URL` is correct:

- Open `https://YOUR-DOMAIN/qr-sheet` on your laptop and print the sheet, or show
  the codes on its screen for your phone to scan.
- The merchant dashboard's **Articles > Open printable sheet** opens the same page.
  The share/QR button beside each article shows an individual code.
- Each code opens that product, for example
  `https://YOUR-DOMAIN/?a=linen-overshirt`.
- An individual QR PNG is available at
  `https://YOUR-DOMAIN/api/articles/linen-overshirt/qr.png?size=600`.

Scan two different product codes, save each item, then open Wishlist and select
exactly those two items to compare. Use the same phone browser for each scan so its
anonymous shopper ID and cart are shared. If the camera opens an embedded browser,
choose **Open in Safari/Chrome** consistently. Use an up-to-date browser with WasmGC
support (the existing app requires this).

## Faster temporary option: keep the app on your laptop

For a short rehearsal, you can use a free Cloudflare Quick Tunnel instead of deploying.
Your laptop and both terminal processes must stay running and connected to the Internet.

1. Keep the key in the repo-root `.env` file. In PowerShell at the repo root, set the
   Java runtime for this terminal and build/start the existing app:

   ```powershell
   $env:JAVA_HOME = 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.3\jbr'
   & "$env:JAVA_HOME\bin\java.exe" -jar gradle/wrapper/gradle-wrapper.jar :buyerApp:wasmJsBrowserDistribution :merchantApp:wasmJsBrowserDistribution
   & "$env:JAVA_HOME\bin\java.exe" -jar gradle/wrapper/gradle-wrapper.jar :server:run
   ```

2. Install `cloudflared` using the official Cloudflare instructions. In another terminal:

   ```powershell
   cloudflared tunnel --url http://localhost:8080
   ```

3. Copy the printed `https://...trycloudflare.com` address. Leave the tunnel running.
   Stop only the app server with Ctrl+C, then restart it in its original terminal:

   ```powershell
   $env:PUBLIC_BASE_URL = 'https://YOUR-TUNNEL.trycloudflare.com'
   & "$env:JAVA_HOME\bin\java.exe" -jar gradle/wrapper/gradle-wrapper.jar :server:run
   ```

4. Open `https://YOUR-TUNNEL.trycloudflare.com/qr-sheet` and scan a product code.
   A new tunnel session gets a new address, so update the base URL and regenerate
   printed codes whenever it changes.

See Cloudflare's [Quick Tunnel instructions](https://developers.cloudflare.com/cloudflare-one/networks/connectors/cloudflare-tunnel/do-more-with-tunnels/trycloudflare/).

## Current demo limits

The catalogue, wishlists, carts and reservations use an in-memory store. Restarting,
redeploying or sleeping the server loses changes and reloads the seed data; a disk
volume alone does not change this. Finish deployment before setting up your demo cart.
The merchant dashboard and write APIs also have no authentication. Share the demo
only with your intended audience and stop it after the event; a public launch needs
persistent storage and authenticated merchant access.

The Dockerfile is prepared for Linux hosting, but a local container build requires
Docker. The native server package and web builds can be checked separately with Gradle.
