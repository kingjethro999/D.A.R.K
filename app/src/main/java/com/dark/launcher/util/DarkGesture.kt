package com.dark.launcher.util

enum class DarkGesture(val id: String, val label: String) {
    TRIPLE_TAP("triple_tap", "TRIPLE TAP"),
    DOUBLE_TAP("double_tap", "DOUBLE TAP"),
    SWIPE_UP_3("swipe_up_3", "3-FINGER SWIPE UP"),
    SWIPE_DOWN_3("swipe_down_3", "3-FINGER SWIPE DOWN");

    companion object {
        fun fromId(id: String?): DarkGesture =
            entries.firstOrNull { it.id == id } ?: TRIPLE_TAP
    }
}
