package com.dark.launcher.ui.hidden

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dark.launcher.data.model.AppInfo
import com.dark.launcher.data.repo.AppRepository
import com.dark.launcher.data.repo.LauncherSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HiddenAppsViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val settings: LauncherSettingsRepository
) : ViewModel() {

    data class HiddenUiState(
        val hiddenApps: List<AppInfo> = emptyList(),
        val loading: Boolean = true
    )

    private val allApps = MutableStateFlow<List<AppInfo>>(emptyList())

    val uiState: StateFlow<HiddenUiState> = combine(
        settings.hiddenAppsFlow,
        allApps
    ) { hiddenSet, apps ->
        val byPackage = apps.associateBy { it.packageName }
        HiddenUiState(
            hiddenApps = hiddenSet.mapNotNull { pkg ->
                byPackage[pkg] ?: AppInfo(name = pkg.substringAfterLast("."), packageName = pkg)
            }.sortedBy { it.name.lowercase() },
            loading = apps.isEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HiddenUiState()
    )

    init {
        viewModelScope.launch {
            allApps.value = appRepository.loadApps()
        }
    }

    fun unhide(packageName: String) {
        viewModelScope.launch { settings.unhideApp(packageName) }
    }
}
