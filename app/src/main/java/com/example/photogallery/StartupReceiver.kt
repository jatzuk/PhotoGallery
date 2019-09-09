package com.example.photogallery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class StartupReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.i(LOG_TAG, "Received broadcast intent: ${intent.action}")
        val isOn = QueryPreferences.isAlarmOn(context)
        PollService.setServiceAlarm(context, isOn)
    }

    companion object {
        private val LOG_TAG = StartupReceiver::class.java.simpleName
    }
}
