package com.dark.launcher.util

import android.os.SystemClock
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

const val TAP_WINDOW_MS = 300L
const val TRIPLE_DETECT_MS = 340L

enum class SwipeDirection { UP, DOWN }

fun Modifier.darkTaps(
    onDoubleTap: () -> Unit,
    onTripleTap: () -> Unit
): Modifier = composed {
    val scope: CoroutineScope = rememberCoroutineScope()
    pointerInput(Unit) {
        var tapCount = 0
        var lastTapTime = 0L
        var pending: Job? = null
        var downX = 0f
        var downY = 0f

        fun reset() {
            tapCount = 0
            pending?.cancel()
            pending = null
        }

        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            downX = down.position.x
            downY = down.position.y

            val up = waitForUpOrCancellation()
            if (up == null) {
                reset()
                return@awaitEachGesture
            }

            val dx = abs(up.position.x - downX)
            val dy = abs(up.position.y - downY)
            val touchSlop = viewConfiguration.touchSlop.toFloat()
            if (dx > touchSlop * 2 || dy > touchSlop * 2) {
                reset()
                return@awaitEachGesture
            }

            val now = SystemClock.uptimeMillis()
            tapCount = if (now - lastTapTime < TAP_WINDOW_MS) tapCount + 1 else 1
            lastTapTime = now

            pending?.cancel()

            when {
                tapCount >= 3 -> {
                    reset()
                    onTripleTap()
                }
                tapCount == 2 -> {
                    pending = scope.launch {
                        delay(TRIPLE_DETECT_MS)
                        if (tapCount == 2) {
                            tapCount = 0
                            onDoubleTap()
                        }
                    }
                }
            }
        }
    }
}

fun Modifier.darkMultiFingerSwipe(
    requiredFingers: Int = 3,
    onSwiped: (SwipeDirection) -> Unit
): Modifier = composed {
    pointerInput(Unit) {
        awaitEachGesture {
            var totalDy = 0f
            var maxPointers = 1
            val slop = viewConfiguration.touchSlop.toFloat() * 2f
            var done = false

            while (!done) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val pressed = event.changes.filter { it.pressed }
                if (pressed.isEmpty()) break
                maxPointers = maxOf(maxPointers, pressed.size)
                totalDy += pressed.first().positionChangeIgnoreConsumed().y

                if (maxPointers >= requiredFingers && abs(totalDy) > slop) {
                    onSwiped(if (totalDy > 0) SwipeDirection.DOWN else SwipeDirection.UP)
                    event.changes.forEach { it.consume() }
                    done = true
                }
            }
        }
    }
}
