package com.dark.launcher.data.repo

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import com.dark.launcher.service.MediaNotificationListener
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class NowPlaying(
    val title: String,
    val artist: String?,
    val appName: String?
)

@Singleton
class MediaRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val sessionManager by lazy {
        runCatching {
            context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        }.getOrNull()
    }

    private val notificationManager by lazy {
        runCatching {
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        }.getOrNull()
    }

    private val listenerComponent by lazy {
        ComponentName(context, MediaNotificationListener::class.java)
    }

    private val controllers = HashMap<String, MediaController>()
    private val appLabels = HashMap<String, String>()

    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying.asStateFlow()

    fun hasNotificationAccess(): Boolean {
        val enabled = runCatching {
            android.provider.Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
        }.getOrNull() ?: return false
        val flat = listenerComponent.flattenToString()
        return enabled.split(':').any { it.equals(flat, ignoreCase = true) }
    }

    private val listener = MediaSessionManager.OnActiveSessionsChangedListener { sessions ->
        controllers.clear()
        (sessions ?: emptyList()).forEach { controllers[it.packageName] = it }
        updateNowPlaying()
    }

    fun start() {
        val manager = sessionManager ?: return
        runCatching {
            manager.getActiveSessions(listenerComponent).forEach { controllers[it.packageName] = it }
            manager.addOnActiveSessionsChangedListener(listener, listenerComponent, mainHandler)
        }
        scope.launch {
            while (isActive) {
                delay(4000)
                updateNowPlaying()
            }
        }
    }

    fun stop() {
        sessionManager?.let {
            runCatching { it.removeOnActiveSessionsChangedListener(listener) }
        }
        controllers.clear()
        _nowPlaying.value = null
    }

    private fun updateNowPlaying() {
        var playing: NowPlaying? = null
        var fallback: NowPlaying? = null
        controllers.values.forEach { controller ->
            val state = controller.playbackState?.state
            val playingNow = state == PlaybackState.STATE_PLAYING ||
                state == PlaybackState.STATE_BUFFERING
            val metadata = controller.metadata
            val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: return@forEach
            if (title.isBlank()) return@forEach
            val app = labelFor(controller.packageName)
            val candidate = NowPlaying(
                title = title,
                artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST),
                appName = app
            )
            if (playingNow) {
                playing = candidate
                return@forEach
            }
            if (fallback == null) fallback = candidate
        }
        val result = playing ?: fallback?.takeIf { controllers.size > 0 } ?: null
        _nowPlaying.value = result
    }

    private fun labelFor(pkg: String): String? {
        appLabels[pkg]?.let { return it }
        val label = runCatching {
            context.packageManager.getApplicationLabel(
                context.packageManager.getApplicationInfo(pkg, 0)
            ).toString()
        }.getOrNull()
        if (label != null) appLabels[pkg] = label
        return label
    }
}
