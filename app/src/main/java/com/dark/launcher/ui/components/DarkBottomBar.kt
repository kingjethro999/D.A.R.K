package com.dark.launcher.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.dark.launcher.data.repo.NowPlaying
import com.dark.launcher.ui.navigation.DarkRoutes
import com.dark.launcher.ui.theme.Amber
import com.dark.launcher.ui.theme.Gray
import com.dark.launcher.ui.theme.SurfaceHigh
import com.dark.launcher.ui.theme.TerminalGreen
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

enum class DarkTab(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    Home(DarkRoutes.HOME, "Home", Icons.Rounded.Home),
    Settings(DarkRoutes.SETTINGS, "Settings", Icons.Rounded.Settings),
    Terminal(DarkRoutes.TERMINAL, "Terminal", Icons.Rounded.Terminal),
    Recorder(DarkRoutes.RECORDER, "Recorder", Icons.Rounded.RadioButtonChecked),
}

@Composable
fun DarkBottomBar(
    currentRoute: String?,
    steps: Int,
    nowPlaying: NowPlaying?,
    onNavigate: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp),
        shape = CircleShape,
        color = SurfaceHigh.copy(alpha = 0.97f),
        shadowElevation = 12.dp,
        tonalElevation = 4.dp,
    ) {
        val scrollState = rememberScrollState()
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            val navWidth = maxWidth
            Row(
                modifier = Modifier
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DarkTab.entries.forEach { tab ->
                    val selected = currentRoute == tab.route
                    TabPill(tab = tab, selected = selected, onClick = { onNavigate(tab.route) })
                }
                StepsPill(steps = steps)
                MusicDisplay(
                    width = navWidth,
                    media = nowPlaying,
                )
            }
        }
    }
}

@Composable
private fun TabPill(
    tab: DarkTab,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) TerminalGreen else SurfaceHigh,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "tabBg",
    )

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(backgroundColor, shape = CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                onClick = onClick,
            )
            .padding(
                horizontal = if (selected) 18.dp else 14.dp,
                vertical = 10.dp,
            )
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessHigh,
                )
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = if (selected) androidx.compose.ui.graphics.Color.Black else Gray,
            modifier = Modifier.size(22.dp),
        )
        if (selected) {
            Spacer(Modifier.width(7.dp))
            Text(
                text = tab.label,
                color = androidx.compose.ui.graphics.Color.Black,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold,
                softWrap = false,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun StepsPill(steps: Int) {
    var showTip by remember { mutableStateOf(false) }
    LaunchedEffect(showTip) {
        if (showTip) {
            delay(2200)
            showTip = false
        }
    }
    val digits = steps.toString().length
    val fontSize = when {
        digits >= 4 -> 13.sp
        digits == 3 -> 16.sp
        else -> 20.sp
    }
    Box(contentAlignment = Alignment.Center) {
        if (showTip) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(1f)
                    .offset(y = (-10).dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(TerminalGreen)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "Steps",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    color = androidx.compose.ui.graphics.Color.Black,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(SurfaceHigh, shape = CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = LocalIndication.current,
                    onClick = { showTip = true },
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = steps.toString(),
                fontFamily = FontFamily.Monospace,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = TerminalGreen,
            )
        }
    }
}

@Composable
private fun MusicDisplay(width: Dp, media: NowPlaying?) {
    val text = if (media == null) {
        "no media playing"
    } else {
        buildString {
            append(media.title)
            media.artist?.takeIf { it.isNotBlank() }?.let { append(" - $it") }
        }
    }
    Row(
        modifier = Modifier
            .width(width)
            .clip(CircleShape)
            .background(SurfaceHigh, shape = CircleShape)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = if (media == null) Gray else Amber,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(10.dp))
        MarqueeText(
            text = text,
            modifier = Modifier.weight(1f),
            color = if (media == null) Gray else Amber,
            fontSize = 13.sp,
        )
        if (media != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = "NOW",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                letterSpacing = 1.sp,
                color = Amber,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun MarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color,
    fontSize: TextUnit,
) {
    var containerWidth by remember { mutableStateOf(0) }
    var textWidth by remember { mutableStateOf(0) }
    val overflow = containerWidth > 0 && textWidth > containerWidth
    val offsetX = remember { Animatable(0f) }
    LaunchedEffect(overflow, textWidth, containerWidth) {
        if (overflow) {
            val distance = (textWidth - containerWidth).toFloat()
            while (true) {
                offsetX.snapTo(0f)
                offsetX.animateTo(
                    targetValue = -distance,
                    animationSpec = tween(
                        durationMillis = (distance * 30f).roundToInt().coerceAtLeast(2500),
                        easing = LinearEasing,
                    ),
                )
                delay(1000)
            }
        } else {
            offsetX.snapTo(0f)
        }
    }
    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { containerWidth = it.width },
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            maxLines = 1,
            softWrap = false,
            fontFamily = FontFamily.Monospace,
            fontSize = fontSize,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            onTextLayout = { textWidth = it.size.width },
            modifier = Modifier.offset { IntOffset(offsetX.value.roundToInt(), 0) },
        )
    }
}
