package com.dark.launcher.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import android.os.Build
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dark.launcher.data.model.AppInfo
import com.dark.launcher.ui.home.PinDialog
import com.dark.launcher.ui.theme.Amber
import com.dark.launcher.ui.theme.Gray
import com.dark.launcher.ui.theme.TerminalGreen
import com.dark.launcher.util.DarkGesture
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenHidden: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showPinDialog by remember { mutableStateOf(false) }
    var showVaultApps by remember { mutableStateOf(false) }
    var showHiddenPin by remember { mutableStateOf(false) }
    var showHiddenPicker by remember { mutableStateOf(false) }
    var showLockPicker by remember { mutableStateOf(false) }
    var showToken by remember { mutableStateOf(false) }

    val stepsPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.ensureStepsRunning()
        if (!granted) viewModel.toast("step access denied - steps will not count")
    }

    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .padding(8.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "D. A. R. K. SETTINGS",
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 15.sp,
                letterSpacing = 3.sp
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { SectionHeader("DISPLAY") }
            item {
                ToggleRow("SHOW CLOCK", state.showTime) { viewModel.setShowTime(it) }
            }
            item {
                ToggleRow("SHOW GIT STATS", state.showGit) { viewModel.setShowGit(it) }
            }
            item {
                ToggleRow("SHOW FITNESS LINE", state.showFitness) { viewModel.setShowFitness(it) }
            }

            item { SectionHeader("FITNESS") }
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "> ${state.stepsToday} STEPS TODAY",
                            fontFamily = FontFamily.Monospace,
                            color = TerminalGreen,
                            fontSize = 13.sp,
                            letterSpacing = 2.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (viewModel.sensorAvailable())
                                "STEP COUNTER: ACTIVE"
                            else
                                "NO STEP SENSOR ON THIS PHONE",
                            fontFamily = FontFamily.Monospace,
                            color = if (viewModel.sensorAvailable()) Gray else Amber,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Steps counted straight from the phone's motion sensor.",
                            fontFamily = FontFamily.Monospace,
                            color = Gray,
                            fontSize = 10.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        Row {
                            SettingsButton("GRANT ACCESS", onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    stepsPermissionLauncher.launch(
                                        android.Manifest.permission.ACTIVITY_RECOGNITION
                                    )
                                } else {
                                    viewModel.toast("not required on this android version")
                                }
                            })
                            Spacer(Modifier.width(10.dp))
                            SettingsButton("REFRESH", onClick = viewModel::refreshHealth)
                        }
                    }
                }
            }

            item { SectionHeader("MEDIA") }
            item {
                val granted = viewModel.mediaAccessGranted()
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "> NOW PLAYING",
                            fontFamily = FontFamily.Monospace,
                            color = TerminalGreen,
                            fontSize = 13.sp,
                            letterSpacing = 2.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (granted)
                                "NOTIFICATION ACCESS: GRANTED"
                            else
                                "NEEDS NOTIFICATION ACCESS",
                            fontFamily = FontFamily.Monospace,
                            color = if (granted) Gray else Amber,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Shows the currently playing song on the home screen. Android requires notification access to read music from other apps.",
                            fontFamily = FontFamily.Monospace,
                            color = Gray,
                            fontSize = 10.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        Row {
                            SettingsButton(
                                if (granted) "RE-OPEN SETTINGS" else "GRANT ACCESS",
                                onClick = viewModel::openNotificationListenerSettings
                            )
                        }
                    }
                }
            }

            item { SectionHeader("SECURITY") }
            item { SettingsRow("CHANGE PIN", onClick = { showPinDialog = true }) }
            item {
                SettingsRow("HIDDEN APPS", onClick = { showHiddenPin = true })
            }

            item { SectionHeader("GESTURES") }
            item {
                SettingsRow(
                    "HIDE APPS: ${state.hiddenGesture.label}",
                    onClick = { showHiddenPicker = true }
                )
            }
            item {
                SettingsRow(
                    "LOCK: ${state.lockGesture.label}",
                    onClick = { showLockPicker = true }
                )
            }

            item { SectionHeader("GITHUB") }
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "PERSONAL ACCESS TOKEN",
                            fontFamily = FontFamily.Monospace,
                            color = Gray,
                            fontSize = 10.sp,
                            letterSpacing = 2.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = state.gitToken,
                            onValueChange = viewModel::updateGitToken,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                            visualTransformation = if (showToken) {
                                androidx.compose.ui.text.input.VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            trailingIcon = {
                                IconButton(onClick = { showToken = !showToken }) {
                                    Icon(
                                        imageVector = if (showToken) {
                                            Icons.Filled.VisibilityOff
                                        } else {
                                            Icons.Filled.Visibility
                                        },
                                        contentDescription = if (showToken) "Hide token" else "Show token",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SettingsButton("SYNC", onClick = viewModel::refreshGit)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = state.message ?: gitSummary(state.gitStats),
                                fontFamily = FontFamily.Monospace,
                                color = if (state.message != null) Amber else Gray,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            item { SectionHeader("VAULT") }
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (state.vaultLocked) "\u25B2 VAULT LOCKED" else "\u25BC VAULT UNLOCKED",
                            fontFamily = FontFamily.Monospace,
                            color = if (state.vaultLocked) Amber else TerminalGreen,
                            fontSize = 12.sp,
                            letterSpacing = 2.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        Row {
                            if (state.vaultLocked) {
                                SettingsButton("UNLOCK", onClick = viewModel::unlockVault)
                            } else {
                                SettingsButton("LOCK", onClick = viewModel::lockVault)
                            }
                            Spacer(Modifier.width(10.dp))
                            SettingsButton("DISTRACT APPS", onClick = { showVaultApps = true })
                        }
                    }
                }
            }

            item { SectionHeader("ABOUT") }
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "D.A.R.K. v${state.versionName} (BUILD ${state.versionCode})",
                            fontFamily = FontFamily.Monospace,
                            color = TerminalGreen,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Developers' Adaptive Responsive Kernel\nA minimalist text launcher.",
                            fontFamily = FontFamily.Monospace,
                            color = Gray,
                            fontSize = 11.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }

    if (showPinDialog) {
        ChangePinDialog(
            onDismiss = { showPinDialog = false },
            onApply = { old, new ->
                viewModel.changePin(old, new) { ok, msg ->
                    if (ok) showPinDialog = false
                    viewModel.toast(msg)
                }
            }
        )
    }

    if (showVaultApps) {
        VaultAppsDialog(
            apps = state.allApps,
            selected = state.vaultApps,
            onToggle = viewModel::toggleVaultApp,
            onDismiss = { showVaultApps = false }
        )
    }

    if (showHiddenPin) {
        PinDialog(
            title = "Hidden Apps",
            onDismiss = { showHiddenPin = false },
            onVerify = viewModel::verifyPin,
            onSuccess = {
                showHiddenPin = false
                onOpenHidden()
            }
        )
    }

    if (showHiddenPicker) {
        GesturePickerDialog(
            title = "HIDE APPS GESTURE",
            current = state.hiddenGesture,
            onSelect = {
                viewModel.setHiddenGesture(it)
                showHiddenPicker = false
            },
            onDismiss = { showHiddenPicker = false }
        )
    }

    if (showLockPicker) {
        GesturePickerDialog(
            title = "LOCK GESTURE",
            current = state.lockGesture,
            onSelect = {
                viewModel.setLockGesture(it)
                showLockPicker = false
            },
            onDismiss = { showLockPicker = false }
        )
    }
}

@Composable
private fun GesturePickerDialog(
    title: String,
    current: DarkGesture,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        confirmButton = {},
        title = { Text(title, fontFamily = FontFamily.Monospace, fontSize = 15.sp) },
        text = {
            Column {
                DarkGesture.entries.forEach { gesture ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(gesture.id) }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = gesture.label,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            letterSpacing = 1.sp
                        )
                        if (gesture == current) {
                            Text(
                                text = "<",
                                fontFamily = FontFamily.Monospace,
                                color = TerminalGreen,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontFamily = FontFamily.Monospace,
        color = Gray,
        fontSize = 11.sp,
        letterSpacing = 3.sp,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp, start = 4.dp)
    )
}

@Composable
private fun SettingsRow(title: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                letterSpacing = 1.sp
            )
            Switch(
                checked = checked,
                onCheckedChange = onChecked,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TerminalGreen,
                    checkedTrackColor = TerminalGreen.copy(alpha = 0.3f)
                )
            )
        }
    }
}

@Composable
private fun SettingsButton(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 11.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
        )
    }
}

@Composable
private fun ChangePinDialog(
    onDismiss: () -> Unit,
    onApply: (old: String, new: String) -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val field = when (step) {
        0 -> oldPin
        1 -> newPin
        else -> confirmPin
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = { Text("CHANGE PIN", fontFamily = FontFamily.Monospace, fontSize = 16.sp) },
        text = {
            Column {
                OutlinedTextField(
                    value = field,
                    onValueChange = {
                        if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                            when (step) {
                                0 -> oldPin = it
                                1 -> newPin = it
                                else -> confirmPin = it
                            }
                            error = ""
                        }
                    },
                    label = {
                        Text(
                            when (step) {
                                0 -> "Current pin"
                                1 -> "New pin"
                                else -> "Confirm new pin"
                            }
                        )
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = error.isNotEmpty(),
                    supportingText = if (error.isNotEmpty()) { { Text(error, color = MaterialTheme.colorScheme.error) } } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = field.length == 4,
                onClick = {
                    when (step) {
                        0 -> {
                            step = 1
                            newPin = ""
                        }
                        1 -> {
                            step = 2
                            confirmPin = ""
                        }
                        else -> {
                            if (newPin != confirmPin) {
                                error = "pins don't match"
                            } else {
                                scope.launch {
                                    onApply(oldPin, newPin)
                                }
                            }
                        }
                    }
                }
            ) { Text("Next", color = TerminalGreen) }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (step == 0) onDismiss() else {
                        step -= 1
                        error = ""
                    }
                }
            ) { Text("Back") }
        }
    )
}

@Composable
private fun VaultAppsDialog(
    apps: List<AppInfo>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .heightIn(max = 560.dp)
                    .clickable(enabled = false) {}
            ) {
                Column(modifier = Modifier.padding(vertical = 16.dp)) {
                    Text(
                        text = "DISTRACTION APPS\n(hidden while vault is locked)",
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                    LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                        items(apps, key = { it.packageName + "_" + it.user }) { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggle(app.packageName) }
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (app.packageName in selected) TerminalGreen else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.width(22.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = app.name.uppercase(),
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                    TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                        Text("Done", color = TerminalGreen)
                    }
                }
            }
        }
    }
}

private fun gitSummary(stats: com.dark.launcher.data.model.GitStats): String {
    if (!stats.synced) return "not synced"
    val fmt = SimpleDateFormat("MMM d HH:mm", Locale.getDefault())
    return "${stats.repos} repos \u00B7 ${stats.commits} commits \u00B7 synced ${fmt.format(Date(stats.lastSync))}"
}
