package com.example.photogallery

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class PollService : IntentService(TAG) {
    override fun onHandleIntent(intent: Intent?) {
        if (!isNetworkAvailableAndConnected()) return
        val query = QueryPreferences.getLastResultId(this)
        val lastResultId = QueryPreferences.getLastResultId(this)
        Log.i(TAG, query)
        val items =
            if (query == null) FlickrFetchr.fetchRecentPhotos(0)
            else FlickrFetchr.searchPhotos(query)
        Log.i(TAG, items.size.toString())
        if (items.isEmpty()) return
        val resultId = items[0].id
        val logInfo =
            if (resultId == lastResultId) "Got an old result: $resultId"
            else "Got a new result: $resultId"
        Log.i(TAG, logInfo)

        val pi = PendingIntent.getActivity(this, 0, PhotoGalleryActivity.newIntent(this), 0)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "channel name"
            val descriptionText = "notification for new pics"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("new_pictures_report", name, importance).apply {
                description = descriptionText
            }

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, "channel_id")
            .setTicker(resources.getString(R.string.new_pictures_title))
            .setSmallIcon(android.R.drawable.ic_menu_report_image)
            .setContentTitle(resources.getString(R.string.new_pictures_title))
            .setContentText(resources.getString(R.string.new_pictures_text))
            .setContentIntent(pi)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) { notify(100, builder.build()) }

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
