package com.dark.launcher.terminal

import com.dark.launcher.data.model.AppInfo
import com.dark.launcher.data.repo.LauncherSettingsRepository
import com.dark.launcher.data.repo.ProfileMode
import com.dark.launcher.data.repo.StepsSource
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandParserTest {

    private val apps = listOf(
        AppInfo(name = "WhatsApp", packageName = "com.whatsapp", user = null),
        AppInfo(name = "Chrome", packageName = "com.android.chrome", user = null)
    )

    private fun mockSettings(): LauncherSettingsRepository = mockk(relaxed = true) {
        every { hiddenAppsFlow } returns flowOf(emptySet())
        every { vaultAppsFlow } returns flowOf(emptySet())
        every { vaultLockedFlow } returns flowOf(false)
        every { pinFlow } returns flowOf("1234")
        every { gitTokenFlow } returns flowOf("")
        every { profileModeFlow } returns flowOf(ProfileMode.NORMAL)
        every { stepsSourceFlow } returns flowOf(StepsSource.SENSOR)
    }

    private fun deps(settings: LauncherSettingsRepository = mockSettings()) = TerminalDeps(
        context = mockk(relaxed = true),
        apps = apps,
        settings = settings,
        fitness = mockk(relaxed = true),
        github = mockk(relaxed = true),
        vault = mockk(relaxed = true),
        ask = mockk(relaxed = true)
    )

    private fun runCmd(command: String, deps: TerminalDeps = deps()): List<String> = runBlocking {
        val lines = mutableListOf<String>()
        executeTerminalCommand(command, deps).collect { lines.add(it) }
        lines
    }

    @Test
    fun help_lists_new_commands() {
        val out = runCmd("help").joinToString("\n")
        assertTrue(out.contains("rec start"))
        assertTrue(out.contains("mode normal"))
        assertTrue(out.contains("alias name"))
    }

    @Test
    fun echo_returns_text() {
        assertEquals("hello world", runCmd("echo hello world").single())
    }

    @Test
    fun clear_emits_clear_token() {
        assertEquals(CLEAR_TERMINAL, runCmd("clear").single())
    }

    @Test
    fun typo_suggests_source() {
        val out = runCmd("srce whatsapp").joinToString("\n")
        assertTrue(out.contains("source"))
    }

    @Test
    fun source_unknown_app_fails() {
        val out = runCmd("source nonexistentappxyz").single()
        assertTrue(out.contains("no package found"))
    }

    @Test
    fun mode_shows_current_profile() {
        val out = runCmd("mode").joinToString("\n")
        assertTrue(out.contains("mode:"))
        assertTrue(out.contains("normal"))
    }
}
