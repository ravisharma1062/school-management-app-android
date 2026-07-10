package com.school.app.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class SchoolMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title
            ?: message.data["title"]
            ?: getString(com.school.app.R.string.app_name)
        val body = message.notification?.body
            ?: message.data["body"]
            ?: ""
        Notifications.show(this, title, body)
    }

    override fun onNewToken(token: String) {
        // The backend currently pushes to topics only (no per-device registration
        // endpoint), so there is nothing to upload yet.
        Log.d("SchoolFCM", "New FCM token issued")
    }
}
