package com.dark.launcher.ui.recorder

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dark.launcher.data.model.Recording
import com.dark.launcher.ui.theme.Amber
import com.dark.launcher.ui.theme.Gray
import com.dark.launcher.ui.theme.TerminalGreen
import com.dark.launcher.util.openEdit
import com.dark.launcher.util.shareRecording
import kotlinx.coroutines.delay

@Composable
fun RecorderScreen(
    viewModel: RecorderViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    var playingPath by remember { mutableStateOf<String?>(null) }
    var showOverlayPermission by remember { mutableStateOf(false) }

    BackHandler { onBack() }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    LaunchedEffect(Unit) {
        while (true) {
            viewModel.refreshOverlayStateNow()
            delay(1500)
        }
    }

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
                text = "D. A. R. K. RECORDER",
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 15.sp,
                letterSpacing = 3.sp
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                onClick = {
                    if (!viewModel.overlayPermissionGranted()) {
                        showOverlayPermission = true
                    } else {
                        viewModel.toggleOverlay {}
                    }
                },
                shape = RoundedCornerShape(16.dp),
                color = if (state.overlayRunning) TerminalGreen else MaterialTheme.colorScheme.surface
            ) {
                Text(
                    text = "OVERLAY: ${if (state.overlayRunning) "ON" else "OFF"}",
                    fontFamily = FontFamily.Monospace,
                    color = if (state.overlayRunning) androidx.compose.ui.graphics.Color.Black else MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)
                )
            }
            Surface(
                onClick = {
                    viewModel.requestRecording {
                        showOverlayPermission = true
                    }
                },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (state.recording) Icons.Rounded.FiberManualRecord else Icons.Rounded.Videocam,
                        contentDescription = null,
                        tint = if (state.recording) TerminalGreen else Amber,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = if (state.recording) "RECORDING..." else "RECORD",
                        fontFamily = FontFamily.Monospace,
                        color = if (state.recording) TerminalGreen else MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = "> SAVED RECORDINGS  \u00B7  ${state.recordings.size}",
            fontFamily = FontFamily.Monospace,
            color = Gray,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(Modifier.height(8.dp))

        if (state.recordings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "NO RECORDINGS YET\n\nTap RECORD to capture your screen.\nThe floating bubble lives over other apps.",
                    fontFamily = FontFamily.Monospace,
                    color = Gray,
                    fontSize = 12.sp,
                    lineHeight = 19.sp,
                    letterSpacing = 1.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.recordings, key = { it.path }) { rec ->
                    RecordingRow(
                        recording = rec,
                        onPlay = { playingPath = rec.path },
                        onDelete = { viewModel.delete(rec) },
                        onShare = { shareRecording(context, java.io.File(rec.path)) },
                        onEdit = { openEdit(context, java.io.File(rec.path)) }
                    )
                }
            }
        }
    }

    playingPath?.let { path ->
        VideoPlayerDialog(
            filePath = path,
            onDismiss = { playingPath = null }
        )
    }

    if (showOverlayPermission) {
        AlertDialog(
            onDismissRequest = { showOverlayPermission = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("Draw over other apps") },
            text = {
                Text("D.A.R.K. needs permission to draw the floating recorder bubble and recording controls over other apps.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showOverlayPermission = false
                        runCatching {
                            context.startActivity(viewModel.overlayPermissionIntent())
                        }
                    }
                ) { Text("Grant", color = TerminalGreen) }
            },
            dismissButton = {
                TextButton(onClick = { showOverlayPermission = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun RecordingRow(
    recording: Recording,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = "\u25B6  ${recording.name}",
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                letterSpacing = 1.sp,
                maxLines = 1,
                softWrap = false
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "${recording.durationLabel}  \u00B7  ${recording.sizeLabel}",
                fontFamily = FontFamily.Monospace,
                color = Gray,
                fontSize = 11.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ActionChip("PLAY", Icons.Rounded.PlayArrow, TerminalGreen, onPlay)
                ActionChip("SHARE", Icons.Rounded.Share, Amber, onShare)
                ActionChip("EDIT", Icons.Rounded.Edit, Amber, onEdit)
                ActionChip("DELETE", Icons.Rounded.Delete, MaterialTheme.colorScheme.error, onDelete)
            }
        }
    }
}

@Composable
private fun ActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(5.dp))
            Text(
                text = label,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 10.sp,
                letterSpacing = 1.sp
            )
        }
    }
}
