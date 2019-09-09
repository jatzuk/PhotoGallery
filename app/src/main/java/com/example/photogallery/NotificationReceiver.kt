package com.example.photogallery

import android.app.Activity
import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.i(LOG_TAG, "received result: $resultCode")
        if (resultCode != Activity.RESULT_OK) return
        val requestCode = intent.getIntExtra(PollService.REQUEST_CODE, 0)
        val notification = intent.getParcelableExtra<Notification>(PollService.NOTIFICATION)
        NotificationManagerCompat.from(context).notify(requestCode, notification)
    }

    companion object {
        private val LOG_TAG = NotificationReceiver::class.java.simpleName
    }
}
