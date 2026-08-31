package com.dark.launcher.service.overlay

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.ImageView
import com.dark.launcher.ui.theme.RecorderGreen
import com.dark.launcher.ui.theme.RecorderGreenAccent
import com.dark.launcher.ui.theme.RecorderGreenLight
import com.dark.launcher.ui.theme.RecorderRed
import com.dark.launcher.ui.theme.RecorderSheetBg
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.flow.StateFlow

private val NodeWhite = Color.White
private val CenterDark = Color(0xFF1A1A1A)
private val CenterBorder = Color(0xFF3A3A3C)

data class RadialNode(
    val icon: ImageVector,
    val tint: Color,
    val contentDescription: String,
    val onClick: () -> Unit,
    val innerDot: Boolean = false
)

/** Semi-circular edge-docked bubble — turns into a circle while dragging */
@Composable
fun BubbleView(
    sizeDp: Dp,
    dockRight: Boolean,
    dragging: Boolean,
    retracted: Boolean,
    recording: Boolean,
    elapsed: StateFlow<String>,
    paused: StateFlow<Boolean>,
    onTap: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (androidx.compose.ui.geometry.Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    val elapsedText by elapsed.collectAsState()
    val isPaused by paused.collectAsState()
    val corner = sizeDp / 2
    val shape = if (dragging) {
        CircleShape
    } else if (dockRight) {
        RoundedCornerShape(topStart = corner, bottomStart = corner)
    } else {
        RoundedCornerShape(topEnd = corner, bottomEnd = corner)
    }
    Box(
        modifier = Modifier
            .size(sizeDp)
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(RecorderGreen, RecorderGreenLight)
                )
            )
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onTap() })
            }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart() },
                    onDrag = { _, amount -> onDrag(amount) },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (recording) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.offset(x = if (retracted && !dragging) (if (dockRight) 6.dp else (-6).dp) else 0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(sizeDp * 0.14f)
                        .clip(CircleShape)
                        .background(if (isPaused) Color(0xFFFFB300) else RecorderRed)
                )
                Spacer(Modifier.width(sizeDp * 0.07f))
                Text(
                    text = elapsedText,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = (sizeDp.value * 0.21f).sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        } else {
            Icon(
                imageVector = Icons.Rounded.Videocam,
                contentDescription = "Screen recorder",
                tint = Color.White,
                modifier = Modifier
                    .size(sizeDp * 0.46f)
                    .offset(x = if (retracted && !dragging) (if (dockRight) 6.dp else (-6).dp) else 0.dp)
            )
        }
    }
}

@Composable
fun RadialMenuView(
    dir: Int,
    nodes: List<RadialNode>,
    scale: Float = 1f,
    onCenter: () -> Unit
) {
    val radius = 90.dp * scale
    val startAngle = 110f
    val step = 45f
    Box(
        modifier = Modifier.size(260.dp * scale, 280.dp * scale),
        contentAlignment = Alignment.Center
    ) {
        nodes.forEachIndexed { i, node ->
            val offset = radialOffset(i, radius, startAngle, step, dir)
            Box(
                modifier = Modifier.offset(x = offset.x, y = offset.y),
                contentAlignment = Alignment.Center
            ) {
                RadialNodeView(node, scale)
            }
        }
        Box(
            modifier = Modifier
                .size(52.dp * scale)
                .clip(CircleShape)
                .background(CenterDark)
                .border(2.dp * scale, CenterBorder, CircleShape)
                .clickable(
                    interactionSource = rememberNoIndication(),
                    indication = null,
                    onClick = onCenter
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier.size(22.dp * scale)
            )
        }
    }
}

@Composable
private fun RadialNodeView(node: RadialNode, scale: Float = 1f) {
    Box(
        modifier = Modifier
            .size(52.dp * scale)
            .clip(CircleShape)
            .background(NodeWhite)
            .clickable(
                interactionSource = rememberNoIndication(),
                indication = null,
                onClick = node.onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (node.innerDot) {
            Box(
                modifier = Modifier
                    .size(22.dp * scale)
                    .clip(CircleShape)
                    .background(RecorderRed)
            )
        } else {
            Icon(
                imageVector = node.icon,
                contentDescription = node.contentDescription,
                tint = node.tint,
                modifier = Modifier.size(24.dp * scale)
            )
        }
    }
}

/** Full-screen countdown overlay */
@Composable
fun CountdownOverlayView(
    number: Int,
    onStartNow: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = number.toString(),
                color = RecorderGreen,
                fontSize = 160.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )
            Spacer(Modifier.height(28.dp))
            Surface(
                onClick = onStartNow,
                shape = RoundedCornerShape(28.dp),
                color = RecorderGreen
            ) {
                Text(
                    text = "Start Now",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 36.dp, vertical = 14.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Cancel",
                color = Color(0xFFAAAAAA),
                fontSize = 14.sp,
                modifier = Modifier
                    .clickable(
                        interactionSource = rememberNoIndication(),
                        indication = null,
                        onClick = onCancel
                    )
                    .padding(12.dp)
            )
        }
    }
}

@Composable
fun RecordingControlsView(
    dir: Int,
    elapsed: StateFlow<String>,
    paused: StateFlow<Boolean>,
    scale: Float = 1f,
    onPauseResume: () -> Unit,
    onStop: () -> Unit,
    onBrush: () -> Unit,
    onTools: () -> Unit
) {
    val elapsedText by elapsed.collectAsState()
    val isPaused by paused.collectAsState()
    val radius = 85.dp * scale
    val startAngle = 100f
    val step = 40f
    val nodes = listOf(
        RadialNode(
            icon = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
            tint = RecorderGreenAccent,
            contentDescription = if (isPaused) "Resume" else "Pause",
            onClick = onPauseResume
        ),
        RadialNode(
            icon = Icons.Rounded.Stop,
            tint = RecorderRed,
            contentDescription = "Stop",
            onClick = onStop
        ),
        RadialNode(
            icon = Icons.Rounded.Brush,
            tint = RecorderGreenAccent,
            contentDescription = "Draw",
            onClick = onBrush
        ),
        RadialNode(
            icon = Icons.Rounded.Build,
            tint = RecorderGreenAccent,
            contentDescription = "Tools",
            onClick = onTools
        )
    )
    Column(
        modifier = Modifier.size(220.dp * scale, 300.dp * scale),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = elapsedText,
            fontFamily = FontFamily.Monospace,
            fontSize = 24.sp * scale,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp * scale,
            color = Color.White,
            modifier = Modifier.padding(top = 4.dp * scale, bottom = 8.dp * scale)
        )
        Box(
            modifier = Modifier.size(200.dp * scale, 240.dp * scale),
            contentAlignment = Alignment.Center
        ) {
            nodes.forEachIndexed { i, node ->
                val offset = radialOffset(i, radius, startAngle, step, dir)
                Box(
                    modifier = Modifier.offset(x = offset.x, y = offset.y),
                    contentAlignment = Alignment.Center
                ) {
                    if (i == 1) {
                        // Stop button — red square inside white circle
                        Box(
                            modifier = Modifier
                                .size(52.dp * scale)
                                .clip(CircleShape)
                                .background(NodeWhite)
                                .clickable(
                                    interactionSource = rememberNoIndication(),
                                    indication = null,
                                    onClick = onStop
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp * scale)
                                    .background(RecorderRed, RoundedCornerShape(3.dp * scale))
                            )
                        }
                    } else {
                        RadialNodeView(node, scale)
                    }
                }
            }
        }
    }
}

@Composable
fun TrashTargetView(over: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (over) 1.2f else 1f,
        animationSpec = spring(),
        label = "trash_scale"
    )
    Box(
        modifier = Modifier
            .size(88.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(if (over) Color(0xCCFF1744) else Color(0x99000000))
            .border(2.dp, if (over) RecorderRed else Color(0xFF4A4A4C), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = "Remove",
            tint = Color.White,
            modifier = Modifier.size(36.dp)
        )
    }
}

@Composable
fun PostSheetView(
    fileName: String,
    sizeLabel: String,
    durationLabel: String,
    thumbnailPath: String?,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onTrim: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = RecorderSheetBg,
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "\u25C6  Recording saved",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close",
                    tint = Color(0xFFAAAAAA),
                    modifier = Modifier
                        .size(28.dp)
                        .clickable(
                            interactionSource = rememberNoIndication(),
                            indication = null,
                            onClick = onClose
                        )
                )
            }
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0A0A0A)),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnailPath != null) {
                    AndroidView(
                        factory = { ctx ->
                            ImageView(ctx).apply {
                                scaleType = ImageView.ScaleType.CENTER_CROP
                                setImageURI(android.net.Uri.parse("file://$thumbnailPath"))
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0x66FFFFFF))
                        .clickable(
                            interactionSource = rememberNoIndication(),
                            indication = null,
                            onClick = onPlay
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = fileName,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = Color(0xFFBDBDBD),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$sizeLabel \u00B7 $durationLabel",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = Color(0xFF757575)
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PostAction("Delete", Icons.Rounded.Delete, Color(0xFFEF5350), onDelete)
                PostAction("Edit", Icons.Rounded.Edit, RecorderGreenAccent, onEdit)
                PostAction("Trim", Icons.Rounded.ContentCut, RecorderGreenAccent, onTrim)
                PostAction("Share", Icons.Rounded.Share, RecorderGreenAccent, onShare)
            }
        }
    }
}

@Composable
private fun PostAction(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = rememberNoIndication(),
                indication = null,
                onClick = onClick
            )
            .padding(8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}

private fun radialOffset(index: Int, radius: Dp, startAngleDeg: Float, stepDeg: Float, dir: Int): DpOffset {
    val angleRad = Math.toRadians((startAngleDeg + index * stepDeg).toDouble())
    val x = (radius.value * cos(angleRad)).dp
    val y = (radius.value * sin(angleRad)).dp
    return DpOffset(x * dir, y)
}

@Composable
private fun rememberNoIndication(): MutableInteractionSource =
    androidx.compose.runtime.remember { MutableInteractionSource() }
