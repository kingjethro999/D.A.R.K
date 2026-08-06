package com.dark.launcher.terminal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

object ShellStream {

    fun run(command: String): Flow<String> = flow {
        try {
            val process = ProcessBuilder("sh", "-c", command)
                .redirectErrorStream(true)
                .start()
            val reader = process.inputStream.bufferedReader()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                emit(line ?: "")
            }
            process.waitFor()
        } catch (e: Exception) {
            emit("dark-fatal: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)
}
