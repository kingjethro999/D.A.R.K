package com.dark.launcher.ui.recorder

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dark.launcher.data.model.Recording
import com.dark.launcher.data.repo.RecorderRepository
import com.dark.launcher.service.RecordOverlayService
import com.dark.launcher.util.canDrawOverlays
import com.dark.launcher.util.overlayPermissionIntent
import com.dark.launcher.util.startOverlayService
import com.dark.launcher.util.stopOverlayService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class RecorderViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recorder: RecorderRepository
) : ViewModel() {

    data class RecorderUiState(
        val recordings: List<Recording> = emptyList(),
        val overlayRunning: Boolean = false,
        val recording: Boolean = false
    )

    private val _uiState = MutableStateFlow(RecorderUiState())
    val uiState: StateFlow<RecorderUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            recorder.recordings.collect { list ->
                _uiState.update { it.copy(recordings = list) }
            }
        }
        refreshOverlayState()
    }

    fun refresh() {
        recorder.refresh()
        refreshOverlayState()
    }

    private fun refreshOverlayState() {
        _uiState.update {
            it.copy(
                overlayRunning = RecordOverlayService.isRunning,
                recording = RecordOverlayService.isRecording
            )
        }
    }

    fun overlayPermissionGranted(): Boolean = canDrawOverlays(context)

    fun overlayPermissionIntent(): Intent = overlayPermissionIntent(context)

    fun toggleOverlay(onMissingPermission: () -> Unit) {
        if (RecordOverlayService.isRunning) {
            stopOverlayService(context)
            _uiState.update { it.copy(overlayRunning = false, recording = false) }
        } else {
            if (!canDrawOverlays(context)) {
                onMissingPermission()
                return
            }
            startOverlayService(context)
            _uiState.update { it.copy(overlayRunning = true) }
        }
    }

    fun requestRecording(onMissingPermission: () -> Unit) {
        if (!canDrawOverlays(context)) {
            onMissingPermission()
            return
        }
        startOverlayService(context)
        _uiState.update { it.copy(overlayRunning = true) }
        RecordOverlayService.requestRecording(context)
    }

    fun delete(recording: Recording) {
        recorder.delete(recording)
    }

    fun refreshOverlayStateNow() {
        refreshOverlayState()
    }
}
