import type { CommandExample, Feature, ScreenshotItem } from "@/types";

export const APP = {
  name: "D.A.R.K.",
  fullName: "Developers' Adaptive Responsive Kernel",
  tagline: "A minimalist text-only launcher for your Android phone.",
  packageName: "com.dark.launcher",
  version: "2.2.0",
  build: 33,
  minAndroid: "Android 8.0 (API 26)",
  targetSdk: 35,
  sourceApk: "/dark-launcher-v2.2.0.apk",
  downloadFileName: "dark-launcher-v2.2.0.apk",
  sizeBytes: 2825912,
  repository: "https://github.com/kingjethro999/D.A.R.K",
} as const;

export const FEATURES: Feature[] = [
  {
    tag: "TEXT-ONLY",
    title: "Radical minimalism",
    description:
      "No icons, no grids, no clutter. A pure monospace list of apps, gestures, and speed.",
  },
  {
    tag: "TERMINAL",
    title: "Built-in command line",
    description:
      "Launch apps with `source whatsapp`, toggle your flashlight with `nox on`, and dump logcat without leaving home.",
  },
  {
    tag: "INSTANT",
    title: "Renders instantly",
    description:
      "A launcher whose only job is to get out of your way. Every frame is cheap, so the home screen never lags.",
  },
  {
    tag: "BOTTOM NAV",
    title: "Persistent bottom bar",
    description:
      "Home, Settings, Terminal and Recorder live in a floating bar. Scroll it to reach a full-width now-playing display and your live step count.",
  },
  {
    tag: "GESTURES",
    title: "Gesture driven",
    description:
      "Double-tap to lock the screen instantly via Device Admin. Triple-tap for hidden apps. Multi-finger swipes for anything you want.",
  },
  {
    tag: "SEARCH",
    title: "Fuzzy app search",
    description:
      "Type a few letters and the list filters itself. Multi-token search across app names and packages.",
  },
  {
    tag: "RECORDER",
    title: "Screen recorder overlay",
    description:
      "A floating bubble docks to your screen edge. Tap it for a radial record menu, watch a 3-2-1 countdown, then save, share, or hand the clip to CapCut.",
  },
  {
    tag: "MUSIC",
    title: "Now playing in the bar",
    description:
      "The track currently in your ears fills the bottom bar's music display. Too long? It scrolls like a marquee. Nothing playing? It tells you so.",
  },
  {
    tag: "STEPS",
    title: "Step count in the bar",
    description:
      "A live step pill sits at the end of the nav — counted straight from your phone's motion sensor, sized down as the digits grow.",
  },
  {
    tag: "VAULT",
    title: "AES vault",
    description:
      "A passphrase-protected vault that encrypts distraction apps away when you need to focus.",
  },
  {
    tag: "OLED",
    title: "OLED black",
    description:
      "Pure black pixels everywhere. On an OLED display that means true blacks and near-zero battery draw.",
  },
  {
    tag: "LONG-PRESS",
    title: "Long-press superpowers",
    description:
      "Hold an app for its quick menu: hide it, copy its package name, add it to the distract list, share, uninstall, or jump to App Info.",
  },
  {
    tag: "ASK",
    title: "Ask D.A.R.K.",
    description:
      "Type `ask what is dark matter` and get a live answer. Firecrawl searches the web, Groq summarizes it, and the kernel answers you in line.",
  },
  {
    tag: "FIND",
    title: "Find any file",
    description:
      "`find my resume` walks your device storage and lists every match by path — the file, wherever it lives on your phone.",
  },
  {
    tag: "PROFILES",
    title: "Focus profiles",
    description:
      "Switch between Normal, Work, and Night modes from Settings or `mode work` in the terminal. Distract apps hide automatically.",
  },
  {
    tag: "WIDGETS",
    title: "Ambient widget strip",
    description:
      "Battery, next alarm, and unread count appear as a monospace line under the clock — still text, still black.",
  },
  {
    tag: "BACKUP",
    title: "Config backup",
    description:
      "Export hidden apps, gestures, vault config, and terminal aliases to an encrypted JSON file. Restore on reinstall.",
  },
];

export const COMMANDS: CommandExample[] = [
  {
    command: "source whatsapp dual",
    output: ["Executing binary: com.whatsapp...", "open dual messenger instance"],
    description: "Fuzzy-launches an installed app — even Samsung dual instances.",
  },
  {
    command: "my name is king jethro",
    output: ["hello, King. i'm D.A.R.K. - type 'help' for commands..."],
    description: "Natural language is understood, not dumped to the shell.",
  },
  {
    command: "srce whatsapp",
    output: [
      "dark: 'srce' is not a D.A.R.K. command. See 'help'.",
      "The most similar command is 'source'.",
    ],
    description: "Typos get git-style suggestions instead of raw errors.",
  },
  {
    command: "nox on",
    output: ["flashlight on"],
    description: "Toggle the torch from the terminal.",
  },
  {
    command: "git refresh",
    output: ["repos 102 | stars 20 | commits 953 | best 2026-04"],
    description: "Pull your GitHub stats to the home screen.",
  },
  {
    command: "stats",
    output: ["week: 12 workouts | sprint avg 11.2s | 8,412 steps"],
    description: "Weekly fitness summary in one line.",
  },
  {
    command: "ask what is dark matter",
    output: [
      "dark matter is an invisible mass that makes up ~27% of the universe...",
      "(via Space.com, Wikipedia, Nasa.gov)",
    ],
    description: "Ask anything — Firecrawl searches the web, Groq answers inline.",
  },
  {
    command: "find my resume",
    output: [
      "3 match(es):",
      "  /storage/emulated/0/Download/resume.pdf",
      "  /storage/emulated/0/Documents/King_Resume.pdf",
      "  /storage/emulated/0/DCIM/resume-backup.pdf",
    ],
    description: "Search the whole device for a file by name.",
  },
  {
    command: "lock",
    output: ["screen locked", "vault pin required: OK"],
    description: "Lock the launcher. Hidden apps re-appear after PIN unlock.",
  },
  {
    command: "rec start",
    output: ["starting recorder — grant screen capture when prompted"],
    description: "Start the screen recorder overlay from the terminal.",
  },
  {
    command: "mode work",
    output: ["profile set to Work", "FOCUS MODE ON — distract apps hidden"],
    description: "Switch focus profile without opening Settings.",
  },
  {
    command: "steps",
    output: ["4,218 steps today (source: sensor)"],
    description: "Print today's step count from the bottom bar sensor.",
  },
  {
    command: "hide chrome",
    output: ["chrome hidden. double-tap + long-press home to unhide."],
    description: "Hide an app from the home screen behind the vault pin.",
  },
];

export const SCREENSHOTS: ScreenshotItem[] = [
  {
    src: "/screenshot1.jpg",
    alt: "D.A.R.K. launcher home screen",
    caption: "HOME SCREEN",
    width: 720,
    height: 1600,
  },
  {
    src: "/screenshotterminalstart.jpg",
    alt: "D.A.R.K. terminal boot sequence",
    caption: "KERNEL BOOT",
    width: 720,
    height: 1600,
  },
  {
    src: "/screenshotterminal.jpg",
    alt: "D.A.R.K. terminal commands",
    caption: "TERMINAL",
    width: 720,
    height: 1600,
  },
];
