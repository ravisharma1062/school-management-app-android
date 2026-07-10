package com.school.app.fcm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.school.app.MainActivity
import com.school.app.R
import com.school.app.domain.model.Role
import com.google.firebase.messaging.FirebaseMessaging

object Notifications {
    const val CHANNEL_NOTICES = "school_notices"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_NOTICES,
                "School notices & homework",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "New notices and homework published by the school"
            },
        )
    }

    fun show(context: Context, title: String, body: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_NOTICES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        NotificationManagerCompat.from(context)
            .notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }
}

/**
 * Topic subscriptions for backend push events. Wrapped in runCatching so the
 * app still works when Firebase isn't configured (no google-services.json).
 */
object PushTopics {
    fun subscribeFor(role: Role) {
        runCatching {
            val messaging = FirebaseMessaging.getInstance()
            messaging.subscribeToTopic("notices-all")
            messaging.subscribeToTopic("notices-${role.name.lowercase()}")
        }
    }

    fun unsubscribeAll() {
        runCatching {
            val messaging = FirebaseMessaging.getInstance()
            messaging.unsubscribeFromTopic("notices-all")
            Role.entries.forEach {
                messaging.unsubscribeFromTopic("notices-${it.name.lowercase()}")
            }
        }
    }
}
