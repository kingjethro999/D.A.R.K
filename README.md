# D.A.R.K. — Developers' Adaptive Responsive Kernel

A minimalist, **text-only**, **OLED-black** Android launcher built with Kotlin and Jetpack Compose. No icons, no grids, no ads, no Play Store. The home screen is the terminal and the terminal is the home screen — everything is a monospace list you can drive by typing.

```
D.A.R.K. 1.1.1 (build 12)
OLED BLACK · TEXT ONLY · NO ADS · BUILT WITH KOTLIN · COMPOSE
```

---

## Features

- **Text-only home screen** — a pure monospace app list. No icons, no grids, no clutter.
- **Built-in terminal** — every command runs from the home screen. Fuzzy app launch, flashlight, logcat, GitHub stats, fitness, and more (full table below).
- **Ask D.A.R.K.** — `ask <anything>` fires a live web search (Firecrawl) and summarizes the answer inline (Groq LLM). Keys are embedded at build time via `secrets.properties`.
- **Find any file** — `find <query>` walks your device storage and lists every path that matches by name.
- **Focus mode / vault** — `lock`, `unlock`, and `vault` gate your distracting apps behind a PIN. `hide <app>` removes an app from the list entirely.
- **Long-press superpowers** — hold an app for its quick menu: hide it, share it, jump to App Info, uninstall it, or launch it.
- **Gesture driven** — double-tap to lock, triple-tap for hidden apps, multi-finger swipes for anything you bind.
- **Fuzzy app search** — a few letters filter the whole list; multi-token search across names and packages.
- **Natural language** — `my name is king jethro` is understood, not dumped to the shell. Typos get git-style suggestions.
- **Fitness at a glance** — daily steps, weekly workouts and sprint averages from the motion sensor (`stats`).
- **Now playing** — the current track scrolls across the top via notification access.
- **GitHub at a glance** — `git refresh` pulls your repo stats onto the home screen.
- **Instant rendering** — every frame is cheap; the home screen never lags.
- **OLED black** — pure black pixels for true blacks and near-zero battery draw.

---

## Terminal commands

| Command | What it does |
| --- | --- |
| `source <app> [dual]` | Fuzzy-launch an installed app (hidden apps require the PIN) |
| `open / play / start <app>` | Natural-language app launch |
| `nox on\|off` | Toggle the flashlight |
| `on\|off wifi` | Open the Wi-Fi panel / toggle |
| `on bluetooth` | Request Bluetooth |
| `uuid` | Generate and copy a UUIDv4 |
| `b64 encode\|decode <text>` | Base64 round-trip |
| `json format <json>` | Pretty-print JSON |
| `log <type> [v1] [v2]` | Log a workout (e.g. `log sprint 100m 11.2s`) |
| `stats` | Weekly fitness summary |
| `git refresh` | Pull GitHub stats (token set in Settings) |
| `lock [app]` | Focus mode ON; with an app, adds it to the distract list |
| `unlock [app]` | Focus mode OFF; with an app, removes it from the distract list |
| `vault lock\|unlock [app]` | Focus mode + the AES file vault |
| `hide <app>` | Hide an app (asks for the hide PIN) |
| `ask <question>` | Live web search (Firecrawl) + LLM answer (Groq) |
| `find <query>` | Search every file on the device by name |
| `logcat [-t N]` | Dump the system log |
| `version` | Show build info |
| `echo / date / whoami / help / clear` | Shell basics |
| *anything else* | Falls through to the Linux shell, or gets a friendly hint |

---

## Building the Android app

Requirements: JDK 17, Android SDK (target 35).

```bash
./gradlew :app:assembleRelease
```

Every assemble auto-increments the version in `version.properties` (patch → minor rollover) and the release APK lands in `app/build/outputs/apk/release/app-release.apk`.

### Embedding the `ask` API keys (optional)

`ask` works only when real keys are embedded at build time. Copy the example file and fill in your own keys:

```bash
cp secrets.properties.example secrets.properties
# edit secrets.properties with your GROQ_API_KEY and FIRECRAWL_API_KEY
```

`secrets.properties` is **gitignored** — never commit live keys. Without it, `ask` reports that it is offline rather than failing.

---

## Website

The marketing/landing site lives in `web/` (Next.js, Tailwind). The interactive terminal demo mirrors the real commands (including `ask` and `find`).

```bash
cd web
pnpm install
pnpm dev          # local dev
pnpm build && pnpm start   # production on :3000
```

Environment keys for the site's author-research endpoint go in `web/.env` (gitignored).

---

## Project structure

```
app/                      Android app (Kotlin + Jetpack Compose)
  src/main/java/com/dark/launcher/
    terminal/CommandParser.kt   terminal command dispatch + deps
    data/repo/                  AppRepository, Fitness, GitHub, Vault, Ask (Firecrawl+Groq)
    util/FileFinder.kt          device-wide file search for `find`
    ui/                         Compose screens: home, terminal, settings, pin dialog
  build.gradle.kts              versioning + BuildConfig secrets wiring
web/                      Landing site (Next.js) + interactive terminal demo
version.properties        auto-bumped on each assemble
secrets.properties.example    template for the `ask` keys
```

---

## Releasing

```bash
./gradlew :app:assembleRelease -q
# version bumps itself; then:
git add version.properties app/build/outputs/apk/release/app-release.apk
git commit
git tag v<version>
git push && git push --tags
gh release create v<version> app/build/outputs/apk/release/app-release.apk
```

Latest release: [github.com/kingjethro999/D.A.R.K/releases](https://github.com/kingjethro999/D.A.R.K/releases)
