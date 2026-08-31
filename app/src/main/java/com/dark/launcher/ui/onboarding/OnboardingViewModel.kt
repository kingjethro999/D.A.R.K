package com.dark.launcher.ui.onboarding

import android.content.Intent
import android.Manifest
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dark.launcher.data.repo.LauncherSettingsRepository
import com.dark.launcher.data.repo.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
    private val settings: LauncherSettingsRepository,
    private val media: MediaRepository
) : ViewModel() {

    data class OnboardingUiState(
        val pinChanged: Boolean = false,
        val overlayGranted: Boolean = false,
        val notificationAccess: Boolean = false,
        val stepsGranted: Boolean = false,
        val showPinSetup: Boolean = false
    )

    private val showPin = MutableStateFlow(false)
    private val refresh = MutableStateFlow(0)

    val uiState: StateFlow<OnboardingUiState> = combine(
        settings.pinChangedFlow,
        showPin,
        refresh
    ) { pinChanged, pinDialog, _ ->
        OnboardingUiState(
            pinChanged = pinChanged,
            overlayGranted = runCatching { Settings.canDrawOverlays(context) }.getOrDefault(false),
            notificationAccess = runCatching { media.hasNotificationAccess() }.getOrDefault(false),
            stepsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                runCatching {
                    context.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED
                }.getOrDefault(false)
            } else true,
            showPinSetup = pinDialog
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OnboardingUiState())

    fun refreshState() {
        refresh.value++
    }

    fun requestPinSetup() {
        showPin.value = true
    }

    fun skipPinSetup() {
        showPin.value = false
    }

    fun savePin(pin: String) {
        viewModelScope.launch {
            settings.setPin(pin)
            showPin.value = false
        }
    }

    fun openNotificationSettings() {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun complete(onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { settings.setOnboardingComplete(true) }
            onDone()
        }
    }

    suspend fun shouldSkip(): Boolean {
        val pinChanged = runCatching { settings.pinChangedFlow.first() }.getOrDefault(false)
        val overlay = runCatching { Settings.canDrawOverlays(context) }.getOrDefault(false)
        val notif = runCatching { media.hasNotificationAccess() }.getOrDefault(false)
        val steps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                context.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            }.getOrDefault(false)
        } else true
        return pinChanged && overlay && notif && steps
    }
}
