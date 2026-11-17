package net.abrahamespinosa.spam_blocker.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import net.abrahamespinosa.spam_blocker.MainActivity
import net.abrahamespinosa.spam_blocker.R

object InboxNotification {
    private const val channelId = "inbox_messages"
    const val actionOpenThread = "net.abrahamespinosa.spam_blocker.OPEN_INBOX_THREAD"
    const val extraThreadAddress = "extra_thread_address"
    const val extraRequestId = "extra_request_id"

    fun threadIntent(context: Context, address: String, requestId: Long): Intent =
        Intent(context, MainActivity::class.java).apply {
            action = actionOpenThread
            putExtra(extraThreadAddress, address)
            putExtra(extraRequestId, requestId)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

    fun show(context: Context, address: String, body: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                channelId,
                "SMS de bandeja",
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(address)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    address.hashCode(),
                    threadIntent(context, address, System.nanoTime()),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(address.hashCode(), notification)
    }
}