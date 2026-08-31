import { APP } from "@/lib/app";

export interface DocBlock {
  type: "p" | "code" | "list" | "table" | "warn" | "heading";
  text?: string;
  items?: string[];
  rows?: { cols: string[]; isHeader?: boolean }[];
  level?: 2 | 3;
}

export interface Doc {
  slug: string;
  title: string;
  nav: string;
  group: string;
  description: string;
  blocks: DocBlock[];
}

export const DOC_GROUPS = ["START", "TERMINAL", "SYSTEM"] as const;

export const DOCS: Doc[] = [
  {
    slug: "installation",
    title: "Installation",
    nav: "Install & set default",
    group: "START",
    description: "Get the APK on your phone and make D.A.R.K. your home.",
    blocks: [
      {
        type: "p",
        text: "D.A.R.K. is a side-loaded APK. There is no Play Store listing, no ads, and no tracking. Install it like any unsigned-of-store app.",
      },
      {
        type: "heading",
        level: 2,
        text: "1. Download the APK",
      },
      {
        type: "list",
        items: [
          "Open the Download page and tap DOWNLOAD APK.",
          `Your browser saves ${APP.downloadFileName} (2.8 MB) to your downloads.`,
          "If Android blocks it, allow 'install unknown apps' from your browser when prompted.",
        ],
      },
      {
        type: "heading",
        level: 2,
        text: "2. Open and install",
      },
      {
        type: "list",
        items: [
          "Tap the downloaded file in your notification or Files app.",
          "Confirm the installer screen. D.A.R.K. needs no special permissions to install.",
          "Minimum supported OS is Android 8.0 (API 26); target SDK is 35.",
        ],
      },
      {
        type: "heading",
        level: 2,
        text: "3. Make it your home",
      },
      {
        type: "list",
        items: [
          "Press your phone's Home button.",
          "Android asks which app to use as home — pick D.A.R.K. and select Always.",
          "If you skipped the prompt, open D.A.R.K., go to Settings, and tap 'Set as default launcher'. On Android 10+ this routes through the system Home role dialog.",
        ],
      },
      {
        type: "warn",
        text: "Your old launcher stays installed. Switching back is one Home-button long-press away.",
      },
    ],
  },
  {
    slug: "home-screen",
    title: "The Home Screen",
    nav: "Home screen anatomy",
    group: "START",
    description: "Every pixel of the default view, explained.",
    blocks: [
      {
        type: "p",
        text: "The launcher renders exactly three things. Anything else on screen is you — notifications, media, or the system bars.",
      },
      {
        type: "heading",
        level: 2,
        text: "Header",
      },
      {
        type: "p",
        text: "The D.A.R.K. wordmark, and underneath it the live clock and date. The clock ticks from ACTION_TIME_TICK and is decoupled from the UI recomposition stream, so the header updates without re-rendering the app list.",
      },
      {
        type: "heading",
        level: 2,
        text: "The bottom bar",
      },
      {
        type: "p",
        text: "A persistent floating bar holds Home, Settings, Terminal and Recorder, with your live step count at the end. It doubles as the media home: scroll the bar left and the now-playing display slides in — full-width, with long titles scrolling like a marquee, or 'no media playing' when idle.",
      },
      {
        type: "heading",
        level: 2,
        text: "The app list",
      },
      {
        type: "list",
        items: [
          "Every launchable app as plain monospace text. No icons, no badges.",
          "Typing filters instantly via multi-token fuzzy search across names and packages.",
          "Tap a row to launch. Tap and hold for the app menu.",
        ],
      },
      {
        type: "heading",
        level: 2,
        text: "Locking the screen",
      },
      {
        type: "p",
        text: "Double-tap the empty space to lock the screen — the launcher registers as a Device Admin so no accessibility service is needed. Triple-tap opens your hidden apps.",
      },
    ],
  },
  {
    slug: "terminal",
    title: "The Terminal",
    nav: "Terminal commands",
    group: "TERMINAL",
    description: "The complete D.A.R.K. command reference.",
    blocks: [
      {
        type: "p",
        text: "The terminal is a genuine parser, not a toy. Commands are dispatched by a when-tree, unknown input is routed to the Linux shell, and typos get git-style suggestions. Type `help` for a summary.",
      },
      {
        type: "heading",
        level: 2,
        text: "App launching",
      },
      {
        type: "code",
        text: "source whatsapp\nopen youtube\ndual instance:  source whatsapp dual",
      },
      {
        type: "p",
        text: "source / open / run fuzzy-match any installed app by name or package. The `dual` flag launches the second (Samsung dual messenger) instance where it exists.",
      },
      {
        type: "heading",
        level: 2,
        text: "Core commands",
      },
      {
        type: "table",
        rows: [
          { cols: ["COMMAND", "WHAT IT DOES"], isHeader: true },
          { cols: ["help", "List all kernel commands"] },
          { cols: ["clear", "Clear the terminal history"] },
          { cols: ["list", "Return to the app list"] },
          { cols: ["version", "Show build info"] },
          { cols: ["date / echo / whoami", "Boring, classic, always available"] },
          { cols: ["nox on | nox off", "Toggle the flashlight"] },
          { cols: ["on wifi | off wifi", "Open the wifi panel"] },
          { cols: ["on bluetooth", "Request bluetooth enable"] },
          { cols: ["uuid", "Generate + copy a UUIDv4"] },
          { cols: ["b64 encode|decode [text]", "Base64 round-trip"] },
          { cols: ["json format [json]", "Pretty-print JSON"] },
        ],
      },
      {
        type: "heading",
        level: 2,
        text: "Git, fitness and the vault",
      },
      {
        type: "code",
        text: "git refresh            pull GitHub stats\nlog sprint 100m 11.2s  log a workout\nstats                  weekly summary\nlock                   focus mode ON\nlock whatsapp          add whatsapp to the distract list\nunlock                 focus mode OFF\nhide telegram          hide an app (asks for the hide PIN)\nvault lock             focus mode + encrypt the AES vault\nvault unlock           focus mode off + decrypt the vault",
      },
      {
        type: "p",
        text: "git refresh needs a GitHub personal access token, added in Settings. lock/unlock is focus mode: apps in the distract list are hidden from the home screen while locked, and `lock <app>` adds one. hide asks for your PIN before hiding an app. vault lock|unlock does the same focus-mode toggle and additionally encrypts/decrypts the vault directory with a PIN-derived AES key. log stores workouts in a local Room database.",
      },
      {
        type: "heading",
        level: 2,
        text: "Ask & Find",
      },
      {
        type: "code",
        text: "ask what is dark matter     live web search + LLM answer\nask best pizza in lagos\nfind my resume              search every file by name\nfind tax receipt 2026",
      },
      {
        type: "p",
        text: "ask fires a Firecrawl web search, passes the best excerpts to a Groq LLM, and prints a short inline answer with sources. It needs the API keys embedded at build time via secrets.properties — without them it reports that it is offline. find walks your device storage (Download, Documents, DCIM, and beyond) and lists every path whose name matches your query, so you can locate files no matter which folder they drifted into.",
      },
      {
        type: "heading",
        level: 2,
        text: "System access",
      },
      {
        type: "table",
        rows: [
          { cols: ["COMMAND", "WHAT IT DOES"], isHeader: true },
          { cols: ["logcat [-t N]", "Dump the last N lines of the system log"] },
          { cols: ["anything else", "Passed straight to the Linux shell (ShellStream)"] },
          { cols: ["ls / pwd / cd ...", "Real shell traversal where permitted"] },
        ],
      },
      {
        type: "warn",
        text: "Shell pass-through and logcat are powerful. Commands still run under the app's UID, not root.",
      },
    ],
  },
  {
    slug: "settings",
    title: "Settings",
    nav: "Settings & PIN",
    group: "SYSTEM",
    description: "Fonts, hidden apps, PINs, and the GitHub token.",
    blocks: [
      {
        type: "p",
        text: "D.A.R.K. Settings lives in the bottom bar, next to Terminal and Recorder — it no longer masquerades as an app in the list.",
      },
      {
        type: "heading",
        level: 2,
        text: "Set as default launcher",
      },
      {
        type: "p",
        text: "Routes to the system role dialog on Android 10+, or the Home app chooser on older builds. D.A.R.K. uses the correct intent per API level automatically.",
      },
      {
        type: "heading",
        level: 2,
        text: "Font",
      },
      {
        type: "p",
        text: "Swap the monospace face used across the home screen and terminal. Choices are bundled locally — no network fetch.",
      },
      {
        type: "heading",
        level: 2,
        text: "Hidden apps & PIN",
      },
      {
        type: "list",
        items: [
          "Add apps to the hidden list from Settings or via `hide <app>`.",
          "Default PIN is 0000 — change it immediately.",
          "Changing the PIN demands the old PIN first, then new, then confirm.",
          "Hidden apps vanish from the list, search, and source launching.",
          "`lock <app>` hides an app temporarily while focus mode is on; `unlock` brings it back.",
        ],
      },
      {
        type: "heading",
        level: 2,
        text: "GitHub token",
      },
      {
        type: "p",
        text: "A personal access token enables `git refresh`. It is stored locally via DataStore and only ever sent to api.github.com.",
      },
    ],
  },
  {
    slug: "gestures",
    title: "Gestures",
    nav: "Gestures",
    group: "SYSTEM",
    description: "Every way to move through D.A.R.K. without a button.",
    blocks: [
      {
        type: "p",
        text: "D.A.R.K. is gesture-first. Detectors are composed from raw pointer streams via awaitEachGesture, so they feel immediate and never fight the list scroll.",
      },
      {
        type: "heading",
        level: 2,
        text: "The core set",
      },
      {
        type: "table",
        rows: [
          { cols: ["GESTURE", "ACTION"], isHeader: true },
          { cols: ["Tap", "Launch the app under your finger"] },
          { cols: ["Long-press", "Open the app menu (hide, share, app info, uninstall)"] },
          { cols: ["Double-tap", "Lock the screen (Device Admin)"] },
          { cols: ["Triple-tap", "Open the hidden apps area"] },
          { cols: ["Type", "Instant fuzzy filter of the app list"] },
        ],
      },
      {
        type: "heading",
        level: 2,
        text: "Accessibility alternative",
      },
      {
        type: "p",
        text: "A bundled accessibility service can drive terminal and hidden-app flows for users who prefer it. It is fully optional and off by default.",
      },
    ],
  },
  {
    slug: "recorder",
    title: "Screen Recorder",
    nav: "Screen recorder",
    group: "SYSTEM",
    description: "The floating overlay that captures your screen.",
    blocks: [
      {
        type: "p",
        text: "D.A.R.K. can record anything on screen without leaving your current app. A floating bubble docks to the screen edge; your captures land in a library inside D.A.R.K. Recorder tab.",
      },
      {
        type: "heading",
        level: 2,
        text: "The overlay",
      },
      {
        type: "list",
        items: [
          "The bubble hugs the screen edge and slides out of the way while you work.",
          "Tap it to fan open a radial menu — spin to Record, tap again to stop.",
          "Drag the bubble elsewhere if it is in your way.",
        ],
      },
      {
        type: "heading",
        level: 2,
        text: "The countdown",
      },
      {
        type: "p",
        text: "A 3-2-1 countdown plays before capture begins, so the start of your recording is never clipped off.",
      },
      {
        type: "heading",
        level: 2,
        text: "The library",
      },
      {
        type: "list",
        items: [
          "Recordings save as MP4 and appear in the Recorder tab's gallery.",
          "Play them back, share via the system share sheet, or hand them straight to CapCut for editing.",
          "If CapCut isn't installed, D.A.R.K. opens its Play Store page.",
        ],
      },
      {
        type: "warn",
        text: "DRM-protected content (streaming movies, DRM video) cannot be captured. The recorder needs overlay permission — D.A.R.K. asks for it the first time you open the tab.",
      },
    ],
  },
  {
    slug: "troubleshooting",
    title: "Troubleshooting",
    nav: "Troubleshooting",
    group: "SYSTEM",
    description: "Common issues and the fixes that actually work.",
    blocks: [
      {
        type: "heading",
        level: 2,
        text: "Can't set as default home app",
      },
      {
        type: "list",
        items: [
          "Open D.A.R.K. Settings → 'Set as default launcher'.",
          "On Android 10+ this must go through the system role dialog — decline the generic chooser if offered.",
          "Some OEMs hide the option; check Settings → Apps → Default apps → Home app.",
        ],
      },
      {
        type: "heading",
        level: 2,
        text: "App doesn't launch from terminal",
      },
      {
        type: "list",
        items: [
          "Check the name — `source whatsapp` beats `source wahtsapp`.",
          "The app may be hidden. Hidden apps refuse to launch via source.",
          "Dual instances on Samsung resolve via a different user handle; add `dual` to the command.",
        ],
      },
      {
        type: "heading",
        level: 2,
        text: "Now playing not showing",
      },
      {
        type: "list",
        items: [
          "Grant notification access to D.A.R.K. in system settings.",
          "The media app must show a notification — most players do.",
        ],
      },
      {
        type: "heading",
        level: 2,
        text: "Flashlight says permission required",
      },
      {
        type: "p",
        text: "Grant camera permission when prompted. `nox` uses CameraManager torch mode and needs no other permissions.",
      },
      {
        type: "heading",
        level: 2,
        text: "Forgot the hidden-apps PIN",
      },
      {
        type: "p",
        text: "Clear the app's data in system settings (Settings → Apps → D.A.R.K. → Storage → Clear data). Hidden apps return, and the PIN resets to 0000.",
      },
    ],
  },
];

export function findDoc(slug: string): Doc | undefined {
  return DOCS.find((d) => d.slug === slug);
}
