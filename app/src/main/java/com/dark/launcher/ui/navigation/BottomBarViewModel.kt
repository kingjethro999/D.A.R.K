package com.dark.launcher.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dark.launcher.data.repo.MediaRepository
import com.dark.launcher.data.repo.NowPlaying
import com.dark.launcher.data.repo.StepSensorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class BottomBarViewModel @Inject constructor(
    stepSensor: StepSensorRepository,
    media: MediaRepository
) : ViewModel() {
    val todaySteps: StateFlow<Int> = stepSensor.todaySteps
    val nowPlaying: StateFlow<NowPlaying?> = media.nowPlaying

    init {
        viewModelScope.launch { stepSensor.start() }
    }
}
