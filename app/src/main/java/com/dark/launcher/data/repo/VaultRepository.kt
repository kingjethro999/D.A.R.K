package com.dark.launcher.data.repo

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class VaultRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: LauncherSettingsRepository
) {
    fun vaultDirectory(): File =
        context.getExternalFilesDir("vault") ?: File(context.filesDir, "vault")

    private fun deriveKey(pin: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(pin.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(digest, "AES")
    }

    suspend fun lock(pin: String): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = vaultDirectory().apply { mkdirs() }
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, deriveKey(pin))
            val iv = cipher.iv
            var count = 0
            dir.listFiles()?.filter { it.isFile }?.forEach { file ->
                val plain = file.readBytes()
                val encrypted = cipher.doFinal(plain)
                file.writeBytes(iv + encrypted)
                count++
            }
            settings.setVaultLocked(true)
            count
        }
    }

    suspend fun unlock(pin: String): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = vaultDirectory().apply { mkdirs() }
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            var count = 0
            dir.listFiles()?.filter { it.isFile }?.forEach { file ->
                val bytes = file.readBytes()
                if (bytes.size > 16) {
                    val iv = bytes.copyOfRange(0, 16)
                    val encrypted = bytes.copyOfRange(16, bytes.size)
                    cipher.init(Cipher.DECRYPT_MODE, deriveKey(pin), IvParameterSpec(iv))
                    file.writeBytes(cipher.doFinal(encrypted))
                    count++
                }
            }
            settings.setVaultLocked(false)
            count
        }
    }
}
