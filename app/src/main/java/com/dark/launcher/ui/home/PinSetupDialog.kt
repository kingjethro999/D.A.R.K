package com.dark.launcher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dark.launcher.ui.theme.Amber
import com.dark.launcher.ui.theme.Gray
import com.dark.launcher.ui.theme.TerminalGreen

@Composable
fun PinSetupDialog(
    onDismiss: () -> Unit,
    onComplete: (String) -> Unit
) {
    var step by remember { mutableStateOf(0) }
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    fun currentInput() = if (step == 0) first else second
    fun setCurrent(value: String) {
        if (step == 0) first = value else second = value
    }

    fun pressDigit(d: Char) {
        val cur = currentInput()
        if (cur.length < 4) {
            setCurrent(cur + d)
            error = false
            if (cur.length == 3) {
                if (step == 0) step = 1 else if (first == second) onComplete(first) else {
                    error = true
                    second = ""
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (step == 0) "Choose a PIN" else "Confirm PIN",
                    fontSize = 18.sp
                )
                Spacer(Modifier.height(16.dp))
                PinDots(input = currentInput(), error = error)
                if (error) {
                    Text("pins don't match", color = Amber, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                PinKeypadRow(listOf('1', '2', '3')) { pressDigit(it) }
                Spacer(Modifier.height(10.dp))
                PinKeypadRow(listOf('4', '5', '6')) { pressDigit(it) }
                Spacer(Modifier.height(10.dp))
                PinKeypadRow(listOf('7', '8', '9')) { pressDigit(it) }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PinKey("\u232b") {
                        setCurrent(currentInput().dropLast(1))
                        error = false
                    }
                    PinKey("0") { pressDigit('0') }
                    PinKey("SKIP") { onDismiss() }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@Composable
private fun PinDots(input: String, error: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(4) { index ->
            val filled = index < input.length
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        2.dp,
                        when {
                            error -> Amber
                            filled -> TerminalGreen
                            else -> Gray
                        },
                        RoundedCornerShape(12.dp)
                    )
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (filled) {
                    Box(
                        Modifier.size(16.dp).clip(CircleShape).background(if (error) Amber else TerminalGreen)
                    )
                }
            }
        }
    }
}

@Composable
private fun PinKeypadRow(digits: List<Char>, onDigit: (Char) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        digits.forEach { PinKey(it.toString()) { onDigit(it) } }
    }
}

@Composable
private fun PinKey(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = if (label.length > 2) 12.sp else 22.sp, fontFamily = FontFamily.Monospace)
    }
}
