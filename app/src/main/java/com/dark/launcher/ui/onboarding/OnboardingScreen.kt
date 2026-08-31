package com.dark.launcher.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dark.launcher.ui.home.PinSetupDialog
import com.dark.launcher.ui.theme.Amber
import com.dark.launcher.ui.theme.Black
import com.dark.launcher.ui.theme.TerminalGreen
import com.dark.launcher.util.overlayPermissionIntent

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    firstLaunch: Boolean = true,
    onComplete: () -> Unit,
    onExit: () -> Unit = onComplete
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val allDone = state.pinChanged && state.overlayGranted && state.notificationAccess && state.stepsGranted

    val stepsLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshState()
    }

    LaunchedEffect(Unit) {
        if (firstLaunch && viewModel.shouldSkip()) onComplete()
    }

    if (state.showPinSetup) {
        PinSetupDialog(
            onDismiss = { viewModel.skipPinSetup() },
            onComplete = { pin -> viewModel.savePin(pin) }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.safeDrawing)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "D. A. R. K.",
            fontFamily = FontFamily.Monospace,
            fontSize = 28.sp,
            letterSpacing = 6.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "SETUP CHECKLIST",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            letterSpacing = 3.sp,
            color = TerminalGreen,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        if (allDone) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = TerminalGreen.copy(alpha = 0.12f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = "\u2713 ALL IS READY TO GO",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 2.sp,
                    color = TerminalGreen,
                    modifier = Modifier.padding(vertical = 14.dp).fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        ChecklistItem(
            title = "Set a PIN",
            detail = "Replace the default 0000 before using hidden apps or vault.",
            done = state.pinChanged,
            action = "SET PIN",
            onAction = { viewModel.requestPinSetup() }
        )
        ChecklistItem(
            title = "Draw over apps",
            detail = "Required for the screen recorder floating bubble.",
            done = state.overlayGranted,
            action = "GRANT",
            onAction = { context.startActivity(overlayPermissionIntent(context)) }
        )
        ChecklistItem(
            title = "Notifications",
            detail = "Enables now-playing in the bottom bar and notification shade.",
            done = state.notificationAccess,
            action = "OPEN",
            onAction = { viewModel.openNotificationSettings() }
        )
        ChecklistItem(
            title = "Activity recognition",
            detail = "Counts steps from your phone's motion sensor.",
            done = state.stepsGranted,
            action = "GRANT",
            onAction = {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    stepsLauncher.launch(android.Manifest.permission.ACTIVITY_RECOGNITION)
                } else {
                    viewModel.refreshState()
                }
            }
        )

        Spacer(Modifier.height(32.dp))
        Surface(
            onClick = {
                viewModel.complete(if (firstLaunch) onComplete else onExit)
            },
            shape = RoundedCornerShape(24.dp),
            color = if (allDone) TerminalGreen else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (allDone) "ALL READY - ENTER D.A.R.K." else "ENTER D.A.R.K.",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 3.sp,
                color = if (allDone) Black else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 16.dp).fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        Text(
            text = if (firstLaunch) "you can finish setup later in Settings" else "back to settings",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = Amber.copy(alpha = 0.7f),
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun ChecklistItem(
    title: String,
    detail: String,
    done: Boolean,
    action: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (done) "\u2713 $title" else "\u25CB $title",
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                color = if (done) TerminalGreen else MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = detail,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        if (!done) {
            Text(
                text = action,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                color = TerminalGreen,
                modifier = Modifier
                    .clickable(onClick = onAction)
                    .padding(8.dp)
            )
        }
    }
}
