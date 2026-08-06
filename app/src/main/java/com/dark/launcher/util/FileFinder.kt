package com.dark.launcher.util

import android.content.Context
import android.provider.MediaStore
import java.io.File

object FileFinder {

    fun search(context: Context, query: String): List<String> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        val out = mutableListOf<String>()
        searchMediaStore(context, q, out)
        searchDir(context.filesDir, q, out)
        context.getExternalFilesDir(null)?.let { searchDir(it, q, out) }
        return out.distinct().take(50)
    }

    private fun searchMediaStore(context: Context, q: String, out: MutableList<String>) {
        runCatching {
            val uri = MediaStore.Files.getContentUri("external")
            val displayName = MediaStore.Files.FileColumns.DISPLAY_NAME
            val data = MediaStore.Files.FileColumns.DATA
            val relative = MediaStore.Files.FileColumns.RELATIVE_PATH
            val projection = arrayOf(displayName, data, relative)
            val selection = "$displayName LIKE ? OR $displayName LIKE ?"
            val args = arrayOf("%$q%", "%${q.replace(' ', '_')}%")
            context.contentResolver.query(
                uri, projection, selection, args,
                "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
            )?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(displayName)
                val dataIdx = cursor.getColumnIndex(data)
                val relIdx = cursor.getColumnIndex(relative)
                while (cursor.moveToNext()) {
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                    var path = if (dataIdx >= 0) cursor.getString(dataIdx) else null
                    if (path.isNullOrBlank()) {
                        val rel = if (relIdx >= 0) cursor.getString(relIdx) else null
                        path = if (rel.isNullOrBlank()) name else "/storage/emulated/0/$rel$name"
                    }
                    if (!path.isNullOrBlank()) out.add(path)
                }
            }
        }
    }

    private fun searchDir(dir: File?, q: String, out: MutableList<String>) {
        if (dir == null || !dir.exists()) return
        runCatching {
            dir.listFiles()?.forEach { f ->
                if (f.isDirectory) {
                    searchDir(f, q, out)
                } else if (f.name.lowercase().contains(q)) {
                    out.add(f.absolutePath)
                }
            }
        }
    }
}
