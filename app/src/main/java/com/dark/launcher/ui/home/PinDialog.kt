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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch

@Composable
fun PinDialog(
    title: String,
    onDismiss: () -> Unit,
    onVerify: suspend (String) -> Boolean,
    onSuccess: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var verifying by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun submit() {
        if (verifying || input.length != 4) return
        verifying = true
        scope.launch {
            val ok = onVerify(input)
            verifying = false
            if (ok) onSuccess() else {
                error = true
                input = ""
            }
        }
    }

    fun pressDigit(d: Char) {
        if (verifying) return
        if (input.length < 4) {
            input += d
            error = false
            if (input.length == 4) submit()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(title, fontSize = 18.sp)
                Spacer(Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { index ->
                        val filled = index < input.length
                        val active = index == input.length && !error
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = 2.dp,
                                    color = when {
                                        error -> Amber
                                        active -> TerminalGreen
                                        else -> Gray
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (filled) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(if (error) Amber else TerminalGreen)
                                )
                            }
                        }
                    }
                }
                if (error) {
                    Spacer(Modifier.height(8.dp))
                    Text("wrong pin", color = Amber, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                } else {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "default pin: 0000",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
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
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PinKey(
                        label = "\u232b",
                        onClick = { if (!verifying && input.isNotEmpty()) { input = input.dropLast(1); error = false } }
                    )
                    PinKey(label = "0", onClick = { pressDigit('0') })
                    PinKey(
                        label = "OK",
                        onClick = { submit() },
                        highlighted = input.length == 4 && !error
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@Composable
private fun PinKeypadRow(digits: List<Char>, onDigit: (Char) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        digits.forEach { d ->
            PinKey(label = d.toString(), onClick = { onDigit(d) })
        }
    }
}

@Composable
private fun PinKey(label: String, onClick: () -> Unit, highlighted: Boolean = false) {
    val bg = if (highlighted) TerminalGreen else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (highlighted) Color.Black else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = fg, fontSize = 24.sp, fontFamily = FontFamily.Monospace)
    }
}
