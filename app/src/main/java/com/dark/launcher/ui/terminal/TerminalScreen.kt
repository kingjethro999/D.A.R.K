package com.dark.launcher.ui.terminal

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dark.launcher.ui.home.PinDialog
import com.dark.launcher.ui.theme.Gray
import com.dark.launcher.ui.theme.TerminalGreen
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay

@Composable
fun TerminalScreen(
    viewModel: TerminalViewModel,
    onBack: () -> Unit,
    onOpenSetup: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    var showPinDialog by remember { mutableStateOf(false) }
    val pendingPin = remember { mutableStateOf<CompletableDeferred<Boolean>?>(null) }

    val cameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result handled by user retrying the command */ }

    DisposableEffect(Unit) {
        viewModel.cameraPermissionRequest = { cameraPermission.launch(Manifest.permission.CAMERA) }
        viewModel.pinPromptRequest = {
            val deferred = CompletableDeferred<Boolean>()
            pendingPin.value = deferred
            showPinDialog = true
            deferred.await()
        }
        viewModel.openSetupRequest = onOpenSetup
        onDispose {
            viewModel.cameraPermissionRequest = {}
            viewModel.pinPromptRequest = { false }
            viewModel.openSetupRequest = {}
        }
    }

    LaunchedEffect(state.history.size) {
        if (state.history.isNotEmpty()) {
            listState.animateScrollToItem(state.history.size - 1)
        }
    }

    BackHandler(enabled = true) {
        if (!state.booting) onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp)
    ) {
        if (state.booting) {
            KernelBootSequence(onBootComplete = viewModel::onBootComplete)
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                items(state.history.size) { index ->
                    TerminalLine(state.history[index])
                }
            }
            Spacer(Modifier.height(10.dp))
            TerminalTextField(
                command = state.input,
                onCommandChange = viewModel::onInputChange,
                onEnter = { viewModel.submit(context) }
            )
        }
    }

    if (showPinDialog) {
        PinDialog(
            title = "Hidden app - enter hide pin",
            onDismiss = {
                showPinDialog = false
                pendingPin.value?.complete(false)
                pendingPin.value = null
            },
            onVerify = viewModel::verifyPin,
            onSuccess = {
                showPinDialog = false
                pendingPin.value?.complete(true)
                pendingPin.value = null
            }
        )
    }
}

@Composable
private fun TerminalLine(line: String) {
    val color = when {
        line.startsWith("dark:") || line.startsWith("dark-fatal:") -> Color(0xFFFF6B6B)
        line.startsWith("dark-err:") -> Color(0xFFFFB74D)
        line.startsWith("root@dark") || line.startsWith("D.A.R.K") -> TerminalGreen
        line.startsWith("usage:") || line.startsWith("D.A.R.K. kernel") -> Gray
        else -> Color(0xFF9EFFC7)
    }
    Text(
        text = line,
        color = color,
        fontFamily = FontFamily.Monospace,
        fontSize = 14.sp,
        lineHeight = 19.sp
    )
}

@Composable
private fun rememberBlinkingCursorAlpha(): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "cursor_blink")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                1f at 0
                1f at 499
                0f at 500
                0f at 1000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha_anim"
    )
    return alpha
}

@Composable
fun TerminalTextField(
    command: String,
    onCommandChange: (String) -> Unit,
    onEnter: () -> Unit
) {
    val cursorAlpha = rememberBlinkingCursorAlpha()
    val textStyle = TextStyle(
        color = TerminalGreen,
        fontFamily = FontFamily.Monospace,
        fontSize = 16.sp
    )

    BasicTextField(
        value = command,
        onValueChange = { new ->
            if (new.contains("\n")) {
                onCommandChange(new.replace("\n", "").trim())
                onEnter()
            } else {
                onCommandChange(new)
            }
        },
        textStyle = textStyle,
        cursorBrush = SolidColor(Color.Transparent),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { innerTextField ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "root@dark:~# ",
                    color = TerminalGreen,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp
                )
                Box(modifier = Modifier.width(IntrinsicSize.Min)) {
                    innerTextField()
                }
                Text(
                    text = "\u2588",
                    color = TerminalGreen.copy(alpha = cursorAlpha),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp
                )
            }
        }
    )
}

@Composable
fun KernelBootSequence(onBootComplete: () -> Unit) {
    val context = LocalContext.current
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0.0"
    }
    val bootString = """
        D.A.R.K. v$versionName (Developers' Adaptive Responsive Kernel)
        Mounting virtual environments... OK
        Initializing background parsers... OK
        System initialized.
    """.trimIndent()

    var displayedText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        for (i in bootString.indices) {
            displayedText = bootString.substring(0, i + 1)
            delay((10..40).random().toLong())
        }
        delay(400)
        onBootComplete()
    }

    Text(
        text = displayedText,
        color = TerminalGreen,
        fontFamily = FontFamily.Monospace,
        fontSize = 15.sp,
        lineHeight = 22.sp
    )
}
