import type { CommandExample, Feature, ScreenshotItem } from "@/types";

export const APP = {
  name: "D.A.R.K.",
  fullName: "Developers' Adaptive Responsive Kernel",
  tagline: "A minimalist text-only launcher for your Android phone.",
  packageName: "com.dark.launcher",
  version: "1.0.6",
  build: 7,
  minAndroid: "Android 8.0 (API 26)",
  targetSdk: 35,
  sourceApk: "/dark-launcher-v1.0.6.apk",
  downloadFileName: "dark-launcher-v1.0.6.apk",
  sizeBytes: 1788007,
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
    tag: "GESTURES",
    title: "Gesture driven",
    description:
      "Double-tap to lock. Triple-tap for hidden apps. Multi-finger swipes for anything you want.",
  },
  {
    tag: "SEARCH",
    title: "Fuzzy app search",
    description:
      "Type a few letters and the list filters itself. Multi-token search across app names and packages.",
  },
  {
    tag: "MUSIC",
    title: "Now playing, always",
    description:
      "The track currently in your ears scrolls across the top of the home screen via notification access.",
  },
  {
    tag: "STEPS",
    title: "Fitness at a glance",
    description:
      "Daily steps, weekly workouts and sprint averages counted straight from your phone's motion sensor.",
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
