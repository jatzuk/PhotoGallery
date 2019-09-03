package com.example.photogallery

import android.app.AlarmManager
import android.app.IntentService
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.SystemClock
import android.util.Log

class PollService : IntentService(TAG) {
    override fun onHandleIntent(intent: Intent?) {
        if (!isNetworkAvailableAndConnected()) return
        val query = QueryPreferences.getLastResultId(this)
        val lastResultId = QueryPreferences.getLastResultId(this)
        val items =
            if (query == null) FlickrFetchr.fetchRecentPhotos(0)
            else FlickrFetchr.searchPhotos(query)
        if (items.isEmpty()) return
        val resultId = items[0].id
        val logInfo =
            if (resultId == lastResultId) "Got an old result: $resultId"
            else "Got a new result: $resultId"
        Log.i(TAG, logInfo)
        QueryPreferences.setLastResultId(this, resultId)
    }

    private fun isNetworkAvailableAndConnected(): Boolean {
        with(getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager) {
            return activeNetworkInfo != null && activeNetworkInfo.isConnected
        }
    }

    companion object {
        private val TAG = PollService::class.java.simpleName
        private const val POLL_INTERVAL_MS = 60_000L

        fun newIntent(context: Context) = Intent(context, PollService::class.java)

        fun setServiceAlarm(context: Context, isOn: Boolean) {
            val pendingIntent = PendingIntent.getService(context, 0, newIntent(context), 0)
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (isOn) {
                am.setRepeating(
                    AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime(),
                    POLL_INTERVAL_MS,
                    pendingIntent
                )
            } else {
                am.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }

        fun isServiceAlarmOn(context: Context) =
            PendingIntent.getService(
                context,
                0,
                newIntent(context),
                PendingIntent.FLAG_NO_CREATE
            ) != null
    }
}
