package com.school.app

import android.app.Application
import com.school.app.data.auth.TokenManager
import com.school.app.data.sync.AttendanceSyncManager
import com.school.app.fcm.Notifications
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.runBlocking
import org.osmdroid.config.Configuration
import javax.inject.Inject

@HiltAndroidApp
class SchoolApp : Application() {

    @Inject
    lateinit var tokenManager: TokenManager

    @Inject
    lateinit var attendanceSyncManager: AttendanceSyncManager

    override fun onCreate() {
        super.onCreate()
        // Small one-time preferences read so the auth interceptor has tokens
        // available before the first request.
        runBlocking { tokenManager.initialize() }
        Notifications.createChannels(this)
        attendanceSyncManager.start()

        // osmdroid needs a user-agent (OSM tile servers reject the default) and an app-private
        // cache dir so it doesn't need WRITE_EXTERNAL_STORAGE on modern Android.
        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().osmdroidBasePath = getExternalFilesDir("osmdroid") ?: filesDir
    }
}
