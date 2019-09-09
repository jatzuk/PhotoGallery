package com.example.photogallery

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat

class PollService : IntentService(LOG_TAG) {
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
        Log.i(LOG_TAG, logInfo)

        val pi = PendingIntent.getActivity(this, 0, PhotoGalleryActivity.newIntent(this), 0)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "channel name"
            val descriptionText = "notification for new pics"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("channel_id", name, importance).apply {
                description = descriptionText
            }

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, "channel_id")
            .setTicker(resources.getString(R.string.new_pictures_title))
            .setSmallIcon(android.R.drawable.ic_menu_report_image)
            .setContentTitle(resources.getString(R.string.new_pictures_title))
            .setContentText(resources.getString(R.string.new_pictures_text))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        showBackgroundNotification(0, notification)
        QueryPreferences.setLastResultId(this, resultId)
    }

    private fun isNetworkAvailableAndConnected(): Boolean {
        with(getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager) {
            return activeNetworkInfo != null && activeNetworkInfo.isConnected
        }
    }

    private fun showBackgroundNotification(requestCode: Int, notification: Notification) {
        val intent = Intent(ACTION_SHOW_NOTIFICATION).apply {
            putExtra(REQUEST_CODE, requestCode)
            putExtra(NOTIFICATION, notification)
        }
        sendOrderedBroadcast(intent, PERM_PRIVATE, null, null, Activity.RESULT_OK, null, null)
    }

    companion object {
        private val LOG_TAG = PollService::class.java.simpleName
        private const val POLL_INTERVAL_MS = 60_000L
        val ACTION_SHOW_NOTIFICATION =
            "${this::class.java.`package`?.name}.SHOW_NOTIFICATION"
        val PERM_PRIVATE = "${this::class.java.`package`?.name}.PRIVATE"
        const val REQUEST_CODE = "REQUEST_CODE"
        const val NOTIFICATION = "NOTIFICATION"

        private fun newIntent(context: Context) = Intent(context, PollService::class.java)

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
            QueryPreferences.setAlarmOn(context, isOn)
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
