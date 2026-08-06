export interface JourneyChapter {
  version: string;
  codename: string;
  date: string;
  blurb: string;
  points: string[];
  highlight?: string;
}

export const JOURNEY: JourneyChapter[] = [
  {
    version: "v0.1",
    codename: "THE IDEA",
    date: "AUG 2026",
    blurb:
      "Started as a rant against bloated launchers. XOS and HiOS ship packed with ads, smart panels and battery killers — the opposite of why anyone buys a phone. The reply: a home screen that is just D. A. R. K., a clock, and a list of apps.",
    points: [
      "Rejected every launcher on the market for shipping bloat",
      "Spec locked to three elements: the wordmark, the clock, the app list",
      "Kotlin + Jetpack Compose chosen. No XML. No icons.",
    ],
    highlight: "screen should be pure black",
  },
  {
    version: "v1.0.0",
    codename: "THE BLACK SCREEN",
    date: "AUG 2026",
    blurb:
      "The first real build. Manifest declares the app as a HOME activity, the list renders pure #000000, and the clock ticks from ACTION_TIME_TICK so it costs almost nothing on an OLED panel.",
    points: [
      "HOME intent filter in the manifest — no root, no system privileges",
      "True AMOLED black: pixels physically off, battery at a whisper",
      "LazyColumn app list queried via PackageManager (MAIN + LAUNCHER)",
      "Single-tap a name and the app launches",
    ],
  },
  {
    version: "v1.0.1",
    codename: "THE TERMINAL",
    date: "AUG 2026",
    blurb:
      "The switch flipped the app list into a real command line. A hand-rolled blinking cursor block, streaming history, and the first commands: nox for the flashlight, on/off for system toggles.",
    points: [
      "Terminal mode toggle — the home screen becomes a CLI",
      "True blinking cursor block animated at a 500ms terminal cadence",
      "nox on|off — torch control via CameraManager, zero permissions",
      "on|off wifi|bluetooth — system panel + bluetooth enable request",
    ],
  },
  {
    version: "v1.0.2",
    codename: "THE PARSER",
    date: "AUG 2026",
    blurb:
      "The command parser became the heart of the app. A tiered fuzzy search, natural-language routing, and git-style suggestions when you mistype — no raw `sh: command not found` noise.",
    points: [
      "source|open|run fuzzy-launch any installed app by name",
      "Multi-token tiered search — fast substring matching, no over-matching",
      "Git-style fallback: 'srce whatsapp' suggests 'source'",
      "Natural language understood: 'my name is king jethro' is greeted, not dumped to a shell",
      "D.A.R.K. Settings injected into the app list as a fake system entry",
    ],
  },
  {
    version: "v1.0.3",
    codename: "HIDDEN APPS",
    date: "AUG 2026",
    blurb:
      "PIN-protected hidden apps, a triple-tap door, and a full PIN-change flow that demands the old code before accepting a new one. Settings, not magic.",
    points: [
      "Hidden apps area gated by a PIN (default 0000), persisted in DataStore",
      "Triple-tap gesture opens the hidden area without a visible entry point",
      "PIN change flow: old PIN → new PIN → confirm, validated against storage",
      "Hidden apps do not appear in search, the list, or source",
    ],
  },
  {
    version: "v1.0.4",
    codename: "GIT · FITNESS · VAULT",
    date: "AUG 2026",
    blurb:
      "The developer feature wave. GitHub GraphQL stats land on the home screen, workouts are logged through the CLI into Room, steps come from the raw motion sensor, and a focus vault AES-encrypts distraction apps away.",
    points: [
      "git refresh — one GraphQL query for repos, stars, commits, best month",
      "log sprint|football|fitness — CLI fitness logger backed by Room (SQLite)",
      "stats — weekly workout summary in a single line",
      "Steps counted from the motion sensor, no Health Connect dependency",
      "vault lock|unlock — PIN-derived AES key encrypts a local directory",
    ],
  },
  {
    version: "v1.0.5",
    codename: "DEVELOPER MODE",
    date: "AUG 2026",
    blurb:
      "Lean in to the Developers' Adaptive Responsive Kernel name. A typewriter boot sequence, genuine Linux shell pass-through, logcat streaming, and the killer detail: launching Samsung dual instances.",
    points: [
      "Typewriter boot sequence plays on terminal open",
      "Unknown commands pass through to a real Linux shell (ShellStream)",
      "logcat -d streams system logs into the terminal",
      "Dual app support — batch LauncherApps resolves per-user activity lists",
      "Now-playing courtesy of a MediaNotificationListener across the top",
    ],
  },
  {
    version: "v1.0.8",
    codename: "FOCUS & RELEASE",
    date: "AUG 2026",
    blurb:
      "The focus pass. lock/unlock and hide became terminal commands, `hide <app>` asks for the PIN, and the prompt/cursor spacing bug was fixed on both the app and the web demo. Shrunk to 1.7MB, signed, and shipped as a GitHub release with the APK attached. This is the build on this website.",
    points: [
      "Clock flow decoupled — home screen no longer recomposes every minute",
      "App label cache — zero PackageManager lookups on the hot path",
      "R8 minify + resource shrink on the release variant",
      "Signed release APK — side-loadable, no Play Store, no ads, no tracking",
      "version.properties auto-increments patch + versionCode on every build",
      "GitHub release with the build artifact attached",
    ],
  },
];

export const CURRENT_RELEASE = "1.0.8";
export const CURRENT_BUILD = 9;
