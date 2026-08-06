package com.dark.launcher.ui.home

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dark.launcher.R
import com.dark.launcher.data.model.AppInfo
import com.dark.launcher.util.launchApp
import com.dark.launcher.util.requestLockScreen
import com.dark.launcher.ui.theme.Amber
import com.dark.launcher.ui.theme.TerminalGreen
import com.dark.launcher.ui.theme.Gray
import com.dark.launcher.util.darkTaps
import com.dark.launcher.util.darkMultiFingerSwipe
import kotlinx.coroutines.flow.first

private val Galaxy = FontFamily(Font(R.font.galaxy_game_plays))

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenSettings: () -> Unit,
    onOpenTerminal: () -> Unit,
    onOpenHidden: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val clock by viewModel.clock.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showLockDialog by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var searchFocused by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val activityRecognitionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.markStepsPermissionRequested()
        viewModel.ensureStepsRunning()
        if (!granted) {
            android.widget.Toast.makeText(context, "step access denied - steps will not count", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshIsDefault()
        viewModel.refreshGit()
        viewModel.ensureStepsRunning()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val granted = context.checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) {
                val alreadyRequested = viewModel.stepsPermissionRequestedFlow().first()
                if (!alreadyRequested) {
                    activityRecognitionLauncher.launch(android.Manifest.permission.ACTIVITY_RECOGNITION)
                }
            }
        }
    }

    val onGesture = { gesture: com.dark.launcher.util.DarkGesture ->
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        when (gesture) {
            state.hiddenGesture -> showPinDialog = true
            state.lockGesture -> {
                if (viewModel.isLockEnabled()) {
                    requestLockScreen(context)
                } else {
                    showLockDialog = true
                }
            }
            else -> Unit
        }
    }

    val onMultiFingerSwipe = { dir: com.dark.launcher.util.SwipeDirection ->
        val gesture = when (dir) {
            com.dark.launcher.util.SwipeDirection.UP -> com.dark.launcher.util.DarkGesture.SWIPE_UP_3
            com.dark.launcher.util.SwipeDirection.DOWN -> com.dark.launcher.util.DarkGesture.SWIPE_DOWN_3
        }
        onGesture(gesture)
    }

    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { viewModel.refreshIsDefault() }

    val healthLauncher = rememberLauncherForActivityResult(
        viewModel.healthPermissionContract()
    ) { viewModel.refreshHealth() }

    BackHandler(enabled = true) {
        if (searchFocused) {
            keyboard?.hide()
            focusManager.clearFocus()
            query = ""
            viewModel.setSearchQuery("")
        }
    }

    val notificationConnection = remember {
        object : NestedScrollConnection {
            var totalDown = 0f
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y > 0) {
                    totalDown += available.y
                    if (totalDown > 60f) {
                        totalDown = 0f
                        viewModel.expandNotifications()
                    }
                } else {
                    totalDown = 0f
                }
                return Offset.Zero
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .nestedScroll(notificationConnection)
            .darkTaps(
                onDoubleTap = { onGesture(com.dark.launcher.util.DarkGesture.DOUBLE_TAP) },
                onTripleTap = { onGesture(com.dark.launcher.util.DarkGesture.TRIPLE_TAP) }
            )
            .darkMultiFingerSwipe(onSwiped = onMultiFingerSwipe)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(12.dp))

        AnimatedVisibility(
            visible = !state.isDefaultLauncher,
            enter = slideInVertically(spring<IntOffset>()) + fadeIn(),
            exit = slideOutVertically(spring<IntOffset>()) + fadeOut()
        ) {
            DefaultLauncherBanner(
                onClick = {
                    val intent = viewModel.defaultLauncherIntent()
                    if (intent != null) roleLauncher.launch(intent)
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "D. A. R. K.",
                fontFamily = Galaxy,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 34.sp,
                letterSpacing = 8.sp,
                fontWeight = FontWeight.Normal
            )
            if (state.showTime) {
                Text(
                    text = clock.first,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 26.sp
                )
            }
        }

        Text(
            text = clock.second,
            fontFamily = FontFamily.Monospace,
            color = Gray,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        AnimatedVisibility(
            visible = state.showGit && state.gitStats.synced,
            enter = slideInVertically(spring<IntOffset>()) + fadeIn(),
            exit = slideOutVertically(spring<IntOffset>()) + fadeOut()
        ) {
            Text(
                text = "> REPOS ${state.gitStats.repos} \u00B7 STARS ${state.gitStats.stars} \u00B7 COMMITS ${state.gitStats.commits} \u00B7 BEST ${state.gitStats.bestMonth}" +
                    if (state.gitOffline) " \u00B7 OFFLINE" else "",
                fontFamily = FontFamily.Monospace,
                color = if (state.gitOffline) Amber else TerminalGreen.copy(alpha = 0.8f),
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        AnimatedVisibility(
            visible = state.showFitness &&
                (state.fitness.stepsToday > 0 || state.fitness.workouts > 0 || state.fitness.sprintAvg != null),
            enter = slideInVertically(spring<IntOffset>()) + fadeIn(),
            exit = slideOutVertically(spring<IntOffset>()) + fadeOut()
        ) {
            Text(
                text = buildFitnessLine(state.fitness),
                fontFamily = FontFamily.Monospace,
                color = TerminalGreen.copy(alpha = 0.6f),
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        AnimatedVisibility(
            visible = state.nowPlaying != null,
            enter = slideInVertically(spring<IntOffset>()) + fadeIn(),
            exit = slideOutVertically(spring<IntOffset>()) + fadeOut()
        ) {
            val np = state.nowPlaying ?: return@AnimatedVisibility
            Text(
                text = "> \u266A ${np.title}${np.artist?.let { " - $it" } ?: ""}",
                fontFamily = FontFamily.Monospace,
                color = Amber.copy(alpha = 0.9f),
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        Spacer(Modifier.height(20.dp))

        SearchField(
            query = query,
            onQueryChange = {
                query = it
                viewModel.setSearchQuery(it)
            },
            onFocusChanged = { searchFocused = it }
        )

        Spacer(Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(state.apps, key = { it.packageName + "_" + it.user }) { app ->
                AppRow(
                    app = app,
                    onClick = {
                        when (app.packageName) {
                            AppInfo.INTERNAL_SETTINGS -> onOpenSettings()
                            AppInfo.INTERNAL_TERMINAL -> onOpenTerminal()
                            else -> launchApp(context, app)
                        }
                    },
                    onLongClick = { selectedApp = app }
                )
            }
        }
    }

    selectedApp?.let { app ->
        AppMenuDialog(
            app = app,
            onDismiss = { selectedApp = null },
            onHide = { viewModel.hideApp(it) }
        )
    }

    if (showPinDialog) {
        PinDialog(
            title = "Hidden Apps",
            onDismiss = { showPinDialog = false },
            onVerify = viewModel::verifyPin,
            onSuccess = {
                showPinDialog = false
                onOpenHidden()
            }
        )
    }

    if (showLockDialog) {
        AlertDialog(
            onDismissRequest = { showLockDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("Screen Lock") },
            text = {
                Text("Double-tap lock needs Accessibility permission. Enable it in system settings to lock the screen from the home screen.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLockDialog = false
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                ) { Text("Go to Settings", color = TerminalGreen) }
            },
            dismissButton = {
                TextButton(onClick = { showLockDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (state.showHealthPrompt) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissHealthPrompt() },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("Health Connect") },
            text = {
                Text("Link Health Connect / Samsung Health to show your daily step count on the home screen. If steps stay at 0, open Health Connect and connect Samsung Health.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        healthLauncher.launch(setOf(viewModel.stepsPermission()))
                    }
                ) { Text("Connect", color = TerminalGreen) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissHealthPrompt() }) { Text("Not Now") }
            }
        )
    }
}

@Composable
private fun DefaultLauncherBanner(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "\u25CF NOT SET AS HOME",
                fontFamily = FontFamily.Monospace,
                color = Amber,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )
            Text(
                text = "SET \u2192",
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp)
            )
            .border(
                width = 1.dp,
                color = if (focused) TerminalGreen else Gray.copy(alpha = 0.4f),
                shape = RoundedCornerShape(10.dp)
            )
            .onFocusChanged {
                focused = it.isFocused
                onFocusChanged(it.isFocused)
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = "> SEARCH APPS...",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp,
                    color = Gray,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                cursorBrush = SolidColor(TerminalGreen),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            )
        }
        if (query.isNotEmpty()) {
            TextButton(onClick = { onQueryChange("") }) {
                Text(
                    text = "CLEAR",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    color = Gray
                )
            }
        }
    }
}

@Composable
private fun AppRow(
    app: AppInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Text(
        text = "\u25B8  " + app.name.uppercase(),
        fontFamily = FontFamily.Monospace,
        color = if (app.isInternal) TerminalGreen.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onBackground,
        fontSize = 15.sp,
        letterSpacing = 2.sp,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 9.dp, horizontal = 2.dp)
    )
}

private fun buildFitnessLine(fitness: com.dark.launcher.data.repo.FitnessSummary): String {
    val parts = mutableListOf<String>()
    if (fitness.stepsToday > 0) parts.add("${fitness.stepsToday} STEPS")
    if (fitness.workouts > 0) parts.add("WEEK ${fitness.workouts} WORKOUTS")
    fitness.sprintAvg?.let { parts.add("SPRINT $it") }
    return "> " + parts.joinToString(" \u00B7 ")
}
