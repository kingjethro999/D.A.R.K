package com.dark.launcher.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.dark.launcher.data.repo.MediaRepository
import com.dark.launcher.data.repo.StepSensorRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DarkService : Service() {
    @Inject lateinit var steps: StepSensorRepository
    @Inject lateinit var media: MediaRepository

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        steps.start()
        media.start()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        steps.stop()
        media.stop()
        super.onDestroy()
    }
}
