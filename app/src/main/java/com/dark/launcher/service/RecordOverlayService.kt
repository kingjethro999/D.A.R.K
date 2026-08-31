package com.dark.launcher.service

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.dark.launcher.R
import com.dark.launcher.data.model.Recording
import com.dark.launcher.data.repo.LauncherSettingsRepository
import com.dark.launcher.data.repo.RecorderRepository
import com.dark.launcher.service.overlay.BubbleView
import com.dark.launcher.service.overlay.CountdownOverlayView
import com.dark.launcher.service.overlay.PostSheetView
import com.dark.launcher.service.overlay.RadialMenuView
import com.dark.launcher.service.overlay.RadialNode
import com.dark.launcher.service.overlay.RecordingControlsView
import com.dark.launcher.service.overlay.TrashTargetView
import com.dark.launcher.ui.theme.DarkTheme
import com.dark.launcher.ui.theme.RecorderGreenAccent
import com.dark.launcher.util.openEdit
import com.dark.launcher.util.openTrim
import com.dark.launcher.util.openDarkHome
import com.dark.launcher.util.openDarkSettings
import com.dark.launcher.util.openRecorderApp
import com.dark.launcher.util.shareRecording
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.util.Locale
import javax.inject.Inject
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RecordOverlayService : Service() {

    @Inject lateinit var recorderRepo: RecorderRepository
    @Inject lateinit var settingsRepo: LauncherSettingsRepository

    private enum class Phase { OFF, IDLE, DRAGGING, PENDING, MENU, COUNTDOWN, RECORDING, PAUSED, POST }

    private lateinit var wm: WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val overlayLifecycleOwner: LifecycleOwner = object : LifecycleOwner {
        override val lifecycle: Lifecycle get() = overlayLifecycle
    }
    private val overlayLifecycle = LifecycleRegistry(overlayLifecycleOwner)
    private val overlayViewModelStoreOwner: ViewModelStoreOwner = object : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore get() = ViewModelStore()
    }
    private val overlaySavedStateRegistryOwner: SavedStateRegistryOwner = object : SavedStateRegistryOwner {
        override val savedStateRegistry: SavedStateRegistry get() = overlaySavedStateRegistryController.savedStateRegistry
        override val lifecycle: Lifecycle get() = overlayLifecycle
    }
    private val overlaySavedStateRegistryController: androidx.savedstate.SavedStateRegistryController by lazy {
        androidx.savedstate.SavedStateRegistryController.create(overlaySavedStateRegistryOwner)
    }

    private var phase = Phase.OFF
    private var dockEdge = EDGE_RIGHT
    private var retracted = false
    private var dragOverTrash = false
    private var stopAfterPost = false

    private val bubbleSizePx = mutableStateOf(0)
    private val draggingState = mutableStateOf(false)
    private val bubbleRight = mutableStateOf(true)

    private var bubbleView: ComposeView? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var menuView: ComposeView? = null
    private var countdownView: ComposeView? = null
    private var controlsView: ComposeView? = null
    private var trashView: ComposeView? = null
    private var sheetView: ComposeView? = null

    private var countdownJob: Job? = null
    private var timerJob: Job? = null
    private var elapsedSeconds = 0
    private val elapsedText = MutableStateFlow("00:00")
    private val pausedFlow = MutableStateFlow(false)
    private val countdownNumber = mutableIntStateOf(5)
    private val dragOverTrashFlow = mutableStateOf(false)
    private val toastText = MutableStateFlow<String?>(null)

    private var projection: MediaProjection? = null
    private var mediaRecorder: MediaRecorder? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var currentFile: File? = null
    private var currentThumb: File? = null
    private var pendingResultCode = 0
    private var pendingResultData: Intent? = null

    private var animator: android.animation.ValueAnimator? = null
    private val retractRunnable = Runnable { retract() }

    private val recv = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) {
            when (i?.action) {
                ACTION_RECORD_CANCELED -> {
                    if (phase == Phase.PENDING || phase == Phase.COUNTDOWN) cancelToIdle()
                }
                ACTION_STOP_RECORDING -> {
                    if (phase == Phase.RECORDING || phase == Phase.PAUSED) stopRecording()
                }
                ACTION_STOP -> stopSelf()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        bubbleSizePx.value = dp(LauncherSettingsRepository.DEFAULT_OVERLAY_SIZE)
        overlaySavedStateRegistryController.performRestore(null)
        overlayLifecycle.currentState = Lifecycle.State.RESUMED
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createChannel()
        val filter = IntentFilter().apply {
            addAction(ACTION_RECORD_CANCELED)
            addAction(ACTION_STOP_RECORDING)
            addAction(ACTION_STOP)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(recv, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(recv, filter)
        }
        scope.launch {
            settingsRepo.recOverlaySizeFlow.collect { sizeDp ->
                bubbleSizePx.value = dp(sizeDp)
                val p = bubbleParams
                if (phase == Phase.IDLE && bubbleView != null && p != null) {
                    val (x, y) = dockPositionFor(dockEdge)
                    p.width = bubbleSizePx.value
                    p.height = bubbleSizePx.value
                    p.x = x
                    p.y = y
                    runCatching { wm.updateViewLayout(bubbleView, p) }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> startRecordingFlow(intent)
            ACTION_STOP_RECORDING -> {
                if (phase == Phase.RECORDING || phase == Phase.PAUSED) stopRecording()
            }
            ACTION_STOP -> stopSelf()
            else -> startOverlay(intent)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ---------------- Overlay start ----------------

    private fun startOverlay(intent: Intent?) {
        if (!Settings.canDrawOverlays(this)) {
            toast("grant draw-over-other-apps first")
            stopSelf()
            return
        }
        stopAfterPost = intent?.getBooleanExtra(EXTRA_STOP_AFTER_POST, false) ?: stopAfterPost
        ensureForeground(bubbleForegroundType())
        if (phase == Phase.OFF) {
            phase = Phase.IDLE
            val (x, y) = dockPositionFor(dockEdge)
            showBubble(x, y)
            scheduleRetract()
        }
    }

    // ---------------- Recording flow ----------------

    private fun startRecordingFlow(intent: Intent) {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        ensureForeground(bubbleForegroundType())
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
        val data: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_RESULT_DATA)
        }
        if (resultCode != Activity.RESULT_OK || data == null) {
            onProjectionCanceled()
            return
        }
        pendingResultCode = resultCode
        pendingResultData = data
        currentFile = recorderRepo.newRecordingFile()
        startCountdown()
    }

    private fun onProjectionCanceled() {
        if (phase == Phase.PENDING) {
            phase = Phase.IDLE
            val (x, y) = dockPositionFor(dockEdge)
            showBubble(x, y)
            scheduleRetract()
        }
    }

    private fun startCountdown() {
        phase = Phase.COUNTDOWN
        cancelRetract()
        stopAnimator()
        removeView(menuView)
        removeView(bubbleView)
        countdownNumber.intValue = 5
        showCountdown()
        countdownJob = scope.launch {
            for (n in 5 downTo 2) {
                countdownNumber.intValue = n
                delay(1000)
            }
            countdownNumber.intValue = 1
            delay(1000)
            startRecording()
        }
    }

    private fun cancelCountdown() {
        countdownJob?.cancel()
        cancelToIdle()
    }

    private fun cancelToIdle() {
        countdownJob?.cancel()
        removeView(countdownView)
        phase = Phase.IDLE
        val (x, y) = dockPositionFor(dockEdge)
        showBubble(x, y)
        scheduleRetract()
    }

    private fun startRecording() {
        countdownJob?.cancel()
        removeView(countdownView)
        val file = currentFile ?: run { cancelToIdle(); return }
        if (!beginMediaProjection(file)) {
            ensureForeground(bubbleForegroundType())
            cancelToIdle()
            return
        }
        phase = Phase.RECORDING
        isRecording = true
        pausedFlow.value = false
        elapsedSeconds = 0
        elapsedText.value = "00:00"
        showRecordingBubble()
        timerJob = scope.launch {
            while (isActive) {
                delay(1000)
                elapsedSeconds++
                val t = formatElapsed(elapsedSeconds)
                elapsedText.value = t
                updateRecordingNotification(t)
            }
        }
    }

    private fun beginMediaProjection(file: File): Boolean {
        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val data = pendingResultData ?: return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, recordingNotification("00:00"), projectionForegroundType())
        } else {
            startForeground(NOTIF_ID, recordingNotification("00:00"))
        }
        val proj = mpm.getMediaProjection(pendingResultCode, data) ?: return false
        projection = proj
        proj.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                if (phase == Phase.RECORDING || phase == Phase.PAUSED) stopRecording()
            }
        }, mainHandler)
        val metrics = android.util.DisplayMetrics().also { wm.defaultDisplay.getRealMetrics(it) }
        val w = metrics.widthPixels
        val h = metrics.heightPixels
        val dpi = metrics.densityDpi
        val recorder = MediaRecorder()
        try {
            mediaRecorder = recorder
            recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            val hasMic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            if (hasMic) recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            if (hasMic) recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setVideoSize(w, h)
            recorder.setVideoFrameRate(30)
            recorder.setVideoEncodingBitRate(12_000_000)
            recorder.setOutputFile(file.absolutePath)
            recorder.prepare()
            virtualDisplay = proj.createVirtualDisplay(
                "DARK-REC",
                w, h, dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                recorder.surface, null, mainHandler
            )
            recorder.start()
            return true
        } catch (e: Exception) {
            runCatching { recorder.release() }
            mediaRecorder = null
            releaseProjection()
            return false
        }
    }

    private fun togglePause() {
        when (phase) {
            Phase.RECORDING -> {
                runCatching { mediaRecorder?.pause() }
                phase = Phase.PAUSED
                timerJob?.cancel()
                pausedFlow.value = true
                updateRecordingNotification("Paused ${elapsedText.value}")
            }
            Phase.PAUSED -> {
                runCatching { mediaRecorder?.resume() }
                phase = Phase.RECORDING
                pausedFlow.value = false
                timerJob = scope.launch {
                    while (isActive) {
                        delay(1000)
                        elapsedSeconds++
                        val t = formatElapsed(elapsedSeconds)
                        elapsedText.value = t
                        updateRecordingNotification(t)
                    }
                }
            }
            else -> Unit
        }
    }

    private fun stopRecording() {
        timerJob?.cancel()
        if (phase == Phase.RECORDING || phase == Phase.PAUSED) {
            phase = Phase.POST
            isRecording = false
            finishProjection()
            val file = currentFile
            if (file != null) {
                currentThumb = recorderRepo.createThumbnail(file)
                recorderRepo.exportToGallery(file)
                recorderRepo.refresh()
                scope.launch {
                    val hideSheet = settingsRepo.hideRecPostSheetFlow.first()
                    if (!hideSheet) showPostSheet(file)
                    showSavedNotification(file)
                }
            }
            removeView(controlsView)
            ensureForeground(bubbleForegroundType())
        }
    }

    private fun finishProjection() {
        runCatching { mediaRecorder?.stop() }
        runCatching { mediaRecorder?.reset() }
        runCatching { mediaRecorder?.release() }
        mediaRecorder = null
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
        releaseProjection()
    }

    private fun releaseProjection() {
        runCatching { projection?.stop() }
        projection = null
    }

    // ---------------- Bubble interactions ----------------

    private fun onBubbleTap() {
        cancelRetract()
        if (phase == Phase.RECORDING || phase == Phase.PAUSED) {
            toggleRecordingPopup()
            return
        }
        if (phase != Phase.IDLE) return
        if (retracted) {
            restoreFromRetract()
            return
        }
        openMenu()
    }

    private fun toggleRecordingPopup() {
        if (controlsView != null) {
            removeView(controlsView)
            return
        }
        openControlsPopup()
    }

    private fun openControlsPopup() {
        removeView(controlsView)
        val bp = bubbleParams ?: return
        val scale = overlayScale()
        val cw = dp((220 * scale).toInt())
        val ch = dp((300 * scale).toInt())
        val cx = bp.x + bubbleSizePx.value / 2
        val cy = bp.y + bubbleSizePx.value / 2
        val x = (if (dockEdge == EDGE_RIGHT) cx - cw + dp(8) else cx - dp(8))
            .coerceIn(-cw / 2, screenW() - cw / 2)
        val y = (cy - ch / 2).coerceIn(0, screenH() - ch)
        val dir = if (dockEdge == EDGE_RIGHT) 1 else -1
        showControls(cw, ch, x, y, dir, scale)
    }

    private fun openMenu() {
        phase = Phase.MENU
        cancelRetract()
        stopAnimator()
        val bp = bubbleParams ?: return
        val cx = bp.x + bubbleSizePx.value / 2
        val cy = bp.y + bubbleSizePx.value / 2
        removeView(bubbleView)
        val scale = overlayScale()
        val menuW = dp((230 * scale).toInt())
        val menuH = dp((250 * scale).toInt())
        val x = (cx - menuW / 2).coerceIn(-menuW / 2, screenW() - menuW / 2)
        val y = (cy - menuH / 2).coerceIn(0, screenH() - menuH)
        val dir = if (dockEdge == EDGE_RIGHT) 1 else -1
        showMenu(x, y, dir, menuW, menuH, scale)
    }

    private fun showMenu(x: Int, y: Int, dir: Int, menuW: Int, menuH: Int, scale: Float) {
        val view = ComposeView(this)
        view.attachOverlayOwners()
        view.setContent {
            DarkTheme {
                RadialMenuView(
                    dir = dir,
                    scale = scale,
                    nodes = listOf(
                        RadialNode(
                            icon = Icons.Rounded.FiberManualRecord,
                            tint = com.dark.launcher.ui.theme.RecorderRed,
                            contentDescription = "Record",
                            onClick = { onRecordClick() },
                            innerDot = true
                        ),
                        RadialNode(
                            icon = Icons.Rounded.Build,
                            tint = RecorderGreenAccent,
                            contentDescription = "Tools",
                            onClick = {
                                closeMenu()
                                openRecorderApp(this@RecordOverlayService)
                            }
                        ),
                        RadialNode(
                            icon = Icons.Rounded.Home,
                            tint = RecorderGreenAccent,
                            contentDescription = "Home",
                            onClick = {
                                closeMenu()
                                openDarkHome(this@RecordOverlayService)
                            }
                        ),
                        RadialNode(
                            icon = Icons.Rounded.Settings,
                            tint = RecorderGreenAccent,
                            contentDescription = "Settings",
                            onClick = {
                                closeMenu()
                                openDarkSettings(this@RecordOverlayService)
                            }
                        )
                    ),
                    onCenter = { closeMenu() }
                )
            }
        }
        val params = overlayParams(menuW = menuW, menuH = menuH, x = x, y = y)
        wm.addView(view, params)
        menuView = view
    }

    private fun closeMenu() {
        removeView(menuView)
        phase = Phase.IDLE
        val (x, y) = dockPositionFor(dockEdge)
        showBubble(x, y)
        scheduleRetract()
    }

    private fun onRecordClick() {
        phase = Phase.PENDING
        removeView(menuView)
        MediaProjectionPermissionActivity.launch(this)
    }

    // ---------------- Drag ----------------

    private fun onBubbleDragStart() {
        if (phase != Phase.IDLE) return
        phase = Phase.DRAGGING
        draggingState.value = true
        cancelRetract()
        stopAnimator()
        dragOverTrashFlow.value = false
        showTrash()
    }

    private fun onBubbleDrag(delta: androidx.compose.ui.geometry.Offset) {
        val p = bubbleParams ?: return
        p.x = (p.x + delta.x).toInt()
        p.y = (p.y + delta.y).toInt()
        wm.updateViewLayout(bubbleView, p)
        bubbleRight.value = (p.x + bubbleSizePx.value / 2) >= screenW() / 2
        updateTrashHighlight()
    }

    private fun updateTrashHighlight() {
        val p = bubbleParams ?: return
        val bx = p.x + bubbleSizePx.value / 2
        val by = p.y + bubbleSizePx.value / 2
        val tx = screenW() / 2
        val ty = screenH() - dp(42)
        val over = sqrt(((bx - tx) * (bx - tx) + (by - ty) * (by - ty)).toDouble()) < dp(72)
        if (over != dragOverTrash) {
            dragOverTrash = over
            dragOverTrashFlow.value = over
        }
    }

    private fun onBubbleDragEnd() {
        removeView(trashView)
        if (dragOverTrash) {
            dragOverTrash = false
            dragOverTrashFlow.value = false
            stopSelf()
            return
        }
        dragOverTrash = false
        dragOverTrashFlow.value = false
        snapToEdge()
    }

    private fun snapToEdge() {
        draggingState.value = false
        val p = bubbleParams ?: return
        val centerX = p.x + bubbleSizePx.value / 2
        dockEdge = if (centerX < screenW() / 2) EDGE_LEFT else EDGE_RIGHT
        bubbleRight.value = dockEdge == EDGE_RIGHT
        retracted = false
        phase = Phase.IDLE
        val x = if (dockEdge == EDGE_RIGHT) screenW() - bubbleSizePx.value else 0
        val maxY = (screenH() - bubbleSizePx.value).coerceAtLeast(0)
        val y = p.y.coerceIn(0, maxY)
        animateTo(x, y)
        scheduleRetract()
    }

    private fun retract() {
        if (phase != Phase.IDLE) return
        val p = bubbleParams ?: return
        val targetX = if (dockEdge == EDGE_RIGHT) {
            screenW() - bubbleSizePx.value + bubbleSizePx.value / 2
        } else {
            -(bubbleSizePx.value / 2)
        }
        retracted = true
        animateTo(targetX, p.y)
    }

    private fun restoreFromRetract() {
        retracted = false
        val p = bubbleParams ?: return
        val x = if (dockEdge == EDGE_RIGHT) screenW() - bubbleSizePx.value else 0
        animateTo(x, p.y)
        scheduleRetract()
    }

    private fun scheduleRetract() {
        cancelRetract()
        mainHandler.postDelayed(retractRunnable, 2500)
    }

    private fun cancelRetract() {
        mainHandler.removeCallbacks(retractRunnable)
    }

    private fun animateTo(targetX: Int, targetY: Int) {
        stopAnimator()
        val p = bubbleParams ?: return
        val view = bubbleView ?: return
        val startX = p.x
        val startY = p.y
        val anim = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 240
            interpolator = android.view.animation.DecelerateInterpolator()
            addUpdateListener {
                if (!view.isAttachedToWindow) return@addUpdateListener
                val t = it.animatedValue as Float
                p.x = (startX + (targetX - startX) * t).toInt()
                p.y = (startY + (targetY - startY) * t).toInt()
                wm.updateViewLayout(view, p)
            }
        }
        anim.start()
        animator = anim
    }

    private fun stopAnimator() {
        animator?.cancel()
        animator = null
    }

    // ---------------- Windows ----------------

    private fun ComposeView.attachOverlayOwners() {
        setViewTreeLifecycleOwner(overlayLifecycleOwner)
        setViewTreeViewModelStoreOwner(overlayViewModelStoreOwner)
        setViewTreeSavedStateRegistryOwner(overlaySavedStateRegistryOwner)
    }

    private fun showRecordingBubble() {
        val (x, y) = dockPositionFor(dockEdge)
        showBubble(x, y)
    }

    private fun showBubble(x: Int, y: Int) {
        removeView(bubbleView)
        val size = bubbleSizePx.value.takeIf { it > 0 } ?: dp(LauncherSettingsRepository.DEFAULT_OVERLAY_SIZE)
        val recording = phase == Phase.RECORDING || phase == Phase.PAUSED
        val view = ComposeView(this)
        view.attachOverlayOwners()
        view.setContent {
            DarkTheme {
                BubbleView(
                    sizeDp = (size / resources.displayMetrics.density).dp,
                    dockRight = bubbleRight.value,
                    dragging = draggingState.value,
                    retracted = retracted,
                    recording = recording,
                    elapsed = elapsedText,
                    paused = pausedFlow,
                    onTap = { onBubbleTap() },
                    onDragStart = { onBubbleDragStart() },
                    onDrag = { onBubbleDrag(it) },
                    onDragEnd = { onBubbleDragEnd() }
                )
            }
        }
        val params = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
        }
        wm.addView(view, params)
        bubbleView = view
        bubbleParams = params
    }

    private fun showCountdown() {
        removeView(countdownView)
        val view = ComposeView(this)
        view.attachOverlayOwners()
        view.setContent {
            DarkTheme {
                CountdownOverlayView(
                    number = countdownNumber.intValue,
                    onStartNow = { startRecording() },
                    onCancel = { cancelCountdown() }
                )
            }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
        wm.addView(view, params)
        countdownView = view
    }

    private fun showControls(cw: Int, ch: Int, x: Int, y: Int, dir: Int, scale: Float) {
        removeView(controlsView)
        val view = ComposeView(this)
        view.attachOverlayOwners()
        view.setContent {
            DarkTheme {
                RecordingControlsView(
                    dir = dir,
                    elapsed = elapsedText,
                    paused = pausedFlow,
                    scale = scale,
                    onPauseResume = {
                        togglePause()
                        removeView(controlsView)
                    },
                    onStop = { stopRecording() },
                    onBrush = { removeView(controlsView) },
                    onTools = { removeView(controlsView) }
                )
            }
        }
        val params = overlayParams(menuW = cw, menuH = ch, x = x, y = y)
        wm.addView(view, params)
        controlsView = view
    }

    private fun showTrash() {
        removeView(trashView)
        val view = ComposeView(this)
        view.attachOverlayOwners()
        view.setContent {
            DarkTheme {
                TrashTargetView(over = dragOverTrashFlow.value)
            }
        }
        val size = dp(84)
        val params = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        }
        wm.addView(view, params)
        trashView = view
    }

    private fun showPostSheet(file: File) {
        removeView(sheetView)
        val thumb = currentThumb?.absolutePath ?: recorderRepo.thumbnailFor(file.absolutePath)?.absolutePath
        val durationMs = runCatching {
            val mmr = android.media.MediaMetadataRetriever()
            try {
                mmr.setDataSource(file.absolutePath)
                mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            } finally {
                runCatching { mmr.release() }
            }
        }.getOrDefault(0L)
        val durationLabel = formatElapsed((durationMs / 1000).toInt())
        val view = ComposeView(this)
        view.attachOverlayOwners()
        view.setContent {
            DarkTheme {
                PostSheetView(
                    fileName = file.name,
                    sizeLabel = formatSize(file.length()),
                    durationLabel = durationLabel,
                    thumbnailPath = thumb,
                    onPlay = {
                        openRecorderApp(this@RecordOverlayService)
                        closePost()
                    },
                    onShare = {
                        shareRecording(this@RecordOverlayService, file)
                        closePost()
                    },
                    onEdit = {
                        openEdit(this@RecordOverlayService, file)
                        closePost()
                    },
                    onTrim = {
                        openTrim(this@RecordOverlayService, file)
                        closePost()
                    },
                    onDelete = {
                        recorderRepo.delete(Recording(file.path, file.name, file.length(), file.lastModified()))
                        closePost()
                    },
                    onClose = { closePost() }
                )
            }
        }
        val height = dp(420)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
        }
        wm.addView(view, params)
        sheetView = view
    }

    private fun closePost() {
        removeView(sheetView)
        if (stopAfterPost) {
            stopAfterPost = false
            stopSelf()
            return
        }
        phase = Phase.IDLE
        val (x, y) = dockPositionFor(dockEdge)
        showBubble(x, y)
        scheduleRetract()
    }

    private fun overlayParams(
        menuW: Int,
        menuH: Int,
        x: Int,
        y: Int
    ) = WindowManager.LayoutParams(
        menuW, menuH,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        this.x = x
        this.y = y
    }

    private fun removeView(view: ComposeView?) {
        if (view == null) return
        runCatching { wm.removeView(view) }
    }

    // ---------------- Geometry helpers ----------------

    private fun dockPositionFor(edge: Int): Pair<Int, Int> {
        val x = if (edge == EDGE_RIGHT) screenW() - bubbleSizePx.value else 0
        val y = (screenH() / 2 - bubbleSizePx.value / 2).coerceIn(0, screenH() - bubbleSizePx.value)
        return x to y
    }

    private fun screenW(): Int = wm.defaultDisplay.let { android.util.DisplayMetrics().also { d -> it.getRealMetrics(d) } }.widthPixels

    private fun screenH(): Int = wm.defaultDisplay.let { android.util.DisplayMetrics().also { d -> it.getRealMetrics(d) } }.heightPixels

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun overlayScale(): Float {
        val base = dp(56).toFloat().coerceAtLeast(1f)
        return (bubbleSizePx.value.toFloat() / base).coerceIn(0.7f, 1.5f)
    }

    private fun formatElapsed(total: Int): String {
        val m = total / 60
        val s = total % 60
        return String.format(Locale.US, "%02d:%02d", m, s)
    }

    private fun formatSize(bytes: Long): String {
        if (bytes >= 1_000_000) return String.format(Locale.US, "%.1f MB", bytes / 1_000_000f)
        if (bytes >= 1_000) return String.format(Locale.US, "%.0f KB", bytes / 1_000f)
        return "$bytes B"
    }

    // ---------------- Foreground / notifications ----------------

    private fun createChannel() {
        val channel = android.app.NotificationChannel(
            CHANNEL_ID,
            getString(R.string.recorder_channel_name),
            android.app.NotificationManager.IMPORTANCE_LOW
        ).apply { description = getString(R.string.recorder_channel_description) }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.createNotificationChannel(channel)
    }

    private fun bubbleForegroundType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }

    private fun projectionForegroundType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        } else {
            0
        }

    private fun ensureForeground(type: Int) {
        val notif = bubbleNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notif, type)
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }

    private fun bubbleNotification(): Notification {
        val content = PendingIntent.getActivity(
            this, 0,
            Intent(this, com.dark.launcher.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(com.dark.launcher.MainActivity.EXTRA_ROUTE, com.dark.launcher.ui.navigation.DarkRoutes.RECORDER)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val record = PendingIntent.getActivity(
            this, 1,
            Intent(this, MediaProjectionPermissionActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val exit = PendingIntent.getService(
            this, 2,
            Intent(this, RecordOverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_recorder_notification)
            .setContentTitle("D.A.R.K. Recorder")
            .setContentText("Floating bubble active \u00B7 swipe for controls")
            .setContentIntent(content)
            .setOngoing(true)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .addAction(0, "Record", record)
            .addAction(0, "Home", content)
            .addAction(0, "Tools", PendingIntent.getActivity(
                this, 4,
                Intent(this, com.dark.launcher.MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(com.dark.launcher.MainActivity.EXTRA_ROUTE, com.dark.launcher.ui.navigation.DarkRoutes.RECORDER)
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            ))
            .addAction(0, "Exit", exit)
            .build()
    }

    private fun recordingNotification(time: String): Notification {
        val stop = PendingIntent.getService(
            this, 3,
            Intent(this, RecordOverlayService::class.java).setAction(ACTION_STOP_RECORDING),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_recorder_notification)
            .setContentTitle("Recording")
            .setContentText("$time \u00B7 tap to open D.A.R.K.")
            .setOngoing(true)
            .setContentIntent(PendingIntent.getActivity(
                this, 0,
                Intent(this, com.dark.launcher.MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(com.dark.launcher.MainActivity.EXTRA_ROUTE, com.dark.launcher.ui.navigation.DarkRoutes.RECORDER)
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            ))
            .addAction(0, "Stop", stop)
            .build()
    }

    private fun updateRecordingNotification(time: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.notify(NOTIF_ID, recordingNotification(time))
    }

    private fun showSavedNotification(file: File) {
        val content = PendingIntent.getActivity(
            this, 0,
            Intent(this, com.dark.launcher.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(com.dark.launcher.MainActivity.EXTRA_ROUTE, com.dark.launcher.ui.navigation.DarkRoutes.RECORDER)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_recorder_notification)
            .setContentTitle("Recording saved")
            .setContentText(file.name)
            .setAutoCancel(true)
            .setContentIntent(content)
            .build()
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.notify(NOTIF_SAVED_ID, notif)
    }

    private fun toast(msg: String) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()
    }

    // ---------------- Teardown ----------------

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        countdownJob?.cancel()
        timerJob?.cancel()
        cancelRetract()
        stopAnimator()
        finishProjection()
        removeView(bubbleView)
        removeView(menuView)
        removeView(countdownView)
        removeView(controlsView)
        removeView(trashView)
        removeView(sheetView)
        runCatching { unregisterReceiver(recv) }
        overlayLifecycle.currentState = Lifecycle.State.DESTROYED
        isRunning = false
        isRecording = false
    }

    companion object {
        const val ACTION_START_OVERLAY = "com.dark.launcher.recorder.START_OVERLAY"
        const val ACTION_START_RECORDING = "com.dark.launcher.recorder.START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.dark.launcher.recorder.STOP_RECORDING"
        const val ACTION_STOP = "com.dark.launcher.recorder.STOP"
        const val ACTION_RECORD_CANCELED = "com.dark.launcher.recorder.RECORD_CANCELED"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        const val EXTRA_STOP_AFTER_POST = "stop_after_post"

        private const val CHANNEL_ID = "screen_recorder"
        private const val NOTIF_ID = 1001
        private const val NOTIF_SAVED_ID = 1002
        private const val EDGE_LEFT = 0
        private const val EDGE_RIGHT = 1

        @Volatile
        var isRunning: Boolean = false
            private set

        @Volatile
        var isRecording: Boolean = false
            private set

        fun requestRecording(context: Context) {
            MediaProjectionPermissionActivity.launch(context)
        }

        private fun dp(v: Int): Int = (v * android.content.res.Resources.getSystem().displayMetrics.density).toInt()
    }
}
