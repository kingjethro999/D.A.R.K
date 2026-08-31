package com.dark.launcher.service

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle

class MediaProjectionPermissionActivity : Activity() {

    private lateinit var mpm: MediaProjectionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        if (savedInstanceState == null) {
            startActivityForResult(mpm.createScreenCaptureIntent(), REQ_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                val intent = Intent(this, RecordOverlayService::class.java).apply {
                    action = RecordOverlayService.ACTION_START_RECORDING
                    putExtra(RecordOverlayService.EXTRA_RESULT_CODE, resultCode)
                    putExtra(RecordOverlayService.EXTRA_RESULT_DATA, data)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            } else {
                sendBroadcast(Intent(RecordOverlayService.ACTION_RECORD_CANCELED))
            }
        }
        finish()
    }

    companion object {
        private const val REQ_CODE = 100
        fun launch(context: Context) {
            val intent = Intent(context, MediaProjectionPermissionActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
