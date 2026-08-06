package com.dark.launcher.ui.terminal

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dark.launcher.data.model.AppInfo
import com.dark.launcher.data.repo.AppRepository
import com.dark.launcher.data.repo.AskRepository
import com.dark.launcher.data.repo.FitnessRepository
import com.dark.launcher.data.repo.GitHubRepository
import com.dark.launcher.data.repo.LauncherSettingsRepository
import com.dark.launcher.data.repo.VaultRepository
import com.dark.launcher.terminal.CLEAR_TERMINAL
import com.dark.launcher.terminal.TerminalDeps
import com.dark.launcher.terminal.executeTerminalCommand
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val settings: LauncherSettingsRepository,
    private val fitness: FitnessRepository,
    private val github: GitHubRepository,
    private val vault: VaultRepository,
    private val ask: AskRepository
) : ViewModel() {

    data class TerminalUiState(
        val history: List<String> = emptyList(),
        val input: String = "",
        val booting: Boolean = true,
        val apps: List<AppInfo> = emptyList()
    )

    private val _state = MutableStateFlow(TerminalUiState())
    val state: StateFlow<TerminalUiState> = _state.asStateFlow()

    var cameraPermissionRequest: () -> Unit = {}

    var pinPromptRequest: suspend () -> Boolean = { false }

    init {
        viewModelScope.launch {
            _state.update { it.copy(apps = appRepository.loadApps()) }
        }
    }

    fun onBootComplete() {
        _state.update { it.copy(booting = false) }
    }

    fun onInputChange(value: String) {
        _state.update { it.copy(input = value) }
    }

    fun submit(context: Context) {
        val command = _state.value.input
        if (command.isBlank()) return

        _state.update {
            it.copy(
                history = it.history + "root@dark:~# $command",
                input = ""
            )
        }

        val deps = TerminalDeps(
            context = context,
            apps = _state.value.apps,
            settings = settings,
            fitness = fitness,
            github = github,
            vault = vault,
            ask = ask,
            onCameraPermissionRequest = { cameraPermissionRequest() },
            onPinVerify = { pinPromptRequest() }
        )

        viewModelScope.launch {
            executeTerminalCommand(command, deps).collect { line ->
                _state.update { current ->
                    when (line) {
                        CLEAR_TERMINAL -> current.copy(history = emptyList())
                        else -> current.copy(history = current.history + line)
                    }
                }
            }
        }
    }

    fun clearHistory() {
        _state.update { it.copy(history = emptyList()) }
    }

    suspend fun verifyPin(input: String): Boolean = settings.verifyPin(input)
}
