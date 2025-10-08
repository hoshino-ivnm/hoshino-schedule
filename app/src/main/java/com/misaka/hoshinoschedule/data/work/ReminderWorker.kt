package com.misaka.hoshinoschedule.data.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.misaka.hoshinoschedule.R
import com.misaka.hoshinoschedule.ui.MainActivityLauncher

class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun doWork(): Result {
        val courseName = inputData.getString(KEY_COURSE_NAME) ?: return Result.failure()
        val subtitle = inputData.getString(KEY_SUBTITLE)
        val notificationId = inputData.getInt(
            KEY_NOTIFICATION_ID,
            (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        )

        ensureChannel()

        val contentIntent = MainActivityLauncher.launchIntent(applicationContext)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(courseName)
            .setContentText(subtitle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(notificationId, notification)
        return Result.success()
    }

    private fun ensureChannel() {
        val manager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            applicationContext.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        )
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "course_reminders"
        const val KEY_COURSE_NAME = "course_name"
        const val KEY_SUBTITLE = "subtitle"
        const val KEY_NOTIFICATION_ID = "notification_id"
    }
}
