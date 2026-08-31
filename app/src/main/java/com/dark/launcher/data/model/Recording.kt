package com.dark.launcher.data.model

data class Recording(
    val path: String,
    val name: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val durationMs: Long = 0
) {
    val sizeLabel: String
        get() {
            if (sizeBytes >= 1_000_000) return String.format(java.util.Locale.US, "%.1f MB", sizeBytes / 1_000_000f)
            if (sizeBytes >= 1_000) return String.format(java.util.Locale.US, "%.0f KB", sizeBytes / 1_000f)
            return "$sizeBytes B"
        }

    val durationLabel: String
        get() {
            val total = durationMs / 1000
            val m = total / 60
            val s = total % 60
            return String.format(java.util.Locale.US, "%02d:%02d", m, s)
        }
}
