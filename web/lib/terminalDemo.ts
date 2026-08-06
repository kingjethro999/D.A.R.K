export interface DemoCommand {
  name: string;
  aliases?: string[];
  usage: string;
  help: string;
  run: (args: string[]) => string[];
}

export const DEMO_COMMANDS: Record<string, DemoCommand> = {
  help: {
    name: "help",
    usage: "help",
    help: "List available commands.",
    run: () => [
      "D.A.R.K. demo commands:",
      "  help               this list",
      "  about              what this is",
      "  version            build info",
      "  source <app>       fuzzy-launch an app",
      "  list               back to the app list",
      "  nox on|off         flashlight",
      "  on|off wifi        wifi panel",
      "  uuid               generate a UUID",
      "  b64 encode <text>  base64",
      "  json format <json> pretty-print",
      "  log <type> ...     log a workout",
      "  stats              weekly fitness",
      "  git refresh        github stats",
      "  vault lock|unlock  AES vault",
      "  logcat -t 20       system log",
      "  neofetch           system info",
      "  date / echo / whoami / clear",
      "",
      "Type anything else and it falls through to the shell.",
    ],
  },
  about: {
    name: "about",
    usage: "about",
    help: "What D.A.R.K. actually is.",
    run: () => [
      "D.A.R.K. is a minimalist, OLED-black, text-only Android launcher.",
      "Built with Kotlin and Jetpack Compose. No icons, no ads, no store.",
      "The home screen is the terminal. The terminal is the home screen.",
      "Learn more in the docs, or grab the APK from /download.",
    ],
  },
  version: {
    name: "version",
    usage: "version",
    help: "Show build info.",
    run: () => ["D.A.R.K. 1.0.6 (build 7)  |  target sdk 35  |  min sdk 26"],
  },
  whoami: {
    name: "whoami",
    usage: "whoami",
    help: "Current user.",
    run: () => ["root"],
  },
  date: {
    name: "date",
    usage: "date",
    help: "Current date and time.",
    run: () => [
      new Date().toLocaleString("en-GB", {
        weekday: "short",
        year: "numeric",
        month: "short",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit",
      }),
    ],
  },
  echo: {
    name: "echo",
    usage: "echo <text>",
    help: "Print text.",
    run: (args) => [args.join(" ") || ""],
  },
  clear: {
    name: "clear",
    usage: "clear",
    help: "Clear the terminal.",
    run: () => [],
  },
  list: {
    name: "list",
    usage: "list",
    help: "Return to the app list.",
    run: () => [
      "apps 74 | hidden 2 | system 4",
      "returning to app list...",
    ],
  },
  source: {
    name: "source",
    aliases: ["open", "run"],
    usage: "source <app> [dual]",
    help: "Fuzzy-launch an installed app.",
    run: (args) => {
      if (args.length === 0)
        return ["usage: source [app name]", "e.g.  source whatsapp dual"];
      const app = args[0].toLowerCase();
      const apps: Record<string, string[]> = {
        whatsapp: ["com.whatsapp", "open main messenger instance"],
        telegram: ["org.telegram.messenger", "open telegram"],
        youtube: ["com.google.android.youtube", "open youtube"],
        dark: ["dark.internal.settings", "opening D.A.R.K. Settings"],
        settings: ["dark.internal.settings", "opening D.A.R.K. Settings"],
        terminal: ["com.termux", "open termux"],
        browser: ["com.android.chrome", "open chrome"],
      };
      const hit = Object.entries(apps).find(([k]) => k.startsWith(app));
      if (!hit)
        return [
          `dark: no app matched '${args[0]}'`,
          "The most similar apps: whatsapp, telegram, youtube, terminal",
        ];
      const [, lines] = hit;
      const out: string[] = [
        `executing binary: ${hit[0]}...`,
        ...lines,
      ];
      if (args.includes("dual"))
        out[1] += "  [dual instance]";
      return out;
    },
  },
  nox: {
    name: "nox",
    aliases: ["flash"],
    usage: "nox on|off",
    help: "Toggle the flashlight.",
    run: (args) => {
      if (!args[0] || !["on", "off"].includes(args[0]))
        return ["usage: nox [on|off]"];
      return [`flashlight ${args[0]}`];
    },
  },
  uuid: {
    name: "uuid",
    usage: "uuid",
    help: "Generate a UUIDv4.",
    run: () => {
      const u = crypto.randomUUID();
      return [`uuidv4: ${u}`, "copied to clipboard"];
    },
  },
  b64: {
    name: "b64",
    usage: "b64 encode|decode <text>",
    help: "Base64 round-trip.",
    run: (args) => {
      if (args.length < 2) return ["usage: b64 [encode|decode] [text]"];
      const [mode, ...rest] = args;
      const text = rest.join(" ");
      if (mode === "encode")
        return [btoa(unescape(encodeURIComponent(text)))];
      if (mode === "decode") {
        try {
          return [decodeURIComponent(escape(atob(text)))];
        } catch {
          return ["dark: invalid base64"];
        }
      }
      return ["usage: b64 [encode|decode] [text]"];
    },
  },
  json: {
    name: "json",
    usage: "json format <json>",
    help: "Pretty-print JSON.",
    run: (args) => {
      const raw = args[0] === "format" ? args.slice(1).join(" ") : args.join(" ");
      if (!raw) return ["usage: json format [json]"];
      try {
        return JSON.stringify(JSON.parse(raw), null, 2).split("\n");
      } catch {
        return ["dark: invalid json"];
      }
    },
  },
  log: {
    name: "log",
    usage: "log <type> [v1] [v2]",
    help: "Log a workout (sprint, football, fitness).",
    run: (args) => {
      if (args.length === 0)
        return ["usage: log [sprint|football|fitness] [params]"];
      return [`saved: ${args.join(" ")}  (2 workouts this week)`];
    },
  },
  stats: {
    name: "stats",
    usage: "stats",
    help: "Weekly fitness summary.",
    run: () => ["week: 12 workouts | sprint avg 11.2s | 8,412 steps"],
  },
  git: {
    name: "git",
    usage: "git refresh",
    help: "Pull GitHub stats to the home screen.",
    run: (args) => {
      if (!args[0] || args[0] !== "refresh")
        return ["usage: git refresh", "e.g.  git refresh"];
      return [
        "querying github...",
        "repos 102 | stars 20 | commits 953 | best 2026-04",
      ];
    },
  },
  vault: {
    name: "vault",
    usage: "vault lock|unlock",
    help: "Lock/unlock the AES vault.",
    run: (args) => {
      if (!args[0] || !["lock", "unlock"].includes(args[0]))
        return ["usage: vault [lock|unlock]", "vault unlock requires pin:"];
      return args[0] === "lock"
        ? ["VAULT LOCKED (3 files encrypted)"]
        : ["VAULT UNLOCKED (3 files decrypted)"];
    },
  },
  logcat: {
    name: "logcat",
    usage: "logcat [-t N]",
    help: "Dump the system log.",
    run: (args) => {
      const n = args.includes("-t")
        ? Number(args[args.indexOf("-t") + 1]) || 5
        : 5;
      const rows = [
        "06-26 09:41:12.004  1376  1376 D ActivityManager: START com.dark.launcher",
        "06-26 09:41:12.118  1376  1376 I WindowManager: RoundedCorners: topCorner=32",
        "06-26 09:41:12.341  5000  5000 E DARK     : nox: torch mode on",
        "06-26 09:41:12.512  5000  5000 D DARK     : fuzzy match 'whatsapp' in 3ms",
        "06-26 09:41:12.684  5000  5000 I DARK     : launching com.whatsapp via LauncherApps",
      ];
      return rows.slice(-n);
    },
  },
  neofetch: {
    name: "neofetch",
    usage: "neofetch",
    help: "System info, classic style.",
    run: () => [
      "       ▄▄▄▄▄▄▄       root@dark",
      "    ▄█████████▄     -----------------",
      "  ▄███████████▄     OS: D.A.R.K. 1.0.6",
      "  ████████████▄     Kernel: Adaptive Responsive",
      "    ███████████     Shell: dark sh 1.0",
      "       ▀█████▀      Apps: 74",
      "        ▀▀▀▀▀▀      Uptime: 3 days, 07:42:11",
    ],
  },
};

const KNOWN = Object.values(DEMO_COMMANDS).flatMap((c) => [
  c.name,
  ...(c.aliases ?? []),
]);

const SIMPLE_APPS = [
  "whatsapp",
  "telegram",
  "youtube",
  "spotify",
  "gmail",
  "maps",
  "camera",
  "photos",
  "terminal",
  "settings",
];

export interface DemoResponse {
  lines: string[];
  clear: boolean;
}

export function runDemoCommand(raw: string): DemoResponse {
  const cmd = raw.trim();
  if (!cmd) return { lines: [], clear: false };
  const tokens = cmd.split(/\s+/);
  const keyword = tokens[0].toLowerCase();
  const args = tokens.slice(1);

  const command = DEMO_COMMANDS[keyword];
  if (command) {
    const lines = command.run(args);
    if (keyword === "clear") return { lines: [], clear: true };
    return { lines, clear: false };
  }

  if (KNOWN.includes(keyword)) {
    return {
      lines: [
        `dark: missing or extra arguments for '${keyword}'. See 'help'.`,
      ],
      clear: false,
    };
  }

  if (keyword === "srce" || keyword === "soucre" || keyword === "opn") {
    return {
      lines: [
        `dark: '${keyword}' is not a D.A.R.K. command. See 'help'.`,
        "The most similar command is 'source'.",
      ],
      clear: false,
    };
  }

  const app = SIMPLE_APPS.find((a) => a.startsWith(keyword));
  if (app) {
    return {
      lines: [
        `executing binary: ${app}...`,
        app === "settings"
          ? "opening D.A.R.K. Settings"
          : `open ${app}`,
      ],
      clear: false,
    };
  }

  const greeting = /my name is|hello|hi|hey/i.test(cmd);
  if (greeting) {
    return {
      lines: [
        "hello. i'm D.A.R.K. - type 'help' for commands, or try 'source whatsapp'.",
      ],
      clear: false,
    };
  }

  if (keyword === "pwd") return { lines: ["/"], clear: false };
  if (keyword === "ls") return { lines: ["apps  list  settings  vault"], clear: false };
  if (keyword === "cat" && args[0] === "welcome.txt")
    return {
      lines: [
        "welcome to D.A.R.K.",
        "a minimalist, text-only, OLED-black launcher.",
        "docs are at /docs. the apk is at /download.",
      ],
      clear: false,
    };

  return {
    lines: [
      `sh: ${keyword}: command not found`,
      `dark: '${cmd}' is not a D.A.R.K. command. See 'help'.`,
    ],
    clear: false,
  };
}

export function completionsFor(prefix: string): string[] {
  const p = prefix.trim().toLowerCase();
  if (!p) return [];
  const all = [...KNOWN, ...SIMPLE_APPS];
  return all.filter((c) => c.startsWith(p));
}
