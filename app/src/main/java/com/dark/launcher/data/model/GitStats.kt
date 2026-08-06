package com.dark.launcher.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GitStats(
    val repos: Int = 0,
    val stars: Int = 0,
    val commits: Int = 0,
    val bestMonth: String = "N/A",
    val lastSync: Long = 0L
) {
    val synced: Boolean get() = lastSync > 0L
}
