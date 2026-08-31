package com.dark.launcher.data.repo

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.dark.launcher.data.model.Recording
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Singleton
class RecorderRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _recordings = MutableStateFlow<List<Recording>>(emptyList())
    val recordings: StateFlow<List<Recording>> = _recordings.asStateFlow()

    init {
        refresh()
    }

    fun recordingsDir(): File = File(
        context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir,
        "recordings"
    ).apply { mkdirs() }

    fun newRecordingFile(): File {
        val name = "rec_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.mp4"
        return File(recordingsDir(), name)
    }

    fun refresh() {
        scope.launch {
            val list = recordingsDir()
                .listFiles { f -> f.isFile && f.extension.equals("mp4", true) }
                .orEmpty()
                .map { f ->
                    Recording(
                        path = f.absolutePath,
                        name = f.name,
                        sizeBytes = f.length(),
                        lastModified = f.lastModified(),
                        durationMs = durationOf(f)
                    )
                }
                .sortedByDescending { it.lastModified }
            _recordings.value = list
        }
    }

    fun delete(recording: Recording) {
        scope.launch {
            runCatching { File(recording.path).delete() }
            runCatching { thumbnailFor(recording.path)?.delete() }
            refresh()
        }
    }

    fun createThumbnail(videoFile: File): File? = runCatching {
        val thumb = File(recordingsDir(), videoFile.nameWithoutExtension + "_thumb.jpg")
        val mmr = MediaMetadataRetriever()
        try {
            mmr.setDataSource(videoFile.absolutePath)
            val frame = mmr.getFrameAtTime(0) ?: return null
            FileOutputStream(thumb).use { out ->
                frame.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            thumb
        } finally {
            runCatching { mmr.release() }
        }
    }.getOrNull()

    fun thumbnailFor(videoPath: String): File? {
        val video = File(videoPath)
        val thumb = File(recordingsDir(), video.nameWithoutExtension + "_thumb.jpg")
        return thumb.takeIf { it.exists() }
    }

    fun exportToGallery(file: File): Uri? = runCatching {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/DARK")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: return null
        resolver.openOutputStream(uri)?.use { out ->
            file.inputStream().use { it.copyTo(out) }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        uri
    }.getOrNull()

    private fun durationOf(file: File): Long = runCatching {
        val mmr = MediaMetadataRetriever()
        try {
            mmr.setDataSource(file.absolutePath)
            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } finally {
            runCatching { mmr.release() }
        }
    }.getOrDefault(0L)
}
