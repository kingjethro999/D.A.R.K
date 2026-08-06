package com.dark.launcher.terminal

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.provider.Settings
import com.dark.launcher.data.model.AppInfo
import com.dark.launcher.data.repo.AskRepository
import com.dark.launcher.data.repo.FitnessRepository
import com.dark.launcher.data.repo.GitHubRepository
import com.dark.launcher.data.repo.LauncherSettingsRepository
import com.dark.launcher.data.repo.VaultRepository
import com.dark.launcher.util.FileFinder
import com.dark.launcher.util.copyToClipboard
import com.dark.launcher.util.launchApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import java.util.Base64
import java.util.UUID

const val CLEAR_TERMINAL = "\u0000CLEAR_TERMINAL\u0000"

data class TerminalDeps(
    val context: Context,
    val apps: List<AppInfo>,
    val settings: LauncherSettingsRepository,
    val fitness: FitnessRepository,
    val github: GitHubRepository,
    val vault: VaultRepository,
    val ask: AskRepository,
    val onCameraPermissionRequest: () -> Unit = {},
    val onPinVerify: suspend () -> Boolean = { false }
)

fun executeTerminalCommand(command: String, deps: TerminalDeps): Flow<String> {
    val tokens = command.trim().lowercase().split("\\s+".toRegex())
    if (tokens.isEmpty() || tokens[0].isBlank()) return flowOf("")

    val keyword = tokens[0]
    val args = tokens.drop(1)

    return when (keyword) {
        "help" -> flowOf(buildHelpText())
        "clear" -> flowOf(CLEAR_TERMINAL)
        "source", "open", "run" -> sourceCommand(args, deps)
        "nox", "flash" -> flashCommand(args, deps)
        "on", "off" -> toggleCommand(keyword, args, deps.context)
        "wifi" -> flowOf(openPanel(Settings.Panel.ACTION_WIFI, deps.context))
        "uuid" -> uuidCommand(deps.context)
        "b64" -> b64Command(args)
        "json" -> jsonCommand(args)
        "log" -> logCommand(args, deps)
        "stats" -> statsCommand(deps)
        "git" -> gitCommand(args, deps)
        "vault" -> vaultCommand(args, deps)
        "lock" -> vaultLockCommand(args, deps)
        "unlock" -> vaultUnlockCommand(args, deps)
        "hide" -> hideCommand(args, deps)
        "ask" -> askCommand(args, deps)
        "find" -> findCommand(args, deps)
        "logcat" -> ShellStream.run("logcat -d ${args.joinToString(" ")}")
        "echo" -> flowOf(args.joinToString(" "))
        "date" -> flowOf(java.text.SimpleDateFormat("EEE, MMM d yyyy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()))
        "whoami" -> flowOf("root")
        "version" -> versionCommand(deps)
        else -> handleUnknown(command, deps)
    }
}

private fun buildHelpText(): String = """
    D.A.R.K. kernel commands:
      source [app]        fuzzy-launch an installed app (hidden apps require pin)
      open|play|start [app]   natural-language app launch
      nox on|off          toggle flashlight
      on|off wifi         open wifi panel / toggle
      on bluetooth        request bluetooth
      uuid                generate + copy a UUIDv4
      b64 encode|decode [text]
      json format [json]  pretty-print JSON
      log [type] [v1] [v2]   log a workout (e.g. log sprint 100m 11.2s)
      stats               weekly fitness summary
      git refresh         pull GitHub stats (token set in settings)
      lock [app]          focus mode ON; with an app, adds it to the distract list
      unlock [app]        focus mode OFF; with an app, removes it from the distract list
      vault lock|unlock [app]  same as lock/unlock, plus the AES file vault
      hide [app]          hide an app (asks for the hide pin)
      ask [question]      live web search + LLM answer (Firecrawl + Groq)
      find [query]        search every file on the device by name
      logcat [-t N]       dump system logcat
      version             show D.A.R.K. build info
      echo / date / whoami
      anything else       passed to the Linux shell
    Unrecognized or natural-language input gets a friendly hint instead of a raw sh error.
""".trimIndent()

private fun sourceCommand(args: List<String>, deps: TerminalDeps): Flow<String> {
    if (args.isEmpty()) return flowOf("usage: source [app name]")
    val query = args.joinToString(" ")

    val target = findApp(deps.apps, query)
    if (target == null) return flowOf("dark: no package found matching '$query'")

    return launchAppTarget(target, deps)
}

private fun launchAppTarget(target: AppInfo, deps: TerminalDeps): Flow<String> = flow {
    if (target.isInternal) {
        val note = when (target.packageName) {
            AppInfo.INTERNAL_TERMINAL -> "already inside the terminal"
            AppInfo.INTERNAL_SETTINGS -> "open D.A.R.K. Settings from the app list"
            else -> "internal module"
        }
        emit("dark: $note")
        return@flow
    }
    val hidden = runCatching { deps.settings.hiddenAppsFlow.first() }.getOrElse { emptySet() }
    if (target.packageName in hidden) {
        emit("dark: '${target.name}' is hidden - enter hide pin:")
        val ok = deps.onPinVerify()
        if (ok) {
            emit("Executing binary: ${target.packageName}...")
            launchApp(deps.context, target)
        } else {
            emit("dark: access denied")
        }
    } else {
        emit("Executing binary: ${target.packageName}...")
        launchApp(deps.context, target)
    }
}

private fun findApp(apps: List<AppInfo>, query: String): AppInfo? {
    val q = query.trim()
    if (q.isEmpty()) return null
    apps.firstOrNull { it.name.equals(q, ignoreCase = true) }?.let { return it }
    apps.firstOrNull { it.name.startsWith(q, ignoreCase = true) }?.let { return it }
    apps.firstOrNull { it.name.contains(q, ignoreCase = true) }?.let { return it }

    val tokens = q.lowercase().split("\\s+".toRegex()).filter { it.isNotEmpty() }
    if (tokens.isEmpty()) return null
    return apps.filter { app ->
        val hay = (app.name + " " + app.packageName).lowercase()
        tokens.all { hay.contains(it) }
    }.maxByOrNull { app ->
        val name = app.name.lowercase()
        tokens.count { name.contains(it) }
    }
}

private fun flashCommand(args: List<String>, deps: TerminalDeps): Flow<String> {
    if (args.isEmpty() || (args[0] != "on" && args[0] != "off")) {
        return flowOf("usage: nox [on|off]")
    }
    return flow {
        try {
            val camera = deps.context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val id = camera.cameraIdList.firstOrNull()
                ?: error("no camera found")
            camera.setTorchMode(id, args[0] == "on")
            emit("flashlight ${args[0]}")
        } catch (e: SecurityException) {
            deps.onCameraPermissionRequest()
            emit("dark: CAMERA permission required - grant it, then retry")
        } catch (e: Exception) {
            emit("dark: flashlight fault - ${e.message}")
        }
    }
}

private fun toggleCommand(keyword: String, args: List<String>, context: Context): Flow<String> {
    if (args.isEmpty()) return flowOf("usage: $keyword [wifi|bluetooth]")
    return flow {
        when (args[0]) {
            "wifi" -> {
                emit(openPanel(Settings.Panel.ACTION_WIFI, context))
            }
            "bluetooth" -> {
                if (keyword == "on") {
                    val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    emit("requesting bluetooth enable...")
                } else {
                    emit(openPanel(Settings.ACTION_BLUETOOTH_SETTINGS, context))
                }
            }
            else -> emit("dark: unknown interface '${args[0]}'")
        }
    }
}

private fun openPanel(action: String, context: Context): String {
    return try {
        val intent = Intent(action)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        "opening system panel..."
    } catch (e: Exception) {
        "dark: could not open panel - ${e.message}"
    }
}

private fun uuidCommand(context: Context): Flow<String> = flow {
    val id = UUID.randomUUID().toString()
    copyToClipboard(context, "UUID", id)
    emit("generated & copied: $id")
}

private fun b64Command(args: List<String>): Flow<String> = flow {
    if (args.size < 2) {
        emit("usage: b64 encode|decode [text]")
        return@flow
    }
    val mode = args[0]
    val raw = args.drop(1).joinToString(" ")
    try {
        when (mode) {
            "encode" -> emit(Base64.getEncoder().encodeToString(raw.toByteArray()))
            "decode" -> emit(String(Base64.getDecoder().decode(raw)))
            else -> emit("usage: b64 encode|decode [text]")
        }
    } catch (e: Exception) {
        emit("dark: b64 fault - ${e.message}")
    }
}

private fun jsonCommand(args: List<String>): Flow<String> = flow {
    if (args.isEmpty()) {
        emit("usage: json format [json]")
        return@flow
    }
    val raw = args.drop(1).joinToString(" ")
    if (args[0] != "format" || raw.isBlank()) {
        emit("usage: json format [json]")
        return@flow
    }
    try {
        val element = Json.parseToJsonElement(raw)
        val pretty = Json { prettyPrint = true }.encodeToString(
            kotlinx.serialization.json.JsonElement.serializer(),
            element
        )
        emit(pretty)
    } catch (e: Exception) {
        emit("dark: invalid json - ${e.message}")
    }
}

private fun logCommand(args: List<String>, deps: TerminalDeps): Flow<String> {
    if (args.size < 2) return flowOf("usage: log [type] [value1] [value2]")
    return flow {
        val type = args[0]
        val value1 = args[1]
        val value2 = args.drop(2).joinToString(" ")
        deps.fitness.log(type, value1, value2)
        emit("[$type] logged: $value1 | $value2")
    }.flowOn(Dispatchers.IO)
}

private fun statsCommand(deps: TerminalDeps): Flow<String> = flow {
    val summary = deps.fitness.buildSummary()
    val sprint = summary.sprintAvg?.let { " | sprint avg $it" } ?: ""
    emit("week: ${summary.workouts} workouts$sprint")
}.flowOn(Dispatchers.IO)

private fun gitCommand(args: List<String>, deps: TerminalDeps): Flow<String> {
    if (args.isEmpty() || args[0] == "help") {
        return flowOf("usage: git refresh")
    }
    val sub = args[0]
    if (sub != "refresh") {
        val similar = if (levenshtein(sub, "refresh") <= 2) {
            ". The most similar command is 'git refresh'"
        } else {
            ". See 'git refresh'"
        }
        return flowOf("git: '$sub' is not a D.A.R.K. git command$similar")
    }
    return flow {
        emit("querying github...")
        val token = deps.settings.gitTokenFlow.first()
        if (token.isBlank()) {
            emit("dark: no token set - add one in D.A.R.K. Settings")
            return@flow
        }
        try {
            val stats = deps.github.fetchStats(token)
            deps.settings.cacheGitStats(stats)
            emit("repos ${stats.repos} | stars ${stats.stars} | commits ${stats.commits} | best ${stats.bestMonth}")
        } catch (e: Exception) {
            emit("dark: github fault - ${e.message}")
        }
    }.flowOn(Dispatchers.IO)
}

private fun vaultCommand(args: List<String>, deps: TerminalDeps): Flow<String> {
    if (args.isEmpty()) return flowOf("usage: vault [lock|unlock] [app]")
    return when (args[0]) {
        "lock" -> vaultLockCommand(args.drop(1), deps, withFileVault = true)
        "unlock" -> vaultUnlockCommand(args.drop(1), deps, withFileVault = true)
        else -> flowOf("usage: vault [lock|unlock] [app]")
    }
}

private fun vaultLockCommand(
    appArgs: List<String>,
    deps: TerminalDeps,
    withFileVault: Boolean = false
): Flow<String> = flow {
    val appName = appArgs.joinToString(" ").trim()
    if (appName.isNotEmpty()) {
        val target = findApp(deps.apps, appName)
        if (target == null) {
            emit("dark: no app found matching '$appName'")
            return@flow
        }
        val current = deps.settings.vaultAppsFlow.first()
        deps.settings.setVaultApps(current + target.packageName)
        emit("'${target.name}' added to the distract list")
    }
    if (withFileVault) {
        val pin = deps.settings.pinFlow.first()
        deps.vault.lock(pin).fold(
            onSuccess = { count -> emit("VAULT LOCKED ($count files encrypted)") },
            onFailure = { e -> emit("dark: vault fault - ${e.message}") }
        )
    } else {
        deps.settings.setVaultLocked(true)
    }
    emit("FOCUS MODE ON")
}.flowOn(Dispatchers.IO)

private fun vaultUnlockCommand(
    appArgs: List<String>,
    deps: TerminalDeps,
    withFileVault: Boolean = false
): Flow<String> = flow {
    val appName = appArgs.joinToString(" ").trim()
    if (appName.isNotEmpty()) {
        val target = findApp(deps.apps, appName)
        if (target == null) {
            emit("dark: no app found matching '$appName'")
            return@flow
        }
        val current = deps.settings.vaultAppsFlow.first()
        val remaining = current - target.packageName
        deps.settings.setVaultApps(remaining)
        emit("'${target.name}' removed from the distract list")
        if (remaining.isNotEmpty()) {
            emit("FOCUS MODE ON (${remaining.size} apps still distracted)")
            return@flow
        }
    }
    if (withFileVault) {
        val pin = deps.settings.pinFlow.first()
        deps.vault.unlock(pin).fold(
            onSuccess = { count -> emit("VAULT UNLOCKED ($count files decrypted)") },
            onFailure = { e -> emit("dark: vault fault - ${e.message}") }
        )
    } else {
        deps.settings.setVaultLocked(false)
    }
    emit("FOCUS MODE OFF")
}.flowOn(Dispatchers.IO)

private fun hideCommand(args: List<String>, deps: TerminalDeps): Flow<String> = flow {
    val appName = args.joinToString(" ").trim()
    if (appName.isEmpty()) {
        emit("usage: hide [app]")
        return@flow
    }
    val target = findApp(deps.apps, appName)
    if (target == null) {
        emit("dark: no app found matching '$appName'")
        return@flow
    }
    emit("'${target.name}' is being hidden - enter hide pin:")
    val ok = deps.onPinVerify()
    if (ok) {
        deps.settings.hideApp(target.packageName)
        emit("hidden: ${target.packageName}")
    } else {
        emit("dark: access denied - '${target.name}' not hidden")
    }
}

private fun askCommand(args: List<String>, deps: TerminalDeps): Flow<String> = flow {
    val query = args.joinToString(" ").trim()
    if (query.isEmpty()) {
        emit("usage: ask [question]")
        emit("e.g.  ask what is jetpack compose")
        return@flow
    }
    emit("asking the web...")
    try {
        val answer = deps.ask.ask(query)
        answer.lineSequence().forEach { emit(it) }
    } catch (e: Exception) {
        emit("dark: ask fault - ${e.message}")
    }
}.flowOn(Dispatchers.IO)

private fun findCommand(args: List<String>, deps: TerminalDeps): Flow<String> = flow {
    val query = args.joinToString(" ").trim()
    if (query.isEmpty()) {
        emit("usage: find [query]")
        emit("e.g.  find my resume")
        return@flow
    }
    emit("searching device for '$query'...")
    val matches = FileFinder.search(deps.context, query)
    if (matches.isEmpty()) {
        emit("dark: no matches for '$query'")
    } else {
        emit("${matches.size} match(es):")
        matches.forEach { emit("  $it") }
    }
}.flowOn(Dispatchers.IO)

private fun versionCommand(deps: TerminalDeps): Flow<String> = flow {
    val info = runCatching {
        deps.context.packageManager.getPackageInfo(deps.context.packageName, 0)
    }.getOrNull()
    if (info == null) {
        emit("dark: could not read package info")
    } else {
        emit("D.A.R.K. ${info.versionName} (build ${info.versionCode})")
    }
}

private val KNOWN_COMMANDS = listOf(
    "help", "clear", "source", "open", "run", "nox", "flash", "on", "off",
    "wifi", "uuid", "b64", "json", "log", "stats", "git", "vault", "lock",
    "unlock", "hide", "ask", "find", "logcat", "echo", "date", "whoami", "version"
)

private val STOPWORDS = setOf(
    "start", "play", "launch", "open", "set", "show", "tell", "what", "how",
    "why", "who", "when", "my", "i", "im", "call", "make", "get", "please",
    "hey", "hello", "hi", "yo", "can", "do", "is", "are", "the", "a", "an"
)

private val SHELL_COMMANDS = setOf(
    "ls", "cd", "pwd", "cat", "mkdir", "rm", "cp", "mv", "touch", "grep",
    "find", "chmod", "chown", "ps", "top", "df", "du", "id", "uname",
    "ifconfig", "ping", "netstat", "adb", "sh", "bash", "zsh", "exit",
    "kill", "env", "export", "tar", "zip", "unzip", "wget", "curl", "ssh",
    "scp", "man", "head", "tail", "sort", "uniq", "wc", "sed", "awk",
    "cut", "tr", "mount", "umount", "sudo", "su", "reboot", "am", "pm",
    "dumpsys", "input", "wm", "settings", "getprop", "setprop"
)

private val SHELL_METACHARS = setOf('|', '&', ';', '>', '<', '$', '`', '"', '(', ')', '!')

private fun looksLikeShell(first: String): Boolean =
    first in SHELL_COMMANDS || first.any { it in SHELL_METACHARS } ||
        first.contains("*") || first.contains("~")

private fun handleUnknown(command: String, deps: TerminalDeps): Flow<String> {
    val trimmed = command.trim()
    val tokens = trimmed.lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }
    if (tokens.isEmpty()) return flowOf("")
    val first = tokens[0]

    if (looksLikeShell(first)) return ShellStream.run(command)

    if (first !in STOPWORDS && (tokens.size == 1 || first.length >= 4)) {
        suggestCommand(first)?.let { suggestion ->
            return flowOf("dark: '$first' is not a D.A.R.K. command. See 'help'. The most similar command is '$suggestion'.")
        }
    }

    naturalLanguage(trimmed, deps)?.let { return it }

    if (tokens.size > 1) {
        return flowOf("dark: unrecognized command '$first'. Type 'help' for D.A.R.K. commands, or 'source [app]' to launch one.")
    }

    return ShellStream.run(command)
}

private fun naturalLanguage(text: String, deps: TerminalDeps): Flow<String>? {
    val lower = text.lowercase()

    val launchMatch = Regex("^(open|play|start|launch)\\s+(.+)$").find(lower)
    if (launchMatch != null) {
        val name = launchMatch.groupValues[2]
        val app = findApp(deps.apps, name)
        return if (app != null) {
            launchAppTarget(app, deps)
        } else {
            flowOf("dark: no app found matching '$name'")
        }
    }

    val whoMatch = Regex("^(my name is|i am|i'm|im|call me)\\s+(.+)$").find(lower)
    if (whoMatch != null) {
        val who = whoMatch.groupValues[2].trim()
        val first = who.split("\\s+".toRegex()).firstOrNull()
            ?.replaceFirstChar { it.uppercase() } ?: who
        return flowOf("hello, $first. i'm D.A.R.K. - type 'help' for commands, or 'source [app]' to launch one.")
    }

    if (text.trim().lowercase() in setOf(
            "hi", "hey", "hello", "yo", "sup", "wassup",
            "good morning", "good afternoon", "good evening"
        )
    ) {
        return flowOf("hey. type 'help' for commands, or 'source [app]' to launch one.")
    }

    if (lower.startsWith("what ") || lower.startsWith("how ") || lower.startsWith("who ") ||
        lower.startsWith("why ") || lower.startsWith("when ") || lower.trim().endsWith("?")
    ) {
        return flowOf("dark: i run commands, not conversations (yet). try 'help' or 'source [app]'.")
    }

    if (Regex("\\b(open|play|start|launch)\\b").containsMatchIn(lower)) {
        val stripped = lower
            .replace(Regex("\\b(open|play|start|launch)\\b"), " ")
            .replace(Regex("[^a-z0-9 .\\-]"), " ")
            .trim()
        val app = findApp(deps.apps, stripped)
        if (app != null) return launchAppTarget(app, deps)
    }

    return null
}

private fun suggestCommand(word: String): String? =
    KNOWN_COMMANDS
        .filter { cmd ->
            val dist = levenshtein(word, cmd)
            dist <= 2 && kotlin.math.abs(word.length - cmd.length) <= 2
        }
        .minByOrNull { levenshtein(word, it) }

private fun levenshtein(a: String, b: String): Int {
    if (a == b) return 0
    if (a.isEmpty()) return b.length
    if (b.isEmpty()) return a.length
    val dp = IntArray(b.length + 1) { it }
    for (i in 1..a.length) {
        var prev = dp[0]
        dp[0] = i
        for (j in 1..b.length) {
            val tmp = dp[j]
            dp[j] = minOf(
                dp[j] + 1,
                dp[j - 1] + 1,
                prev + if (a[i - 1] == b[j - 1]) 0 else 1
            )
            prev = tmp
        }
    }
    return dp[b.length]
}
