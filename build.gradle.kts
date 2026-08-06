plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}

// ---- Auto version bump ----
// The root project is configured before :app, so bumping here means the app
// module already reads the new version when it is configured. Only app build
// tasks tick the version (so `gradle help` / config-only runs don't inflate
// it). Patch rolls over at 9 -> minor, minor rolls over at 19 -> major, so the
// displayed version stays small: 1.0.9 -> 1.1.0 ... 1.19.9 -> 2.0.0.
val isAppBuild = gradle.startParameter.taskRequests.any { req ->
    req.args.any { arg ->
        val task = arg.lowercase()
        task.contains("assemble") || task.contains("install") || task.contains("bundle")
    }
}
val versionFile = file("version.properties")
if (isAppBuild && versionFile.exists()) {
    val props = java.util.Properties().apply {
        versionFile.inputStream().use { load(it) }
    }
    var major = props.getProperty("major", "1").toInt()
    var minor = props.getProperty("minor", "0").toInt()
    var patch = props.getProperty("patch", "0").toInt() + 1
    val code = props.getProperty("versionCode", "1").toInt() + 1

    if (patch >= 10) {
        minor += patch / 10
        patch %= 10
    }
    if (minor >= 20) {
        major += minor / 20
        minor %= 20
    }

    props.setProperty("major", major.toString())
    props.setProperty("minor", minor.toString())
    props.setProperty("patch", patch.toString())
    props.setProperty("versionCode", code.toString())
    versionFile.outputStream().use { props.store(it, "Auto-incremented on each build") }
}
